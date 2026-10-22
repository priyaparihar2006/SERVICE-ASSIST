import React, { useState } from 'react';
import { ArrowRight, Sparkles, LayoutGrid, ListFilter, ChevronDown, ChevronUp, Minimize2, Maximize2 } from 'lucide-react';
import { CategoryCard } from './CategoryCard';
import { ImageWithFallback } from '../common/ImageWithFallback';

interface PopularCategoriesProps {
  categories?: any[];
  onSelectCategory: (categoryId: string) => void;
  onExploreAll: () => void;
}

export const PopularCategories: React.FC<PopularCategoriesProps> = ({
  categories = [],
  onSelectCategory,
  onExploreAll,
}) => {
  const [viewMode, setViewMode] = useState<'grid' | 'compact'>('grid');
  const [isMinimized, setIsMinimized] = useState<boolean>(false);

  return (
    <section className="py-10 sm:py-14 bg-[var(--color-brand-soft)]/70 relative overflow-hidden transition-all duration-300">
      {/* Background subtle green organic blurs */}
      <div className="absolute top-1/2 left-0 -translate-y-1/2 w-80 h-80 rounded-full bg-[var(--color-brand-light)]/70 blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 right-0 w-96 h-96 rounded-full bg-[var(--color-brand-bright)]/10 blur-3xl pointer-events-none" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        {/* Section Header with Redesign / Minimize Controls */}
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-6">
          <div>
            <div className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-[var(--color-brand-hover)] mb-1.5 bg-[var(--color-brand-light)] px-3 py-1 rounded-full w-fit">
              <Sparkles className="w-3.5 h-3.5 text-[var(--color-brand)]" />
              <span>Verified Home Services</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-black text-[var(--color-brand-dark)] tracking-tight font-['Outfit']">
              Everything your home needs.
            </h2>
            <p className="text-xs sm:text-sm text-[var(--color-muted)] mt-0.5 max-w-xl">
              Trusted professionals for every service, right at your doorstep.
            </p>
          </div>

          {/* Action buttons: Compact / Minimize toggles */}
          <div className="flex items-center flex-wrap gap-2">
            {/* View Mode Toggle: Grid vs Compact */}
            <div className="flex items-center bg-white p-1 rounded-xl border border-[var(--color-brand-light)] shadow-2xs">
              <button
                type="button"
                onClick={() => {
                  setViewMode('grid');
                  setIsMinimized(false);
                }}
                className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-all flex items-center gap-1 cursor-pointer ${
                  viewMode === 'grid' && !isMinimized
                    ? 'bg-[var(--color-brand)] text-white shadow-xs'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
                title="Expanded Grid View"
              >
                <LayoutGrid className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Grid</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  setViewMode('compact');
                  setIsMinimized(false);
                }}
                className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-all flex items-center gap-1 cursor-pointer ${
                  viewMode === 'compact' && !isMinimized
                    ? 'bg-[var(--color-brand)] text-white shadow-xs'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
                title="Compact Minimalist View"
              >
                <ListFilter className="w-3.5 h-3.5" />
                <span>Compact</span>
              </button>
            </div>

            {/* Minimize / Expand Toggle Button */}
            <button
              type="button"
              onClick={() => setIsMinimized((prev) => !prev)}
              className="inline-flex items-center gap-1 text-xs font-bold text-gray-700 bg-white hover:bg-gray-50 border border-[var(--color-brand-light)] px-3 py-1.5 rounded-xl shadow-2xs transition-all cursor-pointer"
              title={isMinimized ? 'Expand Services' : 'Minimize Services'}
            >
              {isMinimized ? (
                <>
                  <Maximize2 className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                  <span>Expand All Services</span>
                </>
              ) : (
                <>
                  <Minimize2 className="w-3.5 h-3.5 text-gray-500" />
                  <span>Minimize</span>
                </>
              )}
            </button>

            {/* Explore All Link */}
            <button
              onClick={onExploreAll}
              className="inline-flex items-center gap-1 text-xs font-bold text-[var(--color-brand-hover)] hover:text-[var(--color-brand)] transition-colors group cursor-pointer bg-white px-3 py-1.5 rounded-xl border border-[var(--color-brand-light)] shadow-2xs hover:border-[var(--color-brand)]"
            >
              <span>All Services</span>
              <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
            </button>
          </div>
        </div>

        {/* Minimized View Bar */}
        {isMinimized ? (
          <div className="bg-white/90 backdrop-blur-xs rounded-2xl p-4 border border-[var(--color-brand-light)] shadow-sm flex flex-wrap items-center justify-between gap-3 animate-in fade-in duration-200">
            <div className="flex items-center gap-2 overflow-x-auto py-1">
              {categories.map((cat) => (
                <button
                  key={cat.id}
                  onClick={() => onSelectCategory(cat.id)}
                  className="px-3 py-1.5 rounded-xl bg-gray-50 hover:bg-[var(--color-brand-light)] text-gray-800 hover:text-[var(--color-brand-dark)] text-xs font-bold border border-gray-200 transition-colors whitespace-nowrap cursor-pointer flex items-center gap-1.5"
                >
                  <span>{cat.name}</span>
                  <ArrowRight className="w-3 h-3 text-[var(--color-brand)] opacity-60" />
                </button>
              ))}
            </div>
            <button
              onClick={() => setIsMinimized(false)}
              className="text-xs font-bold text-[var(--color-brand)] hover:underline flex items-center gap-1 cursor-pointer"
            >
              <ChevronDown className="w-3.5 h-3.5" />
              <span>Show Full Categories</span>
            </button>
          </div>
        ) : viewMode === 'compact' ? (
          /* Redesigned Compact View: streamlined cards with reduced height */
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3 sm:gap-4 animate-in fade-in duration-200">
            {categories.map((cat) => (
              <div
                key={cat.id}
                onClick={() => onSelectCategory(cat.id)}
                className="group p-3 rounded-2xl bg-white border border-[var(--color-brand-light)] hover:border-[var(--color-brand)] shadow-2xs hover:shadow-md transition-all duration-200 cursor-pointer flex items-center gap-3"
              >
                <div className="w-12 h-12 rounded-xl overflow-hidden shrink-0 border border-gray-100 bg-gray-50">
                  <ImageWithFallback
                    src={cat.image}
                    alt={cat.name}
                    fallbackTitle={cat.name}
                    className="w-full h-full object-cover group-hover:scale-108 transition-transform duration-300"
                  />
                </div>
                <div className="min-w-0 flex-1">
                  <h4 className="font-bold text-xs sm:text-sm text-[var(--color-brand-dark)] group-hover:text-[var(--color-brand)] truncate font-['Outfit']">
                    {cat.name}
                  </h4>
                  <span className="text-[11px] text-[var(--color-muted)] flex items-center gap-1 mt-0.5">
                    <span>Explore</span>
                    <ArrowRight className="w-3 h-3 text-[var(--color-brand)] group-hover:translate-x-0.5 transition-transform" />
                  </span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          /* Standard 4-column balanced grid */
          <div className="grid grid-cols-2 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-4 gap-4 sm:gap-6 animate-in fade-in duration-200">
            {categories.map((cat) => (
              <CategoryCard
                key={cat.id}
                category={cat}
                onClick={() => onSelectCategory(cat.id)}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  );
};
