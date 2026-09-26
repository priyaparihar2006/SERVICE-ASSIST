import React, { useState, useMemo, useEffect } from 'react';
import { Service, Category } from '../types';
import { Star, Clock, CheckCircle2, Search, SlidersHorizontal, Heart, Sparkles } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useLocation } from '../context/LocationContext';

import { CategoryCard } from '../components/home/CategoryCard';
import { ImageWithFallback } from '../components/common/ImageWithFallback';

interface ServicesPageProps {
  services: Service[];
  categories: Category[];
  initialCategory?: string;
  initialSearch?: string;
  onSelectService: (slug: string) => void;
  onNavigate: (path: string) => void;
}

export const ServicesPage: React.FC<ServicesPageProps> = ({
  services,
  categories,
  initialCategory,
  initialSearch = '',
  onSelectService,
  onNavigate,
}) => {
  const { isFavorite, toggleFavorite } = useAuth();
  const { addItem } = useCart();
  const { selectedCity } = useLocation();

  const [selectedCategory, setSelectedCategory] = useState<string>(initialCategory || 'all');
  const [searchQuery, setSearchQuery] = useState<string>(initialSearch);
  const [sortBy, setSortBy] = useState<'popular' | 'rating' | 'price-asc' | 'price-desc'>('popular');
  const [selectedSubcategory, setSelectedSubcategory] = useState('all');
  const [availableOnly, setAvailableOnly] = useState(true);
  const [showCategoryGrid, setShowCategoryGrid] = useState<boolean>(!initialCategory || initialCategory === 'all');

  useEffect(() => {
    if (initialCategory) {
      setSelectedCategory(initialCategory);
      setSelectedSubcategory('all');
      setShowCategoryGrid(false);
    }
  }, [initialCategory]);

  useEffect(() => {
    if (initialSearch !== undefined) {
      setSearchQuery(initialSearch);
    }
  }, [initialSearch]);

  const subcategories = useMemo(() => {
    return Array.from(
      new Set(
        services
          .filter((s) => {
            if (selectedCategory === 'all') return true;
            const normCat = selectedCategory === 'cat-home-cleaning' ? 'cat-cleaning' : selectedCategory;
            const matchedCat = categories.find((c) => c.id === normCat || c.slug === normCat);
            return s.categoryId === normCat || s.categoryId === matchedCat?.id;
          })
          .map((s) => s.subcategory)
          .filter((s): s is string => Boolean(s && s !== 'General'))
      )
    ).sort();
  }, [services, selectedCategory, categories]);

  const filteredServices = useMemo(() => {
    return services
      .filter((s) => {
        let matchesCategory = selectedCategory === 'all';
        if (!matchesCategory) {
          const normCat = selectedCategory === 'cat-home-cleaning' ? 'cat-cleaning' : selectedCategory;
          const matchedCat = categories.find((c) => c.id === normCat || c.slug === normCat);
          matchesCategory = s.categoryId === normCat || s.categoryId === matchedCat?.id || (matchedCat?.name && s.categoryName === matchedCat.name);
        }

        const matchesSearch =
          !searchQuery.trim() ||
          s.name.toLowerCase().includes(searchQuery.trim().toLowerCase()) ||
          s.shortDesc.toLowerCase().includes(searchQuery.trim().toLowerCase()) ||
          s.categoryName.toLowerCase().includes(searchQuery.trim().toLowerCase()) ||
          (s.subcategory || '').toLowerCase().includes(searchQuery.trim().toLowerCase());

        return (
          matchesCategory &&
          matchesSearch &&
          (selectedSubcategory === 'all' || s.subcategory === selectedSubcategory) &&
          (!availableOnly || s.locations.includes(selectedCity.name))
        );
      })
      .sort((a, b) => {
        if (sortBy === 'rating') return (b.reviewsCount ? b.rating : 0) - (a.reviewsCount ? a.rating : 0);
        if (sortBy === 'price-asc') return a.startingPrice - b.startingPrice;
        if (sortBy === 'price-desc') return b.startingPrice - a.startingPrice;
        return (b.reviewsCount || 0) - (a.reviewsCount || 0);
      });
  }, [services, categories, selectedCategory, selectedSubcategory, availableOnly, searchQuery, sortBy, selectedCity.name]);

  const handleCategoryCardClick = (catId: string) => {
    setSelectedCategory(catId);
    setSelectedSubcategory('all');
    // Smooth scroll down to service list
    const el = document.getElementById('service-catalog-results');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <div className="min-h-screen bg-[var(--color-brand-soft)]/30 py-8 sm:py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Page Header */}
        <div className="mb-8 flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div>
            <div className="inline-flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-[var(--color-brand)] mb-1">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Service Assist Marketplace</span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-black text-[var(--color-ink)] tracking-tight font-['Outfit']">
              All Doorstep Services
            </h1>
            <p className="text-xs sm:text-sm text-gray-500 mt-1 max-w-2xl">
              Browse services with clear visit pricing. Inspection services receive a quote before additional work.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowCategoryGrid((prev) => !prev)}
              className="px-3.5 py-2 bg-white rounded-xl border border-brand-light text-xs font-bold text-[var(--color-brand-hover)] hover:border-[var(--color-brand)] transition-colors cursor-pointer shadow-xs"
            >
              {showCategoryGrid ? 'Hide Category Cards' : 'Browse Category Cards'}
            </button>
          </div>
        </div>

        {/* Section: Category Showcase Image Cards (Section 13) */}
        {showCategoryGrid && (
          <div className="mb-12 bg-white rounded-3xl p-5 sm:p-7 border border-brand-light/80 shadow-xs">
            <div className="flex items-center justify-between mb-5">
              <div>
                <h2 className="text-lg sm:text-xl font-black text-[var(--color-ink)] font-['Outfit']">
                  Browse by Category
                </h2>
                <p className="text-xs text-gray-500">
                  Select any category to view instant packages & rates
                </p>
              </div>
              {selectedCategory !== 'all' && (
                <button
                  onClick={() => setSelectedCategory('all')}
                  className="text-xs font-bold text-[var(--color-brand)] hover:underline cursor-pointer"
                >
                  Show all categories
                </button>
              )}
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3.5 sm:gap-4">
              {categories.map(c => ({ ...c, subtitle: c.description, alt: c.name })).map((cat) => (
                <CategoryCard
                  key={cat.id}
                  category={cat}
                  onClick={() => handleCategoryCardClick(cat.id)}
                />
              ))}
            </div>
          </div>
        )}

        {/* Filter & Controls Bar */}
        <div id="service-catalog-results" className="bg-white rounded-2xl p-4 shadow-xs border border-brand-light/70 mb-8 space-y-4">
          <div className="flex flex-col md:flex-row gap-3 items-stretch md:items-center justify-between">
            {/* Search Input */}
            <div className="relative flex-1">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="Search services (e.g. AC, Deep cleaning, Salon, Tap repair, Laptop)..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)]"
              />
            </div>

            {/* Sort Dropdown */}
            <div className="flex items-center gap-2">
              <SlidersHorizontal className="w-4 h-4 text-gray-400 shrink-0" />
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as any)}
                className="px-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-semibold text-gray-700 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 cursor-pointer"
              >
                <option value="popular">Most Booked</option>
                <option value="rating">Top Rated</option>
                <option value="price-asc">Price: Low to High</option>
                <option value="price-desc">Price: High to Low</option>
              </select>
            </div>
          </div>

          {/* Category Chips Bar */}
          <div className="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar text-xs">
            <button
              onClick={() => { setSelectedCategory('all'); setSelectedSubcategory('all'); }}
              className={`px-3.5 py-1.5 rounded-xl font-bold whitespace-nowrap transition-all cursor-pointer ${
                selectedCategory === 'all'
                  ? 'bg-[var(--color-brand)] text-white shadow-xs'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              All Categories ({services.length})
            </button>
            {categories.map((cat) => {
              const isSelected = selectedCategory === cat.id || selectedCategory === cat.slug;
              const catCount = services.filter((s) => s.categoryId === cat.id).length;
              return (
                <button
                  key={cat.id}
                  onClick={() => { setSelectedCategory(cat.id); setSelectedSubcategory('all'); }}
                  className={`px-3.5 py-1.5 rounded-xl font-semibold whitespace-nowrap transition-all border cursor-pointer ${
                    isSelected
                      ? 'bg-[var(--color-brand-light)] border-[var(--color-brand)] text-[var(--color-brand-hover)] font-bold shadow-xs'
                      : 'border-transparent bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                >
                  {cat.name} ({catCount})
                </button>
              );
            })}
          </div>
          {subcategories.length > 0 && (
            <div className="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar text-xs" aria-label="Subcategory filters">
              <button
                onClick={() => setSelectedSubcategory('all')}
                className={`px-3 py-1.5 rounded-xl whitespace-nowrap ${selectedSubcategory === 'all' ? 'bg-brand text-white' : 'bg-gray-100 text-gray-600'}`}
              >
                All subcategories
              </button>
              {subcategories.map(subcategory => (
                <button
                  key={subcategory}
                  onClick={() => setSelectedSubcategory(subcategory)}
                  className={`px-3 py-1.5 rounded-xl whitespace-nowrap ${selectedSubcategory === subcategory ? 'bg-brand text-white' : 'bg-gray-100 text-gray-600'}`}
                >
                  {subcategory}
                </button>
              ))}
            </div>
          )}
          <label className="flex items-center gap-2 text-xs font-semibold text-gray-600">
            <input
              type="checkbox"
              checked={availableOnly}
              onChange={(event) => setAvailableOnly(event.target.checked)}
              className="accent-[#009051]"
            />
            Available in {selectedCity.name}
          </label>
        </div>

        {/* Results Counter */}
        <div className="flex items-center justify-between mb-6">
          <p className="text-xs font-semibold text-gray-500">
            Showing <span className="font-bold text-gray-900">{filteredServices.length}</span> services
            {selectedCategory !== 'all' && (
              <span className="ml-1 text-[var(--color-brand-hover)]">
                in {categories.find((c) => c.id === selectedCategory || c.slug === selectedCategory)?.name || 'selected category'}
              </span>
            )}
          </p>
          {(selectedCategory !== 'all' || searchQuery) && (
            <button
              onClick={() => {
                setSelectedCategory('all');
                setSelectedSubcategory('all');
                setSearchQuery('');
              }}
              className="text-xs font-bold text-[var(--color-brand)] hover:underline cursor-pointer"
            >
              Clear filters
            </button>
          )}
        </div>

        {/* Services Grid */}
        {filteredServices.length === 0 ? (
          <div className="bg-white rounded-3xl p-12 text-center border border-gray-100 max-w-md mx-auto my-8">
            <Search className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <h3 className="font-bold text-gray-900 text-lg mb-1 font-['Outfit']">No services found</h3>
            <p className="text-xs text-gray-500 mb-6">
              Try searching with different keywords like 'ac', 'cleaning', 'plumbing', or reset your filters.
            </p>
            <button
              onClick={() => {
                setSelectedCategory('all');
                setSelectedSubcategory('all');
                setSearchQuery('');
              }}
              className="px-5 py-2.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
            >
              Reset Search & Filters
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredServices.map((service) => {
              const fav = isFavorite(service.id);
              return (
                <div
                  key={service.id}
                  className="group bg-white rounded-3xl border border-gray-100 hover:border-[var(--color-brand-bright)] shadow-xs hover:shadow-xl transition-all duration-300 overflow-hidden flex flex-col justify-between"
                >
                  <div>
                    {/* Service Image & Badge */}
                    <div className="relative aspect-16/10 overflow-hidden bg-gray-100">
                      <ImageWithFallback
                        src={service.image}
                        alt={service.name}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                      />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent opacity-60" />

                      {/* Favorite Button */}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          toggleFavorite(service.id);
                        }}
                        aria-label={fav ? "Remove from favorites" : "Add to favorites"}
                        className={`absolute top-3 right-3 p-2 rounded-full backdrop-blur-md transition-colors cursor-pointer ${
                          fav ? 'bg-red-50 text-red-500' : 'bg-white/80 text-gray-600 hover:text-red-500'
                        }`}
                      >
                        <Heart className="w-4 h-4" fill={fav ? 'currentColor' : 'none'} />
                      </button>

                      {/* Category Pill */}
                      <div className="absolute bottom-3 left-3 bg-white/90 backdrop-blur-md px-2.5 py-1 rounded-lg text-[10px] font-bold text-[var(--color-brand-hover)] border border-[var(--color-brand-light)]">
                        {service.categoryName}
                      </div>
                    </div>

                    {/* Content */}
                    <div className="p-5">
                      <div className="flex items-center gap-1 text-xs text-amber-500 font-bold mb-1.5">
                        <Star className="w-3.5 h-3.5 fill-amber-400" />
                        <span>{service.rating}</span>
                        <span className="text-gray-400 font-normal">({service.reviewsCount || 0})</span>
                      </div>

                      <h3
                        onClick={() => onSelectService(service.slug)}
                        className="font-bold text-gray-900 text-base leading-snug group-hover:text-[var(--color-brand)] transition-colors cursor-pointer font-['Outfit']"
                      >
                        {service.name}
                      </h3>

                      <p className="text-xs text-gray-500 mt-1 line-clamp-2 leading-relaxed">
                        {service.shortDesc}
                      </p>

                      {/* Key Features/Highlights */}
                      {service.highlights && service.highlights.length > 0 && (
                        <div className="mt-3 space-y-1">
                          {service.highlights.slice(0, 2).map((h, i) => (
                            <div key={i} className="flex items-center gap-1.5 text-[11px] text-gray-600">
                              <CheckCircle2 className="w-3 h-3 text-[var(--color-brand)] shrink-0" />
                              <span className="truncate">{h}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Pricing and Actions Footer */}
                  <div className="p-5 pt-0 mt-auto border-t border-gray-50 flex items-center justify-between gap-3">
                    <div>
                      <span className="text-[10px] text-gray-400 block uppercase font-medium">Starting from</span>
                      <div className="flex items-baseline gap-1.5">
                        <span className="text-lg font-black text-gray-900 font-['Outfit']">
                          ₹{service.startingPrice}
                        </span>
                        {service.priceType && (
                          <span className="text-[10px] text-gray-500">/{service.priceType}</span>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => onSelectService(service.slug)}
                        className="px-3 py-2 text-xs font-bold text-gray-600 hover:text-gray-900 bg-gray-100 hover:bg-gray-200 rounded-xl transition-colors cursor-pointer"
                      >
                        Details
                      </button>

                      <button
                        onClick={() => addItem(service)}
                        className="px-4 py-2 text-xs font-bold text-white bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] rounded-xl shadow-xs transition-all cursor-pointer"
                      >
                        Book Now
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
