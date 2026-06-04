import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function ResetPassword() {
  const { token } = useParams();
  const navigate = useNavigate();
  const { login } = useAuth();
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleReset = async () => {
    if (!password || password.length < 6) return setError('Password must be at least 6 characters');
    if (password !== confirm) return setError('Passwords do not match');
    setLoading(true); setError('');
    try {
      const res = await api.post(`/auth/reset-password/${token}`, { password });
      alert('Password reset successfully! You are now logged in.');
      navigate('/');
    } catch (err) { setError(err.response?.data?.message || 'Reset failed. Link may have expired.'); }
    finally { setLoading(false); }
  };

  return (
    <div style={{ minHeight:'80vh', display:'flex', alignItems:'center', justifyContent:'center', padding:24 }}>
      <div style={{ width:'100%', maxWidth:420 }}>
        <div style={{ textAlign:'center', marginBottom:32 }}>
          <div style={{ width:56, height:56, background:'var(--red)', borderRadius:14, display:'flex', alignItems:'center', justifyContent:'center', fontSize:26, margin:'0 auto 16px' }}>🔐</div>
          <h1 style={{ fontSize:24, marginBottom:8 }}>Set new password</h1>
          <p style={{ color:'var(--text-muted)', fontSize:14 }}>Choose a strong password for your account</p>
        </div>
        <div className="card">
          {error && <div style={{ background:'#fff5f5', border:'1px solid #fecaca', borderRadius:8, padding:'10px 14px', marginBottom:16, color:'var(--red)', fontSize:14 }}>{error}</div>}
          <div className="form-group">
            <label>New password</label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Minimum 6 characters" />
          </div>
          <div className="form-group">
            <label>Confirm password</label>
            <input type="password" value={confirm} onChange={e => setConfirm(e.target.value)} placeholder="Repeat your password"
              onKeyDown={e => e.key==='Enter' && handleReset()} />
          </div>
          <button className="btn btn-primary" onClick={handleReset} disabled={loading} style={{ width:'100%', justifyContent:'center', marginTop:8 }}>
            {loading ? <><div className="spinner" style={{ width:16, height:16 }} /> Resetting...</> : 'Reset Password'}
          </button>
        </div>
      </div>
    </div>
  );
}
