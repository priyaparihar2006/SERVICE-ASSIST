import React, { useState } from 'react';
import { ChevronDown, HelpCircle, PhoneCall } from 'lucide-react';

export const FAQSection: React.FC = () => {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  const faqs = [
    {
      q: 'How does Service Assist verify its service professionals?',
      a: 'Every technician, cleaner, and aesthetician goes through mandatory 3-point background screening including government ID verification (Aadhaar/PAN), court record checks, and police clearance. We also test their hands-on domain competency and train them on safety protocols before onboarding.',
    },
    {
      q: 'What is the 30-Day Service Assist Quality Guarantee?',
      a: 'If any service provided by our partner fails to meet standards or issues persist within 30 days of service completion, we dispatch a senior supervisor to inspect and rectify the work completely free of charge.',
    },
    {
      q: 'Can I reschedule or cancel my booking?',
      a: 'Yes, you can reschedule or cancel directly from your bookings dashboard up to 2 hours before the scheduled slot with zero cancellation fees. Instant refunds are credited to your original payment mode or UPI.',
    },
    {
      q: 'Do I need to supply any tools, chemicals, or equipment?',
      a: 'No! All Service Assist professionals arrive fully equipped with certified commercial gear (high-pressure jet washers, eco-friendly hospital-grade disinfectants, branded single-use salon kits, and professional diagnostic multimeters).',
    },
    {
      q: 'How does pricing work? Are there any hidden fees?',
      a: 'We pride ourselves on 100% upfront rate cards. The price you see at checkout includes labour, standard consumables, and taxes. If specialized replacement spare parts are needed during appliance repairs, the technician will show you the standard rate card for your approval before proceeding.',
    },
  ];

  return (
    <section className="py-20 bg-[var(--color-brand-soft)]/30 border-t border-gray-100">
      <div className="max-w-4xl mx-auto px-4 sm:px-6">
        <div className="text-center mb-12">
          <div className="text-xs font-bold uppercase tracking-wider text-[var(--color-brand)] mb-1">
            Got Questions?
          </div>
          <h2 className="text-3xl sm:text-4xl font-black text-[var(--color-ink)] tracking-tight font-['Outfit']">
            Frequently Asked Questions
          </h2>
          <p className="text-xs sm:text-sm text-gray-500 mt-2">
            Everything you need to know about booking doorstep services with Service Assist.
          </p>
        </div>

        <div className="space-y-3">
          {faqs.map((faq, idx) => {
            const isOpen = openIndex === idx;
            return (
              <div
                key={idx}
                className="bg-white rounded-2xl border border-gray-200/80 overflow-hidden transition-all shadow-xs"
              >
                <button
                  onClick={() => setOpenIndex(isOpen ? null : idx)}
                  className="w-full p-5 text-left flex items-center justify-between gap-4 font-bold text-sm text-gray-900 hover:text-[var(--color-brand)] transition-colors cursor-pointer"
                >
                  <span>{faq.q}</span>
                  <ChevronDown
                    className={`w-4 h-4 text-gray-400 shrink-0 transition-transform duration-200 ${
                      isOpen ? 'rotate-180 text-[var(--color-brand)]' : ''
                    }`}
                  />
                </button>

                {isOpen && (
                  <div className="px-5 pb-5 text-xs sm:text-sm text-gray-600 leading-relaxed border-t border-gray-50 pt-3">
                    {faq.a}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        <div className="mt-10 p-6 bg-[var(--color-brand-light)]/70 border border-brand-light/80 rounded-3xl text-center flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="text-left">
            <h4 className="font-bold text-sm text-gray-900 font-['Outfit']">Still have questions?</h4>
            <p className="text-xs text-gray-500">Our customer happiness team is available 24/7 to assist you.</p>
          </div>
          <div className="flex items-center gap-3">
            <a
              href="tel:18004207378"
              className="px-4 py-2 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white text-xs font-bold rounded-xl flex items-center gap-2 shadow-xs transition-colors"
            >
              <PhoneCall className="w-3.5 h-3.5" />
              <span>Call 1800-420-ASSIST</span>
            </a>
          </div>
        </div>
      </div>
    </section>
  );
};
