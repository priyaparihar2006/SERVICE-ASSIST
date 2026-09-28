import React from 'react';

interface BrandLogoProps {
  className?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  variant?: 'dark' | 'light';
  showTagline?: boolean;
}

export const BrandLogo: React.FC<BrandLogoProps> = ({
  className = '',
  size = 'md',
  variant = 'dark',
  showTagline = false,
}) => {
  const logoSizes = {
    sm: 'w-7 h-7',
    md: 'w-9 h-9',
    lg: 'w-11 h-11',
    xl: 'w-14 h-14',
  };

  const textSizes = {
    sm: 'text-base',
    md: 'text-base min-[390px]:text-lg sm:text-xl',
    lg: 'text-2xl',
    xl: 'text-3xl',
  };

  const isLight = variant === 'light'; // used on dark backgrounds like footer

  return (
    <div className={`flex items-center gap-2.5 select-none ${className}`}>
      {/* Official SA Logo */}
      <div
        className={`${logoSizes[size]} rounded-2xl bg-white p-0.5 shadow-md shadow-[var(--color-brand)]/20 shrink-0 relative group border border-[var(--color-brand-light)] overflow-hidden flex items-center justify-center`}
      >
        <img
          src="/sa-logo.png"
          alt="Service Assist SA Logo"
          className="w-full h-full object-contain transition-transform group-hover:scale-105 duration-300"
        />
      </div>

      {/* Typography */}
      <div className="flex flex-col leading-none">
        <span
          className={`font-black tracking-tight ${textSizes[size]} font-['Outfit'] flex items-center`}
        >
          <span className={isLight ? 'text-white' : 'text-[var(--color-brand-dark)]'}>SERVICE</span>
          <span className="text-[var(--color-brand)] ml-1.5 font-extrabold tracking-normal">ASSIST</span>
        </span>
        {showTagline && (
          <span
            className={`text-[9px] font-semibold tracking-wider uppercase mt-0.5 ${
              isLight ? 'text-[var(--color-brand-light)]/80' : 'text-[var(--color-muted)]'
            }`}
          >
            Doorstep Home Care
          </span>
        )}
      </div>
    </div>
  );
};
