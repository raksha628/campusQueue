import React, { useState, useEffect, useCallback } from 'react';
import CounterCard from '../components/CounterCard';
import TicketCard from '../components/TicketCard';
import { getActiveCounters, getQueueStatus, createTicket, getUserTickets, getTicketById, cancelTicket } from '../services/api';

export default function StudentQueue({ currentUser }) {
  const [counters, setCounters] = useState([]);
  const [queuesMap, setQueuesMap] = useState({});
  const [activeTicket, setActiveTicket] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isTakingTokenId, setIsTakingTokenId] = useState(null);
  const [isRefreshingTicket, setIsRefreshingTicket] = useState(false);
  const [isCancellingTicket, setIsCancellingTicket] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Load active counters and their current queue state
  const loadCountersAndQueues = useCallback(async () => {
    try {
      setErrorMessage('');
      const activeCounters = await getActiveCounters();
      setCounters(activeCounters || []);

      // Fetch queue metrics for each active counter in parallel
      const queuePromises = (activeCounters || []).map(async (c) => {
        try {
          const status = await getQueueStatus(c.id);
          return { counterId: c.id, status };
        } catch {
          return { counterId: c.id, status: null };
        }
      });

      const queueResults = await Promise.all(queuePromises);
      const qMap = {};
      queueResults.forEach((res) => {
        if (res.status) qMap[res.counterId] = res.status;
      });
      setQueuesMap(qMap);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to load active counters');
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Check if current user has an active ticket (WAITING or CALLED)
  const checkUserActiveTicket = useCallback(async () => {
    if (!currentUser?.id) return;
    try {
      const userTickets = await getUserTickets(currentUser.id);
      if (userTickets && userTickets.length > 0) {
        // Find most recent active ticket
        const currentActive = userTickets.find(
          (t) => t.status === 'WAITING' || t.status === 'CALLED'
        );
        if (currentActive) {
          // Fetch fresh ticket data with current people ahead and wait estimate
          const freshTicket = await getTicketById(currentActive.id);
          setActiveTicket(freshTicket);
        } else {
          setActiveTicket(null);
        }
      } else {
        setActiveTicket(null);
      }
    } catch {
      // Ignored for graceful fallback
    }
  }, [currentUser]);

  useEffect(() => {
    loadCountersAndQueues();
  }, [loadCountersAndQueues]);

  useEffect(() => {
    checkUserActiveTicket();
  }, [checkUserActiveTicket]);

  const handleTakeToken = async (counterId) => {
    if (!currentUser?.id) {
      setErrorMessage('Please select a student user from the top navigation first.');
      return;
    }

    try {
      setIsTakingTokenId(counterId);
      setErrorMessage('');
      setSuccessMessage('');

      const newTicket = await createTicket(counterId, currentUser.id);
      setActiveTicket(newTicket);
      setSuccessMessage(`Token #${newTicket.formattedToken} issued successfully for ${newTicket.counterName}!`);

      // Refresh queue counts
      await loadCountersAndQueues();
    } catch (err) {
      setErrorMessage(err.message || 'Failed to take token');
    } finally {
      setIsTakingTokenId(null);
    }
  };

  const handleRefreshTicket = async () => {
    if (!activeTicket?.id) return;
    try {
      setIsRefreshingTicket(true);
      setErrorMessage('');
      const refreshed = await getTicketById(activeTicket.id);
      setActiveTicket(refreshed);
      await loadCountersAndQueues();
    } catch (err) {
      setErrorMessage(err.message || 'Failed to refresh ticket status');
    } finally {
      setIsRefreshingTicket(false);
    }
  };

  const handleCancelTicket = async (ticketId) => {
    if (!window.confirm('Are you sure you want to cancel this waiting token?')) return;
    try {
      setIsCancellingTicket(true);
      setErrorMessage('');
      const cancelled = await cancelTicket(ticketId);
      setActiveTicket(cancelled);
      setSuccessMessage('Ticket cancelled successfully.');
      await loadCountersAndQueues();
    } catch (err) {
      setErrorMessage(err.message || 'Failed to cancel ticket');
    } finally {
      setIsCancellingTicket(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Campus Service Desks</h1>
          <p className="page-subtitle">
            Take a digital queue token for college offices. No standing in physical queues.
          </p>
        </div>

        <button
          className="btn btn-outline"
          onClick={() => {
            loadCountersAndQueues();
            checkUserActiveTicket();
          }}
          disabled={isLoading}
        >
          🔄 Refresh Desks
        </button>
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

      {/* Active Ticket Banner */}
      {activeTicket && (
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '14px', color: 'var(--slate-800)' }}>
            Your Active Ticket
          </h2>
          <TicketCard
            ticket={activeTicket}
            onRefresh={handleRefreshTicket}
            onCancel={handleCancelTicket}
            isRefreshing={isRefreshingTicket}
            isCancelling={isCancellingTicket}
          />
        </div>
      )}

      {/* Available Counters */}
      <h2 style={{ fontSize: '1.25rem', fontWeight: 800, marginBottom: '16px', color: 'var(--slate-800)' }}>
        Available Counters
      </h2>

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '60px 20px' }}>
          <span className="spinner spinner-dark" />
          <div style={{ marginTop: '12px', color: 'var(--slate-500)', fontWeight: 500 }}>
            Loading campus service desks...
          </div>
        </div>
      ) : counters.length === 0 ? (
        <div className="card empty-state">
          <div className="empty-state-icon">🏢</div>
          <div className="empty-state-text">No active service counters found.</div>
        </div>
      ) : (
        <div className="card-grid">
          {counters.map((counter) => (
            <CounterCard
              key={counter.id}
              counter={counter}
              queueInfo={queuesMap[counter.id]}
              onTakeToken={handleTakeToken}
              isProcessing={isTakingTokenId === counter.id}
            />
          ))}
        </div>
      )}
    </div>
  );
}
