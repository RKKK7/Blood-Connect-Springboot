import { useState, useEffect } from 'react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const BLOOD_TYPES = ['A+','A-','B+','B-','AB+','AB-','O+','O-'];

export default function Profile() {
  const { user, isDonor, updateDonorProfile } = useAuth();
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({});
  const [myRequests, setMyRequests] = useState([]);
  const [myDonations, setMyDonations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  // FEATURE 7: AI health tip
  const [healthTip, setHealthTip] = useState(null);
  const [loadingTip, setLoadingTip] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      try {
        if (isDonor) {
          const [profileRes, reqRes] = await Promise.all([
            api.get('/donors/profile').catch(() => ({ data: null })),
            api.get('/requests/my/requests').catch(() => ({ data: [] })),
          ]);
          if (profileRes.data) {
            setProfile(profileRes.data);
            setForm({
              bloodType: profileRes.data.bloodType,
              city: profileRes.data.city,
              state: profileRes.data.state,
              isAvailable: profileRes.data.isAvailable,
              weight: profileRes.data.weight,
              age: profileRes.data.age,
            });
          }
          setMyRequests(reqRes.data);

          // Fetch completed donations for certificate
          try {
            const donRes = await api.get('/requests', { params: { status: 'all', limit: 50 } });
          } catch {}
        } else {
          const reqRes = await api.get('/requests/my/requests').catch(() => ({ data: [] }));
          setMyRequests(reqRes.data);
        }
      } catch {} finally { setLoading(false); }
    };
    fetchData();
  }, []);

  const handleSave = async () => {
    setSaving(true);
    try {
      const res = await api.put('/donors/profile', form);
      setProfile(res.data);
      updateDonorProfile(res.data);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (err) { alert(err.response?.data?.message || 'Failed to save'); }
    finally { setSaving(false); }
  };

  // FEATURE 7: fetch AI health tip
  const fetchHealthTip = async () => {
    setLoadingTip(true);
    try {
      const res = await api.get('/donors/health-tip');
      setHealthTip(res.data);
    } catch { setHealthTip({ title: 'Stay Hydrated', tip: 'Drink plenty of water and rest for 24 hours after donating blood.' }); }
    finally { setLoadingTip(false); }
  };

  const daysSinceDonation = profile?.lastDonated
    ? Math.floor((Date.now() - new Date(profile.lastDonated)) / (1000*60*60*24))
    : null;
  const isEligible = daysSinceDonation === null || daysSinceDonation >= 56;

  if (loading) return <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}><div className="spinner" style={{ width: 36, height: 36 }} /></div>;

  return (
    <div className="page-container" style={{ maxWidth: 960 }}>
      {/* Profile header */}
      <div style={{ display: 'flex', gap: 20, alignItems: 'center', marginBottom: 32, background: 'linear-gradient(135deg, #fff5f5, #fff)', border: '1.5px solid var(--border)', borderRadius: 'var(--radius)', padding: 24 }}>
        <div style={{ width: 64, height: 64, borderRadius: '50%', background: 'var(--red)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 26, fontWeight: 700, fontFamily: 'var(--font-display)', flexShrink: 0 }}>
          {user?.name?.[0]?.toUpperCase()}
        </div>
        <div style={{ flex: 1 }}>
          <h1 style={{ fontSize: 22, marginBottom: 4 }}>{user?.name}</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: 14 }}>{user?.email} · {user?.phone}</p>
          <div style={{ display: 'flex', gap: 8, marginTop: 10, flexWrap: 'wrap' }}>
            <span className="badge badge-open" style={{ textTransform: 'capitalize' }}>{user?.role}</span>
            {profile?.isVerified && <span className="badge badge-verified">✓ Verified Donor</span>}
            {profile && <span className="blood-type-badge" style={{ width: 32, height: 32, fontSize: 12 }}>{profile.bloodType}</span>}
            {/* FEATURE 1: Eligibility pill */}
            {isDonor && (
              <span style={{ fontSize: 12, fontWeight: 600, borderRadius: 100, padding: '3px 12px', border: '1px solid', background: isEligible ? '#f0fdf4' : '#fffbeb', color: isEligible ? '#16a34a' : '#d97706', borderColor: isEligible ? '#bbf7d0' : '#fde68a' }}>
                {isEligible ? '✅ Eligible to donate' : `⏳ Eligible in ${56 - daysSinceDonation}d`}
              </span>
            )}
          </div>

          {/* FEATURE 5: Badges */}
          {profile?.badges?.length > 0 && (
            <div style={{ display: 'flex', gap: 8, marginTop: 10, flexWrap: 'wrap' }}>
              {profile.badges.map(badge => (
                <span key={badge} style={{ fontSize: 12, background: '#fff5f5', border: '1px solid #fecaca', borderRadius: 100, padding: '3px 12px', color: 'var(--red)', fontWeight: 600 }}>
                  {badge}
                </span>
              ))}
            </div>
          )}
        </div>
        {profile && (
          <div style={{ textAlign: 'right' }}>
            <p style={{ fontFamily: 'var(--font-display)', fontSize: 40, fontWeight: 700, color: 'var(--red)', lineHeight: 1 }}>{profile.totalDonations}</p>
            <p style={{ fontSize: 12, color: 'var(--text-muted)' }}>total donations</p>
            {profile.lastDonated && (
              <p style={{ fontSize: 11, color: 'var(--text-dim)', marginTop: 4 }}>
                Last: {new Date(profile.lastDonated).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })}
              </p>
            )}
          </div>
        )}
      </div>

      {/* FEATURE 7: AI Health Tip banner */}
      {isDonor && (
        <div style={{ marginBottom: 24, background: '#f0fdf4', border: '1.5px solid #bbf7d0', borderRadius: 12, padding: '16px 20px', display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16 }}>
          <div style={{ flex: 1 }}>
            <p style={{ fontSize: 12, color: '#16a34a', fontWeight: 700, marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.05em' }}>🤖 AI Health Tip</p>
            {healthTip ? (
              <>
                <p style={{ fontWeight: 600, fontSize: 15, color: '#14532d', marginBottom: 4 }}>{healthTip.title}</p>
                <p style={{ fontSize: 14, color: '#166534', lineHeight: 1.6 }}>{healthTip.tip}</p>
              </>
            ) : (
              <p style={{ fontSize: 14, color: '#16a34a' }}>Get a personalized health tip based on your donation history.</p>
            )}
          </div>
          <button
            className="btn"
            style={{ background: '#16a34a', color: '#fff', fontSize: 13, padding: '8px 16px', whiteSpace: 'nowrap', flexShrink: 0 }}
            onClick={fetchHealthTip}
            disabled={loadingTip}
          >
            {loadingTip ? <><div className="spinner" style={{ width: 14, height: 14 }} /> Loading...</> : healthTip ? '🔄 New Tip' : '💡 Get Tip'}
          </button>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: isDonor ? '1fr 1fr' : '1fr', gap: 24 }}>
        {isDonor && (
          <div className="card">
            <h3 style={{ fontSize: 16, marginBottom: 20 }}>Donor Profile</h3>
            <div className="form-group">
              <label>Blood type</label>
              <select value={form.bloodType || ''} onChange={e => setForm({ ...form, bloodType: e.target.value })}>
                {BLOOD_TYPES.map(t => <option key={t}>{t}</option>)}
              </select>
            </div>
            <div className="grid-2">
              <div className="form-group">
                <label>City</label>
                <input value={form.city || ''} onChange={e => setForm({ ...form, city: e.target.value })} placeholder="Your city" />
              </div>
              <div className="form-group">
                <label>State</label>
                <input value={form.state || ''} onChange={e => setForm({ ...form, state: e.target.value })} placeholder="State" />
              </div>
            </div>
            <div className="grid-2">
              <div className="form-group">
                <label>Age</label>
                <input type="number" value={form.age || ''} onChange={e => setForm({ ...form, age: e.target.value })} placeholder="Age (18–65)" />
              </div>
              <div className="form-group">
                <label>Weight (kg)</label>
                <input type="number" value={form.weight || ''} onChange={e => setForm({ ...form, weight: e.target.value })} placeholder="Min 50 kg" />
              </div>
            </div>

            {/* Eligibility preview */}
            {(form.weight || form.age) && (
              <div style={{ background: form.weight < 50 || form.age < 18 || form.age > 65 ? '#fffbeb' : '#f0fdf4', border: `1px solid ${form.weight < 50 || form.age < 18 || form.age > 65 ? '#fde68a' : '#bbf7d0'}`, borderRadius: 8, padding: '10px 14px', marginBottom: 16, fontSize: 13 }}>
                {form.weight > 0 && form.weight < 50 && <p style={{ color: '#d97706' }}>⚠️ Weight below 50 kg minimum</p>}
                {form.age > 0 && (form.age < 18 || form.age > 65) && <p style={{ color: '#d97706' }}>⚠️ Age must be 18–65</p>}
                {form.weight >= 50 && form.age >= 18 && form.age <= 65 && <p style={{ color: '#16a34a' }}>✅ Profile meets eligibility criteria</p>}
              </div>
            )}

            <div className="form-group">
              <label style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer' }}>
                <input type="checkbox" checked={form.isAvailable !== false} onChange={e => setForm({ ...form, isAvailable: e.target.checked })} style={{ width: 'auto' }} />
                Available to donate
              </label>
            </div>
            <button className="btn btn-primary" onClick={handleSave} disabled={saving} style={{ width: '100%', justifyContent: 'center' }}>
              {saved ? '✓ Saved!' : saving ? 'Saving...' : 'Save Profile'}
            </button>
          </div>
        )}

        <div className="card">
          <h3 style={{ fontSize: 16, marginBottom: 20 }}>My Requests ({myRequests.length})</h3>
          {myRequests.length === 0 ? (
            <div className="empty-state" style={{ padding: 30 }}>
              <p style={{ color: 'var(--text-dim)', fontSize: 14 }}>No requests yet.</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {myRequests.map(r => (
                <div key={r._id} style={{ padding: '12px 14px', background: 'var(--bg-secondary)', borderRadius: 8 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      <span className="blood-type-badge" style={{ width: 32, height: 32, fontSize: 12 }}>{r.bloodType}</span>
                      <div>
                        <p style={{ fontWeight: 500, fontSize: 13 }}>{r.patientName}</p>
                        <p style={{ fontSize: 12, color: 'var(--text-dim)' }}>{r.hospital}</p>
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                      <span className={`badge badge-${r.status}`} style={{ textTransform: 'capitalize' }}>{r.status}</span>
                      {/* FEATURE 4: Show expiry */}
                      {r.status === 'open' && r.expiresAt && (() => {
                        const d = Math.ceil((new Date(r.expiresAt) - Date.now()) / (1000*60*60*24));
                        return d <= 3 ? <span style={{ fontSize: 11, color: '#d97706' }}>⏰ {d}d</span> : null;
                      })()}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// Availability schedule is in the donor profile card — already included above
