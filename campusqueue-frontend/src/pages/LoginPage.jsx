import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      setErrorMessage('Please enter both email and password.');
      return;
    }

    try {
      setIsSubmitting(true);
      setErrorMessage('');
      await login(email.trim(), password);
    } catch (err) {
      setErrorMessage(err.message || 'Login failed. Please verify your credentials.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDemoFill = (demoEmail, demoPass) => {
    setEmail(demoEmail);
    setPassword(demoPass);
    setErrorMessage('');
  };

  return (
    <div className="login-page-wrapper">
      <div className="login-card">
        <div className="login-header">
          <div className="brand" style={{ justifyContent: 'center', marginBottom: '8px' }}>
            <div className="brand-icon">CQ</div>
            <span>CampusQueue</span>
          </div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--slate-900)' }}>
            Sign in to your account
          </h2>
          <p style={{ color: 'var(--slate-500)', fontSize: '0.9rem', marginTop: '4px' }}>
            Digital Queue Management System for College Offices
          </p>
        </div>

        {errorMessage && (
          <div className="alert alert-error" style={{ marginBottom: '18px' }}>
            <span>⚠️</span>
            <div>{errorMessage}</div>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '16px' }}>
            <label htmlFor="login-email" style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--slate-700)', marginBottom: '6px' }}>
              College Email
            </label>
            <input
              id="login-email"
              type="email"
              placeholder="e.g. rahul.sharma@college.edu"
              className="remarks-input"
              style={{ maxWidth: 'none', margin: 0 }}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={isSubmitting}
              required
            />
          </div>

          <div style={{ marginBottom: '22px' }}>
            <label htmlFor="login-pass" style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--slate-700)', marginBottom: '6px' }}>
              Password
            </label>
            <input
              id="login-pass"
              type="password"
              placeholder="••••••••"
              className="remarks-input"
              style={{ maxWidth: 'none', margin: 0 }}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={isSubmitting}
              required
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-lg"
            style={{ width: '100%', marginBottom: '20px' }}
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <>
                <span className="spinner" /> Signing In...
              </>
            ) : (
              'Sign In'
            )}
          </button>
        </form>

        {/* Demo Fast-Login Helpers */}
        <div style={{ borderTop: '1px solid var(--slate-200)', paddingTop: '16px', marginTop: '10px' }}>
          <div style={{ fontSize: '0.78rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--slate-400)', fontWeight: 700, marginBottom: '10px', textAlign: 'center' }}>
            Quick Demo Accounts (1-Click Fill)
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <button
              type="button"
              className="demo-pill"
              onClick={() => handleDemoFill('rahul.sharma@college.edu', 'student123')}
            >
              <span className="badge badge-waiting">STUDENT</span>
              <span>Rahul Sharma (student123)</span>
            </button>

            <button
              type="button"
              className="demo-pill"
              onClick={() => handleDemoFill('sunita.rao@college.edu', 'staff123')}
            >
              <span className="badge badge-called">STAFF</span>
              <span>Dr. Sunita Rao (staff123)</span>
            </button>

            <button
              type="button"
              className="demo-pill"
              onClick={() => handleDemoFill('admin@college.edu', 'admin123')}
            >
              <span className="badge badge-completed">ADMIN</span>
              <span>System Admin (admin123)</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
