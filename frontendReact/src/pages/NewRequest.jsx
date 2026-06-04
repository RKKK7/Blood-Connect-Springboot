import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const BLOOD_TYPES = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export default function NewRequest() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ patientName: '', bloodType: 'O+', units: 1, hospital: '', city: '', contactPhone: user?.phone || '', notes: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true); setError('');
    try {
      const res = await api.post('/requests', form);
      setResult(res.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to post request');
    } finally { setLoading(false); }
  };

  if (result) return (
    <div className="page-container" style={{ maxWidth: 600 }}>
      <div className="card fade-in" style={{ textAlign: 'center', padding: 40 }}>
        <div style={{ fontSize: 48, marginBottom: 16 }}>✅</div>
        <h2 style={{ fontSize: 24, marginBottom: 8 }}>Request posted!</h2>
        <p style={{ color: 'var(--text-muted)', marginBottom: 20 }}>{result.aiResult.summary}</p>
        <div style={{ background: 'var(--bg-secondary)', borderRadius: 10, padding: '16px', marginBottom: 24, textAlign: 'left' }}>
          <p style={{ fontSize: 12, color: 'var(--red)', fontWeight: 600, marginBottom: 8 }}>🤖 AI URGENCY ASSESSMENT</p>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 8 }}>
            <span className={`badge badge-${result.aiResult.urgency}`} style={{ textTransform: 'capitalize' }}>{result.aiResult.urgency}</span>
          </div>
          <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>{result.aiResult.reason}</p>
        </div>
        <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 20 }}>
          Nearby compatible donors have been notified automatically.
        </p>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'center' }}>
          <button className="btn btn-primary" onClick={() => navigate(`/requests/${result.request._id}`)}>View Request</button>
          <button className="btn btn-ghost" onClick={() => navigate('/')}>Go Home</button>
        </div>
      </div>
    </div>
  );

  return (
    <div className="page-container" style={{ maxWidth: 600 }}>
      <h1 className="section-title" style={{ marginBottom: 6 }}>Post a Blood Request</h1>
      <p className="section-subtitle">AI will classify urgency and notify nearby compatible donors instantly.</p>

      <div className="card">
        <form onSubmit={handleSubmit}>
          <div className="grid-2">
            <div className="form-group">
              <label>Patient name</label>
              <input placeholder="Full name" value={form.patientName} onChange={e => setForm({ ...form, patientName: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Contact phone</label>
              <input placeholder="10-digit number" value={form.contactPhone} onChange={e => setForm({ ...form, contactPhone: e.target.value })} required />
            </div>
          </div>

          <div className="grid-2">
            <div className="form-group">
              <label>Blood type needed</label>
              <select value={form.bloodType} onChange={e => setForm({ ...form, bloodType: e.target.value })}>
                {BLOOD_TYPES.map(t => <option key={t}>{t}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label>Units required</label>
              <input type="number" min="1" max="10" value={form.units} onChange={e => setForm({ ...form, units: parseInt(e.target.value) })} required />
            </div>
          </div>

          <div className="grid-2">
            <div className="form-group">
              <label>Hospital name</label>
              <input placeholder="Hospital / clinic name" value={form.hospital} onChange={e => setForm({ ...form, hospital: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>City</label>
              <input placeholder="City" value={form.city} onChange={e => setForm({ ...form, city: e.target.value })} required />
            </div>
          </div>

          <div className="form-group">
            <label>Additional notes (optional)</label>
            <textarea placeholder="Any additional information about the patient or request..." value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} rows={3} />
            <p className="form-hint">The AI will use this to classify urgency and notify donors.</p>
          </div>

          {error && <p className="form-error" style={{ marginBottom: 12 }}>{error}</p>}

          <div style={{ background: 'var(--red-dim)', border: '1px solid var(--border)', borderRadius: 8, padding: '12px 16px', marginBottom: 16 }}>
            <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>
              🤖 <strong>AI-powered:</strong> Groq AI will classify urgency, generate a summary, and automatically notify compatible donors near your location.
            </p>
          </div>

          <button type="submit" className="btn btn-primary" disabled={loading} style={{ width: '100%', justifyContent: 'center', padding: '13px' }}>
            {loading ? <><div className="spinner" style={{ width: 16, height: 16 }} /> Analyzing & posting...</> : '🩸 Post Request'}
          </button>
        </form>
      </div>
    </div>
  );
}
