import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async () => {
    if (!email) return setError('Please enter your email');
    setLoading(true); setError('');
    try {
      await api.post('/auth/forgot-password', { email });
      setSent(true);
    } catch (err) { setError(err.response?.data?.message || 'Something went wrong'); }
    finally { setLoading(false); }
  };

  return (
    <div style={{ minHeight:'80vh', display:'flex', alignItems:'center', justifyContent:'center', padding:'24px' }}>
      <div style={{ width:'100%', maxWidth:420 }}>
        <div style={{ textAlign:'center', marginBottom:32 }}>
          <div style={{ width:56, height:56, background:'var(--red)', borderRadius:14, display:'flex', alignItems:'center', justifyContent:'center', fontSize:26, margin:'0 auto 16px' }}>🩸</div>
          <h1 style={{ fontSize:24, marginBottom:8 }}>Forgot password?</h1>
          <p style={{ color:'var(--text-muted)', fontSize:14 }}>We'll send a reset link to your email</p>
        </div>

        {sent ? (
          <div className="card" style={{ textAlign:'center' }}>
            <div style={{ fontSize:48, marginBottom:16 }}>📬</div>
            <h2 style={{ fontSize:20, marginBottom:8 }}>Check your inbox</h2>
            <p style={{ color:'var(--text-muted)', fontSize:14, lineHeight:1.6, marginBottom:20 }}>
              If an account exists for <strong>{email}</strong>, you'll receive a reset link shortly. Check your spam folder too.
            </p>
            <Link to="/login" className="btn btn-primary" style={{ width:'100%', justifyContent:'center' }}>
              Back to login
            </Link>
          </div>
        ) : (
          <div className="card">
            {error && <div style={{ background:'#fff5f5', border:'1px solid #fecaca', borderRadius:8, padding:'10px 14px', marginBottom:16, color:'var(--red)', fontSize:14 }}>{error}</div>}
            <div className="form-group">
              <label>Email address</label>
              <input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="your@email.com"
                onKeyDown={e => e.key === 'Enter' && handleSubmit()} autoFocus />
            </div>
            <button className="btn btn-primary" onClick={handleSubmit} disabled={loading} style={{ width:'100%', justifyContent:'center', marginTop:8 }}>
              {loading ? <><div className="spinner" style={{ width:16, height:16 }} /> Sending...</> : 'Send Reset Link'}
            </button>
            <p style={{ textAlign:'center', marginTop:16, fontSize:14, color:'var(--text-muted)' }}>
              <Link to="/login" style={{ color:'var(--red)' }}>← Back to login</Link>
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
