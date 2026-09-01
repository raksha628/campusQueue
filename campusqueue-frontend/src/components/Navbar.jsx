import React from 'react';
import { useAuth } from '../context/AuthContext';

export default function Navbar({ activeTab, setActiveTab }) {
  const { currentUser, role, logout } = useAuth();

  const isStudent = role === 'STUDENT';
  const isStaff = role === 'STAFF';
  const isAdmin = role === 'ADMIN';

  const roleBadgeClass = isStudent
    ? 'badge-waiting'
    : isStaff
    ? 'badge-called'
    : 'badge-completed';

  return (
    <header className="navbar">
      <div className="nav-content">
        <div className="brand" onClick={() => setActiveTab('student')} style={{ cursor: 'pointer' }}>
          <div className="brand-icon">CQ</div>
          <span>CampusQueue</span>
        </div>

        <nav className="nav-links">
          {/* Student Desk (Accessible to all authenticated users) */}
          <button
            className={`nav-item ${activeTab === 'student' ? 'active' : ''}`}
            onClick={() => setActiveTab('student')}
          >
            <span>🎓</span> Student Desk
          </button>

          {/* Staff Queue Desk (STAFF and ADMIN only) */}
          {(isStaff || isAdmin) && (
            <button
              className={`nav-item ${activeTab === 'staff' ? 'active' : ''}`}
              onClick={() => setActiveTab('staff')}
            >
              <span>🏢</span> Staff Queue
            </button>
          )}

          {/* Analytics (STAFF and ADMIN only) */}
          {(isStaff || isAdmin) && (
            <button
              className={`nav-item ${activeTab === 'analytics' ? 'active' : ''}`}
              onClick={() => setActiveTab('analytics')}
            >
              <span>📊</span> Analytics
            </button>
          )}

          {/* Counter Management (ADMIN only) */}
          {isAdmin && (
            <button
              className={`nav-item ${activeTab === 'counters' ? 'active' : ''}`}
              onClick={() => setActiveTab('counters')}
            >
              <span>⚙️</span> Counters
            </button>
          )}
        </nav>

        {/* Authenticated User Profile & Logout */}
        <div className="user-profile-bar">
          <div className="user-info-text">
            <span className="user-name-display">{currentUser?.name || 'User'}</span>
            <span className={`badge ${roleBadgeClass}`} style={{ fontSize: '0.7rem', padding: '1px 6px' }}>
              {currentUser?.role || 'STUDENT'}
            </span>
          </div>

          <button
            className="btn btn-outline btn-sm"
            onClick={logout}
            title="Sign out of CampusQueue"
          >
            Sign Out
          </button>
        </div>
      </div>
    </header>
  );
}
