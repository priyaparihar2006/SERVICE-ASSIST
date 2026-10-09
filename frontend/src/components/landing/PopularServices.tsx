import React, { useState } from 'react';
import { 
  Star, 
  ShieldCheck, 
  ArrowRight,
  User
} from 'lucide-react';
import { ServiceItem, CartItem, UserProfile } from '../../types/landing';
import { SERVICES } from '../../data/landing/servicesData';

interface PopularServicesProps {
  selectedCategory?: string;
  onCategoryChange?: (catId: string) => void;
  cartItems?: CartItem[];
  onAddToCart?: (service: ServiceItem) => void;
  onInstantBook: (service: ServiceItem) => void;
  searchQuery?: string;
  currentUser?: UserProfile | null;
}

const TABS = [
  { id: 'all', label: 'All Services' },
  { id: 'ac-appliance', label: 'AC & Appliances' },
  { id: 'deep-cleaning', label: 'Cleaning' },
  { id: 'electrician-plumber', label: 'Electrician & Plumber' },
  { id: 'carpentry-handyman', label: 'Carpentry & Handyman' },
  { id: 'pest-control', label: 'Pest Control' },
  { id: 'painting', label: 'Painting' },
  { id: 'water-purifier', label: 'Water Purifier' },
  { id: 'salon-spa', label: 'Salon & Spa' },
];

export const PopularServices: React.FC<PopularServicesProps> = ({
  selectedCategory,
  onCategoryChange,
  onAddToCart,
  onInstantBook,
  currentUser,
}) => {
  const [activeTab, setActiveTab] = useState(selectedCategory || 'all');

  React.useEffect(() => {
    if (selectedCategory) {
      setActiveTab(selectedCategory);
    }
  }, [selectedCategory]);

  const handleTabChange = (tabId: string) => {
    setActiveTab(tabId);
    if (onCategoryChange) {
      onCategoryChange(tabId);
    }
  };

  const filteredServices = SERVICES.filter((item) => {
    if (activeTab === 'all') return true;
    return item.categoryId === activeTab;
  });

  return (
    <section id="services" className="py-12 sm:py-16 bg-white border-b border-slate-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header matching mockup */}
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-6 gap-4">
          <div className="max-w-2xl text-left">
            <div className="inline-flex items-center gap-1.5 text-emerald-600 text-xs font-bold uppercase tracking-wider mb-2">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
              <span>VERIFIED &amp; INSURED</span>
            </div>
            <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-slate-900 tracking-tight leading-tight">
              Most Booked Home Services
            </h2>
            <p className="mt-1.5 text-xs sm:text-sm text-slate-500 font-normal">
              Certified doorstep experts, complete inclusions, genuine tools, and a 30-day rework warranty.
            </p>
          </div>

          <div className="shrink-0 self-start md:self-end">
            <button 
              onClick={() => handleTabChange('all')}
              className="inline-flex items-center gap-1.5 text-xs sm:text-sm font-bold text-emerald-600 hover:text-emerald-700 px-3.5 py-1.5 rounded-full border border-emerald-500/30 hover:border-emerald-500 transition-colors cursor-pointer"
            >
              <span>View all services</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Filter Pills Tabs */}
        <div className="flex items-center gap-2 overflow-x-auto pb-4 mb-8 no-scrollbar scroll-smooth">
          {TABS.map((tab) => {
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => handleTabChange(tab.id)}
                className={`px-4 py-2 rounded-full text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
                  isActive
                    ? 'bg-slate-900 text-white shadow-md'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* 8 Service Cards Grid (2 rows of 4) */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 sm:gap-6">
          {filteredServices.slice(0, 8).map((item) => {
            return (
              <div
                key={item.id}
                className="bg-white rounded-3xl overflow-hidden border border-slate-200/80 shadow-xs card-3d-interactive flex flex-col justify-between text-left group"
              >
                <div>
                  {/* Photo with Overlay Badges */}
                  <div className="relative aspect-[16/10] overflow-hidden bg-slate-100">
                    <img
                      src={item.imageUrl || 'https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=600&q=80'}
                      alt={item.title}
                      className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-108"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent pointer-events-none" />

                    {/* Star Rating Overlay */}
                    <div className="absolute bottom-2.5 left-2.5 inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-white/95 backdrop-blur-md text-[11px] font-bold text-slate-900 shadow-sm">
                      <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
                      <span>{item.rating}</span>
                      <span className="text-slate-400 font-normal">({(item.reviewsCount / 1000).toFixed(1)}k)</span>
                    </div>

                    {/* Warranty Badge Overlay */}
                    <div className="absolute bottom-2.5 right-2.5 inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-emerald-500/90 backdrop-blur-md text-[10px] font-bold text-white shadow-sm">
                      <ShieldCheck className="w-3 h-3" />
                      <span>30-Day Warranty</span>
                    </div>
                  </div>

                  {/* Body Content */}
                  <div className="p-4 sm:p-5 space-y-3">
                    <h3 className="text-base font-extrabold text-slate-900 group-hover:text-emerald-700 transition-colors leading-snug">
                      {item.title}
                    </h3>

                    {/* Features list matching mockup */}
                    <ul className="space-y-1.5 text-xs text-slate-600">
                      {item.features.slice(0, 3).map((feat, idx) => (
                        <li key={idx} className="flex items-center gap-2">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 shrink-0" />
                          <span className="truncate">{feat}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>

                {/* Bottom Service Specs Row */}
                <div className="px-4 sm:px-5 pb-5 pt-3 border-t border-slate-100 flex items-center justify-between mt-auto">
                  <div className="flex items-center gap-1.5 text-xs font-semibold text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200/60">
                    <ShieldCheck className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                    <span>30-Day Warranty</span>
                  </div>

                  <span className="text-xs font-semibold text-slate-500">
                    ~{item.durationMinutes} mins
                  </span>
                </div>

              </div>
            );
          })}
        </div>

      </div>
    </section>
  );
};

