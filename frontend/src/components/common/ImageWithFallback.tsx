import React, { useEffect, useState } from 'react';
import { Wrench } from 'lucide-react';

interface ImageWithFallbackProps extends React.ImgHTMLAttributes<HTMLImageElement> {
  fallbackTitle?: string;
  fallbackSrc?: string;
}

export const ImageWithFallback: React.FC<ImageWithFallbackProps> = ({
  src,
  alt,
  className,
  fallbackTitle,
  fallbackSrc,
  onError,
  onLoad,
  ...props
}) => {
  const [failedSource, setFailedSource] = useState<string | undefined>();
  const [failedFallback, setFailedFallback] = useState<string | undefined>();
  useEffect(() => { setFailedSource(undefined); setFailedFallback(undefined); }, [src, fallbackSrc]);
  const useFallback = !src || failedSource === src;
  const currentSrc = useFallback && fallbackSrc !== src && failedFallback !== fallbackSrc ? fallbackSrc : src;
  const showTile = !currentSrc || (useFallback && (!fallbackSrc || failedFallback === fallbackSrc || fallbackSrc === src));

  if (showTile) {
    return (
      <div
        className={`flex flex-col items-center justify-center bg-gradient-to-br from-[var(--color-brand-light)] to-[var(--color-brand-soft)] border border-[var(--color-brand-bright)]/20 text-[var(--color-brand)] p-4 text-center ${
          className || 'w-full h-full'
        }`}
      >
        <div className="w-10 h-10 rounded-xl bg-white shadow-xs flex items-center justify-center mb-1 text-[var(--color-brand)]">
          <Wrench className="w-5 h-5 stroke-[2]" />
        </div>
        <span className="text-[11px] font-bold text-[var(--color-ink)] line-clamp-1">
          {fallbackTitle || 'Service Assist'}
        </span>
        <span className="text-[9px] text-[var(--color-brand-hover)] font-semibold">Verified Partner</span>
      </div>
    );
  }

  return (
    <img
      src={currentSrc}
      alt={alt || 'Service Assist'}
      className={className}
      onError={(event) => {
        if (typeof window !== 'undefined' && ['localhost', '127.0.0.1'].includes(window.location.hostname))
          console.warn('Service image failed to load:', currentSrc, alt);
        if (currentSrc === fallbackSrc) setFailedFallback(fallbackSrc);
        else setFailedSource(src);
        onError?.(event);
      }}
      onLoad={(event) => { onLoad?.(event); }}
      referrerPolicy="no-referrer"
      loading="lazy"
      {...props}
    />
  );
};
