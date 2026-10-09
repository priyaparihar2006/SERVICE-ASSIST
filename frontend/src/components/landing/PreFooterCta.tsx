import React from 'react';
import { ArrowRight, UserCheck, ShieldCheck, Tag, User } from 'lucide-react';
import { UserProfile } from '../../types/landing';

interface PreFooterCtaProps {
  onInstantBook: () => void;
  currentUser?: UserProfile | null;
}

export const PreFooterCta: React.FC<PreFooterCtaProps> = ({ 
  onInstantBook,
  currentUser,
}) => {
  return (
    <section className="relative overflow-hidden py-14 sm:py-20 bg-slate-950 text-white">
      {/* Background Image with Dark Vignette */}
      <div className="absolute inset-0 z-0">
        <img
          src="/prefooter-livingroom.jpg"
          alt="Modern Home Interior"
          className="w-full h-full object-cover object-center opacity-30"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-[#070b14] via-[#070b14]/85 to-[#070b14]/70" />
      </div>

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 z-10">
        <div className="flex flex-col lg:flex-row items-center justify-between gap-8">
          
          {/* Left Text & CTA Button */}
          <div className="text-center lg:text-left space-y-4 max-w-xl">
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-black text-white tracking-tight leading-tight font-display">
              Your Home, Our Priority
            </h2>
            <p className="text-sm sm:text-base text-slate-300 leading-relaxed font-normal">
              Connect with certified professionals and get fast, reliable home services today.
            </p>
            <div className="pt-2">
              <button
                onClick={onInstantBook}
                className="px-7 py-3.5 bg-emerald-400 hover:bg-emerald-300 active:bg-emerald-500 text-slate-950 font-black text-sm sm:text-base rounded-full shadow-xl shadow-emerald-400/25 transition-all inline-flex items-center gap-2 cursor-pointer"
              >
                {currentUser ? (
                  <>
                    <span>Request an Expert</span>
                    <ArrowRight className="w-4 h-4 stroke-[2.5]" />
                  </>
                ) : (
                  <>
                    <User className="w-4 h-4 stroke-[2.5]" />
                    <span>Sign In to Get Started</span>
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Right 3 Round Icon Badges with 3D Floating Animations */}
          <div className="flex items-center gap-6 sm:gap-8 shrink-0">
            
            {/* Badge 1: Verified Experts */}
            <div className="flex flex-col items-center text-center space-y-2 animate-float-badge-1 group cursor-default">
              <div className="w-14 h-14 rounded-full bg-white/10 backdrop-blur-md border border-white/20 flex items-center justify-center text-emerald-400 shadow-xl group-hover:scale-110 group-hover:border-emerald-400/60 group-hover:bg-emerald-500/20 group-hover:shadow-emerald-500/25 transition-all duration-300">
                <UserCheck className="w-7 h-7" />
              </div>
              <span className="text-xs font-bold text-slate-200 group-hover:text-white transition-colors">
                Verified Experts
              </span>
            </div>

            {/* Badge 2: 100% Insured Care */}
            <div className="flex flex-col items-center text-center space-y-2 animate-float-badge-2 group cursor-default">
              <div className="w-14 h-14 rounded-full bg-white/10 backdrop-blur-md border border-white/20 flex items-center justify-center text-emerald-400 shadow-xl group-hover:scale-110 group-hover:border-emerald-400/60 group-hover:bg-emerald-500/20 group-hover:shadow-emerald-500/25 transition-all duration-300">
                <ShieldCheck className="w-7 h-7" />
              </div>
              <span className="text-xs font-bold text-slate-200 group-hover:text-white transition-colors">
                100% Insured Care
              </span>
            </div>

            {/* Badge 3: Affordable Pricing */}
            <div className="flex flex-col items-center text-center space-y-2 animate-float-badge-3 group cursor-default">
              <div className="w-14 h-14 rounded-full bg-white/10 backdrop-blur-md border border-white/20 flex items-center justify-center text-emerald-400 shadow-xl group-hover:scale-110 group-hover:border-emerald-400/60 group-hover:bg-emerald-500/20 group-hover:shadow-emerald-500/25 transition-all duration-300">
                <Tag className="w-7 h-7" />
              </div>
              <span className="text-xs font-bold text-slate-200 group-hover:text-white transition-colors">
                Affordable Pricing
              </span>
            </div>

          </div>

        </div>
      </div>
    </section>
  );
};


