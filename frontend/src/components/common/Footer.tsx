import React from 'react';
import {
  ShieldCheck,
  Award,
  HeartHandshake,
  PhoneCall,
  Mail,
  MapPin,
  CheckCircle2,
  ExternalLink,
} from 'lucide-react';
import { CITIES_LIST } from '../../context/LocationContext';
import { BrandLogo } from './BrandLogo';

interface FooterProps {
  onNavigate: (path: string) => void;
}

export const Footer: React.FC<FooterProps> = ({ onNavigate }) => {
  return (
    <footer className="bg-[var(--color-brand-dark)] text-white pt-10 pb-8 border-t border-[var(--color-brand-bright)]/20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Compact Trust Bar */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pb-8 border-b border-white/10 mb-8">
          <div className="flex items-center gap-3 p-3 rounded-xl bg-white/5 border border-white/10">
            <div className="w-9 h-9 rounded-lg bg-[var(--color-brand)]/20 flex items-center justify-center text-[var(--color-brand-bright)] shrink-0 border border-[var(--color-brand-bright)]/30">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-xs text-white font-['Outfit']">100% Verified Partners</h4>
              <p className="text-[11px] text-[var(--color-brand-light)]/70">Background-checked & certified</p>
            </div>
          </div>

          <div className="flex items-center gap-3 p-3 rounded-xl bg-white/5 border border-white/10">
            <div className="w-9 h-9 rounded-lg bg-[var(--color-brand)]/20 flex items-center justify-center text-[var(--color-brand-bright)] shrink-0 border border-[var(--color-brand-bright)]/30">
              <Award className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-xs text-white font-['Outfit']">Clear Service Scope</h4>
              <p className="text-[11px] text-[var(--color-brand-light)]/70">Standardized upfront rates</p>
            </div>
          </div>

          <div className="flex items-center gap-3 p-3 rounded-xl bg-white/5 border border-white/10">
            <div className="w-9 h-9 rounded-lg bg-[var(--color-brand)]/20 flex items-center justify-center text-[var(--color-brand-bright)] shrink-0 border border-[var(--color-brand-bright)]/30">
              <HeartHandshake className="w-5 h-5" />
            </div>
            <div>
              <h4 className="font-bold text-xs text-white font-['Outfit']">Standard Transparent Pricing</h4>
              <p className="text-[11px] text-[var(--color-brand-light)]/70">Inspection & quote before work</p>
            </div>
          </div>
        </div>

        {/* Main Footer Links - Compact Grid */}
        <div className="grid grid-cols-2 md:grid-cols-5 gap-6 sm:gap-8 pb-8 border-b border-white/10">
          {/* Brand Column */}
          <div className="col-span-2 space-y-3">
            <BrandLogo size="md" variant="light" showTagline={true} />
            <p className="text-xs text-[var(--color-brand-light)]/80 max-w-sm leading-relaxed">
              Premium On-Demand Home Services delivered right to your doorstep across India. AC repair, deep sanitation, beauty & master electricians.
            </p>

            <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-[var(--color-brand-light)]/80 pt-1">
              <a href="tel:18004202774" className="flex items-center gap-1.5 hover:text-white transition-colors">
                <PhoneCall className="w-3.5 h-3.5 text-[var(--color-brand-bright)]" />
                <span className="font-semibold text-white">1800-420-ASSIST</span>
              </a>
              <a href="mailto:support@serviceassist.in" className="flex items-center gap-1.5 hover:text-white transition-colors">
                <Mail className="w-3.5 h-3.5 text-[var(--color-brand-bright)]" />
                <span>support@serviceassist.in</span>
              </a>
            </div>

            {/* Social Icons with real vector SVGs */}
            <div className="flex items-center gap-2 pt-2">
              <a
                href="https://instagram.com"
                target="_blank"
                rel="noopener noreferrer"
                className="w-8 h-8 rounded-lg bg-white/5 hover:bg-gradient-to-tr hover:from-amber-500 hover:via-rose-500 hover:to-purple-600 hover:text-white text-[var(--color-brand-light)] border border-white/10 flex items-center justify-center transition-all cursor-pointer shadow-xs hover:scale-105"
                title="Instagram"
                aria-label="Instagram"
              >
                <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24">
                  <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z" />
                </svg>
              </a>

              <a
                href="https://twitter.com"
                target="_blank"
                rel="noopener noreferrer"
                className="w-8 h-8 rounded-lg bg-white/5 hover:bg-black hover:text-white text-[var(--color-brand-light)] border border-white/10 flex items-center justify-center transition-all cursor-pointer shadow-xs hover:scale-105"
                title="X / Twitter"
                aria-label="X (formerly Twitter)"
              >
                <svg className="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24">
                  <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
                </svg>
              </a>

              <a
                href="https://linkedin.com"
                target="_blank"
                rel="noopener noreferrer"
                className="w-8 h-8 rounded-lg bg-white/5 hover:bg-[#0A66C2] hover:text-white text-[var(--color-brand-light)] border border-white/10 flex items-center justify-center transition-all cursor-pointer shadow-xs hover:scale-105"
                title="LinkedIn"
                aria-label="LinkedIn"
              >
                <svg className="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24">
                  <path d="M19 0h-14c-2.761 0-5 2.239-5 5v14c0 2.761 2.239 5 5 5h14c2.762 0 5-2.239 5-5v-14c0-2.761-2.238-5-5-5zm-11 19h-3v-11h3v11zm-1.5-12.268c-.966 0-1.75-.79-1.75-1.764s.784-1.764 1.75-1.764 1.75.79 1.75 1.764-.783 1.764-1.75 1.764zm13.5 12.268h-3v-5.604c0-3.368-4-3.113-4 0v5.604h-3v-11h3v1.765c1.396-2.586 7-2.777 7 2.476v6.759z" />
                </svg>
              </a>

              <a
                href="https://youtube.com"
                target="_blank"
                rel="noopener noreferrer"
                className="w-8 h-8 rounded-lg bg-white/5 hover:bg-[#FF0000] hover:text-white text-[var(--color-brand-light)] border border-white/10 flex items-center justify-center transition-all cursor-pointer shadow-xs hover:scale-105"
                title="YouTube"
                aria-label="YouTube"
              >
                <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24">
                  <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z" />
                </svg>
              </a>

              <a
                href="https://facebook.com"
                target="_blank"
                rel="noopener noreferrer"
                className="w-8 h-8 rounded-lg bg-white/5 hover:bg-[#1877F2] hover:text-white text-[var(--color-brand-light)] border border-white/10 flex items-center justify-center transition-all cursor-pointer shadow-xs hover:scale-105"
                title="Facebook"
                aria-label="Facebook"
              >
                <svg className="w-3.5 h-3.5 fill-current" viewBox="0 0 24 24">
                  <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
                </svg>
              </a>
            </div>
          </div>

          {/* Services Column */}
          <div className="space-y-2.5">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[var(--color-brand-bright)]">Services</h4>
            <ul className="space-y-1.5 text-xs text-[var(--color-brand-light)]/80">
              <li>
                <button onClick={() => onNavigate('/services')} className="hover:text-white transition-colors cursor-pointer text-left">
                  AC & Appliance Repair
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/services')} className="hover:text-white transition-colors cursor-pointer text-left">
                  Full Home Deep Clean
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/services')} className="hover:text-white transition-colors cursor-pointer text-left">
                  Salon & Spa at Home
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/services')} className="hover:text-white transition-colors cursor-pointer text-left">
                  Electrician & Plumbing
                </button>
              </li>
            </ul>
          </div>

          {/* Company Column */}
          <div className="space-y-2.5">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[var(--color-brand-bright)]">Company</h4>
            <ul className="space-y-1.5 text-xs text-[var(--color-brand-light)]/80">
              <li>
                <button onClick={() => onNavigate('/how-it-works')} className="hover:text-white transition-colors cursor-pointer text-left">
                  How It Works
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/offers')} className="hover:text-white transition-colors cursor-pointer text-left">
                  Coupons & Offers
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/professional/dashboard')} className="hover:text-white transition-colors cursor-pointer text-left">
                  Join as Partner
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/support')} className="hover:text-white transition-colors cursor-pointer text-left">
                  Careers & About
                </button>
              </li>
            </ul>
          </div>

          {/* Support & Legal Column */}
          <div className="space-y-2.5">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[var(--color-brand-bright)]">Support & Legal</h4>
            <ul className="space-y-1.5 text-xs text-[var(--color-brand-light)]/80">
              <li>
                <button onClick={() => onNavigate('/support')} className="hover:text-white transition-colors cursor-pointer text-left">
                  Help Center
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/support')} className="hover:text-white transition-colors cursor-pointer text-left">
                  Warranty & Refund
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/support')} className="hover:text-white transition-colors cursor-pointer text-left">
                  Terms of Service
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/support')} className="hover:text-white transition-colors cursor-pointer text-left">
                  Privacy Policy
                </button>
              </li>
            </ul>
          </div>
        </div>

        {/* Compact Cities Row */}
        <div className="pt-5 pb-5 border-b border-white/10 flex flex-col sm:flex-row sm:items-center gap-3">
          <div className="text-xs font-bold uppercase tracking-wider text-[var(--color-brand-bright)] shrink-0 flex items-center gap-1.5">
            <MapPin className="w-3.5 h-3.5 text-[var(--color-brand)]" />
            <span>Serving:</span>
          </div>
          <div className="flex flex-wrap gap-1.5 text-[11px] text-[var(--color-brand-light)]/80">
            {CITIES_LIST.slice(0, 10).map((city) => (
              <span
                key={city.id}
                className="px-2 py-0.5 rounded-md bg-white/5 border border-white/10 hover:border-[var(--color-brand)] hover:text-white transition-colors cursor-pointer"
              >
                {city.name}
              </span>
            ))}
            <span className="px-2 py-0.5 rounded-md bg-white/5 border border-white/10 text-[var(--color-brand-bright)] font-semibold">
              +10 more cities
            </span>
          </div>
        </div>

        {/* Bottom Copyright - Slim Bar */}
        <div className="pt-5 flex flex-col sm:flex-row items-center justify-between text-xs text-[var(--color-line)] gap-3">
          <p>© 2026 Service Assist Technologies Pvt Ltd. All rights reserved.</p>
          <div className="flex items-center gap-3 text-[11px]">
            <span className="flex items-center gap-1 text-[var(--color-brand-light)]">
              <CheckCircle2 className="w-3.5 h-3.5 text-[var(--color-brand-bright)]" />
              ISO 9001:2025 Certified
            </span>
            <span>•</span>
            <span>Doorstep Trust Guarantee</span>
          </div>
        </div>
      </div>
    </footer>
  );
};
