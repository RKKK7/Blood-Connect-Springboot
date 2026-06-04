import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

export default function AdminDashboard() {
  const [data, setData]           = useState(null);
  const [donors, setDonors]       = useState([]);
  const [loading, setLoading]     = useState(true);
  const [seeding, setSeeding]     = useState(false);
  const [tab, setTab]             = useState('overview');
  const [selected, setSelected]   = useState(new Set());
  const [bulkWorking, setBulkWorking] = useState(false);

  const reload = () => Promise.all([
    api.get('/admin/stats'),
    api.get('/donors/all'),
  ]).then(([s,d])=>{ setData(s.data); setDonors(d.data); }).finally(()=>setLoading(false));

  useEffect(() => { reload(); }, []);

  const handleSeed = async () => {
    setSeeding(true);
    try { await api.post('/admin/seed'); alert('Seeded 5 blood requests!'); }
    catch(e) { alert(e.response?.data?.message||'Seed failed'); }
    finally { setSeeding(false); }
  };

  const toggleDonor = (id) => setSelected(prev => {
    const s = new Set(prev);
    s.has(id) ? s.delete(id) : s.add(id);
    return s;
  });

  const bulkVerify = async (isVerified) => {
    if (!selected.size) return alert('Select at least one donor');
    setBulkWorking(true);
    try {
      const r = await api.post('/admin/donors/bulk-verify', { donorIds:[...selected], isVerified });
      alert(r.data.message);
      setSelected(new Set());
      await reload();
    } catch(e) { alert(e.response?.data?.message||'Failed'); }
    finally { setBulkWorking(false); }
  };

  const closeStale = async () => {
    if (!confirm('Close all requests open for 14+ days?')) return;
    setBulkWorking(true);
    try { const r=await api.post('/admin/requests/close-stale'); alert(r.data.message); await reload(); }
    catch(e) { alert(e.response?.data?.message||'Failed'); }
    finally { setBulkWorking(false); }
  };

  if (loading) return <div style={{ display:'flex', justifyContent:'center', padding:80 }}><div className="spinner" style={{ width:36, height:36 }} /></div>;

  const { stats, bloodTypeBreakdown, recentRequests } = data;

  return (
    <div className="page-container">
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:32, flexWrap:'wrap', gap:12 }}>
        <div>
          <h1 className="section-title">Admin Dashboard</h1>
          <p style={{ color:'var(--text-muted)', fontSize:14, marginTop:4 }}>Platform overview and management</p>
        </div>
        <div style={{ display:'flex', gap:10 }}>
          <Link to="/admin/analytics" className="btn btn-secondary" style={{ fontSize:13 }}>📊 Analytics →</Link>
          <button className="btn btn-ghost" onClick={handleSeed} disabled={seeding}>
            {seeding?'Seeding...':'🌱 Seed Data'}
          </button>
        </div>
      </div>

      <div className="grid-4" style={{ marginBottom:32 }}>
        {[
          { label:'Total users',      value:stats.totalUsers,     color:'var(--text)' },
          { label:'Registered donors',value:stats.totalDonors,    color:'var(--red)' },
          { label:'Open requests',    value:stats.openRequests,   color:'#e67e22' },
          { label:'Critical requests',value:stats.criticalRequests,color:'var(--red)' },
        ].map(({ label, value, color }) => (
          <div className="stat-card" key={label}>
            <p className="stat-value" style={{ color }}>{value}</p>
            <p className="stat-label">{label}</p>
          </div>
        ))}
      </div>

      <div style={{ display:'flex', gap:8, marginBottom:24, flexWrap:'wrap' }}>
        {['overview','donors','requests','bulk actions'].map(t => (
          <button key={t} onClick={()=>setTab(t)}
            style={{ padding:'8px 18px', borderRadius:8, border:'none', cursor:'pointer', fontWeight:500, fontSize:13, textTransform:'capitalize', background:tab===t?'var(--red)':'var(--bg-raised)', color:tab===t?'#fff':'var(--text-muted)', transition:'all 0.2s' }}>
            {t}
          </button>
        ))}
      </div>

      {tab==='overview' && (
        <div className="grid-2">
          <div className="card">
            <h3 style={{ fontSize:16, marginBottom:20 }}>Blood type distribution</h3>
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={bloodTypeBreakdown.map(b=>({ name:b._id, count:b.count }))}>
                <XAxis dataKey="name" fontSize={12} /><YAxis fontSize={12} /><Tooltip />
                <Bar dataKey="count" fill="var(--red)" radius={[4,4,0,0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="card">
            <h3 style={{ fontSize:16, marginBottom:16 }}>Recent requests</h3>
            <div style={{ display:'flex', flexDirection:'column', gap:10 }}>
              {recentRequests.slice(0,6).map(r=>(
                <div key={r._id} style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'10px 12px', background:'var(--bg-secondary)', borderRadius:8 }}>
                  <div style={{ display:'flex', gap:10, alignItems:'center' }}>
                    <span className="blood-type-badge" style={{ width:30,height:30,fontSize:11 }}>{r.bloodType}</span>
                    <div><p style={{ fontSize:13, fontWeight:500 }}>{r.patientName}</p><p style={{ fontSize:11, color:'var(--text-dim)' }}>{r.city}</p></div>
                  </div>
                  <span className={`badge badge-${r.urgency}`} style={{ textTransform:'capitalize', fontSize:11 }}>{r.urgency}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {tab==='donors' && (
        <div className="card">
          <h3 style={{ fontSize:16, marginBottom:20 }}>All donors ({donors.length})</h3>
          <div style={{ display:'flex', flexDirection:'column', gap:10 }}>
            {donors.map(d=>(
              <div key={d._id} style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'12px 16px', background:'var(--bg-secondary)', borderRadius:8, flexWrap:'wrap', gap:10 }}>
                <div style={{ display:'flex', gap:12, alignItems:'center' }}>
                  <span className="blood-type-badge" style={{ width:36,height:36,fontSize:13 }}>{d.bloodType}</span>
                  <div>
                    <p style={{ fontWeight:500, fontSize:14 }}>{d.userId?.name}</p>
                    <p style={{ fontSize:12, color:'var(--text-dim)' }}>{d.userId?.email} · {d.city}</p>
                  </div>
                </div>
                <div style={{ display:'flex', gap:8, alignItems:'center' }}>
                  {d.isVerified && <span className="badge badge-verified" style={{ fontSize:11 }}>✓ Verified</span>}
                  <span style={{ fontSize:12, color:'var(--text-muted)' }}>{d.totalDonations} donations</span>
                  <button className={`btn ${d.isVerified?'btn-ghost':'btn-success'}`} style={{ padding:'5px 12px', fontSize:12 }}
                    onClick={async()=>{ await api.put(`/donors/${d._id}/verify`,{isVerified:!d.isVerified}); await reload(); }}>
                    {d.isVerified?'Unverify':'Verify'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {tab==='requests' && (
        <div className="card">
          <h3 style={{ fontSize:16, marginBottom:20 }}>All requests</h3>
          <div style={{ display:'flex', flexDirection:'column', gap:10 }}>
            {recentRequests.map(r=>(
              <div key={r._id} style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'12px 16px', background:'var(--bg-secondary)', borderRadius:8 }}>
                <div style={{ display:'flex', gap:10, alignItems:'center' }}>
                  <span className="blood-type-badge" style={{ width:34,height:34,fontSize:12 }}>{r.bloodType}</span>
                  <div><p style={{ fontWeight:500, fontSize:14 }}>{r.patientName}</p><p style={{ fontSize:12, color:'var(--text-dim)' }}>{r.hospital} · {r.city}</p></div>
                </div>
                <div style={{ display:'flex', gap:8 }}>
                  <span className={`badge badge-${r.urgency}`} style={{ textTransform:'capitalize', fontSize:11 }}>{r.urgency}</span>
                  <span className={`badge badge-${r.status}`} style={{ textTransform:'capitalize', fontSize:11 }}>{r.status}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {tab==='bulk actions' && (
        <div style={{ display:'flex', flexDirection:'column', gap:20 }}>
          <div className="card">
            <h3 style={{ fontSize:16, marginBottom:8 }}>Bulk Verify Donors</h3>
            <p style={{ fontSize:13, color:'var(--text-muted)', marginBottom:16 }}>Select donors below, then verify or unverify all at once.</p>
            <div style={{ display:'flex', gap:10, marginBottom:16 }}>
              <button className="btn btn-success" onClick={()=>bulkVerify(true)} disabled={bulkWorking||!selected.size} style={{ fontSize:13 }}>
                ✅ Verify Selected ({selected.size})
              </button>
              <button className="btn btn-ghost" onClick={()=>bulkVerify(false)} disabled={bulkWorking||!selected.size} style={{ fontSize:13 }}>
                ✕ Unverify Selected
              </button>
              <button className="btn btn-ghost" onClick={()=>setSelected(new Set(donors.map(d=>d._id)))} style={{ fontSize:13 }}>Select All</button>
              <button className="btn btn-ghost" onClick={()=>setSelected(new Set())} style={{ fontSize:13 }}>Clear</button>
            </div>
            <div style={{ display:'flex', flexDirection:'column', gap:8 }}>
              {donors.map(d=>(
                <label key={d._id} style={{ display:'flex', alignItems:'center', gap:12, padding:'10px 14px', background: selected.has(d._id)?'var(--red-dim)':'var(--bg-secondary)', borderRadius:8, cursor:'pointer', border:`1px solid ${selected.has(d._id)?'var(--border)':'transparent'}` }}>
                  <input type="checkbox" checked={selected.has(d._id)} onChange={()=>toggleDonor(d._id)} style={{ width:'auto' }} />
                  <span className="blood-type-badge" style={{ width:32,height:32,fontSize:12 }}>{d.bloodType}</span>
                  <div style={{ flex:1 }}>
                    <p style={{ fontWeight:500, fontSize:14 }}>{d.userId?.name}</p>
                    <p style={{ fontSize:12, color:'var(--text-dim)' }}>{d.city} · {d.totalDonations} donations</p>
                  </div>
                  {d.isVerified && <span className="badge badge-verified" style={{ fontSize:11 }}>✓</span>}
                </label>
              ))}
            </div>
          </div>

          <div className="card" style={{ border:'1.5px solid #fecaca' }}>
            <h3 style={{ fontSize:16, marginBottom:8 }}>Close Stale Requests</h3>
            <p style={{ fontSize:13, color:'var(--text-muted)', marginBottom:16 }}>Automatically close all blood requests that have been open for more than 14 days with no activity.</p>
            <button className="btn" onClick={closeStale} disabled={bulkWorking} style={{ background:'#dc2626', color:'#fff', border:'none', fontSize:13 }}>
              {bulkWorking ? 'Working...' : '🗑️ Close All Stale Requests (14+ days)'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
