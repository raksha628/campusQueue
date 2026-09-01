import React from 'react';

export default function QueueTable({ waitingTickets, onCallSpecific, isCalling }) {
  if (!waitingTickets || waitingTickets.length === 0) {
    return (
      <div className="table-container">
        <div className="empty-state">
          <div className="empty-state-icon">📋</div>
          <div className="empty-state-text">No students are currently waiting in this queue.</div>
        </div>
      </div>
    );
  }

  const formatTime = (timeStr) => {
    if (!timeStr) return '—';
    const d = new Date(timeStr);
    return isNaN(d) ? timeStr : d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="table-container">
      <table>
        <thead>
          <tr>
            <th>Pos</th>
            <th>Token</th>
            <th>Student</th>
            <th>Email</th>
            <th>Time Issued</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {waitingTickets.map((ticket, idx) => (
            <tr key={ticket.id}>
              <td>
                <span style={{ fontWeight: 700, color: 'var(--slate-400)' }}>
                  #{idx + 1}
                </span>
              </td>
              <td>
                <strong style={{ color: 'var(--primary-700)', fontSize: '1rem' }}>
                  #{ticket.formattedToken || `TOKEN-${ticket.tokenNumber}`}
                </strong>
              </td>
              <td>
                <strong>{ticket.userName || 'Student'}</strong>
              </td>
              <td style={{ color: 'var(--slate-500)' }}>
                {ticket.userEmail || '—'}
              </td>
              <td>{formatTime(ticket.createdAt)}</td>
              <td>
                <button
                  className="btn btn-outline btn-sm"
                  disabled={isCalling}
                  onClick={() => onCallSpecific(ticket.id)}
                >
                  Call Now
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
