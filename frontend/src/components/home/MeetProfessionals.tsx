import React from 'react';
import { Professional } from '../../types';
import { Star, ShieldCheck, Award, CheckCircle2, ArrowRight } from 'lucide-react';

interface MeetProfessionalsProps {
  professionals: Professional[];
  onSelectProfessional: (proId: string) => void;
  onExploreServices: () => void;
}

export const MeetProfessionals: React.FC<MeetProfessionalsProps> = ({
  professionals,
  onSelectProfessional,
  onExploreServices,
}) => {
  return (
    <section className="py-16 bg-[#FFF8F2]/30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-10">
          <div>
            <div className="text-xs font-bold uppercase tracking-wider text-[#FF7A00] mb-1">
              Top Rated Experts
            </div>
            <h2 className="text-2xl sm:text-3xl font-black text-[#15252B] tracking-tight font-['Outfit']">
              Meet Our Verified Professionals
            </h2>
          </div>
          <p className="text-xs sm:text-sm text-gray-500 max-w-sm">
            Trained in hygiene, polite conduct, and advanced technical diagnostics for your home.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {professionals.map((pro) => (
            <div
              key={pro.id}
              className="bg-white rounded-3xl p-5 border border-gray-100 shadow-xs hover:border-[#FF9A3D]/50 hover:shadow-xl hover:-translate-y-1 transition-all duration-300 flex flex-col justify-between"
            >
              <div>
                {/* Pro Avatar with Verification Badge */}
                <div className="relative mb-4">
                  <img
                    src={pro.avatar}
                    alt={pro.name}
                    className="w-full h-48 rounded-2xl object-cover object-top border border-gray-100"
                    referrerPolicy="no-referrer"
                  />
                  <div className="absolute top-3 left-3 px-2.5 py-1 rounded-full bg-black/60 backdrop-blur-md text-white text-[10px] font-bold flex items-center gap-1">
                    <ShieldCheck className="w-3 h-3 text-[#FF7A00]" />
                    <span>Verified Pro</span>
                  </div>
                  {pro.badge && (
                    <div className="absolute bottom-3 right-3 px-2.5 py-0.5 rounded-full bg-[#FF7A00] text-white text-[10px] font-extrabold shadow-sm">
                      {pro.badge}
                    </div>
                  )}
                </div>

                <div className="flex items-center justify-between mb-1">
                  <h3 className="font-extrabold text-base text-gray-900 font-['Outfit']">{pro.name}</h3>
                  <div className="flex items-center gap-1 text-xs font-bold text-amber-500">
                    <Star className="w-3.5 h-3.5 fill-[#FF9A3D] text-[#FF9A3D]" />
                    <span>{pro.rating}</span>
                  </div>
                </div>

                <p className="text-xs font-semibold text-[#E85D04] mb-2">{pro.specialization}</p>
                <p className="text-xs text-gray-500 line-clamp-2 leading-relaxed mb-4">
                  {pro.bio}
                </p>
              </div>

              <div className="pt-3 border-t border-gray-100 space-y-3">
                <div className="flex items-center justify-between text-[11px] text-gray-500 font-medium">
                  <span>{pro.jobsCompleted}+ jobs done</span>
                  <span>{pro.experienceYears} years exp.</span>
                </div>

                <button
                  onClick={onExploreServices}
                  className="w-full py-2.5 rounded-xl bg-[#FFF8F2] hover:bg-[#FF7A00] text-[#15252B] hover:text-white font-bold text-xs transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <span>Book This Expert</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
