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
    <section className="py-16 bg-[var(--color-brand-soft)]/40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-2 mb-2">
          <div className="w-6 h-6 rounded-lg bg-[var(--color-brand-light)] flex items-center justify-center text-[var(--color-brand-hover)]">
            <Sparkles className="w-3.5 h-3.5 text-[var(--color-brand)]" />
          </div>
          <span className="text-xs font-bold uppercase tracking-wider text-[var(--color-brand-hover)]">
            Smart Match AI
          </span>
        </div>

        <h2 className="text-2xl sm:text-3xl font-black text-[var(--color-brand-dark)] tracking-tight mb-2 font-['Outfit']">
          Recommended For You
        </h2>
        <p className="text-xs sm:text-sm text-[var(--color-muted)] max-w-xl mb-8">
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
                className="group relative rounded-3xl bg-white border border-[var(--color-brand-light)] hover:border-[var(--color-brand)] shadow-xs hover:shadow-xl transition-all duration-300 p-5 flex flex-col justify-between"
              >
                {/* Reason Banner */}
                <div className="mb-4 inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[var(--color-brand-light)] text-[11px] font-bold text-[var(--color-brand-hover)] w-fit">
                  <Sparkles className="w-3 h-3 text-[var(--color-brand)]" />
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
                    <div className="flex items-center gap-1 text-xs font-bold text-[var(--color-brand-hover)] mb-1">
                      <Star className="w-3.5 h-3.5 fill-[var(--color-brand)] text-[var(--color-brand)]" />
                      <span>{service.rating}</span>
                      <span className="text-[10px] text-[var(--color-muted)]">({service.reviewsCount})</span>
                    </div>

                    <h3
                      onClick={() => onSelectService(service.slug)}
                      className="font-bold text-sm text-[var(--color-brand-dark)] group-hover:text-[var(--color-brand)] transition-colors line-clamp-2 cursor-pointer mb-1 font-['Outfit']"
                    >
                      {service.name}
                    </h3>
                    <p className="text-xs text-[var(--color-muted)] line-clamp-2 leading-relaxed">
                      {service.shortDesc}
                    </p>
                  </div>
                </div>

                {/* Bottom Row */}
                <div className="pt-3 border-t border-[var(--color-brand-light)]/70 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] uppercase font-bold text-[var(--color-muted)] block">Starting at</span>
                    <span className="text-base font-extrabold text-[var(--color-brand-dark)]">₹{service.startingPrice}</span>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => onSelectService(service.slug)}
                      className="px-3 py-2 text-xs font-bold text-[var(--color-muted)] hover:text-[var(--color-brand)] transition-colors cursor-pointer"
                    >
                      Details
                    </button>
                    <button
                      onClick={() => addItem(service)}
                      className="px-4 py-2 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs rounded-xl shadow-xs transition-all active:scale-95 cursor-pointer shadow-[var(--color-brand)]/20"
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
