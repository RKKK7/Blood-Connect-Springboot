import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../services/api";
import { useAuth } from "../context/AuthContext";

const BLOOD_TYPES = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

const ROLES = [
  {
    value: "donor",
    label: "🩸 Donate Blood",
    desc: "Register as a blood donor",
  },
  {
    value: "requester",
    label: "🏥 Request Blood",
    desc: "Post blood requests for patients",
  },
  { value: "admin", label: "🛡️ Admin", desc: "Platform administrator" },
];

export default function Register() {
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    phone: "",
    role: "donor",
    bloodType: "O+",
    city: "",
    adminSecret: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const res = await api.post("/auth/register", form);
      login(res.data);
      navigate(res.data.user.role === "admin" ? "/admin" : "/");
    } catch (err) {
      setError(err.response?.data?.message || "Registration failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        background: "linear-gradient(135deg, #fff5f5 0%, #fff 60%)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: 24,
      }}
    >
      <div style={{ width: "100%", maxWidth: 480 }}>
        <div style={{ textAlign: "center", marginBottom: 32 }}>
          <div style={{ fontSize: 40, marginBottom: 12 }}>🩸</div>
          <h1 style={{ fontSize: 28, marginBottom: 6 }}>Join BloodConnect</h1>
          <p style={{ color: "var(--text-muted)", fontSize: 14 }}>
            Create your account to get started
          </p>
        </div>

        <div className="card">
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>Account type</label>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(3, 1fr)",
                  gap: 8,
                }}
              >
                {ROLES.map(({ value, label, desc }) => (
                  <button
                    key={value}
                    type="button"
                    onClick={() =>
                      setForm({ ...form, role: value, adminSecret: "" })
                    }
                    style={{
                      padding: "12px 8px",
                      borderRadius: 10,
                      border: `2px solid ${form.role === value ? "var(--red)" : "var(--border)"}`,
                      background:
                        form.role === value ? "var(--red-dim)" : "var(--bg)",
                      color:
                        form.role === value
                          ? "var(--red)"
                          : "var(--text-muted)",
                      cursor: "pointer",
                      textAlign: "center",
                      transition: "all 0.15s",
                    }}
                  >
                    <div style={{ fontSize: 18, marginBottom: 4 }}>
                      {label.split(" ")[0]}
                    </div>
                    <div style={{ fontSize: 12, fontWeight: 600 }}>
                      {label.split(" ").slice(1).join(" ")}
                    </div>
                    <div
                      style={{
                        fontSize: 11,
                        color: "var(--text-dim)",
                        marginTop: 3,
                      }}
                    >
                      {desc}
                    </div>
                  </button>
                ))}
              </div>
            </div>

            <div className="grid-2">
              <div className="form-group">
                <label>Full name</label>
                <input
                  placeholder="Your name"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label>Phone</label>
                <input
                  placeholder="10-digit number"
                  value={form.phone}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })}
                />
              </div>
            </div>

            <div className="form-group">
              <label>Email</label>
              <input
                type="email"
                placeholder="you@example.com"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label>Password</label>
              <input
                type="password"
                placeholder="Min 6 characters"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                required
              />
            </div>

            {form.role === "donor" && (
              <div className="grid-2">
                <div className="form-group">
                  <label>Blood type</label>
                  <select
                    value={form.bloodType}
                    onChange={(e) =>
                      setForm({ ...form, bloodType: e.target.value })
                    }
                  >
                    {BLOOD_TYPES.map((t) => (
                      <option key={t}>{t}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>City</label>
                  <input
                    placeholder="Your city"
                    value={form.city}
                    onChange={(e) => setForm({ ...form, city: e.target.value })}
                  />
                </div>
              </div>
            )}

            {form.role === "admin" && (
              <div className="form-group">
                <label>Admin secret code</label>
                <input
                  type="password"
                  placeholder="Enter admin secret code"
                  value={form.adminSecret}
                  onChange={(e) =>
                    setForm({ ...form, adminSecret: e.target.value })
                  }
                  required
                />
                <p className="form-hint">
                  The secret code is set in the server .env as ADMIN_SECRET
                </p>
              </div>
            )}

            {error && (
              <p className="form-error" style={{ marginBottom: 12 }}>
                {error}
              </p>
            )}
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
              style={{
                width: "100%",
                justifyContent: "center",
                marginTop: 4,
                padding: "13px",
              }}
            >
              {loading ? (
                <>
                  <div className="spinner" style={{ width: 16, height: 16 }} />{" "}
                  Creating account...
                </>
              ) : (
                "Create account"
              )}
            </button>
          </form>
        </div>

        <p
          style={{
            textAlign: "center",
            marginTop: 20,
            fontSize: 14,
            color: "var(--text-muted)",
          }}
        >
          Already have an account?{" "}
          <Link to="/login" style={{ color: "var(--red)", fontWeight: 600 }}>
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
