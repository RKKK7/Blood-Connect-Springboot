import { Link } from 'react-router-dom';

const timeAgo = (date) => {
  const diff = Date.now() - new Date(date);
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
};

export default function RequestCard({ request, compact = false }) {
  const urgencyColors = { critical: '#922b21', urgent: '#a04000', normal: '#1a5e3a' };
  const urgencyBg = { critical: '#fde8e8', urgent: '#fef3e2', normal: '#e8f4f0' };

  return (
    <Link to={`/requests/${request._id}`} style={{ textDecoration: 'none', display: 'block' }}>
      <div className="card fade-in" style={{
        cursor: 'pointer', transition: 'all 0.2s', position: 'relative', overflow: 'hidden',
      }}
        onMouseEnter={e => { e.currentTarget.style.borderColor = 'var(--red)'; e.currentTarget.style.transform = 'translateY(-2px)'; e.currentTarget.style.boxShadow = 'var(--shadow)'; }}
        onMouseLeave={e => { e.currentTarget.style.borderColor = 'var(--border)'; e.currentTarget.style.transform = 'none'; e.currentTarget.style.boxShadow = 'var(--shadow-sm)'; }}
      >
        {request.urgency === 'critical' && (
          <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 3, background: 'var(--red)' }} />
        )}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 14 }}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <div className="blood-type-badge">{request.bloodType}</div>
            <div>
              <p style={{ fontWeight: 600, fontSize: 15 }}>{request.patientName}</p>
              <p style={{ fontSize: 13, color: 'var(--text-muted)', marginTop: 2 }}>{request.hospital}</p>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexDirection: 'column', alignItems: 'flex-end' }}>
            <span className={`badge badge-${request.urgency}`} style={{ textTransform: 'capitalize' }}>
              <span className={`urgency-dot ${request.urgency}`} />
              {request.urgency}
            </span>
            <span style={{ fontSize: 11, color: 'var(--text-dim)' }}>{timeAgo(request.createdAt)}</span>
          </div>
        </div>

        {request.aiSummary && !compact && (
          <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 14, lineHeight: 1.5, fontStyle: 'italic' }}>
            "{request.aiSummary}"
          </p>
        )}

        <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
          <span style={{ fontSize: 13, color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: 4 }}>
            📍 {request.city}
          </span>
          <span style={{ fontSize: 13, color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: 4 }}>
            🩸 {request.units} unit{request.units > 1 ? 's' : ''}
          </span>
          <span style={{ fontSize: 13, color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: 4 }}>
            👥 {request.respondedDonors?.length || 0} donor{request.respondedDonors?.length !== 1 ? 's' : ''} responded
          </span>
          <span className={`badge badge-${request.status}`} style={{ marginLeft: 'auto', textTransform: 'capitalize' }}>
            {request.status}
          </span>
        </div>
      </div>
    </Link>
  );
}
