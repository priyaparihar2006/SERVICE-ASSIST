import React, { forwardRef, useEffect, useImperativeHandle, useRef } from 'react';
import { Lock, Send } from 'lucide-react';

export const MAX_MESSAGE_LENGTH = 2000;

interface MessageComposerProps {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  onTyping: () => void;
  disabled: boolean;
  disabledText: string;
}

export interface ComposerHandle {
  focus: () => void;
}

export const MessageComposer = forwardRef<ComposerHandle, MessageComposerProps>(
  ({ value, onChange, onSubmit, onTyping, disabled, disabledText }, ref) => {
    const area = useRef<HTMLTextAreaElement>(null);
    useImperativeHandle(ref, () => ({ focus: () => area.current?.focus() }));

    useEffect(() => {
      const el = area.current;
      if (!el) return;
      el.style.height = 'auto';
      el.style.height = `${Math.min(el.scrollHeight, 128)}px`;
    }, [value]);

    if (disabled) {
      return (
        <div
          className="px-4 py-3 border-t border-[var(--color-line)] bg-[var(--color-brand-soft)] flex items-start gap-2 text-xs text-gray-600"
          role="status"
        >
          <Lock className="w-4 h-4 mt-px text-gray-400 shrink-0" />
          <span>{disabledText}</span>
        </div>
      );
    }

    const canSend = value.trim().length > 0;
    const remaining = MAX_MESSAGE_LENGTH - value.length;
    return (
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (canSend) onSubmit();
        }}
        className="px-3 sm:px-4 py-3 border-t border-[var(--color-line)] bg-white"
      >
        <div className="flex items-end gap-2">
          <textarea
            ref={area}
            value={value}
            rows={1}
            maxLength={MAX_MESSAGE_LENGTH}
            aria-label="Message"
            placeholder="Write a message…"
            enterKeyHint="send"
            onChange={(e) => {
              onChange(e.target.value);
              if (e.target.value) onTyping();
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
                e.preventDefault();
                if (canSend) onSubmit();
              }
            }}
            className="flex-1 resize-none px-4 py-2.5 bg-[var(--color-brand-soft)] border border-[var(--color-line)] rounded-2xl text-base sm:text-sm text-[var(--color-ink)] placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)] max-h-32"
          />
          <button
            type="submit"
            disabled={!canSend}
            aria-label="Send message"
            className="h-11 w-11 shrink-0 rounded-2xl bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] disabled:bg-gray-200 disabled:text-gray-400 text-white flex items-center justify-center transition-colors cursor-pointer disabled:cursor-not-allowed"
          >
            <Send className="w-5 h-5" />
          </button>
        </div>
        <div className="mt-1.5 flex items-center justify-between text-[11px] text-gray-400">
          <span>Keep contact details private — chat here instead.</span>
          {remaining <= 200 && <span className={remaining < 0 ? 'text-red-500' : ''}>{remaining} left</span>}
        </div>
      </form>
    );
  },
);
MessageComposer.displayName = 'MessageComposer';
