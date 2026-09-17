import React from 'react';
import { ArrowRight, Sparkles } from 'lucide-react';
import { SERVICE_CATEGORIES } from '../../data/serviceCategories';
import { CategoryCard } from './CategoryCard';

interface PopularCategoriesProps {
  categories?: any[];
  onSelectCategory: (categoryId: string) => void;
  onExploreAll: () => void;
}

export const PopularCategories: React.FC<PopularCategoriesProps> = ({
  onSelectCategory,
  onExploreAll,
}) => {
  return (
    <section className="py-14 sm:py-18 bg-[#F2FCF7]/70 relative overflow-hidden">
      {/* Background subtle green organic blurs */}
      <div className="absolute top-1/2 left-0 -translate-y-1/2 w-80 h-80 rounded-full bg-[#DDF7EC]/70 blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 right-0 w-96 h-96 rounded-full bg-[#19C995]/10 blur-3xl pointer-events-none" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        {/* Section Header */}
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-8 sm:mb-10">
          <div>
            <div className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-[#087F5B] mb-1.5 bg-[#DDF7EC] px-3 py-1 rounded-full w-fit">
              <Sparkles className="w-3.5 h-3.5 text-[#0B9F6E]" />
              <span>Verified Home Services</span>
            </div>
            <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-[#103C35] tracking-tight font-['Outfit']">
              Everything your home needs.
            </h2>
            <p className="text-xs sm:text-sm text-[#6B817C] mt-1 max-w-xl">
              Trusted professionals for every service, right at your doorstep.
            </p>
          </div>

          <button
            onClick={onExploreAll}
            className="inline-flex items-center gap-1.5 text-xs font-bold text-[#087F5B] hover:text-[#0B9F6E] transition-colors group cursor-pointer self-start sm:self-auto bg-white px-4 py-2.5 rounded-xl border border-[#DDF7EC] shadow-2xs hover:border-[#0B9F6E]"
          >
            <span>Explore All 40+ Services</span>
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </button>
        </div>

        {/* 14 Service Category Image Cards Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3.5 sm:gap-5">
          {SERVICE_CATEGORIES.map((cat) => (
            <CategoryCard
              key={cat.id}
              category={cat}
              onClick={() => onSelectCategory(cat.id)}
            />
          ))}
        </div>
      </div>
    </section>
  );
};
