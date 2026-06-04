import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api, { socket } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function Chat() {
  const { donationId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [messages, setMessages] = useState([]);
  const [donation, setDonation] = useState(null);
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const bottomRef = useRef(null);

  useEffect(() => {
    api.get(`/chat/${donationId}`)
      .then(r => { setMessages(r.data.messages); setDonation(r.data.donation); })
      .catch(() => navigate(-1))
      .finally(() => setLoading(false));

    socket.connect();
    socket.emit('join_chat', donationId);
    socket.on('chat_message', (msg) => {
      setMessages(prev => [...prev, msg]);
    });

    return () => {
      socket.off('chat_message');
      socket.emit('leave_chat', donationId);
    };
  }, [donationId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const send = async () => {
    if (!text.trim() || sending) return;
    setSending(true);
    try {
      await api.post(`/chat/${donationId}`, { message: text.trim() });
      setText('');
    } catch (err) { alert(err.response?.data?.message || 'Failed to send'); }
    finally { setSending(false); }
  };

  const handleKey = (e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); } };

  if (loading) return <div style={{ display:'flex', justifyContent:'center', padding:80 }}><div className="spinner" style={{ width:36, height:36 }} /></div>;

  return (
    <div className="page-container" style={{ maxWidth: 700 }}>
      <button onClick={() => navigate(-1)} className="btn btn-ghost" style={{ marginBottom:16, fontSize:13 }}>← Back</button>

      <div className="card" style={{ padding:0, overflow:'hidden', display:'flex', flexDirection:'column', height:'70vh' }}>
        {/* Header */}
        <div style={{ padding:'16px 20px', background:'var(--red)', color:'#fff', display:'flex', alignItems:'center', gap:12 }}>
          <div style={{ width:40, height:40, borderRadius:'50%', background:'rgba(255,255,255,0.2)', display:'flex', alignItems:'center', justifyContent:'center', fontSize:18 }}>💬</div>
          <div>
            <p style={{ fontWeight:700, fontSize:15 }}>Donation Chat</p>
            <p style={{ fontSize:12, opacity:0.85 }}>
              {donation?.requestId?.bloodType} blood · {donation?.requestId?.hospital}
            </p>
          </div>
          <span style={{ marginLeft:'auto', fontSize:12, background:'rgba(255,255,255,0.2)', padding:'3px 10px', borderRadius:100 }}>
            Private
          </span>
        </div>

        {/* Messages */}
        <div style={{ flex:1, overflowY:'auto', padding:'20px 16px', display:'flex', flexDirection:'column', gap:10, background:'var(--bg-secondary)' }}>
          {messages.length === 0 && (
            <div style={{ textAlign:'center', color:'var(--text-dim)', fontSize:14, margin:'auto' }}>
              <p style={{ fontSize:24, marginBottom:8 }}>💬</p>
              <p>No messages yet. Start the conversation!</p>
              <p style={{ fontSize:12, marginTop:4 }}>Coordinate your donation appointment here.</p>
            </div>
          )}
          {messages.map(msg => {
            const isMine = msg.senderId?._id === user?.id || msg.senderId?._id?.toString() === user?.id;
            return (
              <div key={msg._id} style={{ display:'flex', justifyContent: isMine ? 'flex-end' : 'flex-start' }}>
                <div style={{ maxWidth:'72%' }}>
                  {!isMine && <p style={{ fontSize:11, color:'var(--text-dim)', marginBottom:3, paddingLeft:4 }}>{msg.senderId?.name}</p>}
                  <div style={{
                    background: isMine ? 'var(--red)' : '#fff',
                    color: isMine ? '#fff' : 'var(--text)',
                    padding:'10px 14px', borderRadius: isMine ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
                    fontSize:14, lineHeight:1.5,
                    boxShadow:'0 1px 4px rgba(0,0,0,0.08)',
                  }}>
                    {msg.message}
                  </div>
                  <p style={{ fontSize:10, color:'var(--text-dim)', marginTop:3, textAlign: isMine ? 'right' : 'left', paddingLeft:4 }}>
                    {new Date(msg.createdAt).toLocaleTimeString('en-IN', { hour:'2-digit', minute:'2-digit' })}
                  </p>
                </div>
              </div>
            );
          })}
          <div ref={bottomRef} />
        </div>

        {/* Input */}
        <div style={{ padding:'12px 16px', background:'#fff', borderTop:'1px solid var(--border)', display:'flex', gap:10, alignItems:'flex-end' }}>
          <textarea
            value={text}
            onChange={e => setText(e.target.value)}
            onKeyDown={handleKey}
            placeholder="Type a message... (Enter to send)"
            rows={1}
            style={{ flex:1, resize:'none', borderRadius:12, padding:'10px 14px', fontSize:14, border:'1.5px solid var(--border)', fontFamily:'var(--font-sans)', lineHeight:1.5, maxHeight:100, overflowY:'auto' }}
          />
          <button
            className="btn btn-primary"
            onClick={send}
            disabled={!text.trim() || sending}
            style={{ padding:'10px 20px', borderRadius:12, flexShrink:0 }}
          >
            {sending ? '...' : 'Send →'}
          </button>
        </div>
      </div>
    </div>
  );
}
