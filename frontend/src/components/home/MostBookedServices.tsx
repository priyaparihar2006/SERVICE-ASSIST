import React from 'react';
import { Service } from '../../types';
import { Star, Clock, CheckCircle2, ArrowRight, Heart } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import { ImageWithFallback } from '../common/ImageWithFallback';

interface MostBookedServicesProps {
  services: Service[];
  onSelectService: (slug: string) => void;
  onViewAll: () => void;
}

export const MostBookedServices: React.FC<MostBookedServicesProps> = ({
  services,
  onSelectService,
  onViewAll,
}) => {
  const { isFavorite, toggleFavorite } = useAuth();
  const { addItem } = useCart();

  const mostBooked = services.filter((s) => s.popular || s.rating >= 4.85);

  return (
    <section className="py-16 bg-white border-y border-[var(--color-brand-light)]/70">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section Header */}
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-8">
          <div>
            <div className="text-xs font-bold uppercase tracking-wider text-[var(--color-brand-hover)] mb-1 bg-[var(--color-brand-light)] px-3 py-1 rounded-full w-fit">
              Customer Favorites
            </div>
            <h2 className="text-2xl sm:text-3xl font-black text-[var(--color-brand-dark)] tracking-tight font-['Outfit'] mt-1">
              Most Booked Near You
            </h2>
          </div>
          <button
            onClick={onViewAll}
            className="inline-flex items-center gap-1.5 text-xs font-bold text-[var(--color-brand-hover)] hover:text-[var(--color-brand)] transition-colors group cursor-pointer"
          >
            <span>View All Services</span>
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </button>
        </div>

        {/* Horizontal Scroll Cards Container */}
        <div className="flex gap-6 overflow-x-auto pb-4 pt-1 -mx-4 px-4 sm:mx-0 sm:px-0 no-scrollbar snap-x">
          {mostBooked.map((service) => {
            const fav = isFavorite(service.id);
            return (
              <div
                key={service.id}
                className="group snap-start shrink-0 w-72 sm:w-80 rounded-3xl border border-[var(--color-brand-light)] bg-white hover:border-[var(--color-brand)] shadow-xs hover:shadow-xl transition-all duration-300 overflow-hidden flex flex-col justify-between"
              >
                {/* Image Container with Zoom */}
                <div className="relative h-44 overflow-hidden bg-[var(--color-brand-light)]/30">
                  <ImageWithFallback
                    src={service.image}
                    alt={service.name}
                    fallbackTitle={service.name}
                    className="w-full h-full object-cover object-center group-hover:scale-106 transition-transform duration-500 cursor-pointer"
                    onClick={() => onSelectService(service.slug)}
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent opacity-60 group-hover:opacity-40 transition-opacity pointer-events-none" />

                  {/* Favorite button */}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleFavorite(service.id);
                    }}
                    className={`absolute top-3 right-3 p-2 rounded-full backdrop-blur-md transition-all cursor-pointer ${
                      fav
                        ? 'bg-red-50 text-red-500 shadow-sm'
                        : 'bg-black/25 text-white hover:bg-white hover:text-gray-800'
                    }`}
                  >
                    <Heart className={`w-4 h-4 ${fav ? 'fill-red-500' : ''}`} />
                  </button>

                  {/* Category Pill */}
                  <span className="absolute bottom-3 left-3 px-2.5 py-0.5 rounded-full bg-white/95 backdrop-blur-xs text-[10px] font-bold text-[var(--color-brand-dark)] shadow-xs">
                    {service.categoryName}
                  </span>
                </div>

                {/* Content */}
                <div className="p-5 flex-1 flex flex-col justify-between">
                  <div onClick={() => onSelectService(service.slug)} className="cursor-pointer">
                    {/* Rating & Reviews */}
                    <div className="flex items-center gap-1.5 mb-1.5">
                      <div className="flex items-center gap-1 bg-[var(--color-brand-light)] px-2 py-0.5 rounded-md text-[var(--color-brand-hover)] text-xs font-extrabold">
                        <Star className="w-3.5 h-3.5 fill-[var(--color-brand)] text-[var(--color-brand)]" />
                        <span>{service.rating}</span>
                      </div>
                      <span className="text-[11px] text-[var(--color-muted)] font-medium">
                        ({(service.reviewsCount / 1000).toFixed(1)}k reviews)
                      </span>
                    </div>

                    <h3 className="font-bold text-base text-[var(--color-brand-dark)] mb-1.5 group-hover:text-[var(--color-brand)] transition-colors line-clamp-1 font-['Outfit']">
                      {service.name}
                    </h3>

                    <p className="text-xs text-[var(--color-muted)] line-clamp-2 leading-relaxed mb-4">
                      {service.shortDesc}
                    </p>
                  </div>

                  {/* Meta Details: Duration & Today availability */}
                  <div className="pt-3 border-t border-[var(--color-brand-light)]/80 space-y-3">
                    <div className="flex items-center justify-between text-[11px] text-[var(--color-muted)] font-medium">
                      <div className="flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5 text-[var(--color-muted)]" />
                        <span>~{service.durationMin} mins</span>
                      </div>
                      <div className="flex items-center gap-1 text-[var(--color-brand-hover)] font-semibold">
                        <CheckCircle2 className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                        <span>Available today</span>
                      </div>
                    </div>

                    {/* Price & Book Now CTA */}
                    <div className="flex items-center justify-between gap-2">
                      <div>
                        <span className="text-[10px] text-[var(--color-muted)] uppercase font-bold block">Starts at</span>
                        <div className="flex items-baseline gap-1.5">
                          <span className="text-base font-extrabold text-[var(--color-brand-dark)]">₹{service.startingPrice}</span>
                          {service.originalPrice && (
                            <span className="text-xs text-[var(--color-muted)] line-through">₹{service.originalPrice}</span>
                          )}
                        </div>
                      </div>

                      <button
                        onClick={() => addItem(service)}
                        className="px-4 py-2 bg-[var(--color-brand-light)] hover:bg-[var(--color-brand-hover)] text-[var(--color-brand-hover)] hover:text-white font-bold text-xs rounded-xl transition-all border border-[var(--color-brand)]/30 hover:border-transparent active:scale-95 shadow-xs cursor-pointer"
                      >
                        Book Now
                      </button>
                    </div>
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
