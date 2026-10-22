import React, { useState } from 'react';
import { Clock as ClockIcon, Sun, Moon, Check } from 'lucide-react';

interface ClockTimePickerProps {
  selectedSlot: string;
  onSelectSlot: (slot: string) => void;
  availableSlots: string[];
}

export const ClockTimePicker: React.FC<ClockTimePickerProps> = ({
  selectedSlot,
  onSelectSlot,
  availableSlots,
}) => {
  // Parse hour and meridiem from selected slot, e.g. "02:00 PM - 03:00 PM"
  const parseSlotHour = (slot: string) => {
    const match = slot.match(/^(\d{1,2}):00\s*(AM|PM)/i);
    if (match) {
      return {
        hour: parseInt(match[1], 10),
        period: match[2].toUpperCase() as 'AM' | 'PM',
      };
    }
    return { hour: 2, period: 'PM' as const };
  };

  const currentParsed = parseSlotHour(selectedSlot);
  const [activePeriod, setActivePeriod] = useState<'AM' | 'PM'>(currentParsed.period);

  const hours = [12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];

  // Map hour and period to a slot string
  const formatSlot = (h: number, period: 'AM' | 'PM') => {
    const startHourStr = h < 10 ? `0${h}` : `${h}`;
    let endH = h === 12 ? 1 : h + 1;
    let endPeriod = period;
    if (h === 11 && period === 'AM') endPeriod = 'PM';
    if (h === 11 && period === 'PM') endPeriod = 'AM';
    const endHourStr = endH < 10 ? `0${endH}` : `${endH}`;
    return `${startHourStr}:00 ${period} - ${endHourStr}:00 ${endPeriod}`;
  };

  const handleHourClick = (h: number) => {
    const targetSlot = formatSlot(h, activePeriod);
    // Find matching slot in availableSlots or fallback
    const matched = availableSlots.find((s) => s.startsWith(targetSlot.split(' - ')[0])) || targetSlot;
    onSelectSlot(matched);
  };

  const handlePeriodToggle = (period: 'AM' | 'PM') => {
    setActivePeriod(period);
    const targetSlot = formatSlot(currentParsed.hour, period);
    const matched = availableSlots.find((s) => s.startsWith(targetSlot.split(' - ')[0])) || targetSlot;
    onSelectSlot(matched);
  };

  // Calculate clock hand angle in degrees: 12 is 0 deg, each hour is 30 deg
  const handAngle = (currentParsed.hour % 12) * 30;

  return (
    <div className="bg-gray-50/80 rounded-2xl p-4 border border-gray-200/80 space-y-4">
      {/* Header Bar */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center">
            <ClockIcon className="w-4 h-4" />
          </div>
          <div>
            <span className="text-xs font-bold text-gray-900 block font-['Outfit']">Interactive Clock Picker</span>
            <span className="text-[11px] text-gray-500">Tap hour on clock or choose arrival window</span>
          </div>
        </div>

        {/* AM / PM Toggle */}
        <div className="flex items-center bg-white p-1 rounded-xl border border-gray-200 shadow-2xs">
          <button
            type="button"
            onClick={() => handlePeriodToggle('AM')}
            className={`px-2.5 py-1 rounded-lg text-xs font-extrabold flex items-center gap-1 transition-all cursor-pointer ${
              activePeriod === 'AM'
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'text-gray-500 hover:text-gray-800'
            }`}
          >
            <Sun className="w-3 h-3" />
            <span>AM</span>
          </button>
          <button
            type="button"
            onClick={() => handlePeriodToggle('PM')}
            className={`px-2.5 py-1 rounded-lg text-xs font-extrabold flex items-center gap-1 transition-all cursor-pointer ${
              activePeriod === 'PM'
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'text-gray-500 hover:text-gray-800'
            }`}
          >
            <Moon className="w-3 h-3" />
            <span>PM</span>
          </button>
        </div>
      </div>

      {/* Main Clock Face & Selected Display */}
      <div className="flex flex-col sm:flex-row items-center justify-center gap-6 py-2">
        {/* Analog Clock Dial */}
        <div className="relative w-48 h-48 rounded-full bg-white border-4 border-gray-200 shadow-inner flex items-center justify-center select-none">
          {/* Subtle center hub */}
          <div className="w-4 h-4 rounded-full bg-emerald-600 z-20 shadow-xs border-2 border-white" />

          {/* Clock Hand pointing to selected hour */}
          <div
            className="absolute z-10 bottom-1/2 left-1/2 w-1.5 h-18 bg-emerald-600 rounded-full origin-bottom transition-transform duration-300 ease-out shadow-xs"
            style={{
              transform: `translateX(-50%) rotate(${handAngle}deg)`,
            }}
          >
            {/* Hand Pointer Tip */}
            <div className="w-3 h-3 rounded-full bg-emerald-700 absolute -top-1 -left-0.75 ring-2 ring-emerald-200" />
          </div>

          {/* 12 Hour Number Buttons arranged circularly */}
          {hours.map((h, i) => {
            const angle = (i * 30 - 90) * (Math.PI / 180);
            const radius = 70; // px distance from center
            const x = Math.round(radius * Math.cos(angle));
            const y = Math.round(radius * Math.sin(angle));
            const isSelected = currentParsed.hour === h && currentParsed.period === activePeriod;

            return (
              <button
                key={h}
                type="button"
                onClick={() => handleHourClick(h)}
                className={`absolute w-7 h-7 rounded-full text-xs font-bold flex items-center justify-center transition-all cursor-pointer z-15 ${
                  isSelected
                    ? 'bg-emerald-600 text-white scale-110 shadow-md ring-2 ring-emerald-200'
                    : 'text-gray-700 hover:bg-emerald-50 hover:text-emerald-700 hover:scale-105'
                }`}
                style={{
                  transform: `translate(${x}px, ${y}px)`,
                }}
              >
                {h}
              </button>
            );
          })}
        </div>

        {/* Selected Window Summary Card */}
        <div className="flex-1 w-full bg-white rounded-2xl p-4 border border-emerald-100 shadow-2xs space-y-2 text-center sm:text-left">
          <span className="text-[10px] font-black uppercase tracking-wider text-emerald-800 block">
            Selected Time Slot
          </span>
          <div className="text-lg font-black text-gray-900 font-['Outfit'] flex items-center justify-center sm:justify-start gap-2">
            <span>{selectedSlot}</span>
            <Check className="w-4 h-4 text-emerald-600 stroke-[3]" />
          </div>
          <p className="text-[11px] text-gray-500 leading-relaxed">
            Professional rider will reach your doorstep within this window. You will receive real-time ETA updates.
          </p>
          <div className="pt-2 flex items-center justify-center sm:justify-start gap-1.5 text-xs text-emerald-700 font-semibold">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            <span>Guaranteed on-time arrival</span>
          </div>
        </div>
      </div>

      {/* Quick Slot Selection Buttons beneath Clock */}
      <div className="pt-1">
        <label className="block text-[11px] font-bold text-gray-500 uppercase tracking-wider mb-2">
          Or Quick Select Preset 1-Hour Windows:
        </label>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
          {availableSlots.map((slot) => {
            const isSelected = selectedSlot === slot;
            return (
              <button
                key={slot}
                type="button"
                onClick={() => {
                  onSelectSlot(slot);
                  const parsed = parseSlotHour(slot);
                  setActivePeriod(parsed.period);
                }}
                className={`p-2 rounded-xl border text-xs font-semibold text-center transition-all cursor-pointer ${
                  isSelected
                    ? 'border-emerald-600 bg-emerald-50 text-emerald-900 font-bold shadow-xs ring-1 ring-emerald-500'
                    : 'border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                {slot}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};
