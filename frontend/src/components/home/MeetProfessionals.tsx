import React from 'react';
import { Professional } from '../../types';
import { Star, ShieldCheck, Award, ArrowRight } from 'lucide-react';
import { ImageWithFallback } from '../common/ImageWithFallback';
import { getProfessionalImage, getProfessionFallbackImage } from '../../utils/professionalImages';

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
    <section className="py-16 bg-[var(--color-brand-soft)]/30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-10">
          <div>
            <div className="text-xs font-bold uppercase tracking-wider text-[var(--color-brand)] mb-1">
              Top Rated Experts
            </div>
            <h2 className="text-2xl sm:text-3xl font-black text-[var(--color-ink)] tracking-tight font-['Outfit']">
              Meet Our Verified Professionals
            </h2>
          </div>
          <p className="text-xs sm:text-sm text-gray-500 max-w-sm">
            Trained in hygiene, polite conduct, and advanced technical diagnostics for your home.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {professionals.map((pro) => {
            const imgSrc = getProfessionalImage(pro);
            const fallbackSrc = getProfessionFallbackImage(pro.profession, pro.id);

            return (
              <div
                key={pro.id}
                className="bg-white rounded-3xl p-5 border border-gray-100 shadow-xs hover:border-[var(--color-brand-bright)]/50 hover:shadow-xl hover:-translate-y-1 transition-all duration-300 flex flex-col justify-between"
              >
                <div>
                  {/* Pro Avatar with Verification Badge & Experienced Badge */}
                  <div className="relative mb-4 overflow-hidden rounded-2xl aspect-[4/3] sm:aspect-square bg-orange-50/40">
                    <ImageWithFallback
                      src={imgSrc}
                      fallbackSrc={fallbackSrc}
                      fallbackProfession={pro.profession}
                      alt={`${pro.name} - ${pro.profession}`}
                      className="w-full h-full rounded-2xl object-cover object-top border border-gray-100 transition-transform duration-500 hover:scale-105"
                      referrerPolicy="no-referrer"
                    />
                    <div className="absolute top-3 left-3 px-2.5 py-1 rounded-full bg-black/60 backdrop-blur-md text-white text-[10px] font-bold flex items-center gap-1 shadow-sm">
                      <ShieldCheck className="w-3.5 h-3.5 text-[var(--color-brand)]" />
                      <span>Verified Pro</span>
                    </div>
                    {pro.experienceYears >= 5 && (
                      <div className="absolute top-3 right-3 px-2.5 py-1 rounded-full bg-white/95 backdrop-blur-md text-gray-800 text-[10px] font-bold flex items-center gap-1 shadow-xs">
                        <Award className="w-3 h-3 text-[var(--color-brand)]" />
                        <span>{pro.experienceYears}y exp</span>
                      </div>
                    )}
                  </div>

                  <div className="flex items-center justify-between mb-1">
                    <h3 className="font-extrabold text-base text-gray-900 font-['Outfit'] truncate">{pro.name}</h3>
                    <div className="flex items-center gap-1 text-xs font-bold text-amber-500 shrink-0">
                      <Star className="w-3.5 h-3.5 fill-[var(--color-brand-bright)] text-[var(--color-brand-bright)]" />
                      <span>{pro.rating}</span>
                    </div>
                  </div>

                  <p className="text-xs font-semibold text-[var(--color-brand-hover)] mb-2 truncate">{pro.profession}</p>
                  <p className="text-xs text-gray-500 line-clamp-2 leading-relaxed mb-4">
                    {pro.bio}
                  </p>
                </div>

                <div className="pt-3 border-t border-gray-100 space-y-3">
                  <div className="flex items-center justify-between text-[11px] text-gray-500 font-medium">
                    <span>{pro.completedJobs} jobs done</span>
                    <span>{pro.experienceYears} years exp.</span>
                  </div>

                  <button
                    onClick={() => {
                      onSelectProfessional(pro.id);
                      onExploreServices();
                    }}
                    className="w-full py-2.5 rounded-xl bg-[var(--color-brand-soft)] hover:bg-[var(--color-brand)] text-[var(--color-ink)] hover:text-white font-bold text-xs transition-all flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <span>Book This Expert</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};
