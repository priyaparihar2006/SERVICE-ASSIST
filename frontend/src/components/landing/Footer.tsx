import React from 'react';
import { 
  ShieldCheck, 
  ChevronDown, 
  Facebook, 
  Instagram, 
  Youtube, 
  Linkedin,
  MapPin
} from 'lucide-react';
import { CITIES } from '../../data/landing/servicesData';

interface FooterProps {
  onOpenPartnerModal: () => void;
  onSelectCategory?: (catId: string) => void;
  onCityChange: (cityName: string) => void;
}

export const Footer: React.FC<FooterProps> = ({
  onOpenPartnerModal,
  onSelectCategory,
  onCityChange,
}) => {
  return (
    <footer className="bg-[#070b14] text-slate-400 border-t border-white/[0.08] pt-14 pb-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Main 5-Column Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-8 lg:gap-10 pb-12 border-b border-white/[0.08]">
          
          {/* Column 1: Brand & Socials (span 4) */}
          <div className="lg:col-span-4 space-y-4 text-left">
            <a href="#" className="flex items-center gap-2 group shrink-0">
              <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-emerald-500 to-teal-400 flex items-center justify-center text-slate-950 shadow-md shadow-emerald-500/20 group-hover:scale-105 transition-transform">
                <ShieldCheck className="w-5 h-5 stroke-[2.5]" />
              </div>
              <span className="text-lg font-extrabold tracking-tight text-white group-hover:text-emerald-400 transition-colors">
                Service Assist
              </span>
            </a>

            <p className="text-xs sm:text-sm text-slate-400 leading-relaxed max-w-sm">
              Your trusted partner for home services. Book verified professionals for all your home needs.
            </p>

            {/* Social Icons */}
            <div className="flex items-center gap-3 pt-1">
              <a 
                href="#" 
                className="w-8 h-8 rounded-full bg-white/[0.06] hover:bg-emerald-500/20 hover:text-emerald-400 flex items-center justify-center text-slate-300 transition-colors"
                aria-label="Facebook"
              >
                <Facebook className="w-4 h-4" />
              </a>
              <a 
                href="#" 
                className="w-8 h-8 rounded-full bg-white/[0.06] hover:bg-emerald-500/20 hover:text-emerald-400 flex items-center justify-center text-slate-300 transition-colors"
                aria-label="Instagram"
              >
                <Instagram className="w-4 h-4" />
              </a>
              <a 
                href="#" 
                className="w-8 h-8 rounded-full bg-white/[0.06] hover:bg-emerald-500/20 hover:text-emerald-400 flex items-center justify-center text-slate-300 transition-colors"
                aria-label="YouTube"
              >
                <Youtube className="w-4 h-4" />
              </a>
              <a 
                href="#" 
                className="w-8 h-8 rounded-full bg-white/[0.06] hover:bg-emerald-500/20 hover:text-emerald-400 flex items-center justify-center text-slate-300 transition-colors"
                aria-label="LinkedIn"
              >
                <Linkedin className="w-4 h-4" />
              </a>
            </div>
          </div>

          {/* Column 2: Quick Links (span 2) */}
          <div className="lg:col-span-2 space-y-3 text-left">
            <h4 className="text-xs font-bold text-white uppercase tracking-wider">
              Quick Links
            </h4>
            <ul className="space-y-2 text-xs sm:text-sm">
              <li><a href="#services" className="hover:text-white transition-colors">Services</a></li>
              <li><a href="#how-it-works" className="hover:text-white transition-colors">How It Works</a></li>
              <li><a href="#why-us" className="hover:text-white transition-colors">Why Us</a></li>
              <li>
                <button 
                  onClick={onOpenPartnerModal} 
                  className="hover:text-emerald-400 text-left transition-colors cursor-pointer"
                >
                  Become a Partner
                </button>
              </li>
            </ul>
          </div>

          {/* Column 3: Popular Services (span 2) */}
          <div className="lg:col-span-2 space-y-3 text-left">
            <h4 className="text-xs font-bold text-white uppercase tracking-wider">
              Popular Services
            </h4>
            <ul className="space-y-2 text-xs sm:text-sm">
              <li>
                <button
                  onClick={() => {
                    onSelectCategory && onSelectCategory('ac-appliance');
                    const el = document.getElementById('services');
                    if (el) el.scrollIntoView({ behavior: 'smooth' });
                  }}
                  className="hover:text-white text-left transition-colors cursor-pointer"
                >
                  AC Repair
                </button>
              </li>
              <li>
                <button
                  onClick={() => {
                    onSelectCategory && onSelectCategory('deep-cleaning');
                    const el = document.getElementById('services');
                    if (el) el.scrollIntoView({ behavior: 'smooth' });
                  }}
                  className="hover:text-white text-left transition-colors cursor-pointer"
                >
                  Home Cleaning
                </button>
              </li>
              <li>
                <button
                  onClick={() => {
                    onSelectCategory && onSelectCategory('electrician-plumber');
                    const el = document.getElementById('services');
                    if (el) el.scrollIntoView({ behavior: 'smooth' });
                  }}
                  className="hover:text-white text-left transition-colors cursor-pointer"
                >
                  Electrician &amp; Plumber
                </button>
              </li>
              <li>
                <button
                  onClick={() => {
                    onSelectCategory && onSelectCategory('pest-control');
                    const el = document.getElementById('services');
                    if (el) el.scrollIntoView({ behavior: 'smooth' });
                  }}
                  className="hover:text-white text-left transition-colors cursor-pointer"
                >
                  Pest Control
                </button>
              </li>
            </ul>
          </div>

          {/* Column 4: Support (span 2) */}
          <div className="lg:col-span-2 space-y-3 text-left">
            <h4 className="text-xs font-bold text-white uppercase tracking-wider">
              Support
            </h4>
            <ul className="space-y-2 text-xs sm:text-sm">
              <li><a href="tel:18002008080" className="hover:text-white transition-colors">Help Center</a></li>
              <li><a href="#how-it-works" className="hover:text-white transition-colors">FAQs</a></li>
              <li><a href="#" className="hover:text-white transition-colors">Terms &amp; Conditions</a></li>
              <li><a href="#" className="hover:text-white transition-colors">Privacy Policy</a></li>
            </ul>
          </div>

          {/* Column 5: Download Our App (span 2) */}
          <div className="lg:col-span-2 space-y-3 text-left">
            <h4 className="text-xs font-bold text-white uppercase tracking-wider">
              Download Our App
            </h4>
            <div className="space-y-2 pt-1">
              {/* Google Play */}
              <a
                href="#"
                className="flex items-center gap-2 px-3 py-2 rounded-xl bg-white/[0.06] hover:bg-white/[0.1] border border-white/[0.08] transition-colors group"
              >
                <div className="w-5 h-5 flex items-center justify-center shrink-0">
                  <svg className="w-4 h-4 fill-current text-emerald-400" viewBox="0 0 24 24">
                    <path d="M3.6 1.4A2 2 0 0 0 3 3v18c0 .6.2 1.2.6 1.6l10.3-10.3L3.6 1.4zM15.3 10.9 5.8 1.9c.4-.3.9-.4 1.4-.2l11 6.3-2.9 2.9zM15.3 13.1l2.9 2.9-11 6.3c-.5.2-1 .1-1.4-.2l9.5-9zM19.6 11.2l-2.6-1.5-3 3 3 3 2.6-1.5c.9-.5.9-2.5 0-3z" />
                  </svg>
                </div>
                <div className="text-left">
                  <p className="text-[9px] uppercase tracking-wider text-slate-400 leading-none">GET IT ON</p>
                  <p className="text-xs font-bold text-white leading-tight">Google Play</p>
                </div>
              </a>

              {/* Apple App Store */}
              <a
                href="#"
                className="flex items-center gap-2 px-3 py-2 rounded-xl bg-white/[0.06] hover:bg-white/[0.1] border border-white/[0.08] transition-colors group"
              >
                <div className="w-5 h-5 flex items-center justify-center shrink-0">
                  <svg className="w-4 h-4 fill-current text-white" viewBox="0 0 24 24">
                    <path d="M18.7 19.5c-.8 1.2-1.7 2.4-3 2.5-1.4.1-1.8-.8-3.4-.8-1.5 0-2 .8-3.3.8-1.3 0-2.3-1.3-3.1-2.5-1.7-2.4-3-6.9-1.2-9.9 1-1.5 2.6-2.5 4.3-2.5 1.4 0 2.6.9 3.5.9.8 0 2.3-1.1 3.9-.9 1.4.1 2.5.6 3.2 1.7-2.8 1.7-2.3 5.4.5 6.6-.6 1.6-1.4 3.1-2.4 4.1zM15.5 6.4c.6-.8 1.1-1.9 1-3-.9.1-2.1.6-2.7 1.4-.6.7-1.1 1.8-.9 2.9 1.1.1 2-.5 2.6-1.3z" />
                  </svg>
                </div>
                <div className="text-left">
                  <p className="text-[9px] uppercase tracking-wider text-slate-400 leading-none">Download on the</p>
                  <p className="text-xs font-bold text-white leading-tight">App Store</p>
                </div>
              </a>
            </div>
          </div>

        </div>

        {/* Bottom Bar: Copyright & City / Country selector */}
        <div className="pt-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500">
          <div>
            © {new Date().getFullYear()} Service Assist. All rights reserved.
          </div>

          <div className="flex items-center gap-3 text-slate-400">
            {/* City selector */}
            <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-white/[0.04] border border-white/[0.08]">
              <MapPin className="w-3 h-3 text-emerald-400" />
              <span>Delhi NCR</span>
              <ChevronDown className="w-3 h-3 text-slate-500" />
            </div>

            <span>|</span>

            {/* Country */}
            <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-white/[0.04] border border-white/[0.08]">
              <span>India</span>
              <ChevronDown className="w-3 h-3 text-slate-500" />
            </div>
          </div>
        </div>

      </div>
    </footer>
  );
};

