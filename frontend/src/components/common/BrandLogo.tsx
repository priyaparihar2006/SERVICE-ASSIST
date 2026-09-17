import React from 'react';

interface BrandLogoProps {
  className?: string;
  size?: 'sm' | 'md' | 'lg';
  variant?: 'dark' | 'light';
  showTagline?: boolean;
}

export const BrandLogo: React.FC<BrandLogoProps> = ({
  className = '',
  size = 'md',
  variant = 'dark',
  showTagline = false,
}) => {
  const iconSizes = {
    sm: 'w-7 h-7 rounded-lg',
    md: 'w-9 h-9 rounded-xl',
    lg: 'w-11 h-11 rounded-2xl',
  };

  const svgSizes = {
    sm: 16,
    md: 20,
    lg: 26,
  };

  const textSizes = {
    sm: 'text-base',
    md: 'text-lg sm:text-xl',
    lg: 'text-2xl',
  };

  const isLight = variant === 'light'; // used on dark backgrounds like footer

  return (
    <div className={`flex items-center gap-2.5 select-none ${className}`}>
      {/* Emblem: Modern green house + service tool emblem */}
      <div
        className={`${iconSizes[size]} bg-gradient-to-br from-[#0B9F6E] via-[#087F5B] to-[#103C35] flex items-center justify-center text-white shadow-md shadow-[#0B9F6E]/20 shrink-0 relative group border border-[#19C995]/30`}
      >
        <svg
          width={svgSizes[size]}
          height={svgSizes[size]}
          viewBox="0 0 24 24"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          className="transition-transform group-hover:scale-105 duration-300"
        >
          {/* Modern geometric architectural house outline */}
          <path
            d="M3 10.5L12 3L21 10.5V20C21 20.5523 20.5523 21 20 21H4C3.44772 21 3 20.5523 3 20V10.5Z"
            stroke="white"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          {/* Door / Interior arch */}
          <path
            d="M9.5 21V13.5C9.5 12.6716 10.1716 12 11 12H13C13.8284 12 14.5 12.6716 14.5 13.5V21"
            stroke="#DDF7EC"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          {/* Service Sparkle / Star tool element in the apex */}
          <circle cx="12" cy="8" r="1.5" fill="#19C995" />
        </svg>

        {/* Precision tool accent badge on bottom-right corner */}
        <span className="absolute -bottom-1 -right-1 w-4 h-4 rounded-full bg-white flex items-center justify-center shadow-xs border border-[#0B9F6E]/40">
          <svg
            width="9"
            height="9"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#087F5B"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
          </svg>
        </span>
      </div>

      {/* Typography */}
      <div className="flex flex-col leading-none">
        <span
          className={`font-black tracking-tight ${textSizes[size]} font-['Outfit'] flex items-center`}
        >
          <span className={isLight ? 'text-white' : 'text-[#103C35]'}>SERVICE</span>
          <span className="text-[#0B9F6E] ml-1.5 font-extrabold tracking-normal">ASSIST</span>
        </span>
        {showTagline && (
          <span
            className={`text-[9px] font-semibold tracking-wider uppercase mt-0.5 ${
              isLight ? 'text-[#DDF7EC]/80' : 'text-[#6B817C]'
            }`}
          >
            Premium On-Demand Home Services
          </span>
        )}
      </div>
    </div>
  );
};
