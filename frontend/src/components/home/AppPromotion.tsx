import React from 'react';
import { Smartphone, QrCode, Star, Download, ShieldCheck } from 'lucide-react';

export const AppPromotion: React.FC = () => {
  return (
    <section className="py-16 bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-[#15252B] rounded-3xl sm:rounded-[2.5rem] p-8 sm:p-12 text-white relative overflow-hidden shadow-2xl border border-orange-950/40">
          {/* Subtle background glow */}
          <div className="absolute top-0 right-0 w-96 h-96 rounded-full bg-[#FF7A00]/15 blur-3xl pointer-events-none" />

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
            <div className="lg:col-span-8 space-y-4">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#FF7A00]/20 text-[#FF9A3D] text-xs font-bold">
                <Smartphone className="w-3.5 h-3.5" />
                <span>Service Assist Mobile App</span>
              </div>
              <h2 className="text-3xl sm:text-4xl font-black tracking-tight leading-tight font-['Outfit']">
                Track your professional in real time with our mobile app.
              </h2>
              <p className="text-xs sm:text-sm text-gray-300 max-w-xl leading-relaxed">
                Receive instant arrival alerts, chat directly with technicians, schedule repetitive cleanings, and pay securely via UPI.
              </p>

              <div className="flex flex-wrap items-center gap-4 pt-4">
                {/* Store button 1 */}
                <button
                  type="button"
                  onClick={() => alert('Service Assist App is available for Android & iOS! Scan the QR code or use this web app.')}
                  className="px-5 py-2.5 rounded-2xl bg-white/10 hover:bg-white/20 border border-white/15 flex items-center gap-3 transition-colors text-left cursor-pointer"
                >
                  <div className="text-2xl">🍏</div>
                  <div>
                    <span className="text-[9px] uppercase font-bold text-gray-400 block leading-none">Download on</span>
                    <span className="text-xs font-extrabold text-white block">Apple App Store</span>
                  </div>
                </button>

                {/* Store button 2 */}
                <button
                  type="button"
                  onClick={() => alert('Service Assist App is available for Android & iOS! Scan the QR code or use this web app.')}
                  className="px-5 py-2.5 rounded-2xl bg-white/10 hover:bg-white/20 border border-white/15 flex items-center gap-3 transition-colors text-left cursor-pointer"
                >
                  <div className="text-2xl">🤖</div>
                  <div>
                    <span className="text-[9px] uppercase font-bold text-gray-400 block leading-none">Get it on</span>
                    <span className="text-xs font-extrabold text-white block">Google Play</span>
                  </div>
                </button>
              </div>

              <div className="flex items-center gap-4 pt-2 text-xs text-gray-400">
                <div className="flex items-center gap-1 text-[#FF9A3D] font-bold">
                  <Star className="w-4 h-4 fill-[#FF9A3D]" />
                  <span>4.9 / 5</span>
                </div>
                <span>•</span>
                <span>500,000+ Downloads</span>
                <span>•</span>
                <span>Instant Push Alerts</span>
              </div>
            </div>

            {/* QR Code Card */}
            <div className="lg:col-span-4 flex justify-center">
              <div className="bg-white p-5 rounded-3xl shadow-xl text-center text-gray-900 border border-orange-100 max-w-xs">
                <div className="w-40 h-40 mx-auto bg-gray-50 rounded-2xl flex items-center justify-center p-3 border border-gray-200 mb-3">
                  {/* Decorative QR code matrix */}
                  <div className="grid grid-cols-5 gap-1.5 w-full h-full p-2 bg-white rounded-lg shadow-inner">
                    {[...Array(25)].map((_, i) => (
                      <div
                        key={i}
                        className={`rounded-xs ${
                          i % 2 === 0 || i % 7 === 0 || i === 0 || i === 4 || i === 20 || i === 24
                            ? 'bg-[#15252B]'
                            : 'bg-[#FF7A00]/40'
                        }`}
                      />
                    ))}
                  </div>
                </div>
                <h4 className="font-bold text-xs text-gray-900 font-['Outfit']">Scan to download app</h4>
                <p className="text-[11px] text-gray-500 mt-0.5">Compatible with iOS & Android</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
