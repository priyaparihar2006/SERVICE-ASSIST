import React from 'react';
import { HowItWorks } from '../components/home/HowItWorks';
import { TrustSection } from '../components/home/TrustSection';
import { FAQSection } from '../components/home/FAQSection';
import { ArrowRight } from 'lucide-react';

export const HowItWorksPage: React.FC<{ onNavigate: (path: string) => void }> = ({ onNavigate }) => {
  return (
    <div className="min-h-screen bg-white">
      {/* Hero */}
      <div className="bg-[var(--color-brand-soft)]/40 py-16 text-center border-b border-brand-light/60">
        <div className="max-w-3xl mx-auto px-4">
          <span className="text-xs font-bold uppercase tracking-wider text-[var(--color-brand)] block mb-2">
            The Standard in Indian Home Services
          </span>
          <h1 className="text-3xl sm:text-5xl font-black text-[var(--color-ink)] tracking-tight mb-4 font-['Outfit']">
            How Service Assist Delivers Excellence
          </h1>
          <p className="text-xs sm:text-base text-gray-600 leading-relaxed max-w-xl mx-auto mb-8">
            From algorithmic dispatch to police-verified background checks and hospital-grade sanitization, here is how we make home care transparent and reliable.
          </p>
          <button
            onClick={() => onNavigate('/services')}
            className="px-6 py-3 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white font-bold text-xs rounded-xl shadow-md shadow-brand/20 transition-all inline-flex items-center gap-2 cursor-pointer"
          >
            <span>Browse Services</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      <HowItWorks />

      <TrustSection />

      <FAQSection />
    </div>
  );
};
