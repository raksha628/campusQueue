import React from 'react';

export default function Navbar({ activeTab, setActiveTab, users, currentUser, setCurrentUser }) {
  return (
    <header className="navbar">
      <div className="nav-content">
        <div className="brand">
          <div className="brand-icon">CQ</div>
          <span>CampusQueue</span>
        </div>

        <nav className="nav-links">
          <button
            className={`nav-item ${activeTab === 'student' ? 'active' : ''}`}
            onClick={() => setActiveTab('student')}
          >
            <span>🎓</span> Student Desk
          </button>
          <button
            className={`nav-item ${activeTab === 'staff' ? 'active' : ''}`}
            onClick={() => setActiveTab('staff')}
          >
            <span>🏢</span> Staff Queue
          </button>
          <button
            className={`nav-item ${activeTab === 'analytics' ? 'active' : ''}`}
            onClick={() => setActiveTab('analytics')}
          >
            <span>📊</span> Analytics
          </button>
          <button
            className={`nav-item ${activeTab === 'counters' ? 'active' : ''}`}
            onClick={() => setActiveTab('counters')}
          >
            <span>⚙️</span> Counters
          </button>
        </nav>

        <div className="user-selector-bar">
          <label htmlFor="user-select"><strong>User:</strong></label>
          <select
            id="user-select"
            value={currentUser?.id || ''}
            onChange={(e) => {
              const selected = users.find((u) => u.id === Number(e.target.value));
              if (selected) setCurrentUser(selected);
            }}
          >
            {users.length === 0 ? (
              <option value="">No users found</option>
            ) : (
              users.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.name} ({u.role})
                </option>
              ))
            )}
          </select>
        </div>
      </div>
    </header>
  );
}
