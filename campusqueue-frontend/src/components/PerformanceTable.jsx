import React from 'react';

export default function PerformanceTable({ performanceData, selectedDate, onDateChange, onClearDate, isLoading }) {
  return (
    <div className="card">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '14px', marginBottom: '20px' }}>
        <div>
          <h3 className="card-title">Counter Performance & Service Times</h3>
          <p style={{ color: 'var(--slate-500)', fontSize: '0.85rem' }}>
            Computed in PostgreSQL using conditional aggregation (CASE WHEN)
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <label htmlFor="filter-date" style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--slate-600)' }}>
            Filter Date:
          </label>
          <input
            id="filter-date"
            type="date"
            value={selectedDate || ''}
            onChange={(e) => onDateChange(e.target.value)}
            style={{
              padding: '6px 10px',
              border: '1px solid var(--slate-300)',
              borderRadius: 'var(--radius-sm)',
              fontSize: '0.85rem',
            }}
          />
          {selectedDate && (
            <button className="btn btn-outline btn-sm" onClick={onClearDate}>
              Clear
            </button>
          )}
        </div>
      </div>

      <div className="table-container" style={{ border: 'none', boxShadow: 'none' }}>
        {isLoading ? (
          <div style={{ textAlign: 'center', padding: '30px' }}>
            <span className="spinner spinner-dark" /> Loading performance metrics...
          </div>
        ) : !performanceData || performanceData.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-text">No performance metrics recorded for this date.</div>
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Counter</th>
                <th style={{ textAlign: 'right' }}>Total</th>
                <th style={{ textAlign: 'right' }}>Completed</th>
                <th style={{ textAlign: 'right' }}>Skipped</th>
                <th style={{ textAlign: 'right' }}>Cancelled</th>
                <th style={{ textAlign: 'right' }}>Avg Wait</th>
                <th style={{ textAlign: 'right' }}>Avg Service</th>
              </tr>
            </thead>
            <tbody>
              {performanceData.map((row) => (
                <tr key={row.counterId}>
                  <td>
                    <strong>{row.counterName}</strong>{' '}
                    <span className="counter-badge-code">#{row.counterCode}</span>
                  </td>
                  <td style={{ textAlign: 'right', fontWeight: 700 }}>
                    {row.totalTickets ?? 0}
                  </td>
                  <td style={{ textAlign: 'right', color: 'var(--emerald-600)', fontWeight: 600 }}>
                    {row.completedTickets ?? 0}
                  </td>
                  <td style={{ textAlign: 'right', color: 'var(--amber-600)' }}>
                    {row.skippedTickets ?? 0}
                  </td>
                  <td style={{ textAlign: 'right', color: 'var(--rose-600)' }}>
                    {row.cancelledTickets ?? 0}
                  </td>
                  <td style={{ textAlign: 'right', fontWeight: 600 }}>
                    {row.averageWaitMinutes ?? 0} min
                  </td>
                  <td style={{ textAlign: 'right', fontWeight: 600 }}>
                    {row.averageServiceMinutes ?? 0} min
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
