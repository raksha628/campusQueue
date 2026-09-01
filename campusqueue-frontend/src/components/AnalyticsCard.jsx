import React from 'react';

export default function AnalyticsCard({ label, value, subtext, icon, accentColor }) {
  return (
    <div className="stat-card">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
        <span className="stat-card-label">{label}</span>
        {icon && <span style={{ fontSize: '1.25rem' }}>{icon}</span>}
      </div>

      <div className="stat-card-number" style={{ color: accentColor || 'var(--slate-900)' }}>
        {value ?? '—'}
      </div>

      {subtext && <div className="stat-card-sub">{subtext}</div>}
    </div>
  );
}
