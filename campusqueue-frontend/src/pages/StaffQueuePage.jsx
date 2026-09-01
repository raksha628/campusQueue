import React, { useState, useEffect, useCallback } from 'react';
import QueueStatus from '../components/QueueStatus';
import StaffQueue from '../components/StaffQueue';
import QueueTable from '../components/QueueTable';
import {
  getAllCounters,
  getQueueStatus,
  getWaitingTickets,
  getCurrentTicket,
  callNextTicket,
  callSpecificTicket,
  completeTicket,
  skipTicket,
} from '../services/api';

export default function StaffQueuePage() {
  const [counters, setCounters] = useState([]);
  const [selectedCounterId, setSelectedCounterId] = useState(null);
  const [queueStatus, setQueueStatus] = useState(null);
  const [currentTicket, setCurrentTicket] = useState(null);
  const [waitingTickets, setWaitingTickets] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCallingNext, setIsCallingNext] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);
  const [isSkipping, setIsSkipping] = useState(false);
  const [isCallingSpecificId, setIsCallingSpecificId] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // 1. Load all available counters
  useEffect(() => {
    async function loadCounters() {
      try {
        const data = await getAllCounters();
        setCounters(data || []);
        if (data && data.length > 0) {
          setSelectedCounterId(data[0].id);
        }
      } catch (err) {
        setErrorMessage(err.message || 'Failed to load service counters');
      } finally {
        setIsLoading(false);
      }
    }
    loadCounters();
  }, []);

  // 2. Load queue data for the selected counter
  const loadQueueData = useCallback(async (counterId) => {
    if (!counterId) return;
    try {
      setErrorMessage('');
      const [statusData, waitingData, currentData] = await Promise.all([
        getQueueStatus(counterId).catch(() => null),
        getWaitingTickets(counterId).catch(() => []),
        getCurrentTicket(counterId).catch(() => null),
      ]);

      setQueueStatus(statusData);
      setWaitingTickets(waitingData || []);
      setCurrentTicket(currentData || null);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to load desk queue data');
    }
  }, []);

  useEffect(() => {
    if (selectedCounterId) {
      loadQueueData(selectedCounterId);
    }
  }, [selectedCounterId, loadQueueData]);

  // Actions
  const handleCallNext = async () => {
    if (!selectedCounterId) return;
    try {
      setIsCallingNext(true);
      setErrorMessage('');
      setSuccessMessage('');

      const called = await callNextTicket(selectedCounterId);
      setCurrentTicket(called);
      setSuccessMessage(`Called Ticket #${called.formattedToken} (${called.userName})`);
      await loadQueueData(selectedCounterId);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to call next student');
    } finally {
      setIsCallingNext(false);
    }
  };

  const handleCallSpecific = async (ticketId) => {
    try {
      setIsCallingSpecificId(ticketId);
      setErrorMessage('');
      setSuccessMessage('');

      const called = await callSpecificTicket(ticketId);
      setCurrentTicket(called);
      setSuccessMessage(`Called Ticket #${called.formattedToken} (${called.userName})`);
      await loadQueueData(selectedCounterId);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to call ticket');
    } finally {
      setIsCallingSpecificId(null);
    }
  };

  const handleComplete = async (ticketId, remarks) => {
    try {
      setIsCompleting(true);
      setErrorMessage('');
      setSuccessMessage('');

      const completed = await completeTicket(ticketId, remarks);
      setSuccessMessage(`Completed Ticket #${completed.formattedToken}!`);
      setCurrentTicket(null);
      await loadQueueData(selectedCounterId);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to complete ticket');
    } finally {
      setIsCompleting(false);
    }
  };

  const handleSkip = async (ticketId, remarks) => {
    try {
      setIsSkipping(true);
      setErrorMessage('');
      setSuccessMessage('');

      const skipped = await skipTicket(ticketId, remarks);
      setSuccessMessage(`Skipped Ticket #${skipped.formattedToken} (No-show recorded).`);
      setCurrentTicket(null);
      await loadQueueData(selectedCounterId);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to skip ticket');
    } finally {
      setIsSkipping(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Staff Queue Desk</h1>
          <p className="page-subtitle">
            Manage student queues, call next tokens in sequence, and mark service completions.
          </p>
        </div>

        <div className="counter-picker">
          <label htmlFor="staff-counter-select"><strong>Service Desk:</strong></label>
          <select
            id="staff-counter-select"
            value={selectedCounterId || ''}
            onChange={(e) => setSelectedCounterId(Number(e.target.value))}
            disabled={isLoading}
          >
            {counters.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} (#{c.code}) {!c.isActive ? '— Closed' : ''}
              </option>
            ))}
          </select>
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

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '60px' }}>
          <span className="spinner spinner-dark" />
          <div style={{ marginTop: '12px', color: 'var(--slate-500)' }}>
            Loading staff desk...
          </div>
        </div>
      ) : (
        <>
          {/* Desk Summary Header */}
          <QueueStatus
            statusInfo={queueStatus}
            onRefresh={() => loadQueueData(selectedCounterId)}
            isLoading={false}
          />

          {/* Action Control Box (Call Next / Complete / Skip) */}
          <StaffQueue
            currentTicket={currentTicket}
            waitingCount={waitingTickets.length}
            onCallNext={handleCallNext}
            onComplete={handleComplete}
            onSkip={handleSkip}
            isCallingNext={isCallingNext}
            isCompleting={isCompleting}
            isSkipping={isSkipping}
          />

          {/* Waiting Queue List */}
          <div style={{ marginTop: '30px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--slate-800)' }}>
                Waiting Students Queue ({waitingTickets.length})
              </h2>
              <span className="badge badge-waiting">FIFO Order</span>
            </div>

            <QueueTable
              waitingTickets={waitingTickets}
              onCallSpecific={handleCallSpecific}
              isCalling={isCallingSpecificId !== null}
            />
          </div>
        </>
      )}
    </div>
  );
}
