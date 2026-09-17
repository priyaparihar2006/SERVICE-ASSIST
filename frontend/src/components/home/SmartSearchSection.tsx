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
      <div className="bg-white rounded-3xl shadow-xl shadow-[#103C35]/5 border border-[#DDF7EC] p-4 sm:p-6">
        <form onSubmit={handleSubmit} className="flex flex-col md:flex-row items-stretch gap-3">
          {/* Location button */}
          <button
            type="button"
            onClick={openLocationModal}
            className="flex items-center justify-between gap-3 px-4 py-3.5 rounded-2xl bg-[#F2FCF7] hover:bg-[#DDF7EC]/70 border border-[#DDF7EC] text-left transition-all shrink-0 cursor-pointer"
          >
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-[#DDF7EC] flex items-center justify-center text-[#087F5B]">
                <MapPin className="w-4 h-4" />
              </div>
              <div>
                <span className="text-[10px] uppercase font-bold text-[#6B817C] block tracking-wider">City</span>
                <span className="text-xs font-bold text-[#103C35] block truncate">{selectedCity.name}</span>
              </div>
            </div>
            <ChevronDown className="w-3.5 h-3.5 text-[#6B817C] ml-1" />
          </button>

          {/* Search input field */}
          <div className="flex-1 relative flex items-center">
            <Search className="absolute left-4 w-5 h-5 text-[#6B817C]" />
            <input
              type="text"
              placeholder="What service do you need? (e.g. 'AC not cooling', 'Clean sofa', 'Salon facial')..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="w-full pl-12 pr-4 py-3.5 bg-[#F2FCF7]/70 hover:bg-[#F2FCF7] focus:bg-white border border-[#DDF7EC] rounded-2xl text-xs sm:text-sm text-[#142D2A] focus:outline-none focus:ring-2 focus:ring-[#0B9F6E]/30 focus:border-[#0B9F6E] transition-all"
            />
          </div>

          {/* Submit Search Button */}
          <button
            type="submit"
            className="flex items-center justify-center gap-2 px-7 py-3.5 bg-[#087F5B] hover:bg-[#0B9F6E] text-white font-bold text-sm rounded-2xl transition-all shadow-md shadow-[#087F5B]/20 active:scale-[0.98] shrink-0 cursor-pointer"
          >
            <span>Search</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>

        {/* Popular chips */}
        <div className="mt-4 pt-3 border-t border-[#DDF7EC]/70 flex items-center gap-2 overflow-x-auto text-xs no-scrollbar">
          <span className="text-[#6B817C] text-[11px] font-bold uppercase tracking-wider shrink-0 flex items-center gap-1">
            <Sparkles className="w-3 h-3 text-[#0B9F6E]" />
            <span>Popular:</span>
          </span>
          <div className="flex items-center gap-1.5 flex-nowrap">
            {popularSearches.map((item) => (
              <button
                key={item.label}
                type="button"
                onClick={() => onSearch(item.query)}
                className="whitespace-nowrap px-3 py-1 rounded-xl bg-[#F2FCF7] hover:bg-[#DDF7EC] hover:text-[#087F5B] text-[#142D2A] text-xs font-semibold transition-colors border border-[#DDF7EC] hover:border-[#0B9F6E]/40 cursor-pointer"
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
