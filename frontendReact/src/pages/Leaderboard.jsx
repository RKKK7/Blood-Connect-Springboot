import { useState, useEffect } from 'react';
import api from '../services/api';

const MEDALS = ['🥇', '🥈', '🥉'];

export default function Leaderboard() {
  const [donors, setDonors] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/donors/leaderboard').then(r => setDonors(r.data)).catch(() => {}).finally(() => setLoading(false));
  }, []);

  return (
    <div className="page-container" style={{ maxWidth: 800 }}>
      <div style={{ textAlign: 'center', marginBottom: 40 }}>
        <div style={{ fontSize: 48, marginBottom: 12 }}>🏆</div>
        <h1 className="section-title" style={{ fontSize: 32 }}>Hero Donors</h1>
        <p style={{ color: 'var(--text-muted)', fontSize: 15 }}>Our community's most dedicated life-savers</p>
      </div>

      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 60 }}>
          <div className="spinner" style={{ width: 36, height: 36 }} />
        </div>
      ) : donors.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🩸</div>
          <p className="empty-state-text">No donations recorded yet. Be the first hero!</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {donors.map((donor, i) => (
            <div key={donor._id} className="card fade-in" style={{
              display: 'flex', alignItems: 'center', gap: 16,
              borderLeft: i < 3 ? `4px solid var(--red)` : '1.5px solid var(--border)',
              background: i === 0 ? '#fff9f0' : 'var(--bg)',
            }}>
              <div style={{ fontSize: i < 3 ? 28 : 20, width: 40, textAlign: 'center' }}>
                {i < 3 ? MEDALS[i] : `#${i + 1}`}
              </div>
              <div style={{ width: 48, height: 48, borderRadius: '50%', background: 'var(--red)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: 18, fontFamily: 'var(--font-display)', flexShrink: 0 }}>
                {donor.userId?.name?.[0]?.toUpperCase()}
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                  <span style={{ fontWeight: 600, fontSize: 16 }}>{donor.userId?.name}</span>
                  {donor.isVerified && <span className="badge badge-verified" style={{ fontSize: 11 }}>✓ Verified</span>}
                </div>
                <div style={{ display: 'flex', gap: 12 }}>
                  <span style={{ fontSize: 13, color: 'var(--text-muted)' }}>Blood type: <strong>{donor.bloodType}</strong></span>
                  {donor.city && <span style={{ fontSize: 13, color: 'var(--text-muted)' }}>📍 {donor.city}</span>}
                </div>
              </div>
              <div style={{ textAlign: 'right', flexShrink: 0 }}>
                <p style={{ fontFamily: 'var(--font-display)', fontSize: 28, fontWeight: 700, color: 'var(--red)', lineHeight: 1 }}>{donor.totalDonations}</p>
                <p style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>donations</p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
