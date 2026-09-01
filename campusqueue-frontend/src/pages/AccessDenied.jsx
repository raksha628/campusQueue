import React from 'react';

export default function AccessDenied({ requiredRole, onReturn }) {
  return (
    <div className="card" style={{ maxWidth: '600px', margin: '60px auto', textAlign: 'center', padding: '40px 30px' }}>
      <div style={{ fontSize: '3rem', marginBottom: '16px' }}>🚫</div>
      <h2 style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--slate-900)', marginBottom: '10px' }}>
        Access Denied (403 Forbidden)
      </h2>
      <p style={{ color: 'var(--slate-600)', fontSize: '1rem', lineHeight: 1.6, marginBottom: '24px' }}>
        You do not have the required permissions ({requiredRole}) to access this section of CampusQueue.
      </p>

      <button className="btn btn-primary" onClick={onReturn}>
        ← Return to Authorized Desk
      </button>
    </div>
  );
}
