import React, { useState } from 'react';

export default function StaffQueue({
  currentTicket,
  waitingCount,
  onCallNext,
  onComplete,
  onSkip,
  isCallingNext,
  isCompleting,
  isSkipping,
}) {
  const [remarks, setRemarks] = useState('');

  const handleComplete = () => {
    if (!currentTicket) return;
    onComplete(currentTicket.id, remarks);
    setRemarks('');
  };

  const handleSkip = () => {
    if (!currentTicket) return;
    onSkip(currentTicket.id, remarks);
    setRemarks('');
  };

  return (
    <div className="staff-action-box">
      {currentTicket ? (
        <div>
          <div className="serving-banner">Now Serving at Desk</div>
          <div className="serving-token-display">
            #{currentTicket.formattedToken || `TOKEN-${currentTicket.tokenNumber}`}
          </div>
          <div className="serving-student-info">
            Student: <strong>{currentTicket.userName || 'Student'}</strong> ({currentTicket.userEmail || 'No Email'})
          </div>

          <div style={{ maxWidth: '450px', margin: '0 auto 20px auto' }}>
            <input
              type="text"
              className="remarks-input"
              placeholder="Staff remarks (e.g. Fee receipt issued, Documents verified)..."
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
              disabled={isCompleting || isSkipping}
            />
          </div>

          <div className="staff-button-group">
            <button
              className="btn btn-success btn-lg"
              onClick={handleComplete}
              disabled={isCompleting || isSkipping}
            >
              {isCompleting ? (
                <>
                  <span className="spinner" /> Completing...
                </>
              ) : (
                '✔ Complete Ticket'
              )}
            </button>

            <button
              className="btn btn-warning btn-lg"
              onClick={handleSkip}
              disabled={isCompleting || isSkipping}
            >
              {isSkipping ? (
                <>
                  <span className="spinner" /> Skipping...
                </>
              ) : (
                '⏭ Skip (No-Show)'
              )}
            </button>

            <button
              className="btn btn-primary btn-lg"
              onClick={onCallNext}
              disabled={waitingCount === 0 || isCallingNext || isCompleting || isSkipping}
            >
              {isCallingNext ? (
                <>
                  <span className="spinner" /> Calling...
                </>
              ) : (
                '📢 Call Next Ticket'
              )}
            </button>
          </div>
        </div>
      ) : (
        <div>
          <div style={{ fontSize: '1.2rem', fontWeight: 600, color: 'var(--slate-600)', marginBottom: '16px' }}>
            {waitingCount > 0
              ? `${waitingCount} student(s) waiting in queue`
              : 'No students currently waiting for this counter.'}
          </div>

          <button
            className="btn btn-primary btn-lg"
            onClick={onCallNext}
            disabled={waitingCount === 0 || isCallingNext}
          >
            {isCallingNext ? (
              <>
                <span className="spinner" /> Calling Next...
              </>
            ) : (
              '📢 CALL NEXT STUDENT'
            )}
          </button>
        </div>
      )}
    </div>
  );
}
