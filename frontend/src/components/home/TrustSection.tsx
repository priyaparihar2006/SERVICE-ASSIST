import React from 'react';
import { ShieldCheck, Award, FileCheck, RefreshCw, Star } from 'lucide-react';

export const TrustSection: React.FC = () => {
  const pillars = [
    {
      title: '100% Verified Partners',
      desc: 'Rigorous 3-step screening including Aadhaar, criminal background checks, and police clearance.',
      icon: <ShieldCheck className="w-6 h-6 text-[#19C995]" />,
    },
    {
      title: 'Skill Audited & Certified',
      desc: 'All technicians undergo hands-on technical testing and hygiene protocol drills before induction.',
      icon: <Award className="w-6 h-6 text-[#19C995]" />,
    },
    {
      title: '30-Day Service Warranty',
      desc: 'If anything is not up to mark, our quality supervisor revisits and fixes it at zero extra charge.',
      icon: <RefreshCw className="w-6 h-6 text-[#19C995]" />,
    },
    {
      title: 'No Hidden Surcharges',
      desc: 'Standard itemized rate card for spare parts and labour. What you see is what you pay.',
      icon: <FileCheck className="w-6 h-6 text-[#19C995]" />,
    },
  ];

  return (
    <section className="py-20 bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-[#103C35] rounded-3xl sm:rounded-[2.5rem] p-8 sm:p-12 lg:p-16 text-white relative overflow-hidden shadow-2xl border border-[#19C995]/20">
          {/* Subtle background glow */}
          <div className="absolute top-0 right-0 w-80 h-80 rounded-full bg-[#0B9F6E]/15 blur-3xl pointer-events-none" />

          <div className="max-w-3xl mb-12">
            <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-white/10 text-[#DDF7EC] text-xs font-bold mb-4 border border-[#19C995]/30">
              <Star className="w-3.5 h-3.5 fill-[#19C995] text-[#19C995]" />
              <span>The Service Assist Safety & Quality Promise</span>
            </div>
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-black tracking-tight leading-tight font-['Outfit']">
              Professionals you can genuinely trust in your home.
            </h2>
            <p className="text-sm sm:text-base text-[#DDF7EC]/80 mt-4 leading-relaxed">
              We know inviting someone into your home requires complete peace of mind. Here is how we uphold the highest standard in Indian home services:
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {pillars.map((pillar) => (
              <div
                key={pillar.title}
                className="p-6 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-xs hover:bg-white/10 transition-colors"
              >
                <div className="w-12 h-12 rounded-xl bg-white/10 flex items-center justify-center mb-4 border border-[#19C995]/30">
                  {pillar.icon}
                </div>
                <h3 className="font-bold text-base text-white mb-2 font-['Outfit']">{pillar.title}</h3>
                <p className="text-xs text-[#DDF7EC]/70 leading-relaxed">{pillar.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
};
