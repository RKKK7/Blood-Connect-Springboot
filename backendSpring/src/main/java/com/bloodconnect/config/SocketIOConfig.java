package com.bloodconnect.config;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class SocketIOConfig {

    @Value("${app.socketio.host}")
    private String host;

    @Value("${app.socketio.port}")
    private int port;

    @Bean
    public SocketIOServer socketIOServer() {
        Configuration config = new Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setOrigin(null);                 // allow all origins (dev)
        config.setAllowCustomRequests(true);
        config.setPingInterval(25000);
        config.setPingTimeout(60000);

        SocketIOServer server = new SocketIOServer(config);

        // Mirrors index.js io.on("connection", ...)
        server.addEventListener("join", String.class, (client, userId, ack) -> {
            if (userId != null) client.joinRoom(userId);
        });
        server.addEventListener("join_chat", String.class, (client, donationId, ack) -> {
            if (donationId != null) client.joinRoom("chat_" + donationId);
        });
        server.addEventListener("leave_chat", String.class, (client, donationId, ack) -> {
            if (donationId != null) client.leaveRoom("chat_" + donationId);
        });

        return server;
    }
}
