import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const [form, setForm] = useState({ email:'', password:'' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleLogin = async () => {
    if (!form.email || !form.password) return setError('Please fill all fields');
    setLoading(true); setError('');
    try {
      const res = await api.post('/auth/login', form);
      login(res.data);
      navigate('/');
    } catch (err) { setError(err.response?.data?.message || 'Login failed'); }
    finally { setLoading(false); }
  };

  return (
    <div style={{ minHeight:'80vh', display:'flex', alignItems:'center', justifyContent:'center', padding:24 }}>
      <div style={{ width:'100%', maxWidth:420 }}>
        <div style={{ textAlign:'center', marginBottom:32 }}>
          <div style={{ width:56, height:56, background:'var(--red)', borderRadius:14, display:'flex', alignItems:'center', justifyContent:'center', fontSize:26, margin:'0 auto 16px' }}>🩸</div>
          <h1 style={{ fontSize:26, marginBottom:6 }}>Welcome back</h1>
          <p style={{ color:'var(--text-muted)', fontSize:14 }}>Sign in to your BloodConnect account</p>
        </div>
        <div className="card">
          {error && <div style={{ background:'#fff5f5', border:'1px solid #fecaca', borderRadius:8, padding:'10px 14px', marginBottom:16, color:'var(--red)', fontSize:14 }}>{error}</div>}
          <div className="form-group">
            <label>Email</label>
            <input type="email" value={form.email} onChange={e=>setForm({...form,email:e.target.value})} placeholder="your@email.com" autoFocus />
          </div>
          <div className="form-group">
            <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
              <label>Password</label>
              <Link to="/forgot-password" style={{ fontSize:12, color:'var(--red)' }}>Forgot password?</Link>
            </div>
            <input type="password" value={form.password} onChange={e=>setForm({...form,password:e.target.value})} placeholder="Your password" onKeyDown={e=>e.key==='Enter'&&handleLogin()} />
          </div>
          <button className="btn btn-primary" onClick={handleLogin} disabled={loading} style={{ width:'100%', justifyContent:'center', marginTop:8 }}>
            {loading ? <><div className="spinner" style={{ width:16, height:16 }} /> Signing in...</> : 'Sign In'}
          </button>
          <p style={{ textAlign:'center', marginTop:18, fontSize:14, color:'var(--text-muted)' }}>
            Don't have an account? <Link to="/register" style={{ color:'var(--red)', fontWeight:600 }}>Join as Donor</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
