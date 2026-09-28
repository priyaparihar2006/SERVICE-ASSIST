import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, UserRole, Address } from '../types';
import { api } from '../services/api';

export const DEMO_USERS: Record<'CUSTOMER' | 'PROFESSIONAL' | 'ADMIN', User> = {
  CUSTOMER: {
    id: 'user-priya-01',
    name: 'Priya Sharma',
    email: 'priya@service-assist.test',
    phone: '7892367898',
    role: 'CUSTOMER',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80',
    addresses: [
      {
        id: 'addr-priya-01',
        type: 'Home',
        house: 'Flat 402, Royal Residency',
        street: 'Taj Nagri Phase 2',
        area: 'Fatehabad Road',
        city: 'Agra',
        state: 'Uttar Pradesh',
        pincode: '282001',
        isDefault: true,
      },
    ],
    verified: true,
  },
  PROFESSIONAL: {
    id: 'pro-rajesh',
    name: 'Rajesh Sharma',
    email: 'rajesh@service-assist.test',
    phone: '9876543210',
    role: 'PROFESSIONAL',
    avatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=200&q=80',
    addresses: [],
    verified: true,
  },
  ADMIN: {
    id: 'admin-01',
    name: 'Operations Admin',
    email: 'admin@service-assist.test',
    phone: '9999988888',
    role: 'ADMIN',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80',
    addresses: [],
    verified: true,
  },
};

interface AuthContextType {
  user: User | null;
  role: UserRole;
  isAuthenticated: boolean;
  loading: boolean;
  favorites: string[];
  login: (emailOrPhone: string, password?: string) => Promise<void>;
  loginAsDemo: (role: 'CUSTOMER' | 'PROFESSIONAL' | 'ADMIN') => Promise<void>;
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
  const [user, setUser] = useState<User | null>(() => {
    try {
      const saved = localStorage.getItem('service_assist_user');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });
  const [loading, setLoading] = useState(true);
  const [favorites, setFavorites] = useState<string[]>([]);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);

  const refreshUser = async () => {
    try {
      const data = await api('/auth/me');
      if (data?.user) {
        setUser(data.user);
        localStorage.setItem('service_assist_user', JSON.stringify(data.user));
      }
    } catch {
      // Keep existing user from localStorage if backend is offline
    }
  };

  useEffect(() => {
    refreshUser().finally(() => setLoading(false));
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

  const loginAsDemo = async (role: 'CUSTOMER' | 'PROFESSIONAL' | 'ADMIN') => {
    const demoUser = DEMO_USERS[role];
    try {
      const data = await api('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email: demoUser.email, password: `${demoUser.name.split(' ')[0]}@123456` }),
      });
      if (data?.user) {
        setUser(data.user);
        localStorage.setItem('service_assist_user', JSON.stringify(data.user));
        setIsAuthModalOpen(false);
        return;
      }
    } catch {
      // Offline / demo fallback
    }
    setUser(demoUser);
    localStorage.setItem('service_assist_user', JSON.stringify(demoUser));
    setIsAuthModalOpen(false);
  };

  const login = async (emailOrPhone: string, password?: string) => {
    const cleanId = (emailOrPhone || '').trim().toLowerCase();

    // Check if matches demo users
    let matchedDemo: User | null = null;
    if (cleanId.includes('priya') || cleanId === '7892367898' || cleanId === '9876543210') {
      matchedDemo = DEMO_USERS.CUSTOMER;
    } else if (cleanId.includes('rajesh') || cleanId === '9876543211') {
      matchedDemo = DEMO_USERS.PROFESSIONAL;
    } else if (cleanId.includes('admin') || cleanId === '9999988888') {
      matchedDemo = DEMO_USERS.ADMIN;
    }

    try {
      const data = await api('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email: emailOrPhone, password: password || 'Demo@123456' }),
      });
      if (data?.user) {
        setUser(data.user);
        localStorage.setItem('service_assist_user', JSON.stringify(data.user));
        setIsAuthModalOpen(false);
        return;
      }
    } catch (e: any) {
      // Backend error -> fallback to client demo user
      if (matchedDemo) {
        setUser(matchedDemo);
        localStorage.setItem('service_assist_user', JSON.stringify(matchedDemo));
        setIsAuthModalOpen(false);
        return;
      }
    }

    if (matchedDemo) {
      setUser(matchedDemo);
      localStorage.setItem('service_assist_user', JSON.stringify(matchedDemo));
      setIsAuthModalOpen(false);
      return;
    }

    // Dynamic customer fallback
    const dynamicUser: User = {
      id: 'user-' + Date.now(),
      name: cleanId.includes('@') ? cleanId.split('@')[0].replace(/[._]/g, ' ') : 'Customer',
      email: cleanId.includes('@') ? cleanId : `${cleanId}@service-assist.test`,
      phone: /^\d+$/.test(cleanId) ? cleanId : '7892367898',
      role: 'CUSTOMER',
      verified: true,
      addresses: [
        {
          id: 'addr-default-1',
          type: 'Home',
          house: 'Flat 402, Royal Residency',
          street: 'Taj Nagri Phase 2',
          area: 'Fatehabad Road',
          city: 'Agra',
          state: 'Uttar Pradesh',
          pincode: '282001',
          isDefault: true,
        },
      ],
    };
    setUser(dynamicUser);
    localStorage.setItem('service_assist_user', JSON.stringify(dynamicUser));
    setIsAuthModalOpen(false);
  };

