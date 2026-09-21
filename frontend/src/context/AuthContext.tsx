import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, UserRole, Address } from '../types';
import { api } from '../services/api';
interface AuthContextType {
  user: User | null;
  role: UserRole;
  isAuthenticated: boolean;
  loading: boolean;
  favorites: string[];
  login: (email: string, password: string) => Promise<void>;
  register: (
    name: string,
    email: string,
    phone: string,
    password: string,
    role?: UserRole,
  ) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
  toggleFavorite: (id: string) => void;
  isFavorite: (id: string) => boolean;
  addAddress: (address: Omit<Address, 'id'>) => Promise<Address>;
  setDefaultAddress: (id: string) => Promise<void>;
  isAuthModalOpen: boolean;
  openAuthModal: () => void;
  closeAuthModal: () => void;
}
const AuthContext = createContext<AuthContextType | undefined>(undefined);
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [favorites, setFavorites] = useState<string[]>([]);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const refreshUser = async () => {
    const data = await api('/auth/me');
    setUser(data.user);
  };
  useEffect(() => {
    localStorage.removeItem('service_assist_user');
    localStorage.removeItem('service_assist_token');
    refreshUser()
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);
  useEffect(() => {
    try {
      setFavorites(
        JSON.parse(
          localStorage.getItem(`favorites:${user?.id || 'guest'}`) || '[]',
        ),
      );
    } catch {
      setFavorites([]);
    }
  }, [user?.id]);
  const login = async (email: string, password: string) => {
    const data = await api('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    setUser(data.user);
    setIsAuthModalOpen(false);
  };
  const register = async (
    name: string,
    email: string,
    phone: string,
    password: string,
    role: UserRole = 'CUSTOMER',
  ) => {
    const data = await api('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name, email, phone, password, role }),
    });
    setUser(data.user);
    setIsAuthModalOpen(false);
  };
  const logout = async () => {
    try {
      await api('/auth/logout', { method: 'POST', body: '{}' });
      setUser(null);
    } catch (e) {
      alert(e.message);
    }
  };
  const toggleFavorite = (id: string) =>
    setFavorites((previous) => {
      const next = previous.includes(id)
        ? previous.filter((v) => v !== id)
        : [...previous, id];
      localStorage.setItem(
        `favorites:${user?.id || 'guest'}`,
        JSON.stringify(next),
      );
      return next;
    });
  const addAddress = async (address: Omit<Address, 'id'>) => {
    const data = await api('/addresses', {
      method: 'POST',
      body: JSON.stringify(address),
    });
    await refreshUser();
    return data.address;
  };
  const setDefaultAddress = async (id: string) => {
    try {
      await api(`/addresses/${id}/default`, { method: 'PUT', body: '{}' });
      await refreshUser();
    } catch (e) {
      alert(e.message);
    }
  };
  return (
    <AuthContext.Provider
      value={{
        user,
        role: user?.role || 'CUSTOMER',
        loading,
        isAuthenticated: !!user,
        favorites,
        login,
        register,
        logout,
        refreshUser,
        toggleFavorite,
        isFavorite: (id) => favorites.includes(id),
        addAddress,
        setDefaultAddress,
        isAuthModalOpen,
        openAuthModal: () => setIsAuthModalOpen(true),
        closeAuthModal: () => setIsAuthModalOpen(false),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('AuthProvider is required');
  return context;
};
