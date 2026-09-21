import React from 'react';
import { ArrowRight } from 'lucide-react';
import { ServiceCategory } from '../../data/serviceCategories';
import { ImageWithFallback } from '../common/ImageWithFallback';

interface CategoryCardProps {
  category: ServiceCategory;
  onClick: () => void;
}

export const CategoryCard: React.FC<CategoryCardProps> = ({ category, onClick }) => {
  return (
    <div
      onClick={onClick}
      className="group relative rounded-2xl sm:rounded-3xl border border-[var(--color-brand-light)] bg-white hover:border-[var(--color-brand)] shadow-xs hover:shadow-xl hover:-translate-y-1.5 transition-all duration-300 cursor-pointer overflow-hidden flex flex-col justify-between"
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onClick();
        }
      }}
    >
      {/* Category Image - Consistent dimensions, object-cover */}
      <div className="relative h-40 sm:h-38 md:h-44 w-full overflow-hidden bg-[var(--color-brand-light)]/30">
        <ImageWithFallback
          src={category.image}
          alt={category.alt}
          fallbackTitle={category.name}
          className="w-full h-full object-cover object-center group-hover:scale-106 transition-transform duration-500 ease-out"
        />

        {/* Soft bottom gradient overlay for contrast */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/35 via-transparent to-transparent pointer-events-none" />

        {/* Badge */}
        {category.badge && (
          <span className="absolute top-3 right-3 px-2.5 py-0.5 rounded-full bg-white/95 backdrop-blur-xs text-[10px] font-bold text-[var(--color-brand-hover)] shadow-xs border border-[var(--color-brand-light)]">
            {category.badge}
          </span>
        )}
      </div>

      {/* Card Body */}
      <div className="p-4 sm:p-4.5 flex-1 flex flex-col justify-between bg-white">
        <div>
          {/* Category Title + Arrow */}
          <div className="flex items-center justify-between gap-2 mb-1">
            <h3 className="font-extrabold text-sm sm:text-base text-[var(--color-brand-dark)] group-hover:text-[var(--color-brand)] transition-colors line-clamp-1 font-['Outfit']">
              {category.name}
            </h3>
            <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-[var(--color-brand-soft)] text-[var(--color-muted)] group-hover:bg-[var(--color-brand-light)] group-hover:text-[var(--color-brand-hover)] transition-all shrink-0">
              <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
            </span>
          </div>

          {/* Description */}
          <p className="text-[11px] sm:text-xs text-[var(--color-muted)] line-clamp-1 font-normal mb-2.5">
            {category.description || category.subtitle}
          </p>
        </div>

        {/* Card Footer: Service Count & Arrow */}
        <div className="pt-2.5 border-t border-[var(--color-brand-light)]/80 flex items-center justify-between text-[11px] font-semibold text-[var(--color-muted)] group-hover:text-[var(--color-brand-hover)] transition-colors">
          <span>{category.servicesCount} services</span>
          <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--color-brand)] opacity-0 group-hover:opacity-100 transition-opacity">
            Explore →
          </span>
        </div>
      </div>
    </div>
  );
};
