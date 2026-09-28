import React, { useState } from 'react';
import { useLocation, CITIES_LIST, CityOption } from '../../context/LocationContext';
import { MapPin, Search, Check, Navigation, X } from 'lucide-react';

export const LocationModal: React.FC = () => {
  const { selectedCity, setCity, isLocationModalOpen, closeLocationModal } = useLocation();
  const [searchTerm, setSearchTerm] = useState('');
  const [isDetecting, setIsDetecting] = useState(false);

  if (!isLocationModalOpen) return null;

  const filteredCities = CITIES_LIST.filter(
    (c) =>
      c.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.state.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleDetectLocation = () => {
    setIsDetecting(true);
    if (!navigator.geolocation) { setIsDetecting(false); alert('Location detection is unavailable. Please select your city.'); return; }
    const coordinates: Record<string, [number, number]> = { agra: [27.18, 78.01], delhi: [28.61, 77.21], noida: [28.54, 77.39], gurgaon: [28.46, 77.03], lucknow: [26.85, 80.95], jaipur: [26.91, 75.79], kanpur: [26.45, 80.33], mathura: [27.49, 77.67] };
    navigator.geolocation.getCurrentPosition(position => {
      const { latitude, longitude } = position.coords;
      const nearest = CITIES_LIST.map(city => { const [lat, lon] = coordinates[city.id]; return { city, distance: Math.hypot((latitude - lat) * 111, (longitude - lon) * 111 * Math.cos(latitude * Math.PI / 180)) }; }).sort((a, b) => a.distance - b.distance)[0];
      if (nearest.distance <= 50) setCity(nearest.city); else alert('Your location is outside our listed service areas. Please select a city.');
      setIsDetecting(false);
    }, () => { setIsDetecting(false); alert('Unable to detect location. Please select your city.'); }, { timeout: 10000, maximumAge: 300000 });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative w-full max-w-md bg-white rounded-2xl shadow-2xl border border-gray-100 overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-[var(--color-brand-light)] flex items-center justify-center text-[var(--color-brand)]">
              <MapPin className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-bold text-gray-900 text-lg font-['Outfit']">Select Your City</h3>
              <p className="text-xs text-gray-500">Service availability depends on your area</p>
            </div>
          </div>
          <button
            onClick={closeLocationModal}
            className="p-1.5 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search & GPS button */}
        <div className="p-6 space-y-4">
          <div className="relative">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search city, state or pincode..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)] transition-all"
            />
          </div>

          <button
            onClick={handleDetectLocation}
            disabled={isDetecting}
            className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl border border-brand-light bg-[var(--color-brand-soft)] hover:bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] text-sm font-semibold transition-all group cursor-pointer"
          >
            <Navigation className={`w-4 h-4 text-[var(--color-brand)] group-hover:rotate-45 transition-transform ${isDetecting ? 'animate-spin' : ''}`} />
            {isDetecting ? 'Detecting nearest service zone...' : 'Use Current Location (GPS)'}
          </button>

          {/* Popular Cities */}
          <div>
            <div className="text-xs font-semibold uppercase tracking-wider text-gray-400 mb-2">
              Available Service Hubs
            </div>
            <div className="grid grid-cols-2 gap-2 max-h-60 overflow-y-auto pr-1">
              {filteredCities.map((city: CityOption) => {
                const isSelected = selectedCity.id === city.id;
                return (
                  <button
                    key={city.id}
                    onClick={() => setCity(city)}
                    className={`flex items-center justify-between p-3 rounded-xl border text-left transition-all cursor-pointer ${
                      isSelected
                        ? 'border-[var(--color-brand)] bg-[var(--color-brand-light)] text-[var(--color-ink)] font-bold shadow-xs'
                        : 'border-gray-100 hover:border-brand-light hover:bg-[var(--color-brand-soft)] text-gray-700'
                    }`}
                  >
                    <div>
                      <div className="text-sm">{city.name}</div>
                      <div className="text-[11px] text-gray-400">{city.state}</div>
                    </div>
                    {isSelected && <Check className="w-4 h-4 text-[var(--color-brand)]" />}
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
