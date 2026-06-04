import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import RequestCard from '../components/request/RequestCard';

const BLOOD_TYPES = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export default function Requests() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState({ bloodType: '', urgency: '', city: '', status: 'open' });
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pages, setPages] = useState(1);

  const fetchRequests = async () => {
    setLoading(true);
    try {
      const params = { page, limit: 12, ...filter };
      Object.keys(params).forEach(k => !params[k] && delete params[k]);
      const res = await api.get('/requests', { params });
      setRequests(res.data.requests);
      setTotal(res.data.total);
      setPages(res.data.pages);
    } catch (err) { console.error(err); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchRequests(); }, [filter, page]);

  return (
    <div className="page-container">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <div>
          <h1 className="section-title">Blood Requests</h1>
          <p className="section-subtitle">{total} request{total !== 1 ? 's' : ''} found</p>
        </div>
        <Link to="/requests/new" className="btn btn-primary">+ New Request</Link>
      </div>

      <div style={{ display: 'flex', gap: 10, marginBottom: 28, flexWrap: 'wrap' }}>
        <select value={filter.status} onChange={e => setFilter({ ...filter, status: e.target.value })} style={{ width: 'auto', minWidth: 130 }}>
          <option value="open">Open</option>
          <option value="fulfilled">Fulfilled</option>
          <option value="cancelled">Cancelled</option>
          <option value="">All</option>
        </select>
        <select value={filter.bloodType} onChange={e => setFilter({ ...filter, bloodType: e.target.value })} style={{ width: 'auto', minWidth: 130 }}>
          <option value="">All blood types</option>
          {BLOOD_TYPES.map(t => <option key={t}>{t}</option>)}
        </select>
        <select value={filter.urgency} onChange={e => setFilter({ ...filter, urgency: e.target.value })} style={{ width: 'auto', minWidth: 130 }}>
          <option value="">All urgencies</option>
          <option value="critical">🔴 Critical</option>
          <option value="urgent">🟠 Urgent</option>
          <option value="normal">🟢 Normal</option>
        </select>
        <input placeholder="Search city..." value={filter.city} onChange={e => setFilter({ ...filter, city: e.target.value })} style={{ maxWidth: 180 }} />
      </div>

      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}>
          <div className="spinner" style={{ width: 36, height: 36 }} />
        </div>
      ) : requests.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🩸</div>
          <p className="empty-state-text">No requests found. Try different filters.</p>
        </div>
      ) : (
        <>
          <div className="grid-2">
            {requests.map(r => <RequestCard key={r._id} request={r} />)}
          </div>
          {pages > 1 && (
            <div style={{ display: 'flex', gap: 8, justifyContent: 'center', marginTop: 32 }}>
              <button className="btn btn-ghost" disabled={page === 1} onClick={() => setPage(p => p - 1)}>← Prev</button>
              <span style={{ padding: '8px 16px', fontSize: 14, color: 'var(--text-muted)' }}>Page {page} of {pages}</span>
              <button className="btn btn-ghost" disabled={page === pages} onClick={() => setPage(p => p + 1)}>Next →</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
