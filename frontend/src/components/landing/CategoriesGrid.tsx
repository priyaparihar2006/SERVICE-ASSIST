import React from 'react';
import { 
  ArrowRight, 
  ChevronLeft, 
  ChevronRight,
  Wind,
  Sparkles,
  Zap,
  Hammer,
  ShieldAlert,
  Paintbrush,
  Droplets,
  Scissors
} from 'lucide-react';

interface CategoryCardItem {
  id: string;
  name: string;
  startingPrice: string;
  bgColor: string;
  borderColor: string;
  iconBg: string;
  iconColor: string;
  icon: React.ElementType;
}

const CATEGORIES_DATA: CategoryCardItem[] = [
  {
    id: 'ac-appliance',
    name: 'AC & Appliance Repair',
    startingPrice: '299',
    bgColor: 'bg-[#f0f4f8]',
    borderColor: 'border-slate-200/80',
    iconBg: 'bg-sky-100 text-sky-700',
    iconColor: 'text-sky-600',
    icon: Wind,
  },
  {
    id: 'deep-cleaning',
    name: 'Home Deep Cleaning',
    startingPrice: '399',
    bgColor: 'bg-[#fdf8ee]',
    borderColor: 'border-amber-200/60',
    iconBg: 'bg-amber-100 text-amber-700',
    iconColor: 'text-amber-600',
    icon: Sparkles,
  },
  {
    id: 'electrician-plumber',
    name: 'Electrician & Plumber',
    startingPrice: '199',
    bgColor: 'bg-[#edf8f3]',
    borderColor: 'border-emerald-200/60',
    iconBg: 'bg-emerald-100 text-emerald-700',
    iconColor: 'text-emerald-600',
    icon: Zap,
  },
  {
    id: 'carpentry-handyman',
    name: 'Carpentry & Handyman',
    startingPrice: '249',
    bgColor: 'bg-[#fbf4ee]',
    borderColor: 'border-orange-200/60',
    iconBg: 'bg-orange-100 text-orange-700',
    iconColor: 'text-orange-600',
    icon: Hammer,
  },
  {
    id: 'pest-control',
    name: 'Pest Control',
    startingPrice: '399',
    bgColor: 'bg-[#fefbee]',
    borderColor: 'border-yellow-200/60',
    iconBg: 'bg-yellow-100 text-yellow-700',
    iconColor: 'text-yellow-600',
    icon: ShieldAlert,
  },
  {
    id: 'painting',
    name: 'Painting & Waterproofing',
    startingPrice: '1,499',
    bgColor: 'bg-[#f0f7fb]',
    borderColor: 'border-cyan-200/60',
    iconBg: 'bg-cyan-100 text-cyan-700',
    iconColor: 'text-cyan-600',
    icon: Paintbrush,
  },
  {
    id: 'water-purifier',
    name: 'Water Purifier (RO)',
    startingPrice: '249',
    bgColor: 'bg-[#f2f5fb]',
    borderColor: 'border-indigo-200/60',
    iconBg: 'bg-indigo-100 text-indigo-700',
    iconColor: 'text-indigo-600',
    icon: Droplets,
  },
  {
    id: 'salon-spa',
    name: 'Salon & Spa at Home',
    startingPrice: '699',
    bgColor: 'bg-[#fdf2f7]',
    borderColor: 'border-pink-200/60',
    iconBg: 'bg-pink-100 text-pink-700',
    iconColor: 'text-pink-600',
    icon: Scissors,
  },
];

interface CategoriesGridProps {
  selectedCategory?: string;
  onSelectCategory: (catId: string) => void;
}

