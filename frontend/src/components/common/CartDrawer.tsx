import React, { useState } from 'react';
import { useCart } from '../../context/CartContext';
import { ShoppingBag, X, Plus, Minus, Trash2, Tag, CheckCircle2, AlertCircle, ArrowRight, ShieldCheck, Clock, Calendar } from 'lucide-react';
import { ImageWithFallback } from './ImageWithFallback';
import { getCouponValidityInfo } from '../../utils/couponUtils';

interface CartDrawerProps {
  onNavigate?: (path: string) => void;
}

export const CartDrawer: React.FC<CartDrawerProps> = ({ onNavigate }) => {
  const {
    items,
    removeItem,
    updateQuantity,
    clearCart,
    subtotal,
    discount,
    taxes,
    total,
    totalSaved,
    appliedCoupon,
    couponError,
    couponSuccess,
    applyCoupon,
    removeCoupon,
    isCartDrawerOpen,
    closeCartDrawer,
    openCheckoutModal,
  } = useCart();

  const [couponInput, setCouponInput] = useState('');
  const [isApplying, setIsApplying] = useState(false);

  if (!isCartDrawerOpen) return null;

  const handleApplyCoupon = async (codeToApply?: string) => {
    const code = codeToApply || couponInput;
    if (!code) return;
    setIsApplying(true);
    await applyCoupon(code);
    setIsApplying(false);
    setCouponInput('');
  };

  const handleProceed = () => {
    closeCartDrawer();
    openCheckoutModal();
  };

  const appliedValidity = appliedCoupon ? getCouponValidityInfo(appliedCoupon.expiry) : null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/50 backdrop-blur-xs transition-opacity" onClick={closeCartDrawer} />

      <div className="fixed inset-y-0 right-0 max-w-full flex w-full sm:w-auto">
        <div className="w-full sm:max-w-md max-w-full bg-white shadow-2xl flex flex-col border-l border-gray-100 animate-in slide-in-from-right duration-200 h-full overflow-hidden">
          {/* Header */}
          <div className="p-4 sm:p-5 border-b border-gray-100 flex items-center justify-between bg-white shrink-0">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-[var(--color-brand-light)] flex items-center justify-center text-[var(--color-brand)] shrink-0">
                <ShoppingBag className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-bold text-gray-900 text-base sm:text-lg font-['Outfit']">Your Service Cart</h3>
                <p className="text-[11px] sm:text-xs text-gray-500">{items.length} {items.length === 1 ? 'item' : 'items'} selected</p>
              </div>
            </div>
            <button
              onClick={closeCartDrawer}
              aria-label="Close cart"
              className="p-1.5 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {items.length === 0 ? (
            /* Empty State */
            <div className="flex-1 flex flex-col items-center justify-center p-6 sm:p-8 text-center overflow-y-auto">
              <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-[var(--color-brand-light)] flex items-center justify-center text-[var(--color-brand)] mb-4">
                <ShoppingBag className="w-8 h-8 sm:w-10 sm:h-10 opacity-80" />
              </div>
              <h4 className="font-bold text-gray-800 text-base sm:text-lg mb-1 font-['Outfit']">Your cart is empty</h4>
              <p className="text-xs text-gray-500 max-w-xs mb-6">
                Explore our verified home services like AC repair, salon, deep cleaning, or electrical diagnostics.
              </p>
              <button
                onClick={() => {
                  closeCartDrawer();
                  if (onNavigate) onNavigate('/services');
                }}
                className="px-6 py-2.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-semibold text-xs sm:text-sm rounded-xl transition-all shadow-md shadow-brand/20 cursor-pointer"
              >
                Explore Services
              </button>
            </div>
          ) : (
            <>
              {/* Items List */}
              <div className="flex-1 overflow-y-auto p-3.5 sm:p-4 space-y-3">
                {items.map((item) => (
                  <div
                    key={`${item.service.id}-${item.variant.id}`}
                    className="p-3 bg-white rounded-2xl border border-gray-100 shadow-2xs hover:border-[var(--color-brand-light)] transition-all flex gap-3 items-center"
                  >
                    <ImageWithFallback
                      src={item.service.image}
                      alt={item.service.name}
                      fallbackTitle={item.service.name}
                      className="w-16 h-16 rounded-xl object-cover shrink-0 border border-gray-100"
                    />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-1">
                        <h4 className="font-bold text-xs sm:text-sm text-gray-900 truncate font-['Outfit']">
                          {item.service.name}
                        </h4>
                        <button
                          onClick={() => removeItem(item.variant.id)}
                          className="text-gray-400 hover:text-red-500 p-1 cursor-pointer transition-colors"
                          title="Remove item"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      <p className="text-[11px] text-gray-500 truncate">{item.variant.name}</p>

                      <div className="flex items-center justify-between mt-2">
                        <span className="font-bold text-xs text-[var(--color-ink)]">
                          ₹{item.variant.price * item.quantity}
                        </span>

                        <div className="flex items-center border border-gray-200 rounded-lg bg-gray-50">
                          <button
                            onClick={() => updateQuantity(item.variant.id, item.quantity - 1)}
                            className="p-1 text-gray-500 hover:text-[var(--color-brand)] cursor-pointer"
                          >
                            <Minus className="w-3 h-3" />
                          </button>
                          <span className="px-2 text-xs font-bold text-gray-800">{item.quantity}</span>
                          <button
                            onClick={() => updateQuantity(item.variant.id, item.quantity + 1)}
                            className="p-1 text-gray-500 hover:text-[var(--color-brand)] cursor-pointer"
                          >
                            <Plus className="w-3 h-3" />
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}

                {/* Coupons Section with Validity Indicator */}
                <div className="p-3 sm:p-3.5 rounded-2xl border border-brand-light bg-[var(--color-brand-soft)]/60 space-y-2">
                  <div className="flex items-center justify-between text-xs font-bold text-gray-800">
                    <div className="flex items-center gap-1.5">
                      <Tag className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                      <span>Apply Discount Coupon</span>
                    </div>
                    <span className="text-[10px] text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md font-semibold">
                      Validity up to 30 days
                    </span>
                  </div>

                  {appliedCoupon ? (
                    <div className="p-2 sm:p-2.5 bg-[var(--color-brand-light)] border border-brand-light rounded-xl text-xs space-y-1">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2 min-w-0">
                          <CheckCircle2 className="w-4 h-4 text-[var(--color-brand)] shrink-0" />
                          <div className="min-w-0 truncate">
                            <span className="font-bold text-[var(--color-brand-hover)]">{appliedCoupon.code}</span>
                            <span className="text-[10px] text-gray-600 ml-2">Saved ₹{discount}</span>
                          </div>
                        </div>
                        <button
                          onClick={removeCoupon}
                          className="text-xs font-semibold text-red-500 hover:underline px-2 cursor-pointer shrink-0"
                        >
                          Remove
                        </button>
                      </div>
                      {appliedValidity && (
                        <div className="flex items-center gap-2 text-[10px] text-gray-600 pt-0.5 pl-6">
                          <span className="flex items-center gap-1">
                            <Calendar className="w-3 h-3 text-[var(--color-brand)]" />
                            {appliedValidity.availableUpToText}
                          </span>
                          <span>•</span>
                          <span className="font-semibold text-emerald-800 flex items-center gap-1">
                            <Clock className="w-3 h-3" />
                            {appliedValidity.expiryWithinText}
                          </span>
                        </div>
                      )}
                    </div>
                  ) : (
                    <div className="flex gap-2">
                      <input
                        type="text"
                        placeholder="Enter coupon code"
                        value={couponInput}
                        onChange={(e) => setCouponInput(e.target.value.toUpperCase())}
                        className="flex-1 min-w-0 px-3 py-1.5 bg-white border border-gray-200 rounded-xl text-xs uppercase focus:outline-none focus:ring-1 focus:ring-[var(--color-brand)]"
                      />
                      <button
                        onClick={() => handleApplyCoupon()}
                        disabled={!couponInput || isApplying}
                        className="px-3.5 py-1.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-semibold text-xs rounded-xl disabled:opacity-50 transition-all cursor-pointer shrink-0"
                      >
                        {isApplying ? 'Applying...' : 'Apply'}
                      </button>
                    </div>
                  )}

                  {couponError && (
                    <div className="flex items-center gap-1.5 text-[11px] text-red-600">
                      <AlertCircle className="w-3 h-3 shrink-0" />
                      <span>{couponError}</span>
                    </div>
                  )}

                  {!appliedCoupon && (
                    <div className="flex flex-wrap gap-1.5 pt-1">
                      <button
                        onClick={() => handleApplyCoupon('WELCOME150')}
                        className="text-[10px] font-semibold bg-white border border-dashed border-[var(--color-brand-bright)] text-[var(--color-brand-hover)] px-2 py-0.5 rounded-md hover:bg-[var(--color-brand-light)] transition-colors cursor-pointer flex items-center gap-1"
                      >
                        <span>🏷️ WELCOME150 (₹150 OFF)</span>
                        <span className="text-[9px] text-gray-500 font-normal">· Valid 30d</span>
                      </button>
                      <button
                        onClick={() => handleApplyCoupon('CLEAN10')}
                        className="text-[10px] font-semibold bg-white border border-dashed border-[var(--color-brand-bright)] text-[var(--color-brand-hover)] px-2 py-0.5 rounded-md hover:bg-[var(--color-brand-light)] transition-colors cursor-pointer flex items-center gap-1"
                      >
                        <span>🏷️ CLEAN10 (10% OFF)</span>
                        <span className="text-[9px] text-gray-500 font-normal">· Valid 30d</span>
                      </button>
                    </div>
                  )}
                </div>

                {/* Guarantee trust pill */}
                <div className="flex items-center gap-2 p-2.5 sm:p-3 bg-[var(--color-brand-light)]/80 rounded-xl border border-brand-light text-[var(--color-ink)] text-xs">
                  <ShieldCheck className="w-4 h-4 text-[var(--color-brand)] shrink-0" />
                  <span className="text-[11px] leading-tight">Verified doorstep professionals with 30-day service satisfaction warranty</span>
                </div>
              </div>

              {/* Bottom Sticky Summary */}
              <div className="p-3.5 sm:p-4 border-t border-gray-100 bg-white space-y-2.5 sm:space-y-3 shrink-0">
                {totalSaved > 0 && (
                  <div className="py-1 px-2.5 bg-brand-soft rounded-lg text-center text-xs font-semibold text-brand-hover truncate">
                    🎉 You are saving ₹{totalSaved} on this booking!
                  </div>
                )}

                <div className="space-y-1.5 text-xs">
                  <div className="flex justify-between text-gray-500">
                    <span>Item Total</span>
                    <span>₹{subtotal}</span>
                  </div>

                  {discount > 0 && (
                    <div className="flex justify-between text-[var(--color-brand)] font-semibold">
                      <span>Coupon Discount</span>
                      <span>-₹{discount}</span>
                    </div>
                  )}

                  <div className="flex justify-between text-gray-500">
                    <span>Taxes & Service Fee (5%)</span>
                    <span>₹{taxes}</span>
                  </div>

                  <div className="pt-2 border-t border-gray-100 flex justify-between items-center text-sm font-extrabold text-gray-900 font-['Outfit']">
                    <span>Total Pay</span>
                    <span className="text-base text-[var(--color-brand)]">₹{total}</span>
                  </div>
                </div>

                <div className="flex gap-2 pt-1">
                  <button
                    onClick={clearCart}
                    className="px-3 py-2.5 text-xs text-gray-500 hover:text-red-500 font-semibold border border-gray-200 rounded-xl hover:bg-gray-50 transition-colors cursor-pointer"
                  >
                    Clear
                  </button>
                  <button
                    onClick={handleProceed}
                    className="flex-1 py-3 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs sm:text-sm rounded-xl transition-all shadow-md shadow-brand/20 flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <span>Proceed to Schedule</span>
                    <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
