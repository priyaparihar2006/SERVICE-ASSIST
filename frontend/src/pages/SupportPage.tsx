import { apiFetch } from '../services/api';
import React, { useState } from 'react';
import { PhoneCall, Mail, MessageSquare, ShieldCheck, CheckCircle2, HelpCircle, Send } from 'lucide-react';

export const SupportPage: React.FC = () => {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [submittedTicket, setSubmittedTicket] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await apiFetch('/api/support/ticket', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, email, subject, message }),
      });
      const data = await res.json();
      if (data.ticketId) {
        setSubmittedTicket(data.ticketId);
        setName('');
        setEmail('');
        setSubject('');
        setMessage('');
      }
    } catch (e) {
      alert(e.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#FFF8F2]/30 py-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-2xl mx-auto mb-12">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#FFF1E5] text-[#E85D04] text-xs font-bold mb-2">
            <HelpCircle className="w-3.5 h-3.5 text-[#FF7A00]" />
            <span>24/7 Service Assist Care</span>
          </div>
          <h1 className="text-3xl sm:text-4xl font-black text-[#15252B] tracking-tight font-['Outfit']">
            How Can We Help You Today?
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-2">
            Instant assistance for your scheduled bookings, service quality, and payment queries.
          </p>
        </div>

        {/* Contact Methods */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
          <div className="bg-white p-6 rounded-3xl border border-gray-100 shadow-xs text-center flex flex-col items-center">
            <div className="w-12 h-12 rounded-2xl bg-[#FFF1E5] text-[#FF7A00] flex items-center justify-center mb-4">
              <PhoneCall className="w-6 h-6" />
            </div>
            <h3 className="font-extrabold text-base text-gray-900 mb-1 font-['Outfit']">Toll-Free Helpline</h3>
            <p className="text-xs text-gray-500 mb-4">Speak directly with a support agent 24/7</p>
            <a
              href="tel:18004202774"
              className="text-xs font-bold text-[#E85D04] bg-[#FFF1E5] px-4 py-2 rounded-xl hover:bg-[#FFE5D1] transition-colors"
            >
              1800-420-ASSIST
            </a>
          </div>

          <div className="bg-white p-6 rounded-3xl border border-gray-100 shadow-xs text-center flex flex-col items-center">
            <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center mb-4">
              <MessageSquare className="w-6 h-6" />
            </div>
            <h3 className="font-extrabold text-base text-gray-900 mb-1 font-['Outfit']">WhatsApp Chat</h3>
            <p className="text-xs text-gray-500 mb-4">Get quick booking updates & share photos</p>
            <a
              href="https://wa.me/919876543210"
              target="_blank"
              rel="noreferrer"
              className="text-xs font-bold text-emerald-700 bg-emerald-50 px-4 py-2 rounded-xl hover:bg-emerald-100 transition-colors"
            >
              Chat on WhatsApp
            </a>
          </div>

          <div className="bg-white p-6 rounded-3xl border border-gray-100 shadow-xs text-center flex flex-col items-center">
            <div className="w-12 h-12 rounded-2xl bg-orange-50 text-[#FF7A00] flex items-center justify-center mb-4">
              <Mail className="w-6 h-6" />
            </div>
            <h3 className="font-extrabold text-base text-gray-900 mb-1 font-['Outfit']">Email Support</h3>
            <p className="text-xs text-gray-500 mb-4">Inquiries, invoices & partner registrations</p>
            <a
              href="mailto:support@serviceassist.in"
              className="text-xs font-bold text-[#E85D04] bg-[#FFF1E5] px-4 py-2 rounded-xl hover:bg-[#FFE5D1] transition-colors"
            >
              support@serviceassist.in
            </a>
          </div>
        </div>

        {/* Ticket Form & Warranty info */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Ticket Form */}
          <div className="lg:col-span-7 bg-white p-6 sm:p-8 rounded-3xl border border-gray-100 shadow-xs">
            <h3 className="text-lg font-black text-gray-900 mb-2 font-['Outfit']">Submit a Support Ticket</h3>
            <p className="text-xs text-gray-500 mb-6">
              Our service supervisor will get in touch within 30 minutes.
            </p>

            {submittedTicket && (
              <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 rounded-2xl text-xs text-emerald-900 flex items-center gap-3">
                <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
                <div>
                  <span className="font-bold block">Ticket Created: #{submittedTicket}</span>
                  <span>We have received your issue and assigned a priority resolution specialist.</span>
                </div>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Your Name</label>
                  <input
                    type="text"
                    required
                    placeholder="Priya Sharma"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full p-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[#FF7A00]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 mb-1">Email Address</label>
                  <input
                    type="email"
                    required
                    placeholder="name@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full p-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[#FF7A00]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Subject / Issue Type</label>
                <select
                  required
                  value={subject}
                  onChange={(e) => setSubject(e.target.value)}
                  className="w-full p-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[#FF7A00] cursor-pointer"
                >
                  <option value="">Select subject...</option>
                  <option value="Schedule change or reschedule">Schedule change or reschedule</option>
                  <option value="Claim 30-Day Quality Warranty">Claim 30-Day Quality Warranty</option>
                  <option value="Billing & invoice query">Billing & invoice query</option>
                  <option value="Feedback regarding professional">Feedback regarding professional</option>
                  <option value="Other">Other</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 mb-1">Describe your request</label>
                <textarea
                  rows={4}
                  required
                  placeholder="Include your booking ID if applicable and detailed notes..."
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  className="w-full p-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-1 focus:ring-[#FF7A00]"
                />
              </div>

              <button
                type="submit"
                disabled={submitting}
                className="w-full py-3 bg-[#FF7A00] hover:bg-[#E85D04] text-white font-bold text-xs rounded-xl shadow-xs transition-all flex items-center justify-center gap-2 cursor-pointer shadow-orange-500/20"
              >
                <Send className="w-3.5 h-3.5" />
                <span>{submitting ? 'Submitting...' : 'Submit Support Request'}</span>
              </button>
            </form>
          </div>

          {/* 30-Day Warranty Callout */}
          <div className="lg:col-span-5 bg-gradient-to-br from-[#15252B] to-[#1E343C] text-white p-6 sm:p-8 rounded-3xl shadow-lg space-y-4 border border-orange-500/20">
            <div className="w-12 h-12 rounded-2xl bg-orange-500/20 border border-orange-500/30 flex items-center justify-center text-[#FF9A3D]">
              <ShieldCheck className="w-6 h-6" />
            </div>

            <h3 className="text-xl font-black font-['Outfit']">The Service Assist 30-Day Revisit Guarantee</h3>
            <p className="text-xs text-orange-100/80 leading-relaxed">
              Every job completed by Service Assist partners is protected by our zero-cost warranty. If your AC leaks, a clean tap develops a drip, or deep cleaning misses a corner, we will return and fix it free.
            </p>

            <ul className="space-y-2 text-xs text-orange-100/90 pt-2">
              <li className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-[#FF9A3D] shrink-0" />
                <span>Zero questions asked inspection</span>
              </li>
              <li className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-[#FF9A3D] shrink-0" />
                <span>Assigned to a master quality technician</span>
              </li>
              <li className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-[#FF9A3D] shrink-0" />
                <span>Genuine spare parts backed by warranty</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};