export const CategoriesGrid: React.FC<CategoriesGridProps> = ({
  selectedCategory,
  onSelectCategory,
}) => {
  const handleCardClick = (catId: string) => {
    onSelectCategory(catId);
    const el = document.getElementById('services');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <section id="categories" className="py-12 sm:py-16 bg-white border-b border-slate-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header matching mockup */}
        <div className="flex flex-col md:flex-row md:items-end justify-between mb-8 gap-4">
          <div className="max-w-2xl text-left">
            <div className="inline-flex items-center gap-1.5 text-emerald-600 text-xs font-bold uppercase tracking-wider mb-2">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
              <span>POPULAR SERVICES</span>
            </div>
            <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-slate-900 tracking-tight leading-tight">
              Trusted experts for <br className="hidden sm:inline" />
              <span className="text-slate-900">every job at home</span>
            </h2>
            <p className="mt-1.5 text-xs sm:text-sm text-slate-500 font-normal">
              Explore certified home services, clear pricing inclusions, and verified technician coverage.
            </p>
          </div>

          {/* Right Navigation & View All */}
          <div className="flex items-center gap-3 shrink-0 self-start md:self-end">
            <a
              href="#services"
              className="inline-flex items-center gap-1 text-xs sm:text-sm font-bold text-emerald-600 hover:text-emerald-700 px-3 py-1.5 rounded-full border border-emerald-500/30 hover:border-emerald-500 transition-colors"
            >
              <span>View all services</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </a>

            <div className="hidden sm:flex items-center gap-1.5">
              <button 
                aria-label="Previous" 
                className="w-8 h-8 rounded-full border border-slate-200 flex items-center justify-center text-slate-400 hover:text-slate-700 hover:border-slate-400 transition-colors cursor-pointer"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button 
                aria-label="Next" 
                className="w-8 h-8 rounded-full border border-slate-200 flex items-center justify-center text-slate-400 hover:text-slate-700 hover:border-slate-400 transition-colors cursor-pointer"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        {/* 8 Cards Grid in 2 Columns on Mobile, 4 Columns on Desktop */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-5">
          {CATEGORIES_DATA.map((cat) => {
            const Icon = cat.icon;
            const isSelected = selectedCategory === cat.id;

            return (
              <div
                key={cat.id}
                onClick={() => handleCardClick(cat.id)}
                className={`group rounded-2xl sm:rounded-3xl p-3.5 sm:p-5 border ${cat.borderColor} ${cat.bgColor} card-3d-interactive cursor-pointer flex flex-col justify-between text-left relative overflow-hidden transition-all duration-300 ${
                  isSelected ? 'ring-2 ring-emerald-500 shadow-lg' : ''
                }`}
              >
                {/* 3D Specular Light Sheen Overlay */}
                <div className="absolute inset-0 bg-gradient-to-tr from-white/0 via-white/40 to-white/0 opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none" />

                {/* Top Section: Icon & Name */}
                <div className="space-y-2 sm:space-y-3 relative z-10">
                  <div 
                    style={{ transform: 'translateZ(20px)' }}
                    className="w-9 h-9 sm:w-12 sm:h-12 rounded-xl sm:rounded-2xl bg-white shadow-xs flex items-center justify-center transition-all duration-300 group-hover:scale-110 group-hover:shadow-md"
                  >
                    <Icon className={`w-4.5 h-4.5 sm:w-6 sm:h-6 ${cat.iconColor}`} />
                  </div>
                  <h3 className="text-xs xs:text-sm sm:text-base font-extrabold text-slate-900 group-hover:text-emerald-700 transition-colors leading-snug">
                    {cat.name}
                  </h3>
                </div>

                {/* Bottom Row: Status / Explore */}
                <div className="pt-2.5 sm:pt-4 flex items-center justify-between border-t border-black/5 mt-3 sm:mt-4 relative z-10">
                  <span className="text-[10px] sm:text-xs font-semibold text-emerald-700 flex items-center gap-1.5">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 shrink-0" />
                    <span>Verified Pros</span>
                  </span>
                  <span className="text-[11px] font-bold text-slate-400 group-hover:text-emerald-600 transition-colors flex items-center gap-0.5">
                    View <ArrowRight className="w-3 h-3 group-hover:translate-x-0.5 transition-transform" />
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

