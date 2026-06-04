import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

const TYPE_ICONS = { match: '🩸', request: '📋', donation: '✅', system: '🔔', alert: '🚨' };

export default function Notifications() {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/notifications').then(r => setNotifications(r.data.notifications)).finally(() => setLoading(false));
  }, []);

  const markAll = async () => {
    await api.put('/notifications/read-all');
    setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
  };

  const timeAgo = (date) => {
    const diff = Date.now() - new Date(date);
    const mins = Math.floor(diff / 60000);
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    return `${Math.floor(hrs / 24)}d ago`;
  };

  return (
    <div className="page-container" style={{ maxWidth: 700 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 className="section-title">Notifications</h1>
        {notifications.some(n => !n.isRead) && (
          <button className="btn btn-ghost" onClick={markAll} style={{ fontSize: 13, padding: '7px 14px' }}>Mark all read</button>
        )}
      </div>

      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 60 }}>
          <div className="spinner" style={{ width: 32, height: 32 }} />
        </div>
      ) : notifications.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">🔔</div>
          <p className="empty-state-text">No notifications yet.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {notifications.map(n => (
            <div key={n._id} className={`card fade-in`} style={{ padding: '16px 20px', background: n.isRead ? 'var(--bg)' : '#fff8f8', borderLeft: n.isRead ? '1.5px solid var(--border)' : '3px solid var(--red)' }}>
              <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                <span style={{ fontSize: 20, flexShrink: 0 }}>{TYPE_ICONS[n.type] || '🔔'}</span>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <p style={{ fontWeight: 600, fontSize: 14 }}>{n.title}</p>
                    <span style={{ fontSize: 12, color: 'var(--text-dim)', flexShrink: 0, marginLeft: 8 }}>{timeAgo(n.createdAt)}</span>
                  </div>
                  <p style={{ fontSize: 13, color: 'var(--text-muted)', marginTop: 4, lineHeight: 1.5 }}>{n.message}</p>
                  {n.link && (
                    <Link to={n.link} style={{ fontSize: 13, color: 'var(--red)', fontWeight: 600, marginTop: 8, display: 'inline-block' }}>
                      View details →
                    </Link>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
