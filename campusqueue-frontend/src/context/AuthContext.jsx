import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { getMe, login as apiLogin, logout as apiLogout } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // Restore authenticated session on page load
  const restoreSession = useCallback(async () => {
    try {
      setIsLoading(true);
      const user = await getMe();
      setCurrentUser(user);
    } catch {
      // Unauthenticated session (401)
      setCurrentUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    restoreSession();
  }, [restoreSession]);

  const login = async (email, password) => {
    const user = await apiLogin(email, password);
    setCurrentUser(user);
    return user;
  };

  const logout = async () => {
    try {
      await apiLogout();
    } finally {
      setCurrentUser(null);
    }
  };

  const value = {
    currentUser,
    role: currentUser?.role || null,
    isAuthenticated: Boolean(currentUser),
    isLoading,
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
