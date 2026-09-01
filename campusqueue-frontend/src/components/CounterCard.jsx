import React from 'react';

export default function CounterCard({ counter, queueInfo, onTakeToken, isProcessing }) {
  const isActive = counter.isActive !== false;
  const waitingCount = queueInfo?.waitingCount ?? 0;
  const estimatedWait = queueInfo?.estimatedWaitMinutes ?? 0;
  const servingToken = queueInfo?.servingToken;

  return (
    <div className={`card card-hover ${!isActive ? 'opacity-75' : ''}`}>
      <div className="counter-card-header">
        <div>
          <h3 className="card-title">{counter.name}</h3>
          <span className="counter-badge-code">#{counter.code}</span>
        </div>
        <span className={`badge ${isActive ? 'badge-active' : 'badge-inactive'}`}>
          {isActive ? 'Active' : 'Closed'}
        </span>
      </div>

      <p className="counter-desc">
        {counter.description || 'General student service and inquiries counter.'}
      </p>

      <div className="counter-stats-row">
        <div className="stat-item">
          <span className="stat-label">Waiting</span>
          <span className="stat-value">{waitingCount}</span>
        </div>
        <div className="stat-item">
          <span className="stat-label">Now Serving</span>
          <span className="stat-value" style={{ fontSize: '1rem', color: 'var(--primary-600)' }}>
            {servingToken ? `#${servingToken}` : '—'}
          </span>
        </div>
        <div className="stat-item">
          <span className="stat-label">Est. Wait</span>
          <span className="stat-value">{estimatedWait} min</span>
        </div>
      </div>

      <button
        className="btn btn-primary"
        style={{ width: '100%' }}
        disabled={!isActive || isProcessing}
        onClick={() => onTakeToken(counter.id)}
      >
        {isProcessing ? (
          <>
            <span className="spinner" /> Generating Token...
          </>
        ) : (
          'Take Token'
        )}
      </button>
    </div>
  );
}
