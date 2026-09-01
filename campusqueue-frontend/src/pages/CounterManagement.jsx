import React, { useState, useEffect, useCallback } from 'react';
import { getAllCounters, toggleCounterStatus, createCounter, getQueueStatus } from '../services/api';

export default function CounterManagement() {
  const [counters, setCounters] = useState([]);
  const [queuesMap, setQueuesMap] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [isTogglingId, setIsTogglingId] = useState(null);
  const [isCreating, setIsCreating] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Form state
  const [name, setName] = useState('');
  const [code, setCode] = useState('');
  const [description, setDescription] = useState('');
  const [showAddForm, setShowAddForm] = useState(false);

  const loadCountersAndStatus = useCallback(async () => {
    try {
      setErrorMessage('');
      const data = await getAllCounters();
      setCounters(data || []);

      const qPromises = (data || []).map(async (c) => {
        try {
          const status = await getQueueStatus(c.id);
          return { counterId: c.id, status };
        } catch {
          return { counterId: c.id, status: null };
        }
      });

      const qResults = await Promise.all(qPromises);
      const qMap = {};
      qResults.forEach((res) => {
        if (res.status) qMap[res.counterId] = res.status;
      });
      setQueuesMap(qMap);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to load counters');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCountersAndStatus();
  }, [loadCountersAndStatus]);

  const handleToggle = async (counterId) => {
    try {
      setIsTogglingId(counterId);
      setErrorMessage('');
      setSuccessMessage('');

      const updated = await toggleCounterStatus(counterId);
      setSuccessMessage(`Counter '${updated.name}' is now ${updated.isActive ? 'Active (Open)' : 'Inactive (Closed)'}.`);
      await loadCountersAndStatus();
    } catch (err) {
      setErrorMessage(err.message || 'Failed to toggle counter status');
    } finally {
      setIsTogglingId(null);
    }
  };

  const handleCreateCounter = async (e) => {
    e.preventDefault();
    if (!name.trim() || !code.trim()) {
      setErrorMessage('Counter name and code are required');
      return;
    }

    try {
      setIsCreating(true);
      setErrorMessage('');
      setSuccessMessage('');

      const created = await createCounter({
        name: name.trim(),
        code: code.trim().toUpperCase(),
        description: description.trim(),
      });

      setSuccessMessage(`Service desk '${created.name}' (${created.code}) created successfully!`);
      setName('');
      setCode('');
      setDescription('');
      setShowAddForm(false);
      await loadCountersAndStatus();
    } catch (err) {
      setErrorMessage(err.message || 'Failed to create counter');
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Service Desk Administration</h1>
          <p className="page-subtitle">
            Manage college offices, open/close counters, and monitor desk workloads.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            className="btn btn-primary"
            onClick={() => setShowAddForm(!showAddForm)}
          >
            {showAddForm ? '✕ Close Form' : '+ Add Service Desk'}
          </button>
          <button className="btn btn-outline" onClick={loadCountersAndStatus} disabled={isLoading}>
            🔄 Refresh
          </button>
        </div>
      </div>

      {errorMessage && (
        <div className="alert alert-error">
          <span>⚠️</span>
          <div>{errorMessage}</div>
        </div>
      )}

      {successMessage && (
        <div className="alert alert-success">
          <span>✅</span>
          <div>{successMessage}</div>
        </div>
      )}

      {/* Add New Counter Form Card */}
      {showAddForm && (
        <div className="card" style={{ marginBottom: '28px', border: '2px solid var(--primary-200)' }}>
          <h3 className="card-title" style={{ marginBottom: '16px' }}>Register New Campus Service Desk</h3>
          <form onSubmit={handleCreateCounter}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px', marginBottom: '16px' }}>
              <div>
                <label htmlFor="desk-name" style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '6px' }}>
                  Desk Name *
                </label>
                <input
                  id="desk-name"
                  type="text"
                  placeholder="e.g. Scholarship Cell"
                  className="remarks-input"
                  style={{ maxWidth: 'none', margin: 0 }}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
              </div>

              <div>
                <label htmlFor="desk-code" style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '6px' }}>
                  Desk Code (2-10 chars) *
                </label>
                <input
                  id="desk-code"
                  type="text"
                  placeholder="e.g. SCH"
                  className="remarks-input"
                  style={{ maxWidth: 'none', margin: 0, textTransform: 'uppercase' }}
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  maxLength={10}
                  required
                />
              </div>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label htmlFor="desk-desc" style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '6px' }}>
                Description
              </label>
              <input
                id="desk-desc"
                type="text"
                placeholder="e.g. State and national scholarship verification"
                className="remarks-input"
                style={{ maxWidth: 'none', margin: 0 }}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', gap: '10px' }}>
              <button type="submit" className="btn btn-primary" disabled={isCreating}>
                {isCreating ? <span className="spinner" /> : 'Save Service Desk'}
              </button>
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => setShowAddForm(false)}
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '60px' }}>
          <span className="spinner spinner-dark" />
          <div style={{ marginTop: '12px', color: 'var(--slate-500)' }}>
            Loading service desks...
          </div>
        </div>
      ) : (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Code</th>
                <th>Desk Name</th>
                <th>Description</th>
                <th>Waiting Students</th>
                <th>Serving Token</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {counters.map((c) => {
                const qInfo = queuesMap[c.id];
                return (
                  <tr key={c.id}>
                    <td>
                      <span className="counter-badge-code">#{c.code}</span>
                    </td>
                    <td>
                      <strong>{c.name}</strong>
                    </td>
                    <td style={{ color: 'var(--slate-500)', fontSize: '0.88rem' }}>
                      {c.description || '—'}
                    </td>
                    <td>
                      <strong style={{ color: qInfo?.waitingCount > 0 ? 'var(--amber-600)' : 'var(--slate-600)' }}>
                        {qInfo?.waitingCount ?? 0}
                      </strong>
                    </td>
                    <td>
                      {qInfo?.servingToken ? (
                        <span className="badge badge-called">#{qInfo.servingToken}</span>
                      ) : (
                        <span style={{ color: 'var(--slate-400)' }}>None</span>
                      )}
                    </td>
                    <td>
                      <span className={`badge ${c.isActive ? 'badge-active' : 'badge-inactive'}`}>
                        {c.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        className={`btn btn-sm ${c.isActive ? 'btn-danger' : 'btn-success'}`}
                        disabled={isTogglingId === c.id}
                        onClick={() => handleToggle(c.id)}
                      >
                        {isTogglingId === c.id ? (
                          <span className="spinner" />
                        ) : c.isActive ? (
                          'Deactivate'
                        ) : (
                          'Activate'
                        )}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
