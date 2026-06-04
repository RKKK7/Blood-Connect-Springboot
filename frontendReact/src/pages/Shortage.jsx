/**
 * FEATURE 6: Blood Shortage Dashboard (public page)
 * Color-coded availability per city and blood type
 */
import { useState, useEffect } from 'react';
import api from '../services/api';

const BLOOD_TYPES = ['A+','A-','B+','B-','AB+','AB-','O+','O-'];

const levelColor = {
  red:    { bg: '#fff5f5', border: '#fecaca', text: '#dc2626', label: 'Critical' },
  yellow: { bg: '#fffbeb', border: '#fde68a', text: '#d97706', label: 'Low'      },
  green:  { bg: '#f0fdf4', border: '#bbf7d0', text: '#16a34a', label: 'OK'       },
};

export default function Shortage() {
  const [cities, setCities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    api.get('/shortage').then(r => setCities(r.data)).catch(() => {}).finally(() => setLoading(false));
  }, []);

  const filtered = cities.filter(c => c.city.toLowerCase().includes(search.toLowerCase()));

  const globalSummary = () => {
    let red = 0, yellow = 0, green = 0;
    cities.forEach(c => BLOOD_TYPES.forEach(bt => {
      const lvl = c.bloodTypes[bt]?.level || 'green';
      if (lvl === 'red') red++;
      else if (lvl === 'yellow') yellow++;
      else green++;
    }));
    return { red, yellow, green };
  };

  const summary = globalSummary();

  return (
    <div className="page-container">
      <div style={{ marginBottom: 32 }}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: 8, background: '#fff5f5', border: '1px solid #fecaca', borderRadius: 100, padding: '5px 14px', marginBottom: 16 }}>
          <span style={{ fontSize: 13, color: '#dc2626', fontWeight: 600 }}>🔴 Live Blood Availability</span>
        </div>
        <h1 className="section-title">Blood Shortage Dashboard</h1>
        <p className="section-subtitle">Real-time blood availability per city. No login required.</p>
      </div>

      {/* Summary cards */}
      <div className="grid-3" style={{ marginBottom: 32 }}>
        {[
          { label: 'Critical (no donors)', count: summary.red, color: '#dc2626', bg: '#fff5f5', icon: '🚨' },
          { label: 'Low supply', count: summary.yellow, color: '#d97706', bg: '#fffbeb', icon: '⚠️' },
          { label: 'Adequate supply', count: summary.green, color: '#16a34a', bg: '#f0fdf4', icon: '✅' },
        ].map(s => (
          <div key={s.label} style={{ background: s.bg, border: `1.5px solid ${s.color}30`, borderRadius: 12, padding: '20px 24px' }}>
            <p style={{ fontSize: 32, fontWeight: 700, color: s.color, lineHeight: 1 }}>{s.count} <span style={{ fontSize: 22 }}>{s.icon}</span></p>
            <p style={{ fontSize: 13, color: '#666', marginTop: 4 }}>{s.label}</p>
          </div>
        ))}
      </div>

      {/* Legend */}
      <div style={{ display: 'flex', gap: 16, marginBottom: 24, flexWrap: 'wrap' }}>
        {Object.entries(levelColor).map(([k, v]) => (
          <div key={k} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <div style={{ width: 14, height: 14, borderRadius: 4, background: v.bg, border: `2px solid ${v.border}` }} />
            <span style={{ fontSize: 13, color: '#555', fontWeight: 500 }}>{v.label}</span>
          </div>
        ))}
        <span style={{ fontSize: 13, color: '#999', marginLeft: 'auto' }}>Updated live from donor + request data</span>
      </div>

      <input
        placeholder="Search city..."
        value={search}
        onChange={e => setSearch(e.target.value)}
        style={{ maxWidth: 300, marginBottom: 24 }}
      />

      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}>
          <div className="spinner" style={{ width: 36, height: 36 }} />
        </div>
      ) : filtered.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🏙️</div>
          <p className="empty-state-text">No city data yet. Register donors and post requests to see the dashboard.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          {filtered.map(({ city, bloodTypes }) => (
            <div key={city} className="card">
              <h3 style={{ fontSize: 16, fontWeight: 700, marginBottom: 16 }}>📍 {city}</h3>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 10 }}>
                {BLOOD_TYPES.map(bt => {
                  const info = bloodTypes[bt] || { donors: 0, requests: 0, level: 'green' };
                  const colors = levelColor[info.level];
                  return (
                    <div key={bt} style={{ background: colors.bg, border: `1.5px solid ${colors.border}`, borderRadius: 10, padding: '12px', textAlign: 'center' }}>
                      <div style={{ fontWeight: 700, fontSize: 18, color: 'var(--red)', marginBottom: 6 }}>{bt}</div>
                      <div style={{ fontSize: 11, fontWeight: 700, color: colors.text, background: '#fff', borderRadius: 6, padding: '2px 8px', display: 'inline-block', marginBottom: 8 }}>
                        {colors.label}
                      </div>
                      <div style={{ fontSize: 11, color: '#666' }}>
                        <div>👤 {info.donors} donor{info.donors !== 1 ? 's' : ''}</div>
                        <div>📋 {info.requests} request{info.requests !== 1 ? 's' : ''}</div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
