import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api, { socket } from '../services/api';
import RequestCard from '../components/request/RequestCard';
import { useAuth } from '../context/AuthContext';

const BLOOD_TYPES = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export default function Home() {
  const { user } = useAuth();
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState({ bloodType: '', urgency: '', city: '' });
  const [stats, setStats] = useState({ open: 0, critical: 0, donors: 0 });
  const [liveAlert, setLiveAlert] = useState(null);

  const fetchRequests = async () => {
    try {
      const params = { status: 'open', limit: 12 };
      if (filter.bloodType) params.bloodType = filter.bloodType;
      if (filter.urgency) params.urgency = filter.urgency;
      if (filter.city) params.city = filter.city;
      const res = await api.get('/requests', { params });
      setRequests(res.data.requests);
    } catch (err) { console.error(err); }
    finally { setLoading(false); }
  };

  const fetchStats = async () => {
    try {
      const [openRes, donorRes] = await Promise.all([
        api.get('/requests', { params: { status: 'open', limit: 1 } }),
        api.get('/donors/nearby'),
      ]);
      setStats({ open: openRes.data.total, critical: openRes.data.requests?.filter(r => r.urgency === 'critical').length || 0, donors: donorRes.data.length });
    } catch {}
  };

  useEffect(() => {
    fetchRequests();
    fetchStats();
    socket.connect();
    socket.on('new_request', (req) => {
      setRequests(prev => [req, ...prev.slice(0, 11)]);
      setLiveAlert(req);
      setTimeout(() => setLiveAlert(null), 5000);
    });
    return () => { socket.off('new_request'); socket.disconnect(); };
  }, []);

  useEffect(() => { fetchRequests(); }, [filter]);

  return (
    <div>
      {/* Hero */}
      <div style={{ background: 'linear-gradient(135deg, #fff5f5 0%, #fff 60%)', borderBottom: '1.5px solid var(--border)', padding: '60px 24px 50px' }}>
        <div style={{ maxWidth: 1200, margin: '0 auto' }}>
          <div style={{ maxWidth: 580 }}>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: 8, background: 'var(--red-dim)', border: '1px solid var(--border)', borderRadius: 100, padding: '5px 14px', marginBottom: 20 }}>
              <span className="urgency-dot critical" />
              <span style={{ fontSize: 13, color: 'var(--red)', fontWeight: 600 }}>{stats.open} urgent requests active right now</span>
            </div>
            <h1 style={{ fontSize: 52, marginBottom: 16, lineHeight: 1.1 }}>
              Every drop<br /><span style={{ color: 'var(--red)' }}>saves a life.</span>
            </h1>
            <p style={{ fontSize: 17, color: 'var(--text-muted)', marginBottom: 32, lineHeight: 1.7 }}>
              AI-powered blood donation platform connecting donors with patients in real time. One donation can save up to 3 lives.
            </p>
            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
              {!user ? (
                <>
                  <Link to="/register" className="btn btn-primary" style={{ fontSize: 15, padding: '13px 28px' }}>
                    🩸 Become a Donor
                  </Link>
                  <Link to="/requests/new" className="btn btn-ghost" style={{ fontSize: 15, padding: '13px 28px' }}>
                    Request Blood
                  </Link>
                </>
              ) : (
                <>
                  <Link to="/requests/new" className="btn btn-primary" style={{ fontSize: 15, padding: '13px 28px' }}>
                    + Post a Request
                  </Link>
                  <Link to="/requests" className="btn btn-ghost" style={{ fontSize: 15, padding: '13px 28px' }}>
                    Browse All Requests
                  </Link>
                </>
              )}
            </div>
          </div>

          <div className="grid-3" style={{ marginTop: 48 }}>
            {[
              { value: stats.open, label: 'Open requests', color: 'var(--red)' },
              { value: stats.donors, label: 'Registered donors', color: 'var(--text)' },
              { value: '3', label: 'Lives saved per donation', color: 'var(--green)' },
            ].map(({ value, label, color }) => (
              <div key={label} style={{ background: 'var(--bg)', border: '1.5px solid var(--border)', borderRadius: 'var(--radius)', padding: '20px 24px', boxShadow: 'var(--shadow-sm)' }}>
                <p style={{ fontFamily: 'var(--font-display)', fontSize: 40, fontWeight: 700, color, lineHeight: 1 }}>{value}</p>
                <p style={{ fontSize: 13, color: 'var(--text-muted)', marginTop: 6 }}>{label}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Live alert */}
      {liveAlert && (
        <div className="fade-in" style={{ background: 'var(--red)', color: '#fff', padding: '14px 24px', display: 'flex', alignItems: 'center', gap: 12, justifyContent: 'center' }}>
          <span className="pulse" style={{ fontSize: 16 }}>🚨</span>
          <span style={{ fontWeight: 600 }}>New request: {liveAlert.bloodType} blood needed at {liveAlert.hospital}, {liveAlert.city}</span>
          <Link to={`/requests/${liveAlert._id}`} style={{ color: '#fff', textDecoration: 'underline', fontSize: 13 }}>View →</Link>
        </div>
      )}

      {/* Filters + Feed */}
      <div className="page-container">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24, flexWrap: 'wrap', gap: 16 }}>
          <div>
            <h2 className="section-title">Live Request Feed</h2>
            <p className="section-subtitle" style={{ marginBottom: 0 }}>Real-time blood requests — updated as they come in</p>
          </div>
          <Link to="/requests/new" className="btn btn-primary">+ Post Request</Link>
        </div>

        <div style={{ display: 'flex', gap: 10, marginBottom: 28, flexWrap: 'wrap' }}>
          <select value={filter.bloodType} onChange={e => setFilter({ ...filter, bloodType: e.target.value })} style={{ width: 'auto', minWidth: 130 }}>
            <option value="">All blood types</option>
            {BLOOD_TYPES.map(t => <option key={t}>{t}</option>)}
          </select>
          <select value={filter.urgency} onChange={e => setFilter({ ...filter, urgency: e.target.value })} style={{ width: 'auto', minWidth: 130 }}>
            <option value="">All urgencies</option>
            <option value="critical">Critical</option>
            <option value="urgent">Urgent</option>
            <option value="normal">Normal</option>
          </select>
          <input placeholder="Filter by city..." value={filter.city}
            onChange={e => setFilter({ ...filter, city: e.target.value })}
            style={{ maxWidth: 200 }} />
        </div>

        {loading ? (
          <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}>
            <div className="spinner" style={{ width: 36, height: 36 }} />
          </div>
        ) : requests.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">🩸</div>
            <p className="empty-state-text">No open requests found. Try adjusting filters.</p>
          </div>
        ) : (
          <div className="grid-2">
            {requests.map(r => <RequestCard key={r._id} request={r} />)}
          </div>
        )}

        {requests.length > 0 && (
          <div style={{ textAlign: 'center', marginTop: 32 }}>
            <Link to="/requests" className="btn btn-ghost">View all requests →</Link>
          </div>
        )}
      </div>
    </div>
  );
}
