import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [donorProfile, setDonorProfile] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = localStorage.getItem('user');
    const storedProfile = localStorage.getItem('donorProfile');
    if (stored && token) {
      setUser(JSON.parse(stored));
      if (storedProfile) setDonorProfile(JSON.parse(storedProfile));
    }
    setLoading(false);
  }, []);

  const login = (data) => {
    setUser(data.user);
    setToken(data.token);
    if (data.donorProfile) setDonorProfile(data.donorProfile);
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify(data.user));
    if (data.donorProfile) localStorage.setItem('donorProfile', JSON.stringify(data.donorProfile));
  };

  const logout = () => {
    setUser(null); setToken(null); setDonorProfile(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('donorProfile');
  };

  const updateDonorProfile = (profile) => {
    setDonorProfile(profile);
    localStorage.setItem('donorProfile', JSON.stringify(profile));
  };

  return (
    <AuthContext.Provider value={{
      user, token, donorProfile, login, logout, loading, updateDonorProfile,
      isAdmin: user?.role === 'admin',
      isDonor: user?.role === 'donor',
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
