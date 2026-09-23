import React, { createContext, useState, useEffect, useContext } from 'react';
import api from '../services/api';

const AuthContext = createContext();

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem('token');
      const userId = localStorage.getItem('userId');
      
      if (token && userId) {
        try {
          // Fetch user profile to populate user state
          const res = await api.get(`/profile/${userId}`);
          if (res.data.success && res.data.data) {
            setUser({
              user_id: res.data.data.userId,
              sub: res.data.data.username,
              name: res.data.data.name,
              profilePicUrl: res.data.data.profilePicUrl
            });
          } else {
            throw new Error("Failed to load profile for auth");
          }
        } catch (error) {
          console.error('Auth initialization failed', error);
          localStorage.removeItem('token');
          localStorage.removeItem('userId');
        }
      }
      setLoading(false);
    };
    
    initAuth();
  }, []);

  const login = (token, userId) => {
    localStorage.setItem('token', token);
    localStorage.setItem('userId', userId);
    
    // We fetch immediately or just set basic info if available
    // For full details, let's fetch it, but to prevent block, we'll fetch async
    api.get(`/profile/${userId}`).then(res => {
      if (res.data.success && res.data.data) {
        setUser({
          user_id: res.data.data.userId,
          sub: res.data.data.username,
          name: res.data.data.name,
          profilePicUrl: res.data.data.profilePicUrl
        });
      }
    }).catch(err => {
      console.error('Failed to load profile on login', err);
    });
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, setUser, login, logout, loading }}>
      {!loading && children}
    </AuthContext.Provider>
  );
};
