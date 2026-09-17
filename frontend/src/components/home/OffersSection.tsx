import React, { useState } from 'react';
import { COUPONS } from '../../../server/seedData';
import { Tag, Sparkles, CheckCircle2, Copy, ArrowRight } from 'lucide-react';
import { useCart } from '../../context/CartContext';

export const OffersSection: React.FC = () => {
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
        <div className="text-center max-w-2xl mx-auto mb-10">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#DDF7EC] text-[#087F5B] text-xs font-bold mb-2">
            <Sparkles className="w-3.5 h-3.5 text-[#0B9F6E]" />
            <span>Verified Savings & Deals</span>
          </div>
          <h2 className="text-2xl sm:text-3xl font-black text-[#103C35] tracking-tight font-['Outfit']">
            Special Deals & Coupons For You
          </h2>
          <p className="text-xs sm:text-sm text-[#6B817C] mt-1">
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
                    ? 'border-[#0B9F6E] bg-[#F2FCF7] shadow-md ring-2 ring-[#0B9F6E]/20'
                    : 'border-[#DDF7EC] bg-gradient-to-br from-white to-[#F2FCF7]/40 hover:border-[#0B9F6E] hover:shadow-xl'
                }`}
              >
                {/* Decorative background circle */}
                <div className="absolute -right-8 -bottom-8 w-28 h-28 rounded-full bg-[#DDF7EC]/50 pointer-events-none" />

                <div>
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <span className="px-3 py-1 rounded-xl bg-[#0B9F6E] text-white font-extrabold text-xs tracking-wider uppercase shadow-xs">
                      {coupon.discountType === 'FLAT' ? `₹${coupon.value} OFF` : `${coupon.value}% OFF`}
                    </span>
                    <span className="text-[11px] text-[#6B817C] font-semibold">
                      Min order ₹{coupon.minBookingAmount}
                    </span>
                  </div>

                  <h3 className="font-bold text-[#103C35] text-base mb-1.5 font-['Outfit']">{coupon.description}</h3>
                  <p className="text-xs text-[#6B817C] mb-4">
                    Valid on all services scheduled through {coupon.expiry}.
                  </p>
                </div>

                <div className="pt-4 border-t border-dashed border-[#DDF7EC] flex items-center justify-between gap-3">
                  {/* Code badge with copy */}
                  <div
                    onClick={() => handleCopy(coupon.code)}
                    className="flex items-center gap-2 px-3 py-1.5 bg-[#F2FCF7] hover:bg-[#DDF7EC] rounded-xl cursor-pointer transition-colors border border-[#DDF7EC]"
                    title="Click to copy"
                  >
                    <span className="font-mono font-bold text-xs text-[#103C35] tracking-wider">
                      {coupon.code}
                    </span>
                    {copiedCode === coupon.code ? (
                      <CheckCircle2 className="w-3.5 h-3.5 text-[#0B9F6E]" />
                    ) : (
                      <Copy className="w-3.5 h-3.5 text-[#6B817C]" />
                    )}
                  </div>

                  {/* Apply CTA */}
                  <button
                    onClick={() => handleApply(coupon.code)}
                    disabled={isApplied}
                    className={`px-4 py-2 rounded-xl text-xs font-bold transition-all shadow-xs cursor-pointer ${
                      isApplied
                        ? 'bg-[#087F5B] text-white cursor-default'
                        : 'bg-[#0B9F6E] hover:bg-[#087F5B] text-white active:scale-95 shadow-[#0B9F6E]/20'
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
