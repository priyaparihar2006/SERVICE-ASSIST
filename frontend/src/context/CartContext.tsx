import { apiFetch } from '../services/api';
import React, { createContext, useContext, useState, useEffect } from 'react';
import { Service, ServiceVariant, CartItem, Address, Coupon } from '../types';
import { useAuth } from './AuthContext';

interface CartContextType {
  items: CartItem[];
  addItem: (service: Service, variant?: ServiceVariant) => void;
  removeItem: (variantId: string) => void;
  updateQuantity: (variantId: string, quantity: number) => void;
  clearCart: () => void;
  bookingDate: string;
  setBookingDate: (date: string) => void;
  bookingTimeSlot: string;
  setBookingTimeSlot: (slot: string) => void;
  selectedAddress: Address | null;
  setSelectedAddress: (addr: Address | null) => void;
  specialInstructions: string;
  setSpecialInstructions: (text: string) => void;
  appliedCoupon: Coupon | null;
  couponError: string | null;
  couponSuccess: string | null;
  applyCoupon: (code: string) => Promise<boolean>;
  removeCoupon: () => void;
  subtotal: number;
  discount: number;
  taxes: number;
  total: number;
  totalSaved: number;
  isCartDrawerOpen: boolean;
  openCartDrawer: () => void;
  closeCartDrawer: () => void;
  isCheckoutModalOpen: boolean;
  openCheckoutModal: () => void;
  closeCheckoutModal: () => void;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

export const CartProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, openAuthModal } = useAuth();

  const [items, setItems] = useState<CartItem[]>(() => {
    const saved = localStorage.getItem('service_assist_cart');
    try {
      const parsed = saved ? JSON.parse(saved) : [];
      return Array.isArray(parsed) ? parsed.filter(item => item?.service?.id && item?.variant?.id && Number.isInteger(item.quantity) && item.quantity > 0) : [];
    } catch { return []; }
  });

  // Default booking date to tomorrow
  const getTomorrowString = () => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d.toISOString().split('T')[0];
  };

  const [bookingDate, setBookingDate] = useState<string>(getTomorrowString());
  const [bookingTimeSlot, setBookingTimeSlot] = useState<string>('10:00 AM - 11:00 AM');
  const [selectedAddress, setSelectedAddress] = useState<Address | null>(() => {
    return user?.addresses?.find((a) => a.isDefault) || user?.addresses?.[0] || null;
  });

  useEffect(() => {
    if (!user) { setSelectedAddress(null); return; }
    if (!selectedAddress || !user.addresses.some(a => a.id === selectedAddress.id)) {
      setSelectedAddress(user.addresses.find((a) => a.isDefault) || user.addresses[0] || null);
    }
  }, [user]);

  const [specialInstructions, setSpecialInstructions] = useState<string>('');
  const [appliedCoupon, setAppliedCoupon] = useState<Coupon | null>(null);
  const [couponError, setCouponError] = useState<string | null>(null);
  const [couponSuccess, setCouponSuccess] = useState<string | null>(null);

  const [isCartDrawerOpen, setIsCartDrawerOpen] = useState(false);
  const [isCheckoutModalOpen, setIsCheckoutModalOpen] = useState(false);

  useEffect(() => {
    localStorage.setItem('service_assist_cart', JSON.stringify(items));
  }, [items]);

  const addItem = (service: Service, variant?: ServiceVariant) => {
    const targetVariant = variant || service.variants[0] || {
      id: `var-${service.id}-std`,
      name: 'Standard Package',
      price: service.startingPrice,
      originalPrice: service.originalPrice,
      durationMin: service.durationMin,
      description: service.shortDesc,
      included: service.whatIncluded.slice(0, 3),
    };

    setItems((prev) => {
      const existingIndex = prev.findIndex((item) => item.variant.id === targetVariant.id);
      if (existingIndex > -1) {
        const updated = [...prev];
        updated[existingIndex].quantity += 1;
        return updated;
      }
      return [...prev, { service, variant: targetVariant, quantity: 1 }];
    });
    setIsCartDrawerOpen(true);
  };

  const removeItem = (variantId: string) => {
    setItems((prev) => prev.filter((item) => item.variant.id !== variantId));
  };

  const updateQuantity = (variantId: string, quantity: number) => {
    if (quantity <= 0) {
      removeItem(variantId);
      return;
    }
    setItems((prev) =>
      prev.map((item) => (item.variant.id === variantId ? { ...item, quantity } : item))
    );
  };

  const clearCart = () => {
    setItems([]);
    setAppliedCoupon(null);
    setCouponSuccess(null);
    setCouponError(null);
    localStorage.removeItem('service_assist_cart');
  };

  // Pricing calculations
  const subtotal = items.reduce((sum, item) => sum + item.variant.price * item.quantity, 0);

  // The discount comes from the server (see applyCoupon); the browser never calculates it. Checkout
  // re-validates the coupon and prices the booking again on the server.
  const discount = Math.min(appliedCoupon?.discount ?? 0, subtotal);

  // Tax collection is not configured.
  const taxes = 0; // Configure jurisdiction-specific taxes before charging tax.
  const total = Math.max(0, subtotal - discount + taxes);

  // Original price savings calculation
  const originalTotal = items.reduce((sum, item) => {
    const orig = item.variant.originalPrice || item.variant.price;
    return sum + orig * item.quantity;
  }, 0);
  const totalSaved = Math.max(0, originalTotal - subtotal + discount);

  // Asks the server to price this cart with the coupon. Only the code and the cart's item ids and
  // quantities are sent: amounts and discounts are always calculated on the server.
  const validateCoupon = async (code: string) => {
    const res = await apiFetch('/api/coupons/validate', {
      method: 'POST',
      body: JSON.stringify({
        code,
        items: items.map((i) => ({ serviceId: i.service.id, variantId: i.variant.id, quantity: i.quantity })),
      }),
    });
    return res.json();
  };

  const applyCoupon = async (code: string): Promise<boolean> => {
    setCouponError(null);
    setCouponSuccess(null);
    const cleanCode = code.trim().toUpperCase();
    if (!cleanCode) {
      setCouponError('Enter a coupon code');
      return false;
    }
    if (!items.length) {
      setCouponError('Add a service to your cart before applying a coupon');
      return false;
    }

    try {
      const data = await validateCoupon(cleanCode);
      if (data.valid && data.coupon) {
        setAppliedCoupon({ ...data.coupon, discount: data.discount });
        setCouponSuccess(data.message);
        return true;
      }
      setCouponError(data.message || 'This coupon could not be applied');
      return false;
    } catch (e: any) {
      // Show the server's actual reason (expired, minimum not met, wrong category, already used...).
      setCouponError(e.message || 'This coupon could not be applied');
      return false;
    }
  };

  // The cart changed: price the applied coupon again so the shown discount always matches the server.
  const cartSignature = items.map((i) => `${i.variant.id}:${i.quantity}`).join('|');
  useEffect(() => {
    if (!appliedCoupon) return;
    if (!items.length) {
      setAppliedCoupon(null);
      setCouponSuccess(null);
      return;
    }
    let cancelled = false;
    validateCoupon(appliedCoupon.code)
      .then((data) => {
        if (!cancelled && data.valid && data.coupon) setAppliedCoupon({ ...data.coupon, discount: data.discount });
      })
      .catch((e: any) => {
        // The coupon no longer fits this cart (e.g. its category was removed): drop it and say why.
        // Network/server failures keep it; checkout validates again.
        if (cancelled || !e.status || e.status >= 500 || e.status === 429) return;
        setAppliedCoupon(null);
        setCouponSuccess(null);
        setCouponError(e.message);
      });
    return () => {
      cancelled = true;
    };
  }, [cartSignature]);

  const removeCoupon = () => {
    setAppliedCoupon(null);
    setCouponSuccess(null);
    setCouponError(null);
  };

  return (
    <CartContext.Provider
      value={{
        items,
        addItem,
        removeItem,
        updateQuantity,
        clearCart,
        bookingDate,
        setBookingDate,
        bookingTimeSlot,
        setBookingTimeSlot,
        selectedAddress,
        setSelectedAddress,
        specialInstructions,
        setSpecialInstructions,
        appliedCoupon,
        couponError,
        couponSuccess,
        applyCoupon,
        removeCoupon,
        subtotal,
        discount,
        taxes,
        total,
        totalSaved,
        isCartDrawerOpen,
        openCartDrawer: () => setIsCartDrawerOpen(true),
        closeCartDrawer: () => setIsCartDrawerOpen(false),
        isCheckoutModalOpen,
        openCheckoutModal: () => user ? setIsCheckoutModalOpen(true) : openAuthModal(),
        closeCheckoutModal: () => setIsCheckoutModalOpen(false),
      }}
    >
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within CartProvider');
  }
  return context;
};
