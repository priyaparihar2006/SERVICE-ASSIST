import React, { useState } from 'react';
import { Wrench } from 'lucide-react';

interface ImageWithFallbackProps extends React.ImgHTMLAttributes<HTMLImageElement> {
  fallbackTitle?: string;
}

export const ImageWithFallback: React.FC<ImageWithFallbackProps> = ({
  src,
  alt,
  className,
  fallbackTitle,
  ...props
}) => {
  const [error, setError] = useState(false);

  if (error || !src) {
    return (
      <div
        className={`flex flex-col items-center justify-center bg-gradient-to-br from-[#FFF1E5] to-[#FFF8F2] border border-[#FF9A3D]/20 text-[#FF7A00] p-4 text-center ${
          className || 'w-full h-full'
        }`}
      >
        <div className="w-10 h-10 rounded-xl bg-white shadow-xs flex items-center justify-center mb-1 text-[#FF7A00]">
          <Wrench className="w-5 h-5 stroke-[2]" />
        </div>
        <span className="text-[11px] font-bold text-[#15252B] line-clamp-1">
          {fallbackTitle || 'Service Assist'}
        </span>
        <span className="text-[9px] text-[#E85D04] font-semibold">Verified Partner</span>
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt || 'Service Assist'}
      className={className}
      onError={() => setError(true)}
      referrerPolicy="no-referrer"
      loading="lazy"
      {...props}
    />
  );
};
