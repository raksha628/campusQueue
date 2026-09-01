import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import StudentQueue from './pages/StudentQueue';
import StaffQueuePage from './pages/StaffQueuePage';
import Analytics from './pages/Analytics';
import CounterManagement from './pages/CounterManagement';
import LoginPage from './pages/LoginPage';
import AccessDenied from './pages/AccessDenied';
import { AuthProvider, useAuth } from './context/AuthContext';
import './App.css';

function MainApp() {
  const { currentUser, role, isAuthenticated, isLoading } = useAuth();
  const [activeTab, setActiveTab] = useState('student');

  // Set appropriate default tab when role is resolved
  useEffect(() => {
    if (role === 'STAFF') {
      setActiveTab('staff');
    } else {
      setActiveTab('student');
    }
  }, [role]);

  // Loading Screen (prevents UI flash during session restoration)
  if (isLoading) {
    return (
      <div className="loading-screen">
        <div className="brand" style={{ marginBottom: '16px' }}>
          <div className="brand-icon">CQ</div>
          <span>CampusQueue</span>
        </div>
        <span className="spinner spinner-dark" />
        <div style={{ marginTop: '14px', color: 'var(--slate-500)', fontSize: '0.95rem' }}>
          Verifying security session...
        </div>
      </div>
    );
  }

  // If not authenticated, render Login Page
  if (!isAuthenticated) {
    return <LoginPage />;
  }

  const isStudent = role === 'STUDENT';
  const isStaff = role === 'STAFF';
  const isAdmin = role === 'ADMIN';

  // Render tab with Role-Based Route Guards
  const renderTabContent = () => {
    if (activeTab === 'student') {
      return <StudentQueue />;
    }

    if (activeTab === 'staff') {
      if (isStudent) {
        return <AccessDenied requiredRole="STAFF or ADMIN" onReturn={() => setActiveTab('student')} />;
      }
      return <StaffQueuePage />;
    }

    if (activeTab === 'analytics') {
      if (isStudent) {
        return <AccessDenied requiredRole="STAFF or ADMIN" onReturn={() => setActiveTab('student')} />;
      }
      return <Analytics />;
    }

    if (activeTab === 'counters') {
      if (!isAdmin) {
        return <AccessDenied requiredRole="ADMIN" onReturn={() => setActiveTab('student')} />;
      }
      return <CounterManagement />;
    }

    return <StudentQueue />;
  };

  return (
    <div className="app-container">
      <Navbar activeTab={activeTab} setActiveTab={setActiveTab} />

      <main className="main-content">
        {renderTabContent()}
      </main>

      <footer className="footer">
        <div>
          <strong>CampusQueue</strong> — Digital Queue Management System for College Offices & Service Desks.
        </div>
        <div style={{ marginTop: '4px', fontSize: '0.8rem', color: 'var(--slate-400)' }}>
          Session Authenticated • {currentUser?.name} ({currentUser?.role})
        </div>
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}
