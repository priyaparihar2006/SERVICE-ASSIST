import React from 'react';
import { Service } from '../../types';
import { Sparkles, Star, Clock, ArrowRight, ShieldCheck } from 'lucide-react';
import { useCart } from '../../context/CartContext';
import { ImageWithFallback } from '../common/ImageWithFallback';

interface RecommendedForYouProps {
  services: Service[];
  onSelectService: (slug: string) => void;
}

export const RecommendedForYou: React.FC<RecommendedForYouProps> = ({
  services,
  onSelectService,
}) => {
  const { addItem } = useCart();

  // Pick top 3 services with recommendation reasons
  const recommendedList = services.slice(1, 4);

  return (
    <section className="py-16 bg-[#F2FCF7]/40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-2 mb-2">
          <div className="w-6 h-6 rounded-lg bg-[#DDF7EC] flex items-center justify-center text-[#087F5B]">
            <Sparkles className="w-3.5 h-3.5 text-[#0B9F6E]" />
          </div>
          <span className="text-xs font-bold uppercase tracking-wider text-[#087F5B]">
            Smart Match AI
          </span>
        </div>

        <h2 className="text-2xl sm:text-3xl font-black text-[#103C35] tracking-tight mb-2 font-['Outfit']">
          Recommended For You
        </h2>
        <p className="text-xs sm:text-sm text-[#6B817C] max-w-xl mb-8">
          Personalized home service recommendations based on your seasonal needs, home size, and customer preferences in your neighborhood.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {recommendedList.map((service, idx) => {
            const reasons = [
              'Top booked this weekend in your area',
              'Because seasonal maintenance is due',
              'Frequently booked together with AC care',
            ];
            const reason = service.recommendedReason || reasons[idx % reasons.length];

            return (
              <div
                key={service.id}
                className="group relative rounded-3xl bg-white border border-[#DDF7EC] hover:border-[#0B9F6E] shadow-xs hover:shadow-xl transition-all duration-300 p-5 flex flex-col justify-between"
              >
                {/* Reason Banner */}
                <div className="mb-4 inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#DDF7EC] text-[11px] font-bold text-[#087F5B] w-fit">
                  <Sparkles className="w-3 h-3 text-[#0B9F6E]" />
                  <span>{reason}</span>
                </div>

                <div className="flex gap-4 mb-4">
                  <ImageWithFallback
                    src={service.image}
                    alt={service.name}
                    fallbackTitle={service.name}
                    className="w-24 h-24 rounded-2xl object-cover shrink-0 cursor-pointer group-hover:scale-105 transition-transform"
                    onClick={() => onSelectService(service.slug)}
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-1 text-xs font-bold text-[#087F5B] mb-1">
                      <Star className="w-3.5 h-3.5 fill-[#0B9F6E] text-[#0B9F6E]" />
                      <span>{service.rating}</span>
                      <span className="text-[10px] text-[#6B817C]">({service.reviewsCount})</span>
                    </div>

                    <h3
                      onClick={() => onSelectService(service.slug)}
                      className="font-bold text-sm text-[#103C35] group-hover:text-[#0B9F6E] transition-colors line-clamp-2 cursor-pointer mb-1 font-['Outfit']"
                    >
                      {service.name}
                    </h3>
                    <p className="text-xs text-[#6B817C] line-clamp-2 leading-relaxed">
                      {service.shortDesc}
                    </p>
                  </div>
                </div>

                {/* Bottom Row */}
                <div className="pt-3 border-t border-[#DDF7EC]/70 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] uppercase font-bold text-[#6B817C] block">Starting at</span>
                    <span className="text-base font-extrabold text-[#103C35]">₹{service.startingPrice}</span>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => onSelectService(service.slug)}
                      className="px-3 py-2 text-xs font-bold text-[#6B817C] hover:text-[#0B9F6E] transition-colors cursor-pointer"
                    >
                      Details
                    </button>
                    <button
                      onClick={() => addItem(service)}
                      className="px-4 py-2 bg-[#0B9F6E] hover:bg-[#087F5B] text-white font-bold text-xs rounded-xl shadow-xs transition-all active:scale-95 cursor-pointer shadow-[#0B9F6E]/20"
                    >
                      Add +
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};
