import React, { useState } from 'react';
import { Briefcase, CheckCircle2, ShieldCheck, ArrowRight, X, Phone, User, MapPin } from 'lucide-react';
import { CITIES } from '../../data/landing/servicesData';

interface ProPartnerCtaProps {
  isOpen: boolean;
  onOpen: () => void;
  onClose: () => void;
  showBanner?: boolean;
}

export const ProPartnerCta: React.FC<ProPartnerCtaProps> = ({
  isOpen,
  onOpen,
  onClose,
  showBanner = true,
}) => {
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [city, setCity] = useState(CITIES[0].name);
  const [skill, setSkill] = useState('AC & Appliance Repair');
  const [experience, setExperience] = useState('3-5 Years');
  const [submitted, setSubmitted] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await fetch('/api/partners/apply', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, phone, city, skill, experience }),
      });
      setSubmitted(true);
    } catch {
      setSubmitted(true);
    } finally {
      setSubmitting(false);
    }
  };

  const handleReset = () => {
    setName('');
    phone && setPhone('');
    setSubmitted(false);
    onClose();
  };

  return (
    <>
      {/* Banner Section on Landing Page (optional) */}
      {showBanner && (
        <section className="py-8 sm:py-10 bg-slate-50 border-b border-slate-200">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="bg-gradient-to-br from-slate-900 via-slate-900 to-teal-950 rounded-2xl p-6 sm:p-8 text-white relative overflow-hidden shadow-xl border border-slate-800">
              {/* Background glow */}
              <div className="absolute top-0 right-0 w-80 h-80 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />

              <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-center relative z-10">
                <div className="lg:col-span-8 space-y-3">
                  <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 text-[11px] font-semibold border border-emerald-500/30">
                    <Briefcase className="w-3 h-3" />
                    <span>Partner with Service Assist</span>
                  </div>

                  <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tight font-display">
                    Earn Up to <span className="text-emerald-400">₹45,000 to ₹75,000/month</span> as a Certified Pro
                  </h2>

                  <p className="text-slate-300 text-sm sm:text-base max-w-xl">
                    Get steady daily bookings in your neighborhood, guaranteed on-time weekly direct bank payouts, dedicated health insurance, and genuine spare parts support.
                  </p>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 text-xs font-semibold text-slate-300">
                    <div className="flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                      <span>Weekly Direct Payouts</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                      <span>Free Uniforms & Safety Kits</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                      <span>Accidental Health Cover</span>
                    </div>
                  </div>
                </div>

                <div className="lg:col-span-4 flex justify-start lg:justify-end">
                  <button
                    onClick={onOpen}
                    className="px-6 py-3.5 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white font-extrabold text-sm sm:text-base rounded-xl shadow-lg shadow-emerald-600/30 transition-all flex items-center gap-2 cursor-pointer whitespace-nowrap"
                  >
                    <span>Apply as a Partner Pro</span>
                    <ArrowRight className="w-5 h-5" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </section>
      )}

      {/* Pro Registration Modal */}
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl relative border border-slate-100 max-h-[90vh] overflow-y-auto">
            <button
              onClick={handleReset}
              className="absolute top-5 right-5 p-2 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            {submitted ? (
              <div className="text-center py-6 space-y-4">
                <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto shadow-inner">
                  <ShieldCheck className="w-9 h-9" />
                </div>
                <h3 className="text-2xl font-bold text-slate-900 font-display">
                  Application Received!
                </h3>
                <p className="text-sm text-slate-600">
                  Thank you, <strong>{name || 'Partner'}</strong>! Our Pro Onboarding Coordinator in <strong>{city}</strong> will call your number (<strong>{phone}</strong>) within 2 hours to schedule document verification.
                </p>
                <div className="p-4 bg-slate-50 rounded-2xl text-xs text-slate-600 text-left space-y-1.5">
                  <p className="font-bold text-slate-900">What to keep ready:</p>
                  <p>1. Aadhaar Card / Government Photo ID</p>
                  <p>2. Bank Passbook or Cancelled Cheque</p>
                  <p>3. Trade Tool Kit or Technical Certificate</p>
                </div>
                <button
                  onClick={handleReset}
                  className="w-full py-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-sm font-bold cursor-pointer"
                >
                  Done
                </button>
              </div>
            ) : (
              <div>
                <div className="mb-6">
                  <div className="inline-flex items-center gap-1.5 text-xs font-bold text-emerald-700 uppercase tracking-wider mb-1">
                    <ShieldCheck className="w-3.5 h-3.5" />
                    Verified Pro Recruitment
                  </div>
                  <h3 className="text-2xl font-extrabold text-slate-900 font-display">
                    Join Service Assist as a Pro
                  </h3>
                  <p className="text-xs text-slate-500 mt-1">
                    Fill this quick form and start receiving customer bookings in 24 hours.
                  </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                  <div>
                    <label className="text-xs font-bold text-slate-700 block mb-1">
                      Full Name
                    </label>
                    <div className="relative">
                      <User className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                      <input
                        type="text"
                        required
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        placeholder="e.g. Ramesh Kumar"
                        className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2.5 pl-9 pr-3 text-sm text-slate-800 focus:outline-emerald-500"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="text-xs font-bold text-slate-700 block mb-1">
                      WhatsApp / Mobile Number
                    </label>
                    <div className="relative">
                      <Phone className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                      <input
                        type="tel"
                        required
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        placeholder="e.g. 9876543210"
                        className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2.5 pl-9 pr-3 text-sm text-slate-800 focus:outline-emerald-500"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-xs font-bold text-slate-700 block mb-1">
                        Primary City
                      </label>
                      <select
                        value={city}
                        onChange={(e) => setCity(e.target.value)}
                        className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2.5 px-3 text-sm text-slate-800 focus:outline-emerald-500"
                      >
                        {CITIES.map((c) => (
                          <option key={c.id} value={c.name}>
                            {c.name}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label className="text-xs font-bold text-slate-700 block mb-1">
                        Experience
                      </label>
                      <select
                        value={experience}
                        onChange={(e) => setExperience(e.target.value)}
                        className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2.5 px-3 text-sm text-slate-800 focus:outline-emerald-500"
                      >
                        <option value="1-2 Years">1 - 2 Years</option>
                        <option value="3-5 Years">3 - 5 Years</option>
                        <option value="5-10 Years">5 - 10 Years</option>
                        <option value="10+ Years">10+ Years</option>
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="text-xs font-bold text-slate-700 block mb-1">
                      Your Primary Trade / Skill
                    </label>
                    <select
                      value={skill}
                      onChange={(e) => setSkill(e.target.value)}
                      className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2.5 px-3 text-sm text-slate-800 focus:outline-emerald-500"
                    >
                      <option value="AC & Appliance Repair">AC & Appliance Repair</option>
                      <option value="Electrician & Plumber">Electrician & Plumber</option>
                      <option value="Home Deep Cleaning">Home Deep Cleaning</option>
                      <option value="Carpentry & Handyman">Carpentry & Handyman</option>
                      <option value="Pest Control">Pest Control</option>
                      <option value="Water Purifier (RO)">Water Purifier (RO)</option>
                      <option value="Salon & Spa">Salon & Spa at Home</option>
                    </select>
                  </div>

                  <p className="text-[11px] text-slate-400">
                    By submitting, you agree to our Pro Partner Code of Conduct and background verification check.
                  </p>

                  <button
                    type="submit"
                    disabled={submitting}
                    className="w-full py-3 bg-emerald-600 hover:bg-emerald-700 active:bg-emerald-800 text-white rounded-xl text-sm font-bold transition-colors cursor-pointer shadow-md shadow-emerald-600/30"
                  >
                    {submitting ? 'Submitting Application...' : 'Submit Application'}
                  </button>
                </form>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
};

