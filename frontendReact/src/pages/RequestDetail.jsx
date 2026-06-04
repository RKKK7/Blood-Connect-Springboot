import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const timeAgo = (d) => {
  const diff = Date.now()-new Date(d); const m=Math.floor(diff/60000);
  if(m<60) return `${m}m ago`; const h=Math.floor(m/60);
  if(h<24) return `${h}h ago`; return `${Math.floor(h/24)}d ago`;
};
const STATUS_STEPS = ['pledged','confirmed','completed'];
const STATUS_LABELS = { pledged:'Pledged', confirmed:'Confirmed', completed:'Donated ✓' };

export default function RequestDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [data, setData]           = useState(null);
  const [matches, setMatches]     = useState([]);
  const [loading, setLoading]     = useState(true);
  const [responding, setResponding] = useState(false);
  const [responded, setResponded] = useState(false);
  const [loadingMatches, setLoadingMatches] = useState(false);
  const [eligibility, setEligibility] = useState(null);
  const [sosSending, setSosSending]   = useState(false);
  const [copied, setCopied]           = useState(false);
  // Feedback
  const [feedbackDonationId, setFeedbackDonationId] = useState(null);
  const [feedbackRating, setFeedbackRating] = useState(0);
  const [feedbackOnTime, setFeedbackOnTime] = useState(null);
  const [feedbackComment, setFeedbackComment] = useState('');
  const [submittingFeedback, setSubmittingFeedback] = useState(false);

  const reload = async () => {
    const r = await api.get(`/requests/${id}`);
    setData(r.data);
    setResponded(r.data.request.respondedDonors?.some(d => d===user?.id||d?.toString()===user?.id));
  };

  useEffect(() => {
    (async () => {
      try {
        await reload();
        if (user?.role==='donor') {
          try { const e=await api.get('/requests/eligibility/check'); setEligibility(e.data); } catch {}
        }
      } catch { navigate('/requests'); }
      finally { setLoading(false); }
    })();
  }, [id]);

  const handleRespond = async () => {
    setResponding(true);
    try {
      await api.post(`/requests/${id}/respond`);
      setResponded(true); await reload();
    } catch (err) {
      const issues = err.response?.data?.eligibilityIssues;
      alert(issues ? `Not eligible:\n\n${issues.join('\n')}` : err.response?.data?.message||'Failed');
    } finally { setResponding(false); }
  };

  const handleConfirm = async (donationId) => {
    try { await api.put(`/requests/donations/${donationId}/confirm`); await reload(); }
    catch (err) { alert(err.response?.data?.message||'Failed'); }
  };

  const handleComplete = async (donationId) => {
    try {
      const r = await api.put(`/requests/donations/${donationId}/complete`);
      await reload();
      if (r.data.newBadges?.length) alert(`🎉 Completed!\n\nNew badges:\n${r.data.newBadges.join('\n')}`);
    } catch (err) { alert(err.response?.data?.message||'Failed'); }
  };

  const handleSOS = async () => {
    if (!confirm('Send emergency SOS to ALL compatible donors? This cannot be undone.')) return;
    setSosSending(true);
    try {
      const r = await api.post(`/requests/${id}/sos`);
      alert(`🚨 SOS sent to ${r.data.notified} donors!`);
      await reload();
    } catch (err) { alert(err.response?.data?.message||'Failed'); }
    finally { setSosSending(false); }
  };

  const handleShare = () => {
    const url = window.location.href;
    navigator.clipboard.writeText(url).then(() => { setCopied(true); setTimeout(()=>setCopied(false),2000); });
  };

  const handleWhatsApp = () => {
    if (!data) return;
    const { request } = data;
    const msg = `🚨 Blood Needed!\n\nBlood type: ${request.bloodType}\nPatient: ${request.patientName}\nHospital: ${request.hospital}, ${request.city}\nContact: ${request.contactPhone}\n\n${request.aiSummary}\n\nRespond: ${window.location.href}`;
    window.open(`https://wa.me/?text=${encodeURIComponent(msg)}`, '_blank');
  };

  const submitFeedback = async () => {
    if (!feedbackRating) return alert('Please select a rating');
    setSubmittingFeedback(true);
    try {
      await api.post(`/requests/donations/${feedbackDonationId}/feedback`, {
        rating: feedbackRating, onTime: feedbackOnTime, comment: feedbackComment,
      });
      setFeedbackDonationId(null);
      await reload();
    } catch (err) { alert(err.response?.data?.message||'Failed'); }
    finally { setSubmittingFeedback(false); }
  };

  if (loading) return <div style={{ display:'flex', justifyContent:'center', padding:100 }}><div className="spinner" style={{ width:40, height:40 }} /></div>;

  const { request, donations } = data;
  const isOwner = user?.id===request.requesterId?._id||user?.id===request.requesterId;
  const urgencyColors = { critical:'var(--red)', urgent:'#e67e22', normal:'var(--green)' };

  return (
    <div className="page-container" style={{ maxWidth:940 }}>
      <button onClick={() => navigate(-1)} className="btn btn-ghost" style={{ marginBottom:20, fontSize:13 }}>← Back</button>

      {/* Feedback modal */}
      {feedbackDonationId && (
        <div style={{ position:'fixed', inset:0, background:'rgba(0,0,0,0.5)', zIndex:200, display:'flex', alignItems:'center', justifyContent:'center', padding:20 }}>
          <div className="card" style={{ maxWidth:440, width:'100%' }}>
            <h3 style={{ fontSize:18, marginBottom:16 }}>⭐ Rate this donation</h3>
            <p style={{ fontSize:14, color:'var(--text-muted)', marginBottom:20 }}>How was the donor's response?</p>
            <div style={{ display:'flex', gap:8, marginBottom:20, justifyContent:'center' }}>
              {[1,2,3,4,5].map(s => (
                <button key={s} onClick={()=>setFeedbackRating(s)} style={{ fontSize:32, background:'none', border:'none', cursor:'pointer', opacity: s<=feedbackRating?1:0.3, transition:'opacity 0.15s' }}>★</button>
              ))}
            </div>
            <div style={{ display:'flex', gap:10, marginBottom:16 }}>
              {[true,false].map(v => (
                <button key={String(v)} onClick={()=>setFeedbackOnTime(v)}
                  style={{ flex:1, padding:'8px', borderRadius:8, border:`2px solid ${feedbackOnTime===v?'var(--red)':'var(--border)'}`, background: feedbackOnTime===v?'var(--red-dim)':'var(--bg)', cursor:'pointer', fontSize:13, fontWeight:500 }}>
                  {v ? '✅ On time' : '⏰ Late / no show'}
                </button>
              ))}
            </div>
            <div className="form-group">
              <label>Comment (optional)</label>
              <input value={feedbackComment} onChange={e=>setFeedbackComment(e.target.value)} placeholder="Any additional feedback..." />
            </div>
            <div style={{ display:'flex', gap:10 }}>
              <button className="btn btn-primary" onClick={submitFeedback} disabled={submittingFeedback} style={{ flex:1, justifyContent:'center' }}>
                {submittingFeedback ? 'Submitting...' : 'Submit Rating'}
              </button>
              <button className="btn btn-ghost" onClick={()=>setFeedbackDonationId(null)} style={{ flex:1, justifyContent:'center' }}>Cancel</button>
            </div>
          </div>
        </div>
      )}

      <div style={{ display:'grid', gridTemplateColumns:'1fr 320px', gap:24, alignItems:'start' }}>
        <div>
          <div className="card" style={{ marginBottom:20, borderTop:`4px solid ${urgencyColors[request.urgency]}` }}>
            <div style={{ display:'flex', gap:16, alignItems:'flex-start', marginBottom:20 }}>
              <div className="blood-type-badge" style={{ width:56, height:56, fontSize:16 }}>{request.bloodType}</div>
              <div style={{ flex:1 }}>
                <div style={{ display:'flex', gap:8, alignItems:'center', flexWrap:'wrap', marginBottom:6 }}>
                  <span className={`badge badge-${request.urgency}`} style={{ textTransform:'capitalize', fontSize:13 }}>
                    <span className={`urgency-dot ${request.urgency}`} /> {request.urgency}
                  </span>
                  <span className={`badge badge-${request.status}`} style={{ textTransform:'capitalize' }}>{request.status}</span>
                  {request.sosSent && <span style={{ fontSize:12, fontWeight:700, color:'#fff', background:'#dc2626', borderRadius:100, padding:'2px 10px' }}>🚨 SOS SENT</span>}
                  <span style={{ fontSize:12, color:'var(--text-dim)' }}>{timeAgo(request.createdAt)}</span>
                  {request.expiresAt && request.status==='open' && (() => {
                    const d=Math.ceil((new Date(request.expiresAt)-Date.now())/(1000*60*60*24));
                    return d<=2 ? <span style={{ fontSize:12, color:'#d97706', fontWeight:600, background:'#fffbeb', border:'1px solid #fde68a', borderRadius:100, padding:'2px 10px' }}>⏰ Expires in {d}d</span> : null;
                  })()}
                </div>
                <h1 style={{ fontSize:22, marginBottom:4 }}>Blood needed for {request.patientName}</h1>
                <p style={{ color:'var(--text-muted)', fontSize:14 }}>{request.hospital} · {request.city}</p>
              </div>
            </div>

            {request.aiSummary && (
              <div style={{ background:'var(--red-dim)', border:'1px solid var(--border)', borderRadius:10, padding:'12px 16px', marginBottom:16 }}>
                <p style={{ fontSize:12, color:'var(--red)', fontWeight:600, marginBottom:4 }}>🤖 AI SUMMARY</p>
                <p style={{ fontSize:14, color:'var(--text-muted)', fontStyle:'italic' }}>"{request.aiSummary}"</p>
                <p style={{ fontSize:12, color:'var(--text-dim)', marginTop:6 }}>{request.urgencyReason}</p>
              </div>
            )}

            <div className="grid-2" style={{ gap:12, marginBottom:16 }}>
              {[
                { label:'Units needed', value:`${request.units} unit${request.units>1?'s':''}` },
                { label:'Contact', value:request.contactPhone },
                { label:'Hospital', value:request.hospital },
                { label:'Donors responded', value:request.respondedDonors?.length||0 },
              ].map(({ label, value }) => (
                <div key={label} style={{ background:'var(--bg-secondary)', borderRadius:8, padding:'12px 14px' }}>
                  <p style={{ fontSize:11, color:'var(--text-dim)', fontWeight:600, textTransform:'uppercase', letterSpacing:'0.06em', marginBottom:4 }}>{label}</p>
                  <p style={{ fontSize:15, fontWeight:600 }}>{value}</p>
                </div>
              ))}
            </div>

            {/* Feature 6: Share buttons */}
            <div style={{ display:'flex', gap:8, flexWrap:'wrap' }}>
              <button onClick={handleWhatsApp} style={{ display:'flex', alignItems:'center', gap:6, padding:'7px 14px', borderRadius:8, border:'1px solid #22c55e', background:'#f0fdf4', color:'#16a34a', cursor:'pointer', fontSize:13, fontWeight:600 }}>
                📱 Share on WhatsApp
              </button>
              <button onClick={handleShare} style={{ display:'flex', alignItems:'center', gap:6, padding:'7px 14px', borderRadius:8, border:'1px solid var(--border)', background:'var(--bg-secondary)', color:'var(--text-muted)', cursor:'pointer', fontSize:13, fontWeight:500 }}>
                {copied ? '✓ Copied!' : '🔗 Copy Link'}
              </button>
            </div>
          </div>

          {/* Donors list with lifecycle + chat + feedback */}
          {donations.length > 0 && (
            <div className="card">
              <h3 style={{ fontSize:16, marginBottom:16 }}>Donors who responded ({donations.length})</h3>
              <div style={{ display:'flex', flexDirection:'column', gap:14 }}>
                {donations.map(d => {
                  const stepIdx = STATUS_STEPS.indexOf(d.status);
                  const hasFeedback = d.feedback?.rating;
                  return (
                    <div key={d._id} style={{ padding:'14px 16px', background:'var(--bg-secondary)', borderRadius:10, border:'1px solid var(--border)' }}>
                      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:12 }}>
                        <div style={{ display:'flex', alignItems:'center', gap:10 }}>
                          <div style={{ width:34, height:34, borderRadius:'50%', background:'var(--red)', color:'#fff', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:700, fontSize:14 }}>
                            {d.donorId?.name?.[0]}
                          </div>
                          <div>
                            <p style={{ fontWeight:600, fontSize:14 }}>{d.donorId?.name}</p>
                            {d.donatedAt && <p style={{ fontSize:11, color:'var(--text-dim)' }}>Donated {timeAgo(d.donatedAt)}</p>}
                          </div>
                        </div>
                        <div style={{ display:'flex', gap:8 }}>
                          {d.status==='completed' && (
                            <a href={`/api/certificate/${d._id}?token=${localStorage.getItem("token")}`} target="_blank" rel="noopener noreferrer"
                              style={{ fontSize:12, background:'#f0fdf4', color:'#16a34a', border:'1px solid #bbf7d0', borderRadius:8, padding:'5px 10px', textDecoration:'none', fontWeight:600 }}>
                              📜 Certificate
                            </a>
                          )}
                          {/* Feature 1: Chat button */}
                          <Link to={`/chat/${d._id}`}
                            style={{ fontSize:12, background:'var(--red-dim)', color:'var(--red)', border:'1px solid var(--border)', borderRadius:8, padding:'5px 10px', textDecoration:'none', fontWeight:600 }}>
                            💬 Chat
                          </Link>
                        </div>
                      </div>

                      {/* Progress bar */}
                      <div style={{ display:'flex', alignItems:'center', marginBottom:12 }}>
                        {STATUS_STEPS.map((step,i) => (
                          <div key={step} style={{ display:'flex', alignItems:'center', flex:1 }}>
                            <div style={{ display:'flex', flexDirection:'column', alignItems:'center', flex:1 }}>
                              <div style={{ width:26, height:26, borderRadius:'50%', background: i<=stepIdx?'var(--red)':'var(--bg-raised)', border:`2px solid ${i<=stepIdx?'var(--red)':'var(--border)'}`, display:'flex', alignItems:'center', justifyContent:'center', fontSize:11, color:i<=stepIdx?'#fff':'var(--text-dim)', fontWeight:700 }}>
                                {i<=stepIdx?'✓':i+1}
                              </div>
                              <p style={{ fontSize:10, marginTop:3, color:i===stepIdx?'var(--red)':i<stepIdx?'var(--text-muted)':'var(--text-dim)', fontWeight:i===stepIdx?700:400 }}>{STATUS_LABELS[step]}</p>
                            </div>
                            {i<STATUS_STEPS.length-1 && <div style={{ height:2, flex:1, background:i<stepIdx?'var(--red)':'var(--border)', marginBottom:16 }} />}
                          </div>
                        ))}
                      </div>

                      <div style={{ display:'flex', gap:8, flexWrap:'wrap' }}>
                        {isOwner && d.status==='pledged' && (
                          <button className="btn btn-secondary" style={{ fontSize:12, padding:'5px 12px' }} onClick={()=>handleConfirm(d._id)}>✅ Confirm Appointment</button>
                        )}
                        {isOwner && d.status==='confirmed' && (
                          <button className="btn btn-success" style={{ fontSize:12, padding:'5px 12px' }} onClick={()=>handleComplete(d._id)}>🎉 Mark Donated</button>
                        )}
                        {/* Feature 9: Feedback */}
                        {isOwner && d.status==='completed' && !hasFeedback && (
                          <button className="btn btn-ghost" style={{ fontSize:12, padding:'5px 12px' }} onClick={()=>setFeedbackDonationId(d._id)}>⭐ Rate Donor</button>
                        )}
                        {hasFeedback && (
                          <div style={{ display:'flex', gap:2, alignItems:'center' }}>
                            {[1,2,3,4,5].map(s=><span key={s} style={{ fontSize:14, color:s<=d.feedback.rating?'#f59e0b':'#e5e7eb' }}>★</span>)}
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div style={{ display:'flex', flexDirection:'column', gap:16, position:'sticky', top:80 }}>
          {user?.role==='donor' && request.status==='open' && (
            <div className="card">
              <h3 style={{ fontSize:16, marginBottom:8 }}>Can you help?</h3>
              {eligibility && !eligibility.eligible ? (
                <div style={{ background:'#fffbeb', border:'1px solid #fde68a', borderRadius:8, padding:'12px 14px', marginBottom:12 }}>
                  <p style={{ color:'#d97706', fontWeight:600, fontSize:13, marginBottom:6 }}>⚠️ Not currently eligible</p>
                  {eligibility.issues.map((issue,i)=><p key={i} style={{ fontSize:12, color:'#92400e', lineHeight:1.5 }}>• {issue}</p>)}
                </div>
              ) : eligibility?.eligible ? (
                <div style={{ background:'#f0fdf4', border:'1px solid #bbf7d0', borderRadius:8, padding:'10px 14px', marginBottom:12 }}>
                  <p style={{ color:'#16a34a', fontWeight:600, fontSize:13 }}>✅ You are eligible</p>
                </div>
              ) : null}

              {responded ? (
                <div style={{ background:'var(--green-dim)', border:'1px solid rgba(26,122,74,0.2)', borderRadius:8, padding:'12px 16px', textAlign:'center' }}>
                  <p style={{ color:'var(--green)', fontWeight:600 }}>✓ You've responded!</p>
                  <p style={{ fontSize:13, color:'var(--text-muted)', marginTop:4 }}>The requester will contact you soon.</p>
                </div>
              ) : (
                <button className="btn btn-primary" onClick={handleRespond} disabled={responding||(eligibility&&!eligibility.eligible)} style={{ width:'100%', justifyContent:'center', padding:'13px' }}>
                  {responding ? <><div className="spinner" style={{ width:16, height:16 }} /> Submitting...</> : '🩸 I can donate'}
                </button>
              )}
            </div>
          )}

          {/* Feature 3: SOS Button */}
          {isOwner && request.status==='open' && !request.sosSent && (
            <div className="card" style={{ border:'2px solid #fecaca', background:'#fff5f5' }}>
              <p style={{ fontSize:13, fontWeight:700, color:'var(--red)', marginBottom:6 }}>🚨 Emergency SOS</p>
              <p style={{ fontSize:12, color:'var(--text-muted)', marginBottom:12, lineHeight:1.5 }}>
                Send an emergency alert to <strong>all</strong> compatible donors on the platform. Use only for life-threatening situations.
              </p>
              <button className="btn" onClick={handleSOS} disabled={sosSending} style={{ width:'100%', justifyContent:'center', background:'#dc2626', color:'#fff', border:'none' }}>
                {sosSending ? <><div className="spinner" style={{ width:14, height:14 }} /> Sending...</> : '🚨 Send SOS Broadcast'}
              </button>
            </div>
          )}
          {request.sosSent && (
            <div style={{ background:'#fef2f2', border:'2px solid #fecaca', borderRadius:12, padding:'12px 16px', textAlign:'center' }}>
              <p style={{ fontSize:13, fontWeight:700, color:'var(--red)' }}>🚨 SOS Broadcast Sent</p>
              <p style={{ fontSize:12, color:'var(--text-muted)', marginTop:4 }}>All compatible donors have been notified</p>
            </div>
          )}

          {/* AI Matching */}
          <div className="card">
            <h3 style={{ fontSize:16, marginBottom:8 }}>AI Donor Matching</h3>
            <button className="btn btn-secondary" onClick={async()=>{ setLoadingMatches(true); try{const r=await api.get(`/match/${id}`);setMatches(r.data);}catch{}finally{setLoadingMatches(false);} }} disabled={loadingMatches} style={{ width:'100%', justifyContent:'center' }}>
              {loadingMatches?<><div className="spinner" style={{ width:16,height:16 }}/> Finding...</>:'🤖 Find Matches'}
            </button>
            {matches.length>0 && (
              <div style={{ marginTop:14, display:'flex', flexDirection:'column', gap:8 }}>
                {matches.slice(0,5).map(({ donor, aiScore },i)=>(
                  <div key={donor._id} style={{ padding:'9px 12px', background:i===0?'var(--red-dim)':'var(--bg-secondary)', borderRadius:8, border:i===0?'1px solid var(--border)':'none' }}>
                    <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:3 }}>
                      <div style={{ display:'flex', alignItems:'center', gap:6 }}>
                        {i===0&&<span>🥇</span>}
                        <span style={{ fontWeight:600, fontSize:13 }}>{donor.userId?.name||'Anon'}</span>
                        <span className="blood-type-badge" style={{ width:26,height:26,fontSize:10 }}>{donor.bloodType}</span>
                      </div>
                      <span style={{ fontWeight:700, color:aiScore.score>=70?'var(--green)':'var(--text-muted)', fontSize:14 }}>{aiScore.score}%</span>
                    </div>
                    <p style={{ fontSize:11, color:'var(--text-dim)', lineHeight:1.4 }}>{aiScore.reason}</p>
                  </div>
                ))}
              </div>
            )}
          </div>

          {isOwner && request.status==='open' && (
            <button className="btn btn-success" onClick={async()=>{ await api.put(`/requests/${id}/status`,{status:'fulfilled'}); await reload(); }} style={{ width:'100%', justifyContent:'center' }}>
              ✓ Mark as Fulfilled
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
