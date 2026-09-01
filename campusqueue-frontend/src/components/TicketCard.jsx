import React from 'react';

export default function TicketCard({ ticket, onRefresh, onCancel, isRefreshing, isCancelling }) {
  if (!ticket) return null;

  const isWaiting = ticket.status === 'WAITING';
  const isCalled = ticket.status === 'CALLED';
  const isCompleted = ticket.status === 'COMPLETED';
  const isSkipped = ticket.status === 'SKIPPED';
  const isCancelled = ticket.status === 'CANCELLED';

  const formatTime = (timeStr) => {
    if (!timeStr) return '—';
    const d = new Date(timeStr);
    return isNaN(d) ? timeStr : d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="ticket-active-hero">
      <div className="ticket-hero-grid">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
            <span style={{ fontSize: '0.85rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--slate-400)' }}>
              {ticket.counterName || 'Service Desk'}
            </span>
            <span
              className={`badge badge-${ticket.status?.toLowerCase()}`}
            >
              {ticket.status}
            </span>
          </div>

          <div className="ticket-token-large">
            #{ticket.formattedToken || `TOKEN-${ticket.tokenNumber}`}
          </div>

          <p style={{ color: 'var(--slate-300)', fontSize: '0.95rem' }}>
            Student: <strong>{ticket.userName || 'Student'}</strong>
          </p>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', minWidth: '160px' }}>
          <button
            className="btn btn-outline"
            style={{ backgroundColor: 'rgba(255,255,255,0.1)', color: '#ffffff', borderColor: 'rgba(255,255,255,0.2)' }}
            disabled={isRefreshing}
            onClick={onRefresh}
          >
            {isRefreshing ? <span className="spinner" /> : '🔄 Refresh Status'}
          </button>

          {isWaiting && (
            <button
              className="btn btn-danger btn-sm"
              disabled={isCancelling}
              onClick={() => onCancel(ticket.id)}
            >
              {isCancelling ? <span className="spinner" /> : '✖ Cancel Ticket'}
            </button>
          )}
        </div>
      </div>

      <div className="ticket-meta-grid">
        <div className="ticket-meta-item">
          <span className="ticket-meta-label">People Ahead</span>
          <span className="ticket-meta-val" style={{ color: ticket.peopleAhead === 0 ? 'var(--emerald-500)' : '#ffffff' }}>
            {ticket.peopleAhead ?? 0}
          </span>
        </div>

        <div className="ticket-meta-item">
          <span className="ticket-meta-label">Est. Wait</span>
          <span className="ticket-meta-val">
            {ticket.estimatedWaitMinutes ?? 0} min
          </span>
        </div>

        <div className="ticket-meta-item">
          <span className="ticket-meta-label">Issued At</span>
          <span className="ticket-meta-val" style={{ fontSize: '1.1rem' }}>
            {formatTime(ticket.createdAt)}
          </span>
        </div>
      </div>

      {ticket.remarks && (
        <div style={{ marginTop: '16px', padding: '10px', background: 'rgba(255,255,255,0.05)', borderRadius: '8px', fontSize: '0.88rem', color: 'var(--slate-300)' }}>
          <strong>Desk Remarks:</strong> {ticket.remarks}
        </div>
      )}
    </div>
  );
}
