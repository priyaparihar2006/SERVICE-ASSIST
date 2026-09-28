import React, { useState } from 'react';
import { useCart } from '../../context/CartContext';
import { ShoppingBag, X, Plus, Minus, Trash2, Tag, CheckCircle2, AlertCircle, ArrowRight, ShieldCheck } from 'lucide-react';
import { ImageWithFallback } from './ImageWithFallback';

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
                    key={item.variant.id}
                    className="p-3 sm:p-3.5 rounded-2xl border border-gray-100 bg-gray-50/50 hover:bg-white hover:border-[var(--color-brand-bright)]/40 transition-all shadow-xs"
                  >
                    <div className="flex gap-2.5 sm:gap-3">
                      <ImageWithFallback
                        src={item.service.image}
                        fallbackSrc={item.service.categoryImage}
                        fallbackTitle={item.service.name}
                        alt={item.service.name}
                        className="w-14 h-14 sm:w-16 sm:h-16 rounded-xl object-cover shrink-0 border border-gray-100"
                      />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between gap-1">
                          <h4 className="font-semibold text-gray-900 text-xs sm:text-sm truncate">{item.service.name}</h4>
                          <button
                            onClick={() => removeItem(item.variant.id)}
                            aria-label={`Remove ${item.service.name}`}
                            className="text-gray-400 hover:text-red-500 p-1 cursor-pointer shrink-0"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                        <p className="text-[11px] sm:text-xs text-[var(--color-brand-hover)] font-medium truncate mb-2">{item.variant.name}</p>

                        <div className="flex items-center justify-between gap-2 flex-wrap sm:flex-nowrap">
                          <div className="flex items-baseline gap-1.5">
                            <span className="font-bold text-gray-900 text-xs sm:text-sm">₹{item.variant.price * item.quantity}</span>
                            {item.variant.originalPrice && (
                              <span className="text-[10px] sm:text-xs text-gray-400 line-through">
                                ₹{item.variant.originalPrice * item.quantity}
                              </span>
                            )}
                          </div>

                          {/* Quantity control */}
                          <div className="flex items-center border border-gray-200 bg-white rounded-lg px-1.5 py-0.5 shadow-xs shrink-0">
                            <button
                              onClick={() => updateQuantity(item.variant.id, item.quantity - 1)}
                              aria-label="Decrease quantity"
                              className="p-1 text-gray-500 hover:text-[var(--color-brand)] cursor-pointer"
                            >
                              <Minus className="w-3 h-3" />
                            </button>
                            <span className="px-1.5 sm:px-2 text-xs font-bold text-gray-800">{item.quantity}</span>
                            <button
                              onClick={() => updateQuantity(item.variant.id, item.quantity + 1)}
                              aria-label="Increase quantity"
                              className="p-1 text-gray-500 hover:text-[var(--color-brand)] cursor-pointer"
                            >
                              <Plus className="w-3 h-3" />
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}

                {/* Coupons Section */}
                <div className="p-3 sm:p-3.5 rounded-2xl border border-brand-light bg-[var(--color-brand-soft)]/60 space-y-2">
                  <div className="flex items-center gap-1.5 text-xs font-bold text-gray-800">
                    <Tag className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                    <span>Apply Discount Coupon</span>
                  </div>

                  {appliedCoupon ? (
                    <div className="flex items-center justify-between p-2 sm:p-2.5 bg-[var(--color-brand-light)] border border-brand-light rounded-xl text-xs">
                      <div className="flex items-center gap-2 min-w-0">
                        <CheckCircle2 className="w-4 h-4 text-[var(--color-brand)] shrink-0" />
                        <div className="min-w-0 truncate">
                          <span className="font-bold text-[var(--color-brand-hover)]">{appliedCoupon.code}</span>
                          <p className="text-[10px] sm:text-[11px] text-gray-600">You saved ₹{discount}</p>
                        </div>
                      </div>
                      <button
                        onClick={removeCoupon}
                        className="text-xs font-semibold text-red-500 hover:underline px-2 cursor-pointer shrink-0"
                      >
                        Remove
                      </button>
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
                        className="text-[10px] font-semibold bg-white border border-dashed border-[var(--color-brand-bright)] text-[var(--color-brand-hover)] px-2 py-0.5 rounded-md hover:bg-[var(--color-brand-light)] transition-colors cursor-pointer"
                      >
                        🏷️ WELCOME150 (₹150 OFF)
                      </button>
                      <button
                        onClick={() => handleApplyCoupon('CLEAN10')}
                        className="text-[10px] font-semibold bg-white border border-dashed border-[var(--color-brand-bright)] text-[var(--color-brand-hover)] px-2 py-0.5 rounded-md hover:bg-[var(--color-brand-light)] transition-colors cursor-pointer"
                      >
                        🏷️ CLEAN10 (10% OFF)
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

                <div className="space-y-1.5 text-xs text-gray-600">
                  <div className="flex justify-between">
                    <span>Item Total</span>
                    <span className="font-semibold text-gray-800">₹{subtotal}</span>
                  </div>
                  {discount > 0 && (
                    <div className="flex justify-between text-brand font-medium">
                      <span>Coupon Discount</span>
                      <span className="font-semibold">-₹{discount}</span>
                    </div>
                  )}
                  <div className="flex justify-between">
                    <span>Additional taxes</span>
                    <span>₹{taxes}</span>
                  </div>
                  <div className="flex justify-between font-bold text-xs sm:text-sm text-gray-900 pt-1.5 border-t border-gray-100">
                    <span>Total Amount</span>
                    <span className="text-[var(--color-brand)] text-base font-black">₹{total}</span>
                  </div>
                </div>

                <button
                  onClick={handleProceed}
                  className="w-full flex items-center justify-center gap-2 py-3 px-3 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs sm:text-sm rounded-xl transition-all shadow-lg shadow-brand/25 active:scale-[0.99] cursor-pointer"
                >
                  <span className="truncate">Proceed to Schedule & Checkout</span>
                  <ArrowRight className="w-4 h-4 shrink-0" />
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
