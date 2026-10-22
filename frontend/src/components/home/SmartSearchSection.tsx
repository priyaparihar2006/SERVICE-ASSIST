import React, { useState, useRef, useEffect } from 'react';
import { useLocation } from '../../context/LocationContext';
import { Search, MapPin, ChevronDown, ArrowRight, Sparkles, FolderKanban, CheckCircle2 } from 'lucide-react';
import { findMatchingCategory, getCategorySuggestions, CategoryMatch } from '../../utils/categorySearch';
import { Category } from '../../types';

interface SmartSearchSectionProps {
  categories?: Category[];
  onSearch: (query: string) => void;
  onSelectCategory?: (categoryId: string) => void;
}

export const SmartSearchSection: React.FC<SmartSearchSectionProps> = ({
  categories = [],
  onSearch,
  onSelectCategory,
}) => {
  const { selectedCity, openLocationModal } = useLocation();
  const [query, setQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Prominent popular categories/searches with Carpenter first as requested
  const popularCategoryChips = [
    { label: 'Carpenter', categoryId: 'cat-carpenter', query: 'carpenter' },
    { label: 'Electrician', categoryId: 'cat-electrician', query: 'electrician' },
    { label: 'Plumber', categoryId: 'cat-plumber', query: 'plumber' },
    { label: 'AC Jet Clean', categoryId: 'cat-ac-appliances', query: 'ac repair' },
    { label: 'Home Deep Cleaning', categoryId: 'cat-cleaning', query: 'cleaning' },
    { label: 'Salon at Home', categoryId: 'cat-beauty', query: 'salon' },
    { label: 'Pest Control', categoryId: 'cat-pest-control', query: 'pest control' },
    { label: 'Painting', categoryId: 'cat-painting', query: 'painting' },
  ];

  // Live matching category suggestions as user types
  const suggestions: CategoryMatch[] = getCategorySuggestions(query, categories, 6);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleCategoryNavigate = (categoryId: string) => {
    setIsOpen(false);
    if (onSelectCategory) {
      onSelectCategory(categoryId);
    } else {
      window.location.href = `/services?category=${encodeURIComponent(categoryId)}`;
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;

    setIsOpen(false);

    // 1. Check if the search matches a category directly (e.g. "carpenter", "plumber", etc.)
    const matchedCategory = findMatchingCategory(query.trim(), categories);
    if (matchedCategory) {
      handleCategoryNavigate(matchedCategory.id);
      return;
    }

    // 2. Otherwise navigate to search query
    onSearch(query.trim());
  };

  return (
    <div className="relative -mt-8 z-20 max-w-5xl mx-auto px-4 sm:px-6">
      <div className="bg-white rounded-3xl shadow-xl shadow-[var(--color-brand-dark)]/5 border border-[var(--color-brand-light)] p-4 sm:p-6">
        <form onSubmit={handleSubmit} className="flex flex-col md:flex-row items-stretch gap-3">
          {/* Location button */}
          <button
            type="button"
            onClick={openLocationModal}
            className="flex items-center justify-between gap-3 px-4 py-3.5 rounded-2xl bg-[var(--color-brand-soft)] hover:bg-[var(--color-brand-light)]/70 border border-[var(--color-brand-light)] text-left transition-all shrink-0 cursor-pointer"
          >
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-[var(--color-brand-light)] flex items-center justify-center text-[var(--color-brand-hover)]">
                <MapPin className="w-4 h-4" />
              </div>
              <div>
                <span className="text-[10px] uppercase font-bold text-[var(--color-muted)] block tracking-wider">City</span>
                <span className="text-xs font-bold text-[var(--color-brand-dark)] block truncate">{selectedCity.name}</span>
              </div>
            </div>
            <ChevronDown className="w-3.5 h-3.5 text-[var(--color-muted)] ml-1" />
          </button>

          {/* Search input field with Category autocomplete */}
          <div className="flex-1 relative" ref={dropdownRef}>
            <div className="relative flex items-center h-full">
              <Search className="absolute left-4 w-5 h-5 text-[var(--color-muted)] pointer-events-none" />
              <input
                type="text"
                placeholder="Search category or service (e.g. Carpenter, Plumber, AC Repair)..."
                value={query}
                onFocus={() => setIsOpen(true)}
                onChange={(e) => {
                  setQuery(e.target.value);
                  setIsOpen(true);
                }}
                className="w-full pl-12 pr-4 py-3.5 bg-[var(--color-brand-soft)]/70 hover:bg-[var(--color-brand-soft)] focus:bg-white border border-[var(--color-brand-light)] rounded-2xl text-xs sm:text-sm text-[var(--color-ink)] focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)] transition-all"
              />
            </div>

            {/* Instant Category Suggestions Dropdown */}
            {isOpen && (
              <div className="absolute left-0 right-0 top-full mt-2 bg-white rounded-2xl shadow-2xl border border-[var(--color-brand-light)] py-2 z-50 overflow-hidden animate-in fade-in duration-150">
                <div className="px-4 py-2 border-b border-gray-100 flex items-center justify-between text-[11px] font-bold uppercase tracking-wider text-gray-400">
                  <span className="flex items-center gap-1.5">
                    <FolderKanban className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                    Categories ({suggestions.length})
                  </span>
                  <span>Tap to open category</span>
                </div>

                <div className="max-h-64 overflow-y-auto divide-y divide-gray-50">
                  {suggestions.map((cat) => (
                    <button
                      key={cat.id}
                      type="button"
                      onClick={() => handleCategoryNavigate(cat.id)}
                      className="w-full px-4 py-3 text-left hover:bg-[var(--color-brand-soft)] transition-colors flex items-center justify-between group cursor-pointer"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-xl bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] flex items-center justify-center font-bold text-xs shrink-0 group-hover:scale-105 transition-transform">
                          {cat.name.slice(0, 1)}
                        </div>
                        <div>
                          <p className="text-xs font-bold text-[var(--color-brand-dark)] group-hover:text-[var(--color-brand)] flex items-center gap-1.5">
                            <span>{cat.name}</span>
                            <span className="text-[10px] font-semibold text-gray-400 font-normal">
                              • Category
                            </span>
                          </p>
                          {cat.description && (
                            <p className="text-[11px] text-gray-500 line-clamp-1">{cat.description}</p>
                          )}
                        </div>
                      </div>
                      <ArrowRight className="w-4 h-4 text-gray-300 group-hover:text-[var(--color-brand)] group-hover:translate-x-0.5 transition-all" />
                    </button>
                  ))}
                </div>

                <div className="px-4 py-2 bg-gray-50/70 border-t border-gray-100 flex items-center justify-between text-[11px] text-gray-500">
                  <span>Press <kbd className="px-1.5 py-0.5 bg-white border border-gray-200 rounded text-[10px] font-mono">Enter</kbd> to search</span>
                  {query && (
                    <button
                      type="submit"
                      className="text-[var(--color-brand)] font-bold hover:underline cursor-pointer"
                    >
                      Search all for "{query}"
                    </button>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Submit Search Button */}
          <button
            type="submit"
            className="flex items-center justify-center gap-2 px-7 py-3.5 bg-[var(--color-brand-hover)] hover:bg-[var(--color-brand)] text-white font-bold text-sm rounded-2xl transition-all shadow-md shadow-[var(--color-brand-hover)]/20 active:scale-[0.98] shrink-0 cursor-pointer"
          >
            <span>Search</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>

        {/* Popular chips with Carpenter, Electrician, Plumber, etc. */}
        <div className="mt-4 pt-3 border-t border-[var(--color-brand-light)]/70 flex items-center gap-2 overflow-x-auto text-xs no-scrollbar">
          <span className="text-[var(--color-muted)] text-[11px] font-bold uppercase tracking-wider shrink-0 flex items-center gap-1">
            <Sparkles className="w-3 h-3 text-[var(--color-brand)]" />
            <span>Popular Categories:</span>
          </span>
          <div className="flex items-center gap-1.5 flex-nowrap">
            {popularCategoryChips.map((item) => (
              <button
                key={item.label}
                type="button"
                onClick={() => handleCategoryNavigate(item.categoryId)}
                className="whitespace-nowrap px-3 py-1 rounded-xl bg-[var(--color-brand-soft)] hover:bg-[var(--color-brand-light)] hover:text-[var(--color-brand-hover)] text-[var(--color-ink)] text-xs font-semibold transition-colors border border-[var(--color-brand-light)] hover:border-[var(--color-brand)]/40 cursor-pointer"
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
