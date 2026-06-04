import { useState, useEffect } from 'react';
import api from '../services/api';

const CountUp = ({ end, duration = 1500 }) => {
  const [val, setVal] = useState(0);
  useEffect(() => {
    if (!end) return;
    let start = 0;
    const step = Math.ceil(end / (duration / 16));
    const timer = setInterval(() => {
      start = Math.min(start + step, end);
      setVal(start);
      if (start >= end) clearInterval(timer);
    }, 16);
    return () => clearInterval(timer);
  }, [end]);
  return <>{val.toLocaleString()}</>;
};

export default function Stats() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/analytics/public')
      .then(r => setStats(r.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div style={{ display:'flex', justifyContent:'center', padding:80 }}><div className="spinner" style={{ width:36, height:36 }} /></div>;

  const metrics = [
    { label:'Total Donations', value: stats?.totalDonations, icon:'🩸', color:'var(--red)',  desc:'Completed donations through BloodConnect' },
    { label:'Lives Impacted',  value: stats?.livesClaimed,   icon:'❤️', color:'#e91e63',    desc:'Each donation can save up to 3 lives' },
    { label:'Active Cities',   value: stats?.activeCities,   icon:'🏙️', color:'#1976d2',    desc:'Cities with active blood requests' },
    { label:'Registered Donors', value: stats?.totalDonors,  icon:'👤', color:'#388e3c',    desc:'Donors registered on the platform' },
    { label:'Requests Posted', value: stats?.totalRequests,  icon:'📋', color:'#f57c00',    desc:'Total blood requests since launch' },
    { label:'Fulfillment Rate', value: `${stats?.fulfillmentRate}%`, icon:'✅', color:'var(--green)', desc:'Requests that were successfully fulfilled', noCountUp:true },
  ];

  return (
    <div className="page-container" style={{ maxWidth:1000 }}>
      <div style={{ textAlign:'center', marginBottom:48 }}>
        <div style={{ display:'inline-flex', alignItems:'center', gap:8, background:'var(--red-dim)', border:'1px solid var(--border)', borderRadius:100, padding:'5px 16px', marginBottom:20 }}>
          <span className="urgency-dot critical" />
          <span style={{ fontSize:13, color:'var(--red)', fontWeight:600 }}>Live Platform Statistics</span>
        </div>
        <h1 style={{ fontSize:42, marginBottom:12 }}>Every number is<br /><span style={{ color:'var(--red)' }}>a life touched.</span></h1>
        <p style={{ color:'var(--text-muted)', fontSize:16, maxWidth:480, margin:'0 auto' }}>
          Real-time impact metrics from the BloodConnect platform. No logins needed — this data belongs to everyone.
        </p>
      </div>

      <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:20, marginBottom:48 }}>
        {metrics.map(m => (
          <div key={m.label} style={{ background:'#fff', border:'1.5px solid var(--border)', borderRadius:16, padding:'28px 24px', textAlign:'center', boxShadow:'var(--shadow-sm)' }}>
            <div style={{ fontSize:36, marginBottom:12 }}>{m.icon}</div>
            <p style={{ fontFamily:'var(--font-display)', fontSize:44, fontWeight:700, color:m.color, lineHeight:1, marginBottom:8 }}>
              {m.noCountUp ? m.value : <CountUp end={typeof m.value === 'number' ? m.value : 0} />}
            </p>
            <p style={{ fontWeight:600, fontSize:15, marginBottom:6 }}>{m.label}</p>
            <p style={{ fontSize:12, color:'var(--text-dim)', lineHeight:1.5 }}>{m.desc}</p>
          </div>
        ))}
      </div>

      {/* Recent activity */}
      {stats?.recentDonations?.length > 0 && (
        <div className="card">
          <h3 style={{ fontSize:16, marginBottom:20 }}>🎉 Recent Donations</h3>
          <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
            {stats.recentDonations.map((d, i) => (
              <div key={d._id || i} style={{ display:'flex', alignItems:'center', gap:14, padding:'12px 16px', background:'var(--bg-secondary)', borderRadius:10 }}>
                <div style={{ width:40, height:40, borderRadius:'50%', background:'var(--red)', color:'#fff', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:700, fontSize:14, flexShrink:0 }}>
                  {d.donorId?.name?.[0] || '?'}
                </div>
                <div style={{ flex:1 }}>
                  <p style={{ fontWeight:600, fontSize:14 }}>{d.donorId?.name || 'Anonymous'}</p>
                  <p style={{ fontSize:12, color:'var(--text-muted)' }}>
                    Donated {d.requestId?.bloodType} at {d.requestId?.hospital}, {d.requestId?.city}
                  </p>
                </div>
                <div style={{ textAlign:'right' }}>
                  <span className="blood-type-badge" style={{ width:34, height:34, fontSize:12 }}>{d.requestId?.bloodType}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
