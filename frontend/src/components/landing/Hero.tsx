import React, { useState } from 'react';
import { 
  Star, 
  Search, 
  CheckCircle2, 
  Clock, 
  ShieldCheck, 
  Zap, 
  Wrench, 
  Sparkles, 
  Tag, 
  Wind 
} from 'lucide-react';
import { Tilt3DCard } from './Tilt3DCard';

interface HeroProps {
  currentCity?: string;
  onSelectCategory?: (catId: string) => void;
  onInstantBook?: () => void;
  onOpenAiDiagnostic?: () => void;
}

export const Hero: React.FC<HeroProps> = ({
  onSelectCategory,
  onInstantBook,
}) => {
  const [searchInput, setSearchInput] = useState('');

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const el = document.getElementById('services');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <section className="relative overflow-hidden bg-gradient-to-br from-[#ebfaf3] via-[#f7fcf9] to-[#e4f6ee] text-slate-900 pt-6 sm:pt-14 pb-12 sm:pb-20 border-b border-emerald-100/70">
      
      {/* Background Animated Atmosphere matching user mockup */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden z-0">
        
        {/* Animated Video Background - Dynamic flowing mint blobs, ripples and floating particles */}
        <div 
          className="absolute inset-0 pointer-events-none overflow-hidden z-0"
          style={{
            WebkitMaskImage: 'linear-gradient(to right, rgba(0,0,0,1) 0%, rgba(0,0,0,1) 52%, rgba(0,0,0,0) 78%)',
            maskImage: 'linear-gradient(to right, rgba(0,0,0,1) 0%, rgba(0,0,0,1) 52%, rgba(0,0,0,0) 78%)',
          }}
        >
          <video
            src="/hero-bg-animated.mp4"
            autoPlay
            loop
            muted
            playsInline
            className="w-full h-full object-cover object-center lg:object-left opacity-90 select-none pointer-events-none"
          />
        </div>

        {/* Concentric Circular Orbit Rings centered behind technician with gentle pulse */}
        <div className="absolute top-[62%] sm:top-1/2 -translate-y-1/2 left-1/2 -translate-x-1/2 lg:left-auto lg:translate-x-0 -right-0 sm:right-[0%] lg:right-[4%] xl:right-[8%] w-[380px] xs:w-[440px] sm:w-[720px] lg:w-[860px] aspect-square pointer-events-none select-none z-0 flex items-center justify-center animate-pulse-orbit">
          {/* Inner Orbit Circle */}
          <div className="absolute w-[44%] aspect-square rounded-full border border-emerald-400/40" />
          {/* Middle Orbit Circle */}
          <div className="absolute w-[72%] aspect-square rounded-full border border-emerald-400/30" />
          {/* Outer Orbit Circle */}
          <div className="absolute w-[100%] aspect-square rounded-full border border-emerald-300/20" />
          {/* Soft inner glow gradient */}
          <div className="absolute w-[55%] aspect-square rounded-full bg-emerald-200/25 blur-[60px] sm:blur-[90px]" />
        </div>

        {/* Floating Glassy Mint Orbs matching mockup */}
        <div className="hidden sm:block absolute top-10 left-[48%] w-13 h-13 rounded-full bg-gradient-to-br from-emerald-300/60 to-teal-200/35 border border-emerald-200/60 shadow-lg shadow-emerald-400/10 blur-[0.5px] animate-float-slow" />
        <div className="hidden sm:block absolute bottom-12 left-[52%] w-6 h-6 rounded-full bg-emerald-400/40 border border-emerald-300/40 animate-float-reverse" />
        <div className="absolute bottom-16 right-10 w-24 h-24 rounded-full bg-emerald-200/30 blur-[40px]" />

        {/* Accent 3-line spark burst in top right */}
        <div className="absolute top-7 right-14 sm:right-24 flex gap-1.5 transform rotate-[-25deg] opacity-80 pointer-events-none animate-pulse">
          <span className="w-1.5 h-3.5 rounded-full bg-emerald-400" />
          <span className="w-1.5 h-5 rounded-full bg-emerald-400 -translate-y-1" />
          <span className="w-1.5 h-3.5 rounded-full bg-emerald-400" />
        </div>

        {/* Dot Grid Matrix 1 (center behind text) */}
        <div className="absolute top-1/2 left-[48%] -translate-y-1/2 hidden xl:grid grid-cols-5 gap-3 opacity-30 pointer-events-none">
          {[...Array(25)].map((_, i) => (
            <span key={i} className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
          ))}
        </div>

        {/* Dot Grid Matrix 2 (behind technician's shoulder) */}
        <div className="absolute top-[28%] right-[22%] hidden xl:grid grid-cols-4 gap-2.5 opacity-25 pointer-events-none">
          {[...Array(20)].map((_, i) => (
            <span key={i} className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
          ))}
        </div>

        {/* Subtle Ambient Mint Glows */}
        <div className="absolute -top-24 -left-20 w-[420px] h-[420px] bg-emerald-200/30 rounded-full blur-[130px]" />
        <div className="absolute top-1/3 -right-20 w-[550px] h-[550px] bg-teal-200/30 rounded-full blur-[140px]" />
      </div>

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 z-10">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
          
          {/* Left Column: Heading, Search & Trust Badges */}
          <div className="lg:col-span-7 space-y-4 sm:space-y-5 text-left">
            
            {/* Pill Badge: • TRUSTED HOME SERVICES PLATFORM */}
            <div className="inline-flex items-center gap-1.5 sm:gap-2 px-3 sm:px-3.5 py-1 rounded-full bg-[#dcfce7] text-[#047857] text-[10px] sm:text-[11px] font-extrabold uppercase tracking-wider shadow-2xs border border-emerald-200/70">
              <span className="w-2 h-2 rounded-full bg-[#059669]" />
              <span>TRUSTED HOME SERVICES PLATFORM</span>
            </div>

            {/* Main Headline */}
            <h1 className="text-3xl xs:text-4xl sm:text-5xl lg:text-[56px] xl:text-[60px] font-black tracking-tight leading-[1.12] text-slate-900 font-display">
              Get Expert <br />
              Home Help &amp; Repairs <br />
              <span className="text-[#00dfa2]">in 10 Minutes</span>
            </h1>

            {/* Subtitle */}
            <p className="text-slate-600 text-xs sm:text-sm md:text-base max-w-lg leading-relaxed font-normal">
              Verified professionals for all your home needs — fast, safe and affordable.
            </p>

            {/* Search Input Bar */}
            <form onSubmit={handleSearchSubmit} className="pt-1 max-w-xl">
              <div className="relative bg-white rounded-full p-1.5 sm:p-2 pl-4 sm:pl-6 shadow-xl shadow-slate-200/70 border border-slate-200/80 flex items-center justify-between">
                <input
                  type="text"
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  placeholder="What service do you need? (e.g. AC repair)"
                  className="w-full bg-transparent pr-2 sm:pr-4 text-slate-800 placeholder-slate-400 text-xs sm:text-sm font-medium focus:outline-none"
                />
                <button
                  type="submit"
                  aria-label="Search"
                  className="w-9 h-9 sm:w-11 sm:h-11 rounded-full bg-[#00dfa2] hover:bg-[#00c992] active:bg-[#00b583] flex items-center justify-center text-white shrink-0 transition-all cursor-pointer shadow-md shadow-[#00dfa2]/30"
                >
                  <Search className="w-4 h-4 sm:w-5 sm:h-5 stroke-[2.5]" />
                </button>
              </div>
            </form>

            {/* 4 Trust Badges in a Horizontal Row */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 sm:gap-3 pt-1 sm:pt-2 text-[11px] sm:text-xs text-slate-700 font-semibold">
              <div className="flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 sm:w-4 sm:h-4 text-[#00dfa2] shrink-0 stroke-[2.5]" />
                <span>Verified Experts</span>
              </div>
              <div className="flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5 sm:w-4 sm:h-4 text-[#00dfa2] shrink-0 stroke-[2.5]" />
                <span>Certified Quality</span>
              </div>
              <div className="flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 sm:w-4 sm:h-4 text-[#00dfa2] shrink-0 stroke-[2.5]" />
                <span>On-time Service</span>
              </div>
              <div className="flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5 sm:w-4 sm:h-4 text-[#00dfa2] shrink-0 stroke-[2.5]" />
                <span>100% Safe &amp; Secure</span>
              </div>
            </div>

            {/* Social Proof Bar */}
            <div className="pt-1 sm:pt-2 flex flex-wrap items-center gap-2 sm:gap-3 text-[11px] sm:text-xs text-slate-600">
              {/* Overlapping customer avatars in Retina HD */}
              <div className="flex -space-x-1.5 sm:-space-x-2">
                <img 
                  src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=160&h=160&q=90" 
                  alt="Customer" 
                  className="w-7 h-7 sm:w-8 sm:h-8 rounded-full border-2 border-white object-cover shadow-xs" 
                />
                <img 
                  src="https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=160&h=160&q=90" 
                  alt="Customer" 
                  className="w-7 h-7 sm:w-8 sm:h-8 rounded-full border-2 border-white object-cover shadow-xs" 
                />
                <img 
                  src="https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=160&h=160&q=90" 
                  alt="Customer" 
                  className="w-7 h-7 sm:w-8 sm:h-8 rounded-full border-2 border-white object-cover shadow-xs" 
                />
                <img 
                  src="https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=160&h=160&q=90" 
                  alt="Customer" 
                  className="w-7 h-7 sm:w-8 sm:h-8 rounded-full border-2 border-white object-cover shadow-xs" 
                />
              </div>

              <div className="flex items-center gap-1 font-bold text-slate-900">
                <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                <span>4.8/5</span>
                <span className="text-slate-500 font-normal">(50K+ reviews)</span>
              </div>

              <span className="text-slate-400">·</span>

              <div>
                Trusted by <strong className="text-slate-900 font-bold">5M+ households</strong>
              </div>
            </div>

          </div>

          {/* Right Column: Isolated Technician Cutout with 3D Parallax & Floating Badges */}
          <div className="lg:col-span-5 relative flex justify-center items-center mt-6 lg:mt-0 w-full px-1 xs:px-2 sm:px-0">
            
            <Tilt3DCard maxTilt={6} perspective={1400} glare={false} className="w-full max-w-[330px] xs:max-w-[370px] sm:max-w-[430px] lg:max-w-[470px] relative z-10 pointer-events-none">
              <div className="relative w-full aspect-[3/4] min-h-[460px] xs:min-h-[500px] sm:min-h-[540px] lg:min-h-[580px] flex justify-center items-end preserve-3d">

                {/* Isolated Technician Cutout in 4K Ultra HD */}
                <img
                  src="/hero-technician-cutout.png"
                  alt="Service Assist Technician"
                  fetchPriority="high"
                  decoding="async"
                  style={{ transform: 'translateZ(20px)' }}
                  className="w-full max-w-[280px] xs:max-w-[320px] sm:max-w-[380px] lg:max-w-[420px] h-auto object-contain object-bottom drop-shadow-[0_12px_24px_rgba(0,0,0,0.08)] drop-shadow-[0_28px_42px_rgba(4,120,87,0.22)] select-none pointer-events-none transition-transform"
                />

                {/* Floating Badge 1: AC Repair (Top Left) */}
                <button
                  type="button"
                  onClick={() => onSelectCategory && onSelectCategory('ac-appliance')}
                  style={{ transform: 'translateZ(55px)' }}
                  className="pointer-events-auto absolute top-1 xs:top-2 -left-1 sm:-left-6 lg:-left-8 bg-white text-slate-900 rounded-xl sm:rounded-2xl px-2.5 py-1.5 sm:px-4 sm:py-2.5 shadow-lg sm:shadow-xl shadow-slate-200/90 border border-slate-100 flex items-center gap-2 sm:gap-3 transition-transform hover:scale-105 sm:hover:scale-110 cursor-pointer z-30 text-left animate-float-badge-1"
                >
                  <div className="w-7 h-7 sm:w-10 sm:h-10 rounded-lg sm:rounded-xl bg-sky-50 text-sky-500 flex items-center justify-center shrink-0">
                    <Wind className="w-3.5 h-3.5 sm:w-5 sm:h-5" />
                  </div>
                  <div>
                    <p className="text-[10px] xs:text-[11px] sm:text-xs font-black leading-tight text-slate-900">AC Repair</p>
                    <p className="text-[9px] sm:text-[10px] text-emerald-600 font-semibold">Certified Pro</p>
                  </div>
                </button>

                {/* Floating Badge 2: 10 min Quick Booking (Top Right) */}
                <div 
                  style={{ transform: 'translateZ(65px)' }}
                  className="pointer-events-auto absolute top-2 xs:top-3 -right-1 sm:-right-6 lg:-right-8 bg-[#00dfa2] text-slate-950 font-black rounded-xl sm:rounded-3xl px-2.5 py-1.5 sm:px-4 sm:py-2.5 shadow-lg sm:shadow-xl shadow-[#00dfa2]/30 flex items-center gap-1.5 sm:gap-2.5 z-30 animate-float-badge-2"
                >
                  {/* Top-right accent burst lines */}
                  <div className="absolute -top-2.5 -right-1 sm:-top-3.5 sm:-right-2 flex gap-0.5 sm:gap-1 pointer-events-none rotate-12">
                    <span className="w-0.5 sm:w-1 h-2 sm:h-3 rounded-full bg-[#00dfa2]" />
                    <span className="w-0.5 sm:w-1 h-3 sm:h-4.5 rounded-full bg-[#00dfa2] -translate-y-0.5" />
                    <span className="w-0.5 sm:w-1 h-2 sm:h-3 rounded-full bg-[#00dfa2]" />
                  </div>

                  <div className="w-6 h-6 sm:w-8 sm:h-8 rounded-lg sm:rounded-xl bg-slate-950/10 flex items-center justify-center shrink-0">
                    <Zap className="w-3.5 h-3.5 sm:w-4.5 sm:h-4.5 fill-slate-950 text-slate-950 stroke-none" />
                  </div>
                  <div className="text-left">
                    <p className="text-[10px] xs:text-[11px] sm:text-xs font-black leading-tight">10 min</p>
                    <p className="text-[9px] sm:text-[10px] font-bold text-slate-900/80">Fast Dispatch</p>
                  </div>
                </div>

                {/* Floating Badge 3: Plumber (Mid Left) */}
                <button
                  type="button"
                  onClick={() => onSelectCategory && onSelectCategory('electrician-plumber')}
                  style={{ transform: 'translateZ(50px)' }}
                  className="pointer-events-auto absolute top-[37%] -left-2 sm:-left-8 lg:-left-12 bg-white text-slate-900 rounded-xl sm:rounded-2xl px-2.5 py-1.5 sm:px-4 sm:py-2.5 shadow-lg sm:shadow-xl shadow-slate-200/90 border border-slate-100 flex items-center gap-2 sm:gap-3 transition-transform hover:scale-105 sm:hover:scale-110 cursor-pointer z-30 text-left animate-float-badge-3"
                >
                  <div className="w-7 h-7 sm:w-10 sm:h-10 rounded-lg sm:rounded-xl bg-emerald-50 text-emerald-500 flex items-center justify-center shrink-0">
                    <Wrench className="w-3.5 h-3.5 sm:w-5 sm:h-5" />
                  </div>
                  <div>
                    <p className="text-[10px] xs:text-[11px] sm:text-xs font-black leading-tight text-slate-900">Plumber</p>
                    <p className="text-[9px] sm:text-[10px] text-emerald-600 font-semibold">Doorstep Repair</p>
                  </div>
                </button>

                {/* Floating Badge 4: Electrician (Mid Right) */}
                <button
                  type="button"
                  onClick={() => onSelectCategory && onSelectCategory('electrician-plumber')}
                  style={{ transform: 'translateZ(55px)' }}
                  className="pointer-events-auto absolute top-[33%] -right-2 sm:-right-8 lg:-right-10 bg-white text-slate-900 rounded-xl sm:rounded-2xl px-2.5 py-1.5 sm:px-4 sm:py-2.5 shadow-lg sm:shadow-xl shadow-slate-200/90 border border-slate-100 flex items-center gap-2 sm:gap-3 transition-transform hover:scale-105 sm:hover:scale-110 cursor-pointer z-30 text-left animate-float-badge-4"
                >
                  <div className="w-7 h-7 sm:w-10 sm:h-10 rounded-lg sm:rounded-xl bg-amber-50 text-amber-500 flex items-center justify-center shrink-0">
                    <Zap className="w-3.5 h-3.5 sm:w-5 sm:h-5 text-amber-500" />
                  </div>
                  <div>
                    <p className="text-[10px] xs:text-[11px] sm:text-xs font-black leading-tight text-slate-900">Electrician</p>
                    <p className="text-[9px] sm:text-[10px] text-emerald-600 font-semibold">Safety Certified</p>
                  </div>
                </button>

                {/* Floating Badge 5: Cleaning (Lower Right) */}
                <button
                  type="button"
                  onClick={() => onSelectCategory && onSelectCategory('deep-cleaning')}
                  style={{ transform: 'translateZ(45px)' }}
                  className="pointer-events-auto absolute top-[62%] -right-1 sm:-right-6 lg:-right-8 bg-white text-slate-900 rounded-xl sm:rounded-2xl px-2.5 py-1.5 sm:px-4 sm:py-2.5 shadow-lg sm:shadow-xl shadow-slate-200/90 border border-slate-100 flex items-center gap-2 sm:gap-3 transition-transform hover:scale-105 sm:hover:scale-110 cursor-pointer z-30 text-left animate-float-badge-1"
                >
                  <div className="w-7 h-7 sm:w-10 sm:h-10 rounded-lg sm:rounded-xl bg-teal-50 text-teal-500 flex items-center justify-center shrink-0">
                    <Sparkles className="w-3.5 h-3.5 sm:w-5 sm:h-5 text-teal-500" />
                  </div>
                  <div>
                    <p className="text-[10px] xs:text-[11px] sm:text-xs font-black leading-tight text-slate-900">Cleaning</p>
                    <p className="text-[9px] sm:text-[10px] text-emerald-600 font-semibold">Deep Clean</p>
                  </div>
                </button>

                {/* Floating Bottom Wide Badge: Verified Professionals */}
                <div 
                  style={{ transform: 'translateZ(60px)' }}
                  className="pointer-events-auto absolute -bottom-2 sm:-bottom-3 left-1/2 -translate-x-1/2 w-[95%] sm:w-[94%] bg-gradient-to-r from-[#034c35] via-[#066144] to-[#034c35] text-white rounded-xl sm:rounded-3xl px-3 py-2.5 sm:px-4.5 sm:py-3.5 shadow-2xl shadow-emerald-950/25 border border-emerald-400/25 flex items-center gap-2.5 sm:gap-3.5 z-30 animate-float-slow"
                >
                  <div className="w-8 h-8 sm:w-11 sm:h-11 rounded-xl sm:rounded-2xl bg-emerald-500/20 text-[#00dfa2] border border-emerald-400/25 flex items-center justify-center shrink-0 shadow-inner">
                    <ShieldCheck className="w-4 h-4 sm:w-5.5 sm:h-5.5 stroke-[2.5]" />
                  </div>
                  <div className="text-left">
                    <p className="text-[11px] xs:text-xs sm:text-sm font-extrabold leading-tight text-white tracking-wide">Verified Professionals</p>
                    <p className="text-[9px] xs:text-[10px] sm:text-[11px] text-emerald-200/90 font-medium">Background-checked &amp; trained experts</p>
                  </div>
                </div>

              </div>
            </Tilt3DCard>
          </div>

        </div>
      </div>
    </section>
  );
};

