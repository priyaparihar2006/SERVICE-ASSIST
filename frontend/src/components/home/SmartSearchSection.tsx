import React, { useState } from 'react';
import { useLocation } from '../../context/LocationContext';
import { Search, MapPin, ChevronDown, ArrowRight, Sparkles } from 'lucide-react';

interface SmartSearchSectionProps {
  onSearch: (query: string) => void;
}

export const SmartSearchSection: React.FC<SmartSearchSectionProps> = ({ onSearch }) => {
  const { selectedCity, openLocationModal } = useLocation();
  const [query, setQuery] = useState('');

  const popularSearches = [
    { label: 'AC Jet Service', query: 'ac jet' },
    { label: 'Bathroom Deep Cleaning', query: 'bathroom' },
    { label: 'Salon at Home', query: 'salon' },
    { label: 'Electrician on Demand', query: 'electrician' },
    { label: 'Plumbing Leaks', query: 'plumbing' },
    { label: 'Sofa Shampooing', query: 'sofa' },
    { label: 'Cockroach Pest Shield', query: 'pest' },
  ];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      onSearch(query.trim());
    }
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

          {/* Search input field */}
          <div className="flex-1 relative flex items-center">
            <Search className="absolute left-4 w-5 h-5 text-[var(--color-muted)]" />
            <input
              type="text"
              placeholder="What service do you need? (e.g. 'AC not cooling', 'Clean sofa', 'Salon facial')..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="w-full pl-12 pr-4 py-3.5 bg-[var(--color-brand-soft)]/70 hover:bg-[var(--color-brand-soft)] focus:bg-white border border-[var(--color-brand-light)] rounded-2xl text-xs sm:text-sm text-[var(--color-ink)] focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)] transition-all"
            />
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

        {/* Popular chips */}
        <div className="mt-4 pt-3 border-t border-[var(--color-brand-light)]/70 flex items-center gap-2 overflow-x-auto text-xs no-scrollbar">
          <span className="text-[var(--color-muted)] text-[11px] font-bold uppercase tracking-wider shrink-0 flex items-center gap-1">
            <Sparkles className="w-3 h-3 text-[var(--color-brand)]" />
            <span>Popular:</span>
          </span>
          <div className="flex items-center gap-1.5 flex-nowrap">
            {popularSearches.map((item) => (
              <button
                key={item.label}
                type="button"
                onClick={() => onSearch(item.query)}
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
