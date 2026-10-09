import React from 'react';
import { ShieldCheck, Tag, Clock, Award } from 'lucide-react';

export const TrustGuarantees: React.FC = () => {
  const pillars = [
    {
      icon: <Award className="w-6 h-6 text-emerald-600" />,
      title: 'Verified & Trained Experts',
      desc: 'Background-verified professionals',
    },
    {
      icon: <Tag className="w-6 h-6 text-emerald-600" />,
      title: 'Upfront & Transparent Pricing',
      desc: 'No hidden charges',
    },
    {
      icon: <Clock className="w-6 h-6 text-emerald-600" />,
      title: 'On-Time Service',
      desc: 'We respect your time',
    },
    {
      icon: <ShieldCheck className="w-6 h-6 text-emerald-600" />,
      title: '30-Day Service Warranty',
      desc: 'Your satisfaction is guaranteed!',
    },
  ];

  return (
    <section id="why-us" className="py-12 sm:py-16 bg-white border-b border-slate-100">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header matching mockup */}
        <div className="text-center max-w-2xl mx-auto mb-10">
          <div className="inline-flex items-center gap-1.5 text-emerald-600 text-xs font-bold uppercase tracking-wider mb-2">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
            <span>WHY CHOOSE SERVICE ASSIST</span>
          </div>

          <h2 className="text-2xl sm:text-3xl lg:text-4xl font-black text-slate-900 tracking-tight leading-tight">
            Why Thousands of Families Trust Us
          </h2>
        </div>

        {/* 4 Cards Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {pillars.map((item, idx) => (
            <div
              key={idx}
              className="bg-[#fafcfb] hover:bg-white rounded-2xl p-6 border border-slate-200/80 hover:border-emerald-300 shadow-xs card-3d-interactive text-center space-y-3 group cursor-default"
            >
              <div 
                style={{ transform: 'translateZ(18px)' }}
                className="w-12 h-12 rounded-2xl bg-emerald-50 flex items-center justify-center mx-auto transition-transform duration-300 group-hover:scale-115 group-hover:shadow-sm"
              >
                {item.icon}
              </div>
              <h3 className="text-sm font-extrabold text-slate-900 leading-snug group-hover:text-emerald-700 transition-colors">
                {item.title}
              </h3>
              <p className="text-xs text-slate-500 leading-relaxed font-normal">
                {item.desc}
              </p>
            </div>
          ))}
        </div>

      </div>
    </section>
  );
};

