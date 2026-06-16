package com.bloodconnect.controller;

import com.bloodconnect.model.User;
import com.bloodconnect.repository.DonorProfileRepository;
import com.bloodconnect.repository.UserRepository;
import com.bloodconnect.security.JwtUtil;
import com.bloodconnect.service.EmailService;
import com.bloodconnect.util.ApiException;
import com.bloodconnect.util.Presenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for AuthController - no Spring context, no HTTP.
 * We drive the controller methods directly with mocked collaborators and
 * assert on the returned maps and the ApiExceptions thrown on bad input.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock UserRepository userRepo;
    @Mock DonorProfileRepository donorRepo;
    @Mock PasswordEncoder encoder;
    @Mock JwtUtil jwt;
    @Mock Presenter present;
    @Mock EmailService email;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                userRepo, donorRepo, encoder, jwt, present, email,
                "admin-secret", "http://localhost:5173");
    }

    private Map<String, Object> body(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i].toString(), kv[i + 1]);
        return m;
    }

    @Test
    @DisplayName("register: a new donor gets a JWT and a donor profile is created")
    void registerNewDonor() {
        when(userRepo.existsByEmailIgnoreCase("asha@example.com")).thenReturn(false);
        when(encoder.encode("secret1")).thenReturn("hashed-pw");
        when(userRepo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });
        when(jwt.sign("user-1")).thenReturn("jwt-token");
        when(present.userAuth(any(User.class))).thenReturn(Map.of("id", "user-1"));

        Map<String, Object> res = controller.register(body(
                "name", "Asha", "email", "asha@example.com", "password", "secret1",
                "role", "donor", "bloodType", "O+", "city", "Bengaluru"));

        assertThat(res.get("token")).isEqualTo("jwt-token");
        assertThat(res).containsKey("user");
        verify(donorRepo).save(any()); // donor role + bloodType => profile created
    }

    @Test
    @DisplayName("register: duplicate email is rejected with 409")
    void registerDuplicateEmail() {
        when(userRepo.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> controller.register(body(
                "name", "Dup", "email", "dup@example.com", "password", "secret1")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(409);

        verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("login: wrong/unknown credentials are rejected with 401")
    void loginInvalidCredentials() {
        when(userRepo.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.login(body(
                "email", "ghost@example.com", "password", "whatever")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(401);
    }

    @Test
    @DisplayName("login: correct credentials return a token")
    void loginSuccess() {
        User u = new User();
        u.setId("user-9");
        u.setEmail("req@example.com");
        u.setPassword("hashed-pw");
        u.setRole("requester");

        when(userRepo.findByEmailIgnoreCase("req@example.com")).thenReturn(Optional.of(u));
        when(encoder.matches("secret1", "hashed-pw")).thenReturn(true);
        when(jwt.sign("user-9")).thenReturn("tok-9");
        when(present.userAuth(u)).thenReturn(Map.of("id", "user-9"));

        Map<String, Object> res = controller.login(body(
                "email", "req@example.com", "password", "secret1"));

        assertThat(res.get("token")).isEqualTo("tok-9");
        assertThat(res.get("donorProfile")).isNull(); // requester has no donor profile
        verify(donorRepo, never()).findByUserId(anyString());
    }
}
