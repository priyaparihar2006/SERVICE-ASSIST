import React, { createContext, useContext, useState, useEffect } from 'react';

export interface CityOption {
  id: string;
  name: string;
  state: string;
  popular?: boolean;
}

export const CITIES_LIST: CityOption[] = [
  { id: 'agra', name: 'Agra', state: 'Uttar Pradesh', popular: true },
  { id: 'delhi', name: 'Delhi NCR', state: 'Delhi', popular: true },
  { id: 'noida', name: 'Noida', state: 'Uttar Pradesh', popular: true },
  { id: 'gurgaon', name: 'Gurgaon', state: 'Haryana', popular: true },
  { id: 'lucknow', name: 'Lucknow', state: 'Uttar Pradesh', popular: true },
  { id: 'jaipur', name: 'Jaipur', state: 'Rajasthan', popular: true },
  { id: 'kanpur', name: 'Kanpur', state: 'Uttar Pradesh' },
  { id: 'mathura', name: 'Mathura', state: 'Uttar Pradesh' },
];

interface LocationContextType {
  selectedCity: CityOption;
  setCity: (city: CityOption) => void;
  isLocationModalOpen: boolean;
  openLocationModal: () => void;
  closeLocationModal: () => void;
}

const LocationContext = createContext<LocationContextType | undefined>(undefined);

export const LocationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [selectedCity, setSelectedCity] = useState<CityOption>(() => {
    const saved = localStorage.getItem('service_assist_city');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        // fallback
      }
    }
    return CITIES_LIST[0]; // Agra by default as per request
  });

  const [isLocationModalOpen, setIsLocationModalOpen] = useState(false);

  const setCity = (city: CityOption) => {
    setSelectedCity(city);
    localStorage.setItem('service_assist_city', JSON.stringify(city));
    setIsLocationModalOpen(false);
  };

  return (
    <LocationContext.Provider
      value={{
        selectedCity,
        setCity,
        isLocationModalOpen,
        openLocationModal: () => setIsLocationModalOpen(true),
        closeLocationModal: () => setIsLocationModalOpen(false),
      }}
    >
      {children}
    </LocationContext.Provider>
  );
};

export const useLocation = () => {
  const context = useContext(LocationContext);
  if (!context) {
    throw new Error('useLocation must be used within LocationProvider');
  }
  return context;
};
