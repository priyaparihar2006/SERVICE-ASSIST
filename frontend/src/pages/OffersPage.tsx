import React, { useState } from 'react';
import { COUPONS } from '../../server/seedData';
import { Sparkles, CheckCircle2, Copy } from 'lucide-react';
import { useCart } from '../context/CartContext';

export const OffersPage: React.FC<{ onNavigate: (path: string) => void }> = ({ onNavigate }) => {
  const { applyCoupon, openCartDrawer, appliedCoupon } = useCart();
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  const handleApply = async (code: string) => {
    await applyCoupon(code);
    openCartDrawer();
  };

  const handleCopy = (code: string) => {
    navigator.clipboard.writeText(code);
    setCopiedCode(code);
    setTimeout(() => setCopiedCode(null), 2000);
  };

  return (
    <div className="min-h-screen bg-[#FFF8F2]/30 py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-2xl mx-auto mb-12">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#FFF1E5] text-[#E85D04] text-xs font-bold mb-2">
            <Sparkles className="w-3.5 h-3.5 text-[#FF7A00]" />
            <span>Active Discount Vouchers</span>
          </div>
          <h1 className="text-3xl sm:text-4xl font-black text-[#15252B] tracking-tight font-['Outfit']">
            Service Assist Deals & Promotional Offers
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-2">
            Save more on high-standard AC servicing, home deep cleaning, beauty salon, and repairs.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-12">
          {COUPONS.map((coupon) => {
            const isApplied = appliedCoupon?.code === coupon.code;
            return (
              <div
                key={coupon.code}
                className={`bg-white rounded-3xl p-6 border transition-all flex flex-col justify-between shadow-xs ${
                  isApplied
                    ? 'border-[#FF7A00] ring-2 ring-[#FF7A00]/20 bg-[#FFF1E5]/30'
                    : 'border-gray-100 hover:border-[#FF9A3D] hover:shadow-lg'
                }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <span className="px-3 py-1 rounded-xl bg-[#FF7A00] text-white font-black text-xs font-['Outfit']">
                      {coupon.discountType === 'FLAT' ? `FLAT ₹${coupon.value} OFF` : `${coupon.value}% OFF`}
                    </span>
                    <span className="text-[11px] text-gray-400 font-semibold">
                      Min Booking: ₹{coupon.minBookingAmount}
                    </span>
                  </div>

                  <h3 className="font-extrabold text-base text-gray-900 mb-2 font-['Outfit']">{coupon.description}</h3>
                  <p className="text-xs text-gray-500 mb-6 leading-relaxed">
                    Applicable on all eligible home categories. Can be combined with standard 30-day warranty.
                  </p>
                </div>

                <div className="pt-4 border-t border-dashed border-gray-200 flex items-center justify-between">
                  <div
                    onClick={() => handleCopy(coupon.code)}
                    className="flex items-center gap-1.5 px-3 py-1.5 bg-gray-50 hover:bg-[#FFF8F2] rounded-xl cursor-pointer border border-gray-200"
                    title="Click to copy"
                  >
                    <span className="font-mono font-bold text-xs text-gray-800">{coupon.code}</span>
                    {copiedCode === coupon.code ? (
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                    ) : (
                      <Copy className="w-3.5 h-3.5 text-gray-400" />
                    )}
                  </div>

                  <button
                    onClick={() => handleApply(coupon.code)}
                    disabled={isApplied}
                    className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                      isApplied
                        ? 'bg-emerald-600 text-white cursor-default'
                        : 'bg-[#FF7A00] hover:bg-[#E85D04] text-white shadow-xs shadow-orange-500/20'
                    }`}
                  >
                    {isApplied ? 'Applied ✓' : 'Apply to Cart'}
                  </button>
                </div>
              </div>
            );
          })}
        </div>

        {/* CTA */}
        <div className="bg-gradient-to-r from-[#15252B] to-[#1E343C] text-white rounded-3xl p-8 text-center max-w-3xl mx-auto border border-orange-500/20">
          <h3 className="text-2xl font-black mb-2 font-['Outfit']">Ready to book your service?</h3>
          <p className="text-xs text-orange-100/80 max-w-md mx-auto mb-6">
            Choose your preferred time slot and enjoy verified home care with 100% satisfaction guarantee.
          </p>
          <button
            onClick={() => onNavigate('/services')}
            className="px-6 py-3 bg-[#FF7A00] hover:bg-[#E85D04] text-white font-bold text-xs rounded-xl shadow-lg shadow-orange-500/30 transition-all cursor-pointer"
          >
            Explore All Services Now
          </button>
        </div>
      </div>
    </div>
  );
};
