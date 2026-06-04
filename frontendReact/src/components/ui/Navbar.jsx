import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useState, useEffect } from 'react';
import api from '../../services/api';

export default function Navbar() {
  const { user, logout, isAdmin, isDonor } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [unread, setUnread] = useState(0);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const h = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', h);
    return () => window.removeEventListener('scroll', h);
  }, []);

  useEffect(() => {
    if (user) api.get('/notifications').then(r => setUnread(r.data.unreadCount)).catch(() => {});
  }, [user, location.pathname]);

  const isActive = (p) => location.pathname === p || location.pathname.startsWith(p + '/');

  const navLink = (path, label) => (
    <Link to={path} style={{ padding:'6px 12px', borderRadius:8, fontSize:13, fontWeight:500, color:isActive(path)?'var(--red)':'var(--text-muted)', background:isActive(path)?'var(--red-dim)':'transparent', transition:'all 0.15s' }}>
      {label}
    </Link>
  );

  return (
    <nav style={{ position:'sticky', top:0, zIndex:100, background:scrolled?'rgba(255,255,255,0.92)':'rgba(255,255,255,0.98)', backdropFilter:'blur(16px)', borderBottom:`1.5px solid ${scrolled?'var(--border)':'transparent'}`, transition:'all 0.3s' }}>
      <div style={{ maxWidth:1200, margin:'0 auto', padding:'0 24px', display:'flex', alignItems:'center', justifyContent:'space-between', height:64 }}>
        <Link to="/" style={{ display:'flex', alignItems:'center', gap:10 }}>
          <div style={{ width:36, height:36, background:'var(--red)', borderRadius:10, display:'flex', alignItems:'center', justifyContent:'center', fontSize:18 }}>🩸</div>
          <span style={{ fontFamily:'var(--font-display)', fontWeight:700, fontSize:18, color:'var(--red)' }}>BloodConnect</span>
        </Link>

        <div style={{ display:'flex', alignItems:'center', gap:2 }}>
          {navLink('/', 'Home')}
          {navLink('/requests', 'Requests')}
          {navLink('/shortage', 'Shortage')}
          {navLink('/stats', 'Impact')}
          {navLink('/leaderboard', 'Leaders')}
          {isDonor && navLink('/donations/history', 'My Donations')}
          {isAdmin && navLink('/admin', 'Admin')}
        </div>

        <div style={{ display:'flex', alignItems:'center', gap:8 }}>
          {user ? (
            <>
              <Link to="/notifications" style={{ position:'relative', padding:8, borderRadius:8, color:'var(--text-muted)', display:'flex', alignItems:'center' }}>
                🔔
                {unread > 0 && <span style={{ position:'absolute', top:4, right:4, width:16, height:16, background:'var(--red)', borderRadius:'50%', fontSize:10, color:'#fff', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:700 }}>{unread}</span>}
              </Link>
              <Link to="/profile" style={{ display:'flex', alignItems:'center', gap:6, padding:'6px 12px', borderRadius:8, background:'var(--bg-secondary)', border:'1.5px solid var(--border)' }}>
                <div style={{ width:24, height:24, borderRadius:'50%', background:'var(--red)', display:'flex', alignItems:'center', justifyContent:'center', color:'#fff', fontSize:11, fontWeight:700 }}>{user.name?.[0]?.toUpperCase()}</div>
                <span style={{ fontSize:13, fontWeight:500, color:'var(--text)' }}>{user.name.split(' ')[0]}</span>
              </Link>
              <button className="btn btn-ghost" onClick={()=>{ logout(); navigate('/'); }} style={{ padding:'6px 14px', fontSize:13 }}>Sign out</button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost" style={{ padding:'8px 18px', fontSize:13 }}>Sign in</Link>
              <Link to="/register" className="btn btn-primary" style={{ padding:'8px 18px', fontSize:13 }}>Join as Donor</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
