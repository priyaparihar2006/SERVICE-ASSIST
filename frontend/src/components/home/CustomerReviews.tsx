import React from 'react';
import { Review } from '../../types';
import { Star, CheckCircle2 } from 'lucide-react';

interface CustomerReviewsProps {
  reviews: Review[];
}

export const CustomerReviews: React.FC<CustomerReviewsProps> = ({ reviews }) => {
  return (
    <section className="py-20 bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-2xl mx-auto mb-14">
          <div className="text-xs font-bold uppercase tracking-wider text-[#FF7A00] mb-1">
            Real Experiences
          </div>
          <h2 className="text-3xl sm:text-4xl font-black text-[#15252B] tracking-tight font-['Outfit']">
            Loved by 100,000+ Happy Homes
          </h2>
          <p className="text-xs sm:text-sm text-gray-500 mt-2">
            Read authentic reviews from verified customers who booked through Service Assist.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {reviews.slice(0, 3).map((review) => (
            <div
              key={review.id}
              className="bg-[#FFF8F2]/40 rounded-3xl p-6 border border-gray-100 flex flex-col justify-between hover:border-[#FF9A3D]/40 hover:shadow-lg transition-all"
            >
              <div>
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-1">
                    {[...Array(5)].map((_, i) => (
                      <Star
                        key={i}
                        className={`w-4 h-4 ${
                          i < review.rating
                            ? 'fill-[#FF9A3D] text-[#FF9A3D]'
                            : 'fill-gray-200 text-gray-200'
                        }`}
                      />
                    ))}
                  </div>
                  <span className="text-[11px] text-gray-400 font-medium">{review.date}</span>
                </div>

                <p className="text-xs sm:text-sm text-gray-700 leading-relaxed italic mb-6">
                  "{review.comment}"
                </p>
              </div>

              <div className="pt-4 border-t border-gray-200/60 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <img
                    src={review.customerAvatar}
                    alt={review.customerName}
                    className="w-10 h-10 rounded-full object-cover border border-orange-200"
                    referrerPolicy="no-referrer"
                  />
                  <div>
                    <h4 className="font-bold text-xs text-gray-900 font-['Outfit']">{review.customerName}</h4>
                    <p className="text-[10px] text-gray-400">{review.serviceName}</p>
                  </div>
                </div>

                <div className="flex items-center gap-1 text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-1 rounded-md">
                  <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                  <span>Verified</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
