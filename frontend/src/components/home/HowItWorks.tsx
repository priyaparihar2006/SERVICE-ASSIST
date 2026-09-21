import React from 'react';
import { Search, Calendar, UserCheck, ShieldCheck, CheckCircle2 } from 'lucide-react';

export const HowItWorks: React.FC = () => {
  const steps = [
    {
      num: '01',
      title: 'Choose a service',
      desc: 'Browse 40+ specialized categories with crystal clear upfront pricing and package variants.',
      icon: <Search className="w-5 h-5" />,
      color: 'bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] border-[var(--color-brand)]/30',
    },
    {
      num: '02',
      title: 'Pick your time',
      desc: 'Select same-day instant doorstep booking or schedule convenient 1-hour arrival windows.',
      icon: <Calendar className="w-5 h-5" />,
      color: 'bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] border-[var(--color-brand)]/30',
    },
    {
      num: '03',
      title: 'Expert arrives',
      desc: 'A background-checked, fully verified professional arrives at your doorstep with certified tools.',
      icon: <UserCheck className="w-5 h-5" />,
      color: 'bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] border-[var(--color-brand)]/30',
    },
    {
      num: '04',
      title: "Relax — it's done",
      desc: 'Inspect completed work, pay seamlessly online or via UPI, and enjoy our 30-day rework warranty.',
      icon: <ShieldCheck className="w-5 h-5" />,
      color: 'bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] border-[var(--color-brand)]/30',
    },
  ];

  return (
    <section className="py-20 bg-[var(--color-brand-soft)]/50 border-t border-[var(--color-brand-light)]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-2xl mx-auto mb-16">
          <div className="text-xs font-bold uppercase tracking-wider text-[var(--color-brand-hover)] mb-1 bg-[var(--color-brand-light)] px-3 py-1 rounded-full w-fit mx-auto">
            Simple & Transparent
          </div>
          <h2 className="text-3xl sm:text-4xl font-black text-[var(--color-brand-dark)] tracking-tight font-['Outfit'] mt-2">
            How Service Assist Works
          </h2>
          <p className="text-xs sm:text-sm text-[var(--color-muted)] mt-2">
            Effortless home care in four quick steps without multiple vendor phone calls or guesswork.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 relative">
          {steps.map((step) => (
            <div
              key={step.num}
              className="relative bg-white rounded-3xl p-6 border border-[var(--color-brand-light)] shadow-xs hover:border-[var(--color-brand)] hover:shadow-xl hover:-translate-y-1 transition-all duration-300 flex flex-col justify-between"
            >
              {/* Step number badge */}
              <div className="flex items-center justify-between mb-6">
                <div className={`w-12 h-12 rounded-2xl flex items-center justify-center border ${step.color}`}>
                  {step.icon}
                </div>
                <span className="text-3xl font-black text-[var(--color-brand-light)] select-none font-['Outfit']">
                  {step.num}
                </span>
              </div>

              <div>
                <h3 className="font-extrabold text-base text-[var(--color-brand-dark)] mb-2 font-['Outfit']">{step.title}</h3>
                <p className="text-xs text-[var(--color-muted)] leading-relaxed">{step.desc}</p>
              </div>

              <div className="mt-6 pt-4 border-t border-[var(--color-brand-light)]/60 flex items-center gap-1.5 text-xs font-bold text-[var(--color-brand-hover)]">
                <CheckCircle2 className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                <span>Verified Quality</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
