import React, { useState } from 'react';
import { Search, X, Star, Clock, ShieldCheck, ArrowRight } from 'lucide-react';
import { SERVICES } from '../../data/landing/servicesData';
import { ServiceItem } from '../../types/landing';

interface SearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelectService: (service: ServiceItem) => void;
}

export const SearchModal: React.FC<SearchModalProps> = ({
  isOpen,
  onClose,
  onSelectService,
}) => {
  const [query, setQuery] = useState('');

  if (!isOpen) return null;

  const results = SERVICES.filter(
    (s) =>
      s.title.toLowerCase().includes(query.toLowerCase()) ||
      s.shortDesc.toLowerCase().includes(query.toLowerCase()) ||
      s.features.some((f) => f.toLowerCase().includes(query.toLowerCase()))
  );

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center pt-20 px-4 bg-slate-950/70 backdrop-blur-sm animate-in fade-in duration-150">
      <div className="bg-white rounded-3xl max-w-xl w-full p-5 sm:p-6 shadow-2xl relative border border-slate-100 max-h-[80vh] flex flex-col">
        {/* Search Input Bar */}
        <div className="flex items-center gap-3 pb-4 border-b border-slate-100">
          <Search className="w-5 h-5 text-slate-400" />
          <input
            type="text"
            autoFocus
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search AC jet service, pipe leak, deep clean, pest control..."
            className="flex-1 bg-transparent text-sm sm:text-base font-semibold text-slate-900 placeholder-slate-400 focus:outline-hidden"
          />
          <button
            onClick={onClose}
            className="p-1.5 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-700 cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Results List */}
        <div className="mt-4 flex-1 overflow-y-auto space-y-2">
          {results.length === 0 ? (
            <div className="text-center py-12 text-slate-400 text-xs">
              No matching services found for &ldquo;{query}&rdquo;
            </div>
          ) : (
            results.map((service) => (
              <div
                key={service.id}
                onClick={() => {
                  onSelectService(service);
                  onClose();
                }}
                className="p-3.5 rounded-xl hover:bg-slate-50 border border-slate-100 hover:border-emerald-300 transition-all flex items-center justify-between cursor-pointer group"
              >
                <div>
                  <h4 className="text-sm font-bold text-slate-900 group-hover:text-emerald-700">
                    {service.title}
                  </h4>
                  <p className="text-[11px] text-slate-500 line-clamp-1 mt-0.5">
                    {service.shortDesc}
                  </p>
                  <div className="flex items-center gap-2 mt-1.5 text-[11px]">
                    <span className="flex items-center gap-0.5 text-amber-600 font-bold">
                      <Star className="w-3 h-3 fill-amber-400 text-amber-500" />
                      {service.rating}
                    </span>
                    <span className="text-slate-300">·</span>
                    <span className="text-slate-500 flex items-center gap-1">
                      <Clock className="w-3 h-3 text-slate-400" />
                      ~{service.durationMinutes} min
                    </span>
                  </div>
                </div>

                <div className="text-right pl-4">
                  <span className="text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full inline-flex items-center gap-0.5 group-hover:bg-emerald-100 transition-colors">
                    Explore <ArrowRight className="w-3 h-3" />
                  </span>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

