import React, { createContext, useContext, useState, useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import api from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('user');
    return savedUser ? JSON.parse(savedUser) : null;
  });

  const [isAuthenticated, setIsAuthenticated] = useState(() => {
    return !!localStorage.getItem('user');
  });

  useEffect(() => {
    const handleLogout = () => {
      localStorage.removeItem('user');
      setUser(null);
      setIsAuthenticated(false);
      queryClient.clear();
    };

    window.addEventListener('auth:logout', handleLogout);
    setIsLoading(false);

    return () => window.removeEventListener('auth:logout', handleLogout);
  }, []);

  const login = (userData) => {
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    setIsAuthenticated(true);
  };

  // Hydrates the full profile (fullName, roles) from the backend.
  // Used after login/register and on app start when a session exists.
  const refreshProfile = async () => {
    try {
      const res = await api.get('/api/users/me');
      const profile = {
        email: res.data.email,
        fullName: res.data.fullName,
        roles: res.data.roles,
      };
      localStorage.setItem('user', JSON.stringify(profile));
      setUser(profile);
      setIsAuthenticated(true);
      return profile;
    } catch {
      return null;
    }
  };

  useEffect(() => {
    if (localStorage.getItem('user')) {
      refreshProfile();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const queryClient = useQueryClient();

  const logout = async () => {
    try {
      await api.post('/api/auth/logout');
    } catch {
      // ignore
    }
    localStorage.removeItem('user');
    setUser(null);
    setIsAuthenticated(false);
    queryClient.clear(); // Clear all cached data on logout
  };

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, isLoading, login, logout, refreshProfile }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
