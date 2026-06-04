import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const STATUS_COLOR = { pledged:'#f57c00', confirmed:'#1976d2', completed:'var(--green)', cancelled:'#999' };

export default function DonationHistory() {
  const { user } = useAuth();
  const [donations, setDonations] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/requests/donor/history')
      .then(r => setDonations(r.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const completed = donations.filter(d => d.status === 'completed');
  const totalBloodMl = completed.length * 450; // ~450ml per donation

  if (loading) return <div style={{ display:'flex', justifyContent:'center', padding:80 }}><div className="spinner" style={{ width:36, height:36 }} /></div>;

  return (
    <div className="page-container" style={{ maxWidth:800 }}>
      <h1 className="section-title" style={{ marginBottom:8 }}>Donation History</h1>
      <p className="section-subtitle">Your complete record of blood donations</p>

      {/* Summary cards */}
      <div className="grid-3" style={{ margin:'24px 0' }}>
        {[
          { label:'Total Donations', value: completed.length, icon:'🩸', color:'var(--red)' },
          { label:'Lives Impacted',  value: completed.length * 3, icon:'❤️', color:'#e91e63' },
          { label:'Blood Donated',   value: `${(totalBloodMl/1000).toFixed(1)}L`, icon:'💉', color:'#1976d2', noCount:true },
        ].map(s => (
          <div key={s.label} style={{ background:'#fff', border:'1.5px solid var(--border)', borderRadius:12, padding:'20px', textAlign:'center' }}>
            <div style={{ fontSize:28, marginBottom:8 }}>{s.icon}</div>
            <p style={{ fontFamily:'var(--font-display)', fontSize:32, fontWeight:700, color:s.color, lineHeight:1 }}>{s.value}</p>
            <p style={{ fontSize:13, color:'var(--text-muted)', marginTop:6 }}>{s.label}</p>
          </div>
        ))}
      </div>

      {donations.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🩸</div>
          <p className="empty-state-text">No donation history yet.</p>
          <Link to="/requests" className="btn btn-primary" style={{ marginTop:16 }}>Browse requests to donate</Link>
        </div>
      ) : (
        <div style={{ position:'relative' }}>
          {/* Timeline line */}
          <div style={{ position:'absolute', left:20, top:0, bottom:0, width:2, background:'var(--border)', zIndex:0 }} />

          <div style={{ display:'flex', flexDirection:'column', gap:0 }}>
            {donations.map((d, i) => {
              const req = d.requestId;
              return (
                <div key={d._id} style={{ display:'flex', gap:20, paddingBottom:24, position:'relative', zIndex:1 }}>
                  {/* Timeline dot */}
                  <div style={{ flexShrink:0, width:40, height:40, borderRadius:'50%', background: d.status==='completed' ? 'var(--red)' : 'var(--bg-raised)', border:`3px solid ${STATUS_COLOR[d.status] || '#ccc'}`, display:'flex', alignItems:'center', justifyContent:'center', fontSize:16, marginTop:4 }}>
                    {d.status==='completed' ? '✓' : d.status==='pledged' ? '🤝' : d.status==='confirmed' ? '⏰' : '✕'}
                  </div>

                  <div className="card" style={{ flex:1, marginBottom:0 }}>
                    <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:12 }}>
                      <div style={{ display:'flex', gap:10, alignItems:'center' }}>
                        <span className="blood-type-badge" style={{ width:36, height:36, fontSize:13 }}>{req?.bloodType || '?'}</span>
                        <div>
                          <p style={{ fontWeight:700, fontSize:15 }}>{req?.patientName || 'Unknown Patient'}</p>
                          <p style={{ fontSize:13, color:'var(--text-muted)' }}>{req?.hospital}, {req?.city}</p>
                        </div>
                      </div>
                      <div style={{ display:'flex', gap:8, alignItems:'center' }}>
                        <span style={{ fontSize:12, fontWeight:700, color: STATUS_COLOR[d.status], background: `${STATUS_COLOR[d.status]}18`, border:`1px solid ${STATUS_COLOR[d.status]}30`, borderRadius:100, padding:'3px 12px', textTransform:'capitalize' }}>
                          {d.status}
                        </span>
                      </div>
                    </div>

                    <div style={{ display:'flex', gap:16, fontSize:12, color:'var(--text-dim)', flexWrap:'wrap' }}>
                      <span>📅 Pledged: {new Date(d.createdAt).toLocaleDateString('en-IN', { day:'numeric', month:'short', year:'numeric' })}</span>
                      {d.donatedAt && <span>✅ Donated: {new Date(d.donatedAt).toLocaleDateString('en-IN', { day:'numeric', month:'short', year:'numeric' })}</span>}
                    </div>

                    {/* Feedback stars if completed */}
                    {d.status === 'completed' && d.feedback?.rating && (
                      <div style={{ marginTop:10, display:'flex', alignItems:'center', gap:8 }}>
                        <div style={{ display:'flex', gap:2 }}>
                          {[1,2,3,4,5].map(s => (
                            <span key={s} style={{ fontSize:16, color: s<=d.feedback.rating ? '#f59e0b' : '#e5e7eb' }}>★</span>
                          ))}
                        </div>
                        <span style={{ fontSize:12, color:'var(--text-muted)' }}>Rated by requester</span>
                      </div>
                    )}

                    <div style={{ display:'flex', gap:8, marginTop:12, flexWrap:'wrap' }}>
                      {req?._id && (
                        <Link to={`/requests/${req._id}`} className="btn btn-ghost" style={{ fontSize:12, padding:'5px 12px' }}>
                          View Request →
                        </Link>
                      )}
                      {d.status === 'completed' && (
                        <a href={`/api/certificate/${d._id}?token=${localStorage.getItem("token")}`} target="_blank" rel="noopener noreferrer"
                          style={{ fontSize:12, background:'#f0fdf4', color:'#16a34a', border:'1px solid #bbf7d0', borderRadius:8, padding:'5px 12px', textDecoration:'none', fontWeight:600 }}>
                          📜 Certificate
                        </a>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
