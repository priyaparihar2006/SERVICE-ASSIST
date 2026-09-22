import React, { useState } from 'react';
import { Service, ServiceVariant } from '../types';
import {
  Star,
  Clock,
  ShieldCheck,
  CheckCircle2,
  XCircle,
  Award,
  Calendar,
  Sparkles,
  ShoppingBag,
  Heart,
  ChevronRight,
  Share2,
} from 'lucide-react';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { ImageWithFallback } from '../components/common/ImageWithFallback';

interface ServiceDetailPageProps {
  service: Service;
  onBack: () => void;
  onNavigate: (path: string) => void;
}

export const ServiceDetailPage: React.FC<ServiceDetailPageProps> = ({
  service,
  onBack,
  onNavigate,
}) => {
  const { addItem, openCartDrawer } = useCart();
  const { isFavorite, toggleFavorite } = useAuth();

  const [selectedVariant, setSelectedVariant] = useState<ServiceVariant>(
    service.variants[0] || {
      id: 'var-default',
      name: 'Standard Package',
      price: service.startingPrice,
      originalPrice: service.originalPrice,
      durationMin: service.durationMin,
      description: service.shortDesc,
      included: service.whatIncluded,
    }
  );

  const [copied, setCopied] = useState(false);
  const fav = isFavorite(service.id);

  const handleAddToCart = () => {
    addItem(service, selectedVariant);
  };

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="min-h-screen bg-[var(--color-brand-soft)]/30 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Breadcrumb & Navigation */}
        <div className="flex items-center justify-between gap-4 mb-6">
          <div className="flex items-center gap-2 text-xs font-semibold text-gray-500">
            <button onClick={onBack} className="hover:text-gray-900 transition-colors cursor-pointer">
              Home
            </button>
            <ChevronRight className="w-3.5 h-3.5 text-gray-400" />
            <button onClick={onBack} className="hover:text-gray-900 transition-colors cursor-pointer">
              {service.categoryName}
            </button>
            <ChevronRight className="w-3.5 h-3.5 text-gray-400" />
            <span className="text-gray-900 font-bold truncate max-w-xs">{service.name}</span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleShare}
              className="p-2 rounded-xl border border-gray-200 bg-white hover:bg-gray-50 text-gray-600 transition-all text-xs font-semibold flex items-center gap-1.5 cursor-pointer"
            >
              <Share2 className="w-3.5 h-3.5" />
              <span>{copied ? 'Copied Link!' : 'Share'}</span>
            </button>
            <button
              onClick={() => toggleFavorite(service.id)}
              className={`p-2 rounded-xl border transition-all cursor-pointer ${
                fav
                  ? 'border-red-200 bg-red-50 text-red-500'
                  : 'border-gray-200 bg-white hover:bg-gray-50 text-gray-600'
              }`}
            >
              <Heart className={`w-4 h-4 ${fav ? 'fill-red-500' : ''}`} />
            </button>
          </div>
        </div>

        {/* Main Grid: Left Details & Right Sticky Booking Card */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Left Column (8 cols) */}
          <div className="lg:col-span-8 space-y-8">
            {/* Header & Hero Image */}
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-100 shadow-xs space-y-6">
              <div className="relative rounded-2xl overflow-hidden aspect-16/9 bg-gray-100 border border-gray-100">
                <ImageWithFallback
                  src={service.image}
                  alt={service.name}
                  fallbackTitle={service.name}
                  className="w-full h-full object-cover"
                />
                <span className="absolute bottom-4 left-4 px-3 py-1 rounded-full bg-black/60 backdrop-blur-md text-white text-xs font-bold">
                  {service.categoryName}
                </span>
              </div>

              <div>
                <div className="flex flex-wrap items-center gap-3 mb-2">
                  {service.reviewsCount > 0 && <div className="flex items-center gap-1 bg-amber-50 px-2.5 py-1 rounded-lg text-amber-800 text-xs font-extrabold">
                    <Star className="w-4 h-4 fill-[var(--color-brand-bright)] text-[var(--color-brand-bright)]" />
                    <span>{service.rating}</span>
                  </div>}
                  {service.reviewsCount > 0 && <span className="text-xs text-gray-400 font-medium">
                    ({service.reviewsCount} customer ratings)
                  </span>}
                  <span className="text-gray-300">•</span>
                  <div className="flex items-center gap-1 text-xs text-gray-600 font-semibold">
                    <Clock className="w-3.5 h-3.5 text-gray-400" />
                    <span>{selectedVariant.durationMin} mins</span>
                  </div>
                  <span className="text-gray-300">•</span>
                  {service.warrantyPolicy && <span className="text-xs font-bold text-brand bg-brand-soft px-2 py-0.5 rounded">{service.warrantyPolicy}</span>}
                  {service.isDemo && <span className="text-xs font-semibold text-gray-500">Demo catalog</span>}
                </div>

                <h1 className="text-2xl sm:text-3xl font-black text-[var(--color-ink)] tracking-tight mb-3 font-['Outfit']">
                  {service.name}
                </h1>
                <p className="text-xs sm:text-sm text-gray-600 leading-relaxed">
                  {service.description}
                </p>
                {service.priceType === 'INSPECTION' && <p className="mt-2 text-xs font-semibold text-amber-800">The listed amount is an inspection fee. Parts and further work require a separate quote and your approval.</p>}
              </div>
            </div>

            {/* Package Variants Section */}
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-100 shadow-xs space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-extrabold text-base text-gray-900 font-['Outfit']">Select Service Variant</h3>
                  <p className="text-xs text-gray-500">Choose the package that fits your home requirement</p>
                </div>
                <span className="text-xs font-bold text-[var(--color-brand-hover)] bg-[var(--color-brand-light)] px-2.5 py-1 rounded-lg">
                  {service.variants.length} options available
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {service.variants.map((v) => {
                  const isSelected = selectedVariant.id === v.id;
                  return (
                    <div
                      key={v.id}
                      onClick={() => setSelectedVariant(v)}
                      className={`p-4 rounded-2xl border cursor-pointer transition-all flex flex-col justify-between ${
                        isSelected
                          ? 'border-[var(--color-brand)] bg-[var(--color-brand-light)]/50 shadow-xs ring-2 ring-[var(--color-brand)]/20'
                          : 'border-gray-200 hover:border-gray-300 bg-white'
                      }`}
                    >
                      <div>
                        <div className="flex items-start justify-between gap-2 mb-1">
                          <h4 className="font-bold text-xs text-gray-900 font-['Outfit']">{v.name}</h4>
                          <span className="text-xs font-black text-gray-900">₹{v.price}</span>
                        </div>
                        <p className="text-[11px] text-gray-500 line-clamp-2 leading-relaxed mb-3">
                          {v.description}
                        </p>
                      </div>

                      <div className="flex items-center justify-between pt-2 border-t border-gray-100 text-[11px]">
                        <span className="text-gray-400 font-medium">⏱ ~{v.durationMin} mins</span>
                        <span
                          className={`font-bold ${
                            isSelected ? 'text-[var(--color-brand-hover)]' : 'text-gray-400'
                          }`}
                        >
                          {isSelected ? 'Selected ✓' : 'Select'}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* What is Included & What is NOT included */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Included */}
              <div className="bg-white rounded-3xl p-6 border border-gray-100 shadow-xs">
                <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-brand-hover mb-4">
                  <CheckCircle2 className="w-4 h-4 text-brand" />
                  <span>What is Included</span>
                </div>
                <ul className="space-y-3">
                  {service.whatIncluded.map((inc, i) => (
                    <li key={i} className="flex items-start gap-2.5 text-xs text-gray-700 leading-relaxed">
                      <CheckCircle2 className="w-4 h-4 text-brand shrink-0 mt-0.5" />
                      <span>{inc}</span>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Not Included */}
              <div className="bg-white rounded-3xl p-6 border border-gray-100 shadow-xs">
                <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-red-600 mb-4">
                  <XCircle className="w-4 h-4 text-red-500" />
                  <span>What is Excluded</span>
                </div>
                <ul className="space-y-3">
                  {service.whatExcluded.map((exc, i) => (
                    <li key={i} className="flex items-start gap-2.5 text-xs text-gray-700 leading-relaxed">
                      <XCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
                      <span>{exc}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            {/* Step-by-Step Procedure */}
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-100 shadow-xs space-y-4">
              <h3 className="font-extrabold text-base text-gray-900 font-['Outfit']">Standard Service Procedure</h3>
              <p className="text-xs text-gray-500">
                Our technicians adhere to a strict multi-point standard operating protocol:
              </p>

              <div className="space-y-4 pt-2">
                {!service.steps?.length && <p className="text-xs text-gray-500">Your professional will explain the procedure before work begins.</p>}
                {(service.steps || []).map((st, i) => (
                  <div key={i} className="flex items-start gap-4">
                    <div className="w-8 h-8 rounded-xl bg-[var(--color-brand-light)] text-[var(--color-brand)] font-black text-xs flex items-center justify-center shrink-0 border border-brand-light font-['Outfit']">
                      0{i + 1}
                    </div>
                    <div className="pt-1">
                      <p className="text-xs text-gray-800 leading-relaxed font-semibold">{st}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Right Column: Sticky Booking & Price Card (4 cols) */}
          <div className="lg:col-span-4 lg:sticky lg:top-24 space-y-4">
            <div className="bg-white rounded-3xl p-6 border border-brand-light/70 shadow-lg shadow-brand-dark/5 space-y-5">
              <div>
                <span className="text-[10px] uppercase font-bold text-gray-400 block tracking-wider mb-1">
                  Selected Package
                </span>
                <h3 className="font-bold text-base text-gray-900 leading-snug font-['Outfit']">
                  {selectedVariant.name}
                </h3>
              </div>

              {/* Price summary */}
              <div className="p-4 bg-gray-50 rounded-2xl border border-gray-100 space-y-2">
                <div className="flex items-baseline justify-between">
                  <span className="text-xs text-gray-600 font-medium">Package Rate</span>
                  <div className="flex items-baseline gap-1.5">
                    <span className="text-2xl font-black text-gray-900">₹{selectedVariant.price}</span>
                    {selectedVariant.originalPrice && (
                      <span className="text-xs text-gray-400 line-through">
                        ₹{selectedVariant.originalPrice}
                      </span>
                    )}
                  </div>
                </div>

                <div className="flex items-center justify-between text-[11px] text-brand font-semibold pt-1 border-t border-gray-200/60">
                  <span>Final price confirmed at checkout</span>
                  <span>{service.priceType === 'INSPECTION' ? 'Inspection visit fee applies' : 'Service visit included'}</span>
                </div>
              </div>

              {/* Trust highlights */}
              <div className="space-y-2.5 text-xs text-gray-600">
                {service.warrantyPolicy && <div className="flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-[var(--color-brand)]" />
                  <span>{service.warrantyPolicy}</span>
                </div>}
                <div className="flex items-center gap-2">
                  <Award className="w-4 h-4 text-amber-500" />
                  <span>Qualified professional assigned for this service</span>
                </div>
                <div className="flex items-center gap-2">
                  <Calendar className="w-4 h-4 text-brand-soft0" />
                  <span>Choose an available slot at checkout</span>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="space-y-2 pt-2">
                <button
                  onClick={handleAddToCart}
                  className="w-full flex items-center justify-center gap-2 py-3.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-sm rounded-xl transition-all shadow-md shadow-brand/20 active:scale-[0.98] cursor-pointer"
                >
                  <ShoppingBag className="w-4 h-4" />
                  <span>Add to Cart & Select Slot</span>
                </button>

                <button
                  onClick={() => {
                    addItem(service, selectedVariant);
                    openCartDrawer();
                  }}
                  className="w-full py-2.5 bg-gray-50 hover:bg-[var(--color-brand-soft)] text-gray-700 hover:text-[var(--color-brand-hover)] font-bold text-xs rounded-xl transition-all border border-gray-200 cursor-pointer"
                >
                  Book Instant Now
                </button>
              </div>
            </div>

            {/* Quick Promo Banner */}
            <div className="p-4 bg-[var(--color-brand-light)] border border-brand-light rounded-2xl text-xs text-brand-dark flex items-center gap-3">
              <Sparkles className="w-5 h-5 text-[var(--color-brand)] shrink-0" />
              <div>
                <span className="font-bold block">First time on Service Assist?</span>
                <p className="text-[11px] text-[var(--color-brand-hover)]">Use coupon WELCOME150 for flat ₹150 OFF at checkout.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
