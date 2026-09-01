import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import StudentQueue from './pages/StudentQueue';
import StaffQueuePage from './pages/StaffQueuePage';
import Analytics from './pages/Analytics';
import CounterManagement from './pages/CounterManagement';
import { getAllUsers, createUser } from './services/api';
import './App.css';

export default function App() {
  const [activeTab, setActiveTab] = useState('student');
  const [users, setUsers] = useState([]);
  const [currentUser, setCurrentUser] = useState(null);
  const [isLoadingUsers, setIsLoadingUsers] = useState(true);

  // Fetch users and initialize active student session
  useEffect(() => {
    async function initUsers() {
      try {
        let userList = await getAllUsers();
        if (!userList || userList.length === 0) {
          // If no users exist yet, seed a default student
          const defaultStudent = await createUser({
            name: 'Pooja Hegde',
            email: 'pooja.student@college.edu',
            role: 'STUDENT',
          });
          userList = [defaultStudent];
        }
        setUsers(userList);
        setCurrentUser(userList[0]);
      } catch (err) {
        console.error('Failed to fetch initial users:', err);
      } finally {
        setIsLoadingUsers(false);
      }
    }
    initUsers();
  }, []);

  return (
    <div className="app-container">
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        users={users}
        currentUser={currentUser}
        setCurrentUser={setCurrentUser}
      />

      <main className="main-content">
        {activeTab === 'student' && <StudentQueue currentUser={currentUser} />}
        {activeTab === 'staff' && <StaffQueuePage currentUser={currentUser} />}
        {activeTab === 'analytics' && <Analytics />}
        {activeTab === 'counters' && <CounterManagement />}
      </main>

      <footer className="footer">
        <div>
          <strong>CampusQueue</strong> — Digital Queue Management System for College Offices & Service Desks.
        </div>
        <div style={{ marginTop: '4px', fontSize: '0.8rem', color: 'var(--slate-400)' }}>
          Powered by React + Vite + Spring Boot + PostgreSQL
        </div>
      </footer>
    </div>
  );
}
