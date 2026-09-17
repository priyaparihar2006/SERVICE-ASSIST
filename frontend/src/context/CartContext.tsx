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
  const { user } = useAuth();

  const [items, setItems] = useState<CartItem[]>(() => {
    const saved = localStorage.getItem('service_assist_cart');
    return saved ? JSON.parse(saved) : [];
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
    if (!selectedAddress && user?.addresses?.length) {
      setSelectedAddress(user.addresses.find((a) => a.isDefault) || user.addresses[0]);
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

  let discount = 0;
  if (appliedCoupon && subtotal >= appliedCoupon.minBookingAmount) {
    if (appliedCoupon.discountType === 'FLAT') {
      discount = appliedCoupon.value;
    } else {
      discount = Math.round((subtotal * appliedCoupon.value) / 100);
      if (appliedCoupon.maxDiscount && discount > appliedCoupon.maxDiscount) {
        discount = appliedCoupon.maxDiscount;
      }
    }
  }

  // Ensure discount doesn't exceed subtotal
  discount = Math.min(discount, subtotal);

  // 5% standard GST
  const taxes = subtotal > 0 ? Math.round((subtotal - discount) * 0.05) : 0;
  const total = Math.max(0, subtotal - discount + taxes);

  // Original price savings calculation
  const originalTotal = items.reduce((sum, item) => {
    const orig = item.variant.originalPrice || item.variant.price;
    return sum + orig * item.quantity;
  }, 0);
  const totalSaved = Math.max(0, originalTotal - subtotal + discount);

  const applyCoupon = async (code: string): Promise<boolean> => {
    setCouponError(null);
    setCouponSuccess(null);
    const cleanCode = code.trim().toUpperCase();

    try {
      const res = await fetch('/api/coupons/validate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code: cleanCode, amount: subtotal }),
      });
      const data = await res.json();
      if (data.valid && data.coupon) {
        setAppliedCoupon(data.coupon);
        setCouponSuccess(data.message);
        return true;
      } else {
        setCouponError(data.message || 'Invalid coupon');
        return false;
      }
    } catch (e) {
      // Fallback local check
      if (cleanCode === 'WELCOME150' && subtotal >= 399) {
        setAppliedCoupon({
          code: 'WELCOME150',
          discountType: 'FLAT',
          value: 150,
          minBookingAmount: 399,
          description: 'Flat ₹150 OFF on first booking',
          expiry: '2026-12-31',
        });
        setCouponSuccess('Applied! Saved ₹150 with WELCOME150');
        return true;
      }
      setCouponError('Invalid coupon or minimum amount not met');
      return false;
    }
  };

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
        openCheckoutModal: () => setIsCheckoutModalOpen(true),
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
