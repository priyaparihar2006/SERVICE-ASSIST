import React from 'react';
import {
  ShieldCheck,
  Award,
  HeartHandshake,
  PhoneCall,
  Mail,
  MapPin,
  Sparkles,
  CheckCircle2,
} from 'lucide-react';
import { CITIES_LIST } from '../../context/LocationContext';
import { BrandLogo } from './BrandLogo';

interface FooterProps {
  onNavigate: (path: string) => void;
}

export const Footer: React.FC<FooterProps> = ({ onNavigate }) => {
  return (
    <footer className="bg-[#103C35] text-white pt-16 pb-12 border-t border-[#19C995]/20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Top Trust Banner */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5 pb-12 border-b border-white/10 mb-12">
          <div className="flex items-center gap-4 p-4.5 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-xs">
            <div className="w-12 h-12 rounded-xl bg-[#0B9F6E]/20 flex items-center justify-center text-[#19C995] shrink-0 border border-[#19C995]/30">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <div>
              <h4 className="font-bold text-sm text-white font-['Outfit']">100% Verified Partners</h4>
              <p className="text-xs text-[#DDF7EC]/70 mt-0.5">Thorough background checks & criminal verification</p>
            </div>
          </div>

          <div className="flex items-center gap-4 p-4.5 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-xs">
            <div className="w-12 h-12 rounded-xl bg-[#0B9F6E]/20 flex items-center justify-center text-[#19C995] shrink-0 border border-[#19C995]/30">
              <Award className="w-6 h-6" />
            </div>
            <div>
              <h4 className="font-bold text-sm text-white font-['Outfit']">30-Day Service Warranty</h4>
              <p className="text-xs text-[#DDF7EC]/70 mt-0.5">Free revisit guarantee if you are not 100% satisfied</p>
            </div>
          </div>

          <div className="flex items-center gap-4 p-4.5 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-xs">
            <div className="w-12 h-12 rounded-xl bg-[#0B9F6E]/20 flex items-center justify-center text-[#19C995] shrink-0 border border-[#19C995]/30">
              <HeartHandshake className="w-6 h-6" />
            </div>
            <div>
              <h4 className="font-bold text-sm text-white font-['Outfit']">Standard Transparent Pricing</h4>
              <p className="text-xs text-[#DDF7EC]/70 mt-0.5">Upfront rate cards with zero hidden visit charges</p>
            </div>
          </div>
        </div>

        {/* Main Footer Links - 5 Required Columns + Brand Info */}
        <div className="grid grid-cols-2 md:grid-cols-6 gap-8 pb-12 border-b border-white/10">
          {/* Brand Column */}
          <div className="col-span-2 space-y-4">
            <BrandLogo size="lg" variant="light" showTagline={true} />
            <p className="text-xs text-[#DDF7EC]/80 max-w-sm leading-relaxed">
              Premium On-Demand Home Services delivered right to your doorstep across India. From AC jet servicing and deep bathroom scrubbing to certified cosmetologists and master electricians.
            </p>

            <div className="flex flex-col gap-2 text-xs text-[#DDF7EC]/80 pt-1">
              <div className="flex items-center gap-2">
                <PhoneCall className="w-3.5 h-3.5 text-[#19C995]" />
                <span className="font-medium text-white">1800-420-ASSIST (Toll Free)</span>
              </div>
              <div className="flex items-center gap-2">
                <Mail className="w-3.5 h-3.5 text-[#19C995]" />
                <span className="font-medium text-white">support@serviceassist.in</span>
              </div>
            </div>

            {/* Social Icons */}
            <div className="flex items-center gap-3 pt-2">
              {[
                { name: 'Instagram', icon: '📷' },
                { name: 'Twitter', icon: '𝕏' },
                { name: 'LinkedIn', icon: 'in' },
                { name: 'YouTube', icon: '▶' },
              ].map((s) => (
                <span
                  key={s.name}
                  className="w-8 h-8 rounded-lg bg-white/5 hover:bg-[#0B9F6E] hover:text-white border border-white/10 flex items-center justify-center text-xs font-bold text-[#DDF7EC] cursor-pointer transition-colors"
                  title={s.name}
                >
                  {s.icon}
                </span>
              ))}
            </div>
          </div>

          {/* Column 1: Company */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[#19C995]">Company</h4>
            <ul className="space-y-2 text-xs text-[#DDF7EC]/80">
              <li>
                <button onClick={() => onNavigate('/how-it-works')} className="hover:text-white transition-colors cursor-pointer">
                  About Us
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/how-it-works')} className="hover:text-white transition-colors cursor-pointer">
                  Our Impact
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/offers')} className="hover:text-white transition-colors cursor-pointer">
                  Special Offers
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/support')} className="hover:text-white transition-colors cursor-pointer">
                  Careers & Culture
                </button>
              </li>
            </ul>
          </div>

          {/* Column 2: Services */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[#19C995]">Services</h4>
            <ul className="space-y-2 text-xs text-[#DDF7EC]/80">
              <li>
                <button onClick={() => onNavigate('/services')} className="hover:text-white transition-colors cursor-pointer">
                  AC & Appliances
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/services')} className="hover:text-white transition-colors cursor-pointer">
                  Home Cleaning
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/services')} className="hover:text-white transition-colors cursor-pointer">
                  Salon & Beauty
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/services')} className="hover:text-white transition-colors cursor-pointer">
                  Electrician & Plumbing
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/services')} className="hover:text-white transition-colors cursor-pointer">
                  Wall Painting
                </button>
              </li>
            </ul>
          </div>

          {/* Column 3: For Professionals */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[#19C995]">For Professionals</h4>
            <ul className="space-y-2 text-xs text-[#DDF7EC]/80">
              <li>
                <button onClick={() => onNavigate('/professional/dashboard')} className="hover:text-white transition-colors cursor-pointer">
                  Join as Partner
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/professional/dashboard')} className="hover:text-white transition-colors cursor-pointer">
                  Partner Dashboard
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/professional/dashboard')} className="hover:text-white transition-colors cursor-pointer">
                  Earnings & Benefits
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/support')} className="hover:text-white transition-colors cursor-pointer">
                  Safety Training
                </button>
              </li>
            </ul>
          </div>

          {/* Column 4 & 5: Support & Legal */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[#19C995]">Support & Legal</h4>
            <ul className="space-y-2 text-xs text-[#DDF7EC]/80">
              <li>
                <button onClick={() => onNavigate('/support')} className="hover:text-white transition-colors cursor-pointer">
                  Help Center
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/support')} className="hover:text-white transition-colors cursor-pointer">
                  Warranty & Refund
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/support')} className="hover:text-white transition-colors cursor-pointer">
                  Terms of Service
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/support')} className="hover:text-white transition-colors cursor-pointer">
                  Privacy Policy
                </button>
              </li>
              <li>
                <button onClick={() => onNavigate('/support')} className="hover:text-white transition-colors cursor-pointer">
                  Anti-discrimination
                </button>
              </li>
            </ul>
          </div>
        </div>

        {/* Cities Section */}
        <div className="pt-8 pb-8 border-b border-white/10">
          <div className="text-xs font-bold uppercase tracking-wider text-[#19C995] mb-3 flex items-center gap-1.5">
            <MapPin className="w-3.5 h-3.5 text-[#0B9F6E]" />
            <span>Serving 20+ Major Cities Across India</span>
          </div>
          <div className="flex flex-wrap gap-2 text-xs text-[#DDF7EC]/80">
            {CITIES_LIST.map((city) => (
              <span
                key={city.id}
                className="px-3 py-1 rounded-lg bg-white/5 border border-white/10 hover:border-[#0B9F6E] hover:text-white transition-colors cursor-pointer"
              >
                {city.name}
              </span>
            ))}
          </div>
        </div>

        {/* Bottom Copyright */}
        <div className="pt-8 flex flex-col sm:flex-row items-center justify-between text-xs text-[#A3B8B4] gap-4">
          <p>© 2026 Service Assist Technologies Private Limited. All rights reserved.</p>
          <div className="flex items-center gap-4 text-[11px]">
            <span className="flex items-center gap-1 text-[#DDF7EC]">
              <CheckCircle2 className="w-3.5 h-3.5 text-[#19C995]" />
              ISO 9001:2025 Certified
            </span>
            <span>•</span>
            <span>Premium On-Demand Home Services</span>
          </div>
        </div>
      </div>
    </footer>
  );
};