  const register = async (
    name: string,
    email: string,
    phone: string,
    password: string,
    role: UserRole = 'CUSTOMER',
  ) => {
    try {
      const data = await api('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ name, email, phone, password, role }),
      });
      if (data?.user) {
        setUser(data.user);
        localStorage.setItem('service_assist_user', JSON.stringify(data.user));
        setIsAuthModalOpen(false);
        return;
      }
    } catch {
      // Fallback client registration
    }
    const newUser: User = {
      id: 'user-' + Date.now(),
      name,
      email,
      phone,
      role,
      verified: true,
      addresses: [
        {
          id: 'addr-' + Date.now(),
          type: 'Home',
          house: 'Flat 402, Royal Residency',
          street: 'Taj Nagri Phase 2',
          area: 'Fatehabad Road',
          city: 'Agra',
          state: 'Uttar Pradesh',
          pincode: '282001',
          isDefault: true,
        },
      ],
    };
    setUser(newUser);
    localStorage.setItem('service_assist_user', JSON.stringify(newUser));
    setIsAuthModalOpen(false);
  };

  const logout = async () => {
    try {
      await api('/auth/logout', { method: 'POST', body: '{}' }).catch(() => {});
    } finally {
      localStorage.removeItem('service_assist_user');
      setUser(null);
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
    const newAddr: Address = {
      id: 'addr-' + Date.now(),
      ...address,
    };
    try {
      const data = await api('/addresses', {
        method: 'POST',
        body: JSON.stringify(address),
      });
      if (data?.address) {
        await refreshUser();
        return data.address;
      }
    } catch {
      // Fallback local address update
    }
    setUser((prev) => {
      if (!prev) return prev;
      const updated = {
        ...prev,
        addresses: [...(prev.addresses || []), newAddr],
      };
      localStorage.setItem('service_assist_user', JSON.stringify(updated));
      return updated;
    });
    return newAddr;
  };

  const setDefaultAddress = async (id: string) => {
    try {
      await api(`/addresses/${id}/default`, { method: 'PUT', body: '{}' }).catch(() => {});
    } finally {
      setUser((prev) => {
        if (!prev) return prev;
        const updated = {
          ...prev,
          addresses: (prev.addresses || []).map((a) => ({
            ...a,
            isDefault: a.id === id,
          })),
        };
        localStorage.setItem('service_assist_user', JSON.stringify(updated));
        return updated;
      });
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
        loginAsDemo,
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

