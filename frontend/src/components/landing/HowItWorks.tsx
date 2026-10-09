import React from 'react';
import { ArrowRight, CheckCircle2, Clock } from 'lucide-react';
import { Tilt3DCard } from './Tilt3DCard';

interface HowItWorksProps {
  onBookService?: () => void;
}

export const HowItWorks: React.FC<HowItWorksProps> = ({ onBookService }) => {
  const handleBookClick = () => {
    if (onBookService) {
      onBookService();
    } else {
      const el = document.getElementById('services');
      if (el) el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <section id="how-it-works" className="py-12 sm:py-16 bg-[#fafcfb] border-b border-slate-100 relative overflow-hidden">
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        
        {/* Section Header matching mockup */}
        <div className="text-center max-w-2xl mx-auto mb-10 sm:mb-12">
          <div className="inline-flex items-center gap-1.5 text-emerald-600 text-xs font-bold uppercase tracking-wider mb-2">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
            <span>SIMPLE 3-STEP PROCESS</span>
          </div>

          <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-slate-900 tracking-tight leading-tight">
            How Service Assist <span className="text-emerald-600">Works</span>
          </h2>
          <p className="mt-1.5 text-xs sm:text-sm text-slate-500 font-normal">
            Get trusted home services in just 3 simple steps.
          </p>
        </div>

        {/* 2-Column Showcase */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-center">
          
          {/* Left Column: 3-Step Sequence */}
          <div className="lg:col-span-7 space-y-8">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 items-center">
              
              {/* Step 1 */}
              <div className="bg-white rounded-2xl p-5 border border-slate-100 shadow-xs card-3d-interactive text-center space-y-2 group cursor-default">
                <div className="w-10 h-10 rounded-full bg-emerald-100 text-emerald-700 font-black text-base flex items-center justify-center mx-auto transition-all duration-300 group-hover:scale-115 group-hover:bg-emerald-600 group-hover:text-white shadow-xs">
                  1
                </div>
                <h3 className="text-sm font-extrabold text-slate-900 group-hover:text-emerald-700 transition-colors">
                  Choose a service
                </h3>
                <p className="text-xs text-slate-500 leading-relaxed">
                  Select the service you need for your home.
                </p>
              </div>

              {/* Step 2 */}
              <div className="bg-white rounded-2xl p-5 border border-slate-100 shadow-xs card-3d-interactive text-center space-y-2 group cursor-default">
                <div className="w-10 h-10 rounded-full bg-emerald-100 text-emerald-700 font-black text-base flex items-center justify-center mx-auto transition-all duration-300 group-hover:scale-115 group-hover:bg-emerald-600 group-hover:text-white shadow-xs">
                  2
                </div>
                <h3 className="text-sm font-extrabold text-slate-900 group-hover:text-emerald-700 transition-colors">
                  Pick a time
                </h3>
                <p className="text-xs text-slate-500 leading-relaxed">
                  Choose a slot that works for you.
                </p>
              </div>

              {/* Step 3 */}
              <div className="bg-white rounded-2xl p-5 border border-slate-100 shadow-xs card-3d-interactive text-center space-y-2 group cursor-default">
                <div className="w-10 h-10 rounded-full bg-emerald-100 text-emerald-700 font-black text-base flex items-center justify-center mx-auto transition-all duration-300 group-hover:scale-115 group-hover:bg-emerald-600 group-hover:text-white shadow-xs">
                  3
                </div>
                <h3 className="text-sm font-extrabold text-slate-900 group-hover:text-emerald-700 transition-colors">
                  Expert arrives
                </h3>
                <p className="text-xs text-slate-500 leading-relaxed">
                  A verified professional comes to your home.
                </p>
              </div>

            </div>

            {/* Explore services CTA Button */}
            <div className="pt-2 text-center sm:text-left">
              <button
                onClick={handleBookClick}
                className="px-6 py-3 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white font-extrabold text-sm rounded-full shadow-lg shadow-emerald-600/30 transition-all inline-flex items-center gap-2 cursor-pointer hover:shadow-xl hover:-translate-y-0.5"
              >
                <span>Explore All Services</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Right Column: Customer Photo with 3D Tilt & Floating Live Tracking Card */}
          <div className="lg:col-span-5 relative flex justify-center items-center">
            
            <Tilt3DCard maxTilt={10} perspective={1200} glare={true} className="max-w-[360px] sm:max-w-[400px]">
              <div className="relative w-full aspect-[4/3] preserve-3d">
                {/* Customer Photo */}
                <div 
                  style={{ transform: 'translateZ(10px)' }}
                  className="relative w-full h-full rounded-3xl overflow-hidden shadow-xl border border-slate-200 bg-slate-100"
                >
                  <img
                    src="/customer-booking.jpg"
                    alt="Happy customer using Service Assist app"
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/30 via-transparent to-transparent pointer-events-none" />
                </div>

                {/* Floating Live Tracking Card with 3D Elevation */}
                <div 
                  style={{ transform: 'translateZ(45px)' }}
                  className="absolute -bottom-5 -right-2 sm:-right-6 bg-white/95 backdrop-blur-md rounded-2xl p-4 shadow-2xl border border-slate-100 max-w-[240px] text-left z-20 space-y-2.5 animate-float-slow"
                >
                  <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
                    <div className="w-6 h-6 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center shrink-0">
                      <CheckCircle2 className="w-4 h-4" />
                    </div>
                    <div>
                      <p className="text-[11px] font-black text-slate-900 leading-tight">Service Confirmed</p>
                      <p className="text-[9px] text-slate-400">An expert will arrive in 15 mins</p>
                    </div>
                  </div>

                  {/* Progress Milestones */}
                  <div className="space-y-1.5 text-[10px]">
                    <div className="flex items-center gap-1.5 text-emerald-700 font-semibold">
                      <CheckCircle2 className="w-3 h-3 text-emerald-500 shrink-0" />
                      <span>Request Received</span>
                    </div>
                    <div className="flex items-center gap-1.5 text-emerald-700 font-semibold">
                      <CheckCircle2 className="w-3 h-3 text-emerald-500 shrink-0" />
                      <span>Expert Assigned</span>
                    </div>
                    <div className="flex items-center gap-1.5 text-slate-700 font-bold">
                      <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-ping shrink-0 ml-0.5 mr-0.5" />
                      <span>On the way</span>
                    </div>
                    <div className="flex items-center gap-1.5 text-slate-400 font-normal">
                      <Clock className="w-3 h-3 text-slate-300 shrink-0" />
                      <span>Service Completed</span>
                    </div>
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

