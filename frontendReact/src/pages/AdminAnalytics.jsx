import { useState, useEffect } from 'react';
import api from '../services/api';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Legend, PieChart, Pie, Cell } from 'recharts';

const COLORS = ['#e53e3e','#f59e0b','#10b981','#3b82f6','#8b5cf6','#ec4899','#06b6d4','#84cc16'];
const RISK_COLOR = { high:'#dc2626', medium:'#d97706', low:'#16a34a' };

export default function AdminAnalytics() {
  const [data, setData]         = useState(null);
  const [forecast, setForecast] = useState(null);
  const [tab, setTab]           = useState('overview');
  const [loading, setLoading]   = useState(true);
  const [loadingForecast, setLoadingForecast] = useState(false);

  useEffect(() => {
    api.get('/analytics/admin')
      .then(r => setData(r.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const loadForecast = async () => {
    setLoadingForecast(true);
    try {
      const r = await api.get('/analytics/forecast');
      setForecast(r.data);
    } catch {} finally { setLoadingForecast(false); }
  };

  if (loading) return <div style={{ display:'flex', justifyContent:'center', padding:80 }}><div className="spinner" style={{ width:36, height:36 }} /></div>;

  const tabs = ['overview','blood types','cities','forecast'];

  return (
    <div className="page-container">
      <div style={{ marginBottom:32 }}>
        <h1 className="section-title">Platform Analytics</h1>
        <p style={{ color:'var(--text-muted)', fontSize:14 }}>Deep insights into platform performance</p>
      </div>

      {/* Summary row */}
      <div className="grid-4" style={{ marginBottom:28 }}>
        {[
          { label:'Avg Response Time', value:`${data?.avgResponseHours || 0}h`, color:'var(--red)', icon:'⚡' },
          { label:'Days of Data',      value:30,                                color:'#1976d2',    icon:'📅' },
          { label:'Top City',          value: data?.topCities?.[0]?._id || '—', color:'#388e3c',   icon:'🏙️', noNum:true },
          { label:'Blood Types Tracked', value:8,                               color:'#f57c00',   icon:'🩸' },
        ].map(s => (
          <div className="stat-card" key={s.label}>
            <p style={{ fontSize:22, marginBottom:6 }}>{s.icon}</p>
            <p className="stat-value" style={{ color:s.color, fontSize:26 }}>{s.value}</p>
            <p className="stat-label">{s.label}</p>
          </div>
        ))}
      </div>

      <div style={{ display:'flex', gap:8, marginBottom:24, flexWrap:'wrap' }}>
        {tabs.map(t => (
          <button key={t} onClick={() => { setTab(t); if(t==='forecast' && !forecast) loadForecast(); }}
            style={{ padding:'8px 18px', borderRadius:8, border:'none', cursor:'pointer', fontWeight:500, fontSize:13, textTransform:'capitalize', background: tab===t ? 'var(--red)' : 'var(--bg-raised)', color: tab===t ? '#fff' : 'var(--text-muted)', transition:'all 0.2s' }}>
            {t}
          </button>
        ))}
      </div>

      {tab === 'overview' && (
        <div className="grid-2">
          <div className="card">
            <h3 style={{ fontSize:16, marginBottom:20 }}>Requests — Last 30 Days</h3>
            <ResponsiveContainer width="100%" height={220}>
              <LineChart data={data?.dailyRequests || []}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="_id" tick={{ fontSize:10 }} tickFormatter={v => v.slice(5)} />
                <YAxis tick={{ fontSize:11 }} />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="total"     stroke="#e53e3e" strokeWidth={2} dot={false} name="Total" />
                <Line type="monotone" dataKey="critical"  stroke="#f59e0b" strokeWidth={1.5} dot={false} name="Critical" />
                <Line type="monotone" dataKey="fulfilled" stroke="#10b981" strokeWidth={1.5} dot={false} name="Fulfilled" />
              </LineChart>
            </ResponsiveContainer>
          </div>

          <div className="card">
            <h3 style={{ fontSize:16, marginBottom:20 }}>Weekly New Users</h3>
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={data?.weeklyUsers || []}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="_id" tick={{ fontSize:11 }} />
                <YAxis tick={{ fontSize:11 }} />
                <Tooltip />
                <Bar dataKey="count" fill="var(--red)" radius={[4,4,0,0]} name="New users" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {tab === 'blood types' && (
        <div className="grid-2">
          <div className="card">
            <h3 style={{ fontSize:16, marginBottom:20 }}>Requests by Blood Type</h3>
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={data?.byBloodType || []}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="_id" tick={{ fontSize:12 }} />
                <YAxis tick={{ fontSize:11 }} />
                <Tooltip />
                <Bar dataKey="total" fill="var(--red)" radius={[4,4,0,0]} name="Total" />
                <Bar dataKey="fulfilled" fill="#10b981" radius={[4,4,0,0]} name="Fulfilled" />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="card">
            <h3 style={{ fontSize:16, marginBottom:20 }}>Fulfillment Rate by Type</h3>
            <div style={{ display:'flex', flexDirection:'column', gap:10 }}>
              {(data?.byBloodType || []).map(bt => {
                const rate = bt.total > 0 ? Math.round((bt.fulfilled/bt.total)*100) : 0;
                return (
                  <div key={bt._id} style={{ display:'flex', alignItems:'center', gap:12 }}>
                    <span className="blood-type-badge" style={{ width:32, height:32, fontSize:11, flexShrink:0 }}>{bt._id}</span>
                    <div style={{ flex:1 }}>
                      <div style={{ display:'flex', justifyContent:'space-between', marginBottom:4 }}>
                        <span style={{ fontSize:12, color:'var(--text-muted)' }}>{bt.total} requests</span>
                        <span style={{ fontSize:12, fontWeight:700, color: rate>=60?'var(--green)':rate>=30?'#d97706':'var(--red)' }}>{rate}%</span>
                      </div>
                      <div style={{ height:6, background:'var(--bg-raised)', borderRadius:3, overflow:'hidden' }}>
                        <div style={{ width:`${rate}%`, height:'100%', background: rate>=60?'var(--green)':rate>=30?'#d97706':'var(--red)', borderRadius:3, transition:'width 0.8s ease' }} />
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {tab === 'cities' && (
        <div className="grid-2">
          <div className="card">
            <h3 style={{ fontSize:16, marginBottom:20 }}>Top Cities by Requests</h3>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={data?.topCities || []} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis type="number" tick={{ fontSize:11 }} />
                <YAxis dataKey="_id" type="category" tick={{ fontSize:12 }} width={80} />
                <Tooltip />
                <Bar dataKey="count" fill="var(--red)" radius={[0,4,4,0]} name="Requests" />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="card">
            <h3 style={{ fontSize:16, marginBottom:20 }}>City Share</h3>
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie data={data?.topCities || []} dataKey="count" nameKey="_id" cx="50%" cy="50%" outerRadius={100} label={({ _id, percent }) => `${_id} ${(percent*100).toFixed(0)}%`} labelLine={false} fontSize={11}>
                  {(data?.topCities || []).map((_, i) => <Cell key={i} fill={COLORS[i%COLORS.length]} />)}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {tab === 'forecast' && (
        <div>
          {loadingForecast ? (
            <div style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:16, padding:60 }}>
              <div className="spinner" style={{ width:40, height:40 }} />
              <p style={{ color:'var(--text-muted)', fontSize:14 }}>AI is analyzing 30 days of data...</p>
            </div>
          ) : forecast ? (
            <div>
              <div className="card" style={{ marginBottom:20, background:'#fff5f5', border:'1px solid #fecaca' }}>
                <p style={{ fontSize:12, color:'var(--red)', fontWeight:700, marginBottom:8, textTransform:'uppercase', letterSpacing:'0.05em' }}>🤖 AI FORECAST SUMMARY</p>
                <p style={{ fontSize:15, color:'var(--text)', lineHeight:1.7 }}>{forecast.summary}</p>
                {forecast.topRisk && (
                  <div style={{ marginTop:12, display:'inline-flex', alignItems:'center', gap:8, background:'#dc2626', color:'#fff', borderRadius:8, padding:'6px 14px' }}>
                    <span>🚨</span>
                    <span style={{ fontWeight:700 }}>Highest risk next week: {forecast.topRisk}</span>
                  </div>
                )}
              </div>

              <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:16 }}>
                {(forecast.predictions || []).map((p, i) => (
                  <div key={i} style={{ background:'#fff', border:`2px solid ${RISK_COLOR[p.riskLevel]}30`, borderRadius:12, padding:20 }}>
                    <div style={{ display:'flex', justifyContent:'space-between', marginBottom:12 }}>
                      <span className="blood-type-badge" style={{ width:40, height:40, fontSize:14 }}>{p.bloodType}</span>
                      <span style={{ fontSize:12, fontWeight:700, color:RISK_COLOR[p.riskLevel], background:`${RISK_COLOR[p.riskLevel]}15`, border:`1px solid ${RISK_COLOR[p.riskLevel]}30`, borderRadius:100, padding:'3px 10px', textTransform:'capitalize' }}>
                        {p.riskLevel} risk
                      </span>
                    </div>
                    <p style={{ fontWeight:600, fontSize:14, marginBottom:4 }}>{p.city}</p>
                    <p style={{ fontSize:13, color:'var(--text-muted)', lineHeight:1.5, marginBottom:10 }}>{p.reason}</p>
                    <p style={{ fontSize:12, color:'var(--text-dim)' }}>Recommended donors to recruit: <strong>{p.recommendedDonors}</strong></p>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div style={{ textAlign:'center', padding:60 }}>
              <p style={{ fontSize:32, marginBottom:12 }}>🔮</p>
              <p style={{ color:'var(--text-muted)' }}>Click the Forecast tab to run AI demand prediction</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
