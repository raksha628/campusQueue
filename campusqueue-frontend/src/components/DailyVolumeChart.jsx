import React from 'react';

export default function DailyVolumeChart({ dailyData }) {
  if (!dailyData || dailyData.length === 0) {
    return (
      <div className="chart-container">
        <h3 className="card-title" style={{ marginBottom: '14px' }}>Daily Ticket Volume</h3>
        <div className="empty-state" style={{ padding: '30px 20px' }}>
          <div className="empty-state-text">No daily volume data available yet.</div>
        </div>
      </div>
    );
  }

  const maxCount = Math.max(...dailyData.map((d) => d.ticketCount || 0), 1);

  const formatDateLabel = (dateStr) => {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      const monthIdx = parseInt(parts[1], 10) - 1;
      return `${parts[2]} ${months[monthIdx] || parts[1]}`;
    }
    return dateStr;
  };

  return (
    <div className="chart-container">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <div>
          <h3 className="card-title">Daily Ticket Volume</h3>
          <p style={{ color: 'var(--slate-500)', fontSize: '0.85rem' }}>
            Historical ticket generation volume per calendar date (PostgreSQL Aggregation)
          </p>
        </div>
        <span className="badge badge-called">{dailyData.length} Day(s) Recorded</span>
      </div>

      <div className="bar-chart">
        {dailyData.map((item, idx) => {
          const count = item.ticketCount || 0;
          const heightPercent = Math.max((count / maxCount) * 100, 6);

          return (
            <div key={idx} className="bar-group" title={`${item.date}: ${count} tickets`}>
              <div className="bar-value">{count}</div>
              <div className="bar-fill" style={{ height: `${heightPercent}%` }} />
              <div className="bar-label">{formatDateLabel(item.date)}</div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
