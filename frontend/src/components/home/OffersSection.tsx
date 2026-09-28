import React, { useState } from 'react';
import { useOffers } from '../../hooks/useOffers';
import { Tag, Sparkles, CheckCircle2, Copy, ArrowRight } from 'lucide-react';
import { useCart } from '../../context/CartContext';

export const OffersSection: React.FC = () => {
  const { offers: COUPONS, error, loading } = useOffers();
  const { applyCoupon, openCartDrawer, appliedCoupon } = useCart();
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  const handleApply = async (code: string) => {
    await applyCoupon(code);
    setTimeout(() => {
      openCartDrawer();
    }, 400);
  };

  const handleCopy = (code: string) => {
    navigator.clipboard.writeText(code);
    setCopiedCode(code);
    setTimeout(() => setCopiedCode(null), 2000);
  };

  return (
    <section className="py-16 bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {error && <p role="alert">{error}</p>}
        {loading && <p role="status">Loading offers...</p>}
        {!loading && !error && !COUPONS.length && <p>No active offers.</p>}
        <div className="text-center max-w-2xl mx-auto mb-10">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] text-xs font-bold mb-2">
            <Sparkles className="w-3.5 h-3.5 text-[var(--color-brand)]" />
            <span>Verified Savings & Deals</span>
          </div>
          <h2 className="text-2xl sm:text-3xl font-black text-[var(--color-brand-dark)] tracking-tight font-['Outfit']">
            Special Deals & Coupons For You
          </h2>
          <p className="text-xs sm:text-sm text-[var(--color-muted)] mt-1">
            Apply active promotional vouchers directly to save instantly on your doorstep bookings.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {COUPONS.slice(0, 3).map((coupon) => {
            const isApplied = appliedCoupon?.code === coupon.code;
            return (
              <div
                key={coupon.code}
                className={`relative rounded-3xl p-6 border transition-all duration-300 flex flex-col justify-between overflow-hidden ${
                  isApplied
                    ? 'border-[var(--color-brand)] bg-[var(--color-brand-soft)] shadow-md ring-2 ring-[var(--color-brand)]/20'
                    : 'border-[var(--color-brand-light)] bg-gradient-to-br from-white to-[var(--color-brand-soft)]/40 hover:border-[var(--color-brand)] hover:shadow-xl'
                }`}
              >
                {/* Decorative background circle */}
                <div className="absolute -right-8 -bottom-8 w-28 h-28 rounded-full bg-[var(--color-brand-light)]/50 pointer-events-none" />

                <div>
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <span className="px-3 py-1 rounded-xl bg-[var(--color-brand)] text-white font-extrabold text-xs tracking-wider uppercase shadow-xs">
                      {coupon.discountType === 'FLAT' ? `₹${coupon.value} OFF` : `${coupon.value}% OFF`}
                    </span>
                    <span className="text-[11px] text-[var(--color-muted)] font-semibold">
                      Min order ₹{coupon.minBookingAmount}
                    </span>
                  </div>

                  <h3 className="font-bold text-[var(--color-brand-dark)] text-base mb-1.5 font-['Outfit']">{coupon.description}</h3>
                  <p className="text-xs text-[var(--color-muted)] mb-4">
                    Valid on all services scheduled through {coupon.expiry}.
                  </p>
                </div>

                <div className="pt-4 border-t border-dashed border-[var(--color-brand-light)] flex items-center justify-between gap-3">
                  {/* Code badge with copy */}
                  <div
                    onClick={() => handleCopy(coupon.code)}
                    className="flex items-center gap-2 px-3 py-1.5 bg-[var(--color-brand-soft)] hover:bg-[var(--color-brand-light)] rounded-xl cursor-pointer transition-colors border border-[var(--color-brand-light)]"
                    title="Click to copy"
                  >
                    <span className="font-mono font-bold text-xs text-[var(--color-brand-dark)] tracking-wider">
                      {coupon.code}
                    </span>
                    {copiedCode === coupon.code ? (
                      <CheckCircle2 className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                    ) : (
                      <Copy className="w-3.5 h-3.5 text-[var(--color-muted)]" />
                    )}
                  </div>

                  {/* Apply CTA */}
                  <button
                    onClick={() => handleApply(coupon.code)}
                    disabled={isApplied}
                    className={`px-4 py-2 rounded-xl text-xs font-bold transition-all shadow-xs cursor-pointer ${
                      isApplied
                        ? 'bg-[var(--color-brand-hover)] text-white cursor-default'
                        : 'bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white active:scale-95 shadow-[var(--color-brand)]/20'
                    }`}
                  >
                    {isApplied ? 'Applied ✓' : 'Apply Offer'}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};
