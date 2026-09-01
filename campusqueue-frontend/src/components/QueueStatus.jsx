import React from 'react';

export default function QueueStatus({ statusInfo, onRefresh, isLoading }) {
  if (!statusInfo) return null;

  return (
    <div className="card" style={{ marginBottom: '24px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '14px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--slate-900)' }}>
              {statusInfo.counterName}
            </h2>
            <span className="counter-badge-code">#{statusInfo.counterCode}</span>
            <span className={`badge ${statusInfo.isActive ? 'badge-active' : 'badge-inactive'}`}>
              {statusInfo.isActive ? 'Open' : 'Closed'}
            </span>
          </div>
          <p style={{ color: 'var(--slate-500)', fontSize: '0.9rem', marginTop: '4px' }}>
            Average Service Duration: <strong>{statusInfo.averageServiceMinutes ?? 5} min/student</strong>
          </p>
        </div>

        <button
          className="btn btn-outline btn-sm"
          onClick={onRefresh}
          disabled={isLoading}
        >
          {isLoading ? <span className="spinner spinner-dark" /> : '🔄 Refresh Queue'}
        </button>
      </div>

      <div className="counter-stats-row" style={{ marginTop: '20px', marginBottom: 0 }}>
        <div className="stat-item">
          <span className="stat-label">Currently Serving</span>
          <span className="stat-value" style={{ color: 'var(--primary-700)' }}>
            {statusInfo.servingToken ? `#${statusInfo.servingToken}` : 'None'}
          </span>
        </div>
        <div className="stat-item">
          <span className="stat-label">Waiting Students</span>
          <span className="stat-value">{statusInfo.waitingCount ?? 0}</span>
        </div>
        <div className="stat-item">
          <span className="stat-label">Queue Est. Wait</span>
          <span className="stat-value">{statusInfo.estimatedWaitMinutes ?? 0} min</span>
        </div>
      </div>
    </div>
  );
}
