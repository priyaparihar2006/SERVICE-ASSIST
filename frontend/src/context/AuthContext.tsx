import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, UserRole, Address } from '../types';

interface AuthContextType {
  user: User | null;
  role: UserRole;
  token: string | null;
  isAuthenticated: boolean;
  favorites: string[];
  login: (email: string, role?: UserRole) => Promise<void>;
  register: (name: string, email: string, phone: string, role?: UserRole) => Promise<void>;
  logout: () => void;
  switchDemoRole: (role: UserRole) => void;
  toggleFavorite: (serviceId: string) => void;
  isFavorite: (serviceId: string) => boolean;
  addAddress: (address: Omit<Address, 'id'>) => Promise<Address>;
  setDefaultAddress: (addressId: string) => void;
  isAuthModalOpen: boolean;
  openAuthModal: () => void;
  closeAuthModal: () => void;
}

const DEFAULT_CUSTOMER: User = {
  id: 'usr-customer-1',
  name: 'Priya Sharma',
  email: 'priya.sharma@example.com',
  phone: '+91 98765 12345',
  role: 'CUSTOMER',
  avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80',
  createdAt: '2026-01-15',
  addresses: [
    {
      id: 'addr-1',
      type: 'Home',
      house: 'Flat 402, Lotus Grandeur',
      street: 'Fatehabad Road',
      area: 'Tajganj',
      city: 'Agra',
      state: 'Uttar Pradesh',
      pincode: '282001',
      landmark: 'Opposite Amarvilas',
      isDefault: true,
    },
    {
      id: 'addr-2',
      type: 'Work',
      house: 'Suite 3B, Cyber Park',
      street: 'Golf Course Road',
      area: 'Sector 54',
      city: 'Gurgaon',
      state: 'Haryana',
      pincode: '122002',
      landmark: 'Near Horizon Center',
      isDefault: false,
    },
  ],
};

const DEFAULT_PRO: User = {
  id: 'usr-pro-1',
  name: 'Rahul Sharma',
  email: 'rahul.technician@serviceassist.in',
  phone: '+91 98765 43210',
  role: 'PROFESSIONAL',
  avatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=200&q=80',
  createdAt: '2025-11-20',
  addresses: [
    {
      id: 'addr-pro-1',
      type: 'Work',
      house: 'Shop 12, Sanjay Place Complex',
      street: 'MG Road',
      area: 'Civil Lines',
      city: 'Agra',
      state: 'Uttar Pradesh',
      pincode: '282002',
      isDefault: true,
    },
  ],
};

const DEFAULT_ADMIN: User = {
  id: 'usr-admin-1',
  name: 'Aarav Singhania',
  email: 'admin@serviceassist.in',
  phone: '+91 99999 00000',
  role: 'ADMIN',
  avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=200&q=80',
  createdAt: '2025-08-01',
  addresses: [],
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('service_assist_user');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        // ignore
      }
    }
    return DEFAULT_CUSTOMER;
  });

  const [token, setToken] = useState<string | null>(() => {
    return localStorage.getItem('service_assist_token') || 'demo-auth-token-123';
  });

  const [favorites, setFavorites] = useState<string[]>(() => {
    const saved = localStorage.getItem('service_assist_favs');
    return saved ? JSON.parse(saved) : ['srv-ac-foamjet', 'srv-bathroom-deep'];
  });

  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);

  const role: UserRole = user?.role || 'CUSTOMER';

  const login = async (email: string, roleToUse: UserRole = 'CUSTOMER') => {
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, role: roleToUse }),
      });
      const data = await res.json();
      if (data.user) {
        setUser(data.user);
        setToken(data.token);
        localStorage.setItem('service_assist_user', JSON.stringify(data.user));
        localStorage.setItem('service_assist_token', data.token);
        setIsAuthModalOpen(false);
      }
    } catch (e) {
      // Fallback in-memory
      let chosen = DEFAULT_CUSTOMER;
      if (roleToUse === 'PROFESSIONAL') chosen = DEFAULT_PRO;
      if (roleToUse === 'ADMIN') chosen = DEFAULT_ADMIN;
      setUser(chosen);
      setIsAuthModalOpen(false);
    }
  };

  const register = async (name: string, email: string, phone: string, roleToUse: UserRole = 'CUSTOMER') => {
    try {
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, email, phone, role: roleToUse }),
      });
      const data = await res.json();
      if (data.user) {
        setUser(data.user);
        setToken(data.token);
        localStorage.setItem('service_assist_user', JSON.stringify(data.user));
        localStorage.setItem('service_assist_token', data.token);
        setIsAuthModalOpen(false);
      }
    } catch (e) {
      const newUser: User = {
        id: `usr-${Date.now()}`,
        name,
        email,
        phone,
        role: roleToUse,
        createdAt: new Date().toISOString().split('T')[0],
        addresses: [],
      };
      setUser(newUser);
      setIsAuthModalOpen(false);
    }
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('service_assist_user');
    localStorage.removeItem('service_assist_token');
  };

  const switchDemoRole = (newRole: UserRole) => {
    let target = DEFAULT_CUSTOMER;
    if (newRole === 'PROFESSIONAL') target = DEFAULT_PRO;
    if (newRole === 'ADMIN') target = DEFAULT_ADMIN;
    setUser(target);
    localStorage.setItem('service_assist_user', JSON.stringify(target));
  };

  const toggleFavorite = (serviceId: string) => {
    setFavorites((prev) => {
      const exists = prev.includes(serviceId);
      const updated = exists ? prev.filter((id) => id !== serviceId) : [...prev, serviceId];
      localStorage.setItem('service_assist_favs', JSON.stringify(updated));
      return updated;
    });
  };

  const isFavorite = (serviceId: string) => favorites.includes(serviceId);

  const addAddress = async (addrData: Omit<Address, 'id'>): Promise<Address> => {
    const newAddr: Address = {
      ...addrData,
      id: `addr-${Date.now()}`,
    };

    if (user) {
      const updatedAddrs = user.addresses ? [...user.addresses] : [];
      if (newAddr.isDefault) {
        updatedAddrs.forEach((a) => (a.isDefault = false));
      }
      updatedAddrs.push(newAddr);
      const updatedUser = { ...user, addresses: updatedAddrs };
      setUser(updatedUser);
      localStorage.setItem('service_assist_user', JSON.stringify(updatedUser));
    }

    return newAddr;
  };

  const setDefaultAddress = (addressId: string) => {
    if (!user) return;
    const updated = user.addresses.map((a) => ({
      ...a,
      isDefault: a.id === addressId,
    }));
    const updatedUser = { ...user, addresses: updated };
    setUser(updatedUser);
    localStorage.setItem('service_assist_user', JSON.stringify(updatedUser));
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        role,
        token,
        isAuthenticated: !!user,
        favorites,
        login,
        register,
        logout,
        switchDemoRole,
        toggleFavorite,
        isFavorite,
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
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
