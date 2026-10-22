import React, { useState } from 'react';
import { Hero } from '../components/home/Hero';
import { SmartSearchSection } from '../components/home/SmartSearchSection';
import { PopularCategories } from '../components/home/PopularCategories';
import { MostBookedServices } from '../components/home/MostBookedServices';
import { RecommendedForYou } from '../components/home/RecommendedForYou';
import { OffersSection } from '../components/home/OffersSection';
import { HowItWorks } from '../components/home/HowItWorks';
import { TrustSection } from '../components/home/TrustSection';
import { MeetProfessionals } from '../components/home/MeetProfessionals';
import { CustomerReviews } from '../components/home/CustomerReviews';
import { FAQSection } from '../components/home/FAQSection';
import { AppPromotion } from '../components/home/AppPromotion';
import { RequestExpertModal } from '../components/home/RequestExpertModal';
import { Category, Service, Professional, Review } from '../types';

interface HomePageProps {
  categories: Category[];
  services: Service[];
  professionals: Professional[];
  reviews: Review[];
  onNavigate: (path: string) => void;
  onSelectCategory: (categoryId: string) => void;
  onSelectService: (slug: string) => void;
}

export const HomePage: React.FC<HomePageProps> = ({
  categories,
  services,
  professionals,
  reviews,
  onNavigate,
  onSelectCategory,
  onSelectService,
}) => {
  const [isRequestExpertOpen, setIsRequestExpertOpen] = useState(false);
  const [selectedExpert, setSelectedExpert] = useState<Professional | null>(null);

  const handleOpenRequestExpert = (pro?: Professional | null) => {
    setSelectedExpert(pro || null);
    setIsRequestExpertOpen(true);
  };

  return (
    <div className="min-h-screen">
      <Hero
        onExplore={() => onNavigate('/services')}
        onBook={() => onNavigate('/services')}
        onRequestExpert={() => handleOpenRequestExpert(null)}
        onNavigate={onNavigate}
        onSelectService={onSelectService}
        onSelectCategory={onSelectCategory}
      />

      <SmartSearchSection
        categories={categories}
        onSearch={(query) => onNavigate(`/services?search=${encodeURIComponent(query)}`)}
        onSelectCategory={onSelectCategory}
      />

      <PopularCategories
        categories={categories}
        onSelectCategory={onSelectCategory}
        onExploreAll={() => onNavigate('/services')}
      />

      <MostBookedServices
        services={services}
        onSelectService={onSelectService}
        onViewAll={() => onNavigate('/services')}
      />

      <RecommendedForYou
        services={services}
        onSelectService={onSelectService}
      />

      <OffersSection />

      <HowItWorks />

      <TrustSection />

      <MeetProfessionals
        professionals={professionals}
        onSelectProfessional={(id) => onNavigate(`/services`)}
        onExploreServices={() => onNavigate('/services')}
        onRequestExpert={(pro) => handleOpenRequestExpert(pro)}
      />

      <CustomerReviews reviews={reviews} />

      <FAQSection />

      <AppPromotion />

      {/* Interactive Request an Expert Modal on Home (Requirement 2) */}
      <RequestExpertModal
        isOpen={isRequestExpertOpen}
        onClose={() => setIsRequestExpertOpen(false)}
        selectedProfessional={selectedExpert}
        professionals={professionals}
        services={services}
        categories={categories}
        onNavigate={onNavigate}
      />
    </div>
  );
};
