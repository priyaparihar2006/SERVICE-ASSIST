import React, { useState } from 'react';
import { initials } from './chatUtils';

interface AvatarProps {
  name: string;
  src?: string | null;
  size?: 'sm' | 'md';
  online?: boolean;
}

export const Avatar: React.FC<AvatarProps> = ({ name, src, size = 'md', online }) => {
  const [failed, setFailed] = useState(false);
  const box = size === 'sm' ? 'w-10 h-10 text-xs' : 'w-11 h-11 text-sm';
  return (
    <span className={`relative shrink-0 inline-flex ${box}`}>
      {src && !failed ? (
        <img
          src={src}
          alt=""
          className={`${box} rounded-2xl object-cover border border-[var(--color-brand-light)]`}
          referrerPolicy="no-referrer"
          onError={() => setFailed(true)}
        />
      ) : (
        <span
          aria-hidden="true"
          className={`${box} rounded-2xl bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] font-bold flex items-center justify-center font-['Outfit']`}
        >
          {initials(name)}
        </span>
      )}
      {online && (
        <span
          role="img"
          aria-label="Online"
          className="absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full bg-[var(--color-brand-bright)] ring-2 ring-white"
        />
      )}
    </span>
  );
};
