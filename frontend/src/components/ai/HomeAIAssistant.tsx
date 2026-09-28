import { apiFetch } from '../../services/api';
import React, { useState, useRef, useEffect } from 'react';
import { Bot, Send, X, Sparkles, ArrowRight } from 'lucide-react';

interface ChatMessage {
  id: string;
  sender: 'user' | 'bot';
  text: string;
  timestamp: string;
  action?: {
    title: string;
    link: string;
  };
}

interface HomeAIAssistantProps {
  onNavigate?: (path: string) => void;
}

// Local smart diagnostic helper for instant fallback
function getLocalDiagnosis(query: string): { text: string; action: { title: string; link: string } } {
  const q = query.toLowerCase();

  if (q.includes('ac') || q.includes('air conditioner') || q.includes('cool') || q.includes('filter')) {
    return {
      text: "Based on your description, your AC likely needs deep jet cleaning or refrigerant gas level check. Regular servicing restores fast cooling and reduces power consumption.",
      action: { title: "Explore AC & Appliance Services", link: "/services?category=cat-appliances" },
    };
  }

  if (q.includes('leak') || q.includes('pipe') || q.includes('tap') || q.includes('faucet') || q.includes('drain') || q.includes('plumb') || q.includes('water')) {
    return {
      text: "Plumbing leaks or drain blocks should be addressed quickly to prevent water damage. Our verified doorstep plumbers carry standard replacement fittings for instant fix.",
      action: { title: "Explore Plumbing Services", link: "/services?category=cat-plumbing" },
    };
  }

  if (q.includes('clean') || q.includes('dust') || q.includes('bhk') || q.includes('sofa') || q.includes('bathroom') || q.includes('kitchen')) {
    return {
      text: "We offer professional deep cleaning using mechanized scrubbing, steam sanitization, and eco-friendly solutions for homes, kitchens, bathrooms, and upholstery.",
      action: { title: "Explore Cleaning Packages", link: "/services?category=cat-cleaning" },
    };
  }

  if (q.includes('salon') || q.includes('hair') || q.includes('facial') || q.includes('wax') || q.includes('makeup') || q.includes('spa') || q.includes('massage') || q.includes('pedicure') || q.includes('manicure')) {
    return {
      text: "Our certified beauticians bring single-use hygiene kits and premium salon products right to your home for a safe, pampering experience.",
      action: { title: "Explore Salon & Spa Services", link: "/services?category=cat-salon-women" },
    };
  }

  if (q.includes('laptop') || q.includes('computer') || q.includes('windows') || q.includes('mac') || q.includes('screen') || q.includes('keyboard') || q.includes('ram') || q.includes('ssd') || q.includes('slow')) {
    return {
      text: "Our certified hardware and software technicians provide doorstep diagnostics, SSD/RAM upgrades, screen replacements, and OS tune-ups with full data privacy.",
      action: { title: "Explore Laptop & Computer Repair", link: "/services?category=cat-laptop-repair" },
    };
  }

  if (q.includes('electric') || q.includes('switch') || q.includes('light') || q.includes('wire') || q.includes('fan') || q.includes('mcb') || q.includes('fuse') || q.includes('shock')) {
    return {
      text: "Our licensed electricians can safely fix tripping breakers, faulty switchboards, ceiling fans, and indoor wiring with standard warranty on parts.",
      action: { title: "Explore Electrician Services", link: "/services?category=cat-electrician" },
    };
  }

  if (q.includes('paint') || q.includes('wall') || q.includes('damp') || q.includes('waterproof')) {
    return {
      text: "We provide dust-free mechanical sanding, moisture meter assessment, and professional wall painting with top Asian Paints / Berger paints.",
      action: { title: "Explore Painting Services", link: "/services?category=cat-painting" },
    };
  }

  if (q.includes('pest') || q.includes('cockroach') || q.includes('termite') || q.includes('bedbug') || q.includes('ant') || q.includes('mosquito')) {
    return {
      text: "Our eco-safe pest control treatments use government-approved odorless gels and sprays that are 100% safe for pets and children.",
      action: { title: "Explore Pest Control Services", link: "/services?category=cat-pest-control" },
    };
  }

  return {
    text: "I recommend scheduling a doorstep diagnostic inspection with our verified Service Assist professionals. We offer standard upfront pricing, verified specialists, and a 30-day rework warranty.",
    action: { title: "Browse All Services", link: "/services" },
  };
}

export const HomeAIAssistant: React.FC<HomeAIAssistantProps> = ({ onNavigate }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome-1',
      sender: 'bot',
      text: "Namaste! I'm HomeAI, your Service Assist smart home advisor. Describe any household issue or service you need, and I will diagnose it and match you with verified doorstep professionals.",
      timestamp: 'Just now',
    },
  ]);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isOpen]);

  const handleSend = async (userText?: string) => {
    const textToSend = userText || input;
    if (!textToSend.trim() || loading) return;

    const userMsg: ChatMessage = {
      id: `usr-${Date.now()}`,
      sender: 'user',
      text: textToSend,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      const res = await apiFetch('/api/ai/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: textToSend }),
      });

      if (!res.ok) {
        throw new Error('API unavailable');
      }

      const data = await res.json();
      const local = getLocalDiagnosis(textToSend);

      const botMsg: ChatMessage = {
        id: `bot-${Date.now()}`,
        sender: 'bot',
        text: data.reply?.replace(/\[ACTION:.*?\]/g, '').trim() || local.text,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        action: data.action || local.action,
      };

      setMessages((prev) => [...prev, botMsg]);
    } catch {
      // Graceful local diagnosis fallback
      const local = getLocalDiagnosis(textToSend);
      setMessages((prev) => [
        ...prev,
        {
          id: `bot-${Date.now()}`,
          sender: 'bot',
          text: local.text,
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          action: local.action,
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const quickPrompts = [
    'My AC is cooling slowly',
    'Leaking pipe in bathroom',
    'Deep clean 2BHK flat',
    'Need salon at home',
  ];

  return (
    <>
      {/* Floating launcher trigger */}
      <div className="fixed bottom-6 right-6 z-40">
        {!isOpen && (
          <button
            onClick={() => setIsOpen(true)}
            className="group flex items-center gap-2.5 px-4 py-3 bg-gradient-to-r from-[var(--color-brand)] to-[var(--color-brand-hover)] hover:from-[var(--color-brand-hover)] hover:to-[var(--color-brand-dark)] text-white rounded-full shadow-xl shadow-[var(--color-brand)]/30 hover:scale-105 active:scale-95 transition-all cursor-pointer border border-[var(--color-brand-bright)]/40"
          >
            <div className="relative">
              <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center">
                <Bot className="w-5 h-5 text-white" />
              </div>
              <span className="absolute -top-0.5 -right-0.5 w-3 h-3 bg-[var(--color-brand-bright)] rounded-full border-2 border-[var(--color-brand-dark)] animate-pulse" />
            </div>
            <div className="text-left hidden sm:block">
              <span className="text-xs font-black block leading-tight font-['Outfit']">Ask HomeAI</span>
              <span className="text-[10px] text-[var(--color-brand-light)] block">Instant Diagnosis & Booking</span>
            </div>
          </button>
        )}
      </div>

      {/* Assistant Modal / Drawer */}
      {isOpen && (
        <div className="fixed bottom-6 right-4 sm:right-6 z-50 w-[calc(100vw-2rem)] sm:w-[420px] h-[580px] bg-white rounded-3xl shadow-2xl border border-[var(--color-brand-light)] flex flex-col overflow-hidden animate-in slide-in-from-bottom-5 duration-200">
          {/* Header */}
          <div className="p-4 bg-gradient-to-r from-[var(--color-brand-dark)] via-[var(--color-brand-hover)] to-[var(--color-brand)] text-white flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-2xl bg-white/20 backdrop-blur-sm flex items-center justify-center text-white border border-[var(--color-brand-bright)]/30">
                <Sparkles className="w-5 h-5 text-[var(--color-brand-bright)]" />
              </div>
              <div>
                <h4 className="font-bold text-sm leading-tight flex items-center gap-1.5 font-['Outfit']">
                  HomeAI Advisor
                  <span className="text-[10px] bg-[var(--color-brand-light)]/20 text-[var(--color-brand-light)] px-2 py-0.5 rounded-full font-medium border border-white/20">
                    Service Assist AI
                  </span>
                </h4>
                <p className="text-[11px] text-[var(--color-brand-light)]/80">Troubleshoot household issues & get instant booking</p>
              </div>
            </div>
            <button
              onClick={() => setIsOpen(false)}
              className="p-1.5 text-white/80 hover:text-white rounded-full hover:bg-white/10 cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Messages Body */}
          <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-[var(--color-brand-soft)]/40 text-xs">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex flex-col ${msg.sender === 'user' ? 'items-end' : 'items-start'}`}
              >
                <div
                  className={`max-w-[85%] rounded-2xl p-3 shadow-2xs leading-relaxed ${
                    msg.sender === 'user'
                      ? 'bg-[var(--color-brand-hover)] text-white rounded-tr-none'
                      : 'bg-white text-[var(--color-ink)] border border-[var(--color-brand-light)] rounded-tl-none'
                  }`}
                >
                  <p>{msg.text}</p>
                  <span
                    className={`text-[9px] mt-1 block ${
                      msg.sender === 'user' ? 'text-[var(--color-brand-light)]/80 text-right' : 'text-[var(--color-muted)]'
                    }`}
                  >
                    {msg.timestamp}
                  </span>
                </div>

                {msg.action && (
                  <div className="mt-2 ml-1">
                    <button
                      onClick={() => {
                        if (onNavigate) onNavigate(msg.action!.link);
                        setIsOpen(false);
                      }}
                      className="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white rounded-xl text-xs font-bold shadow-xs cursor-pointer transition-transform hover:scale-105"
                    >
                      <span>{msg.action.title}</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}
              </div>
            ))}

            {loading && (
              <div className="flex items-center gap-2 text-xs text-[var(--color-muted)] p-2 bg-white rounded-2xl w-fit border border-[var(--color-brand-light)]">
                <div className="w-2 h-2 rounded-full bg-[var(--color-brand)] animate-ping" />
                <span>HomeAI is analyzing your query...</span>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Quick Prompts */}
          <div className="px-4 py-2 bg-white border-t border-[var(--color-brand-light)] flex items-center gap-1.5 overflow-x-auto text-[11px] no-scrollbar">
            {quickPrompts.map((prompt) => (
              <button
                key={prompt}
                onClick={() => handleSend(prompt)}
                className="whitespace-nowrap px-2.5 py-1 rounded-lg bg-[var(--color-brand-soft)] hover:bg-[var(--color-brand-light)] hover:text-[var(--color-brand-hover)] text-[var(--color-ink)] transition-colors cursor-pointer font-medium border border-[var(--color-brand-light)]"
              >
                {prompt}
              </button>
            ))}
          </div>

          {/* Input Area */}
          <div className="p-3 bg-white border-t border-[var(--color-brand-light)]">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSend();
              }}
              className="flex items-center gap-2"
            >
              <input
                type="text"
                placeholder="Ask e.g. 'My fan is vibrating and making noise'..."
                value={input}
                onChange={(e) => setInput(e.target.value)}
                className="flex-1 px-3.5 py-2.5 bg-[var(--color-brand-soft)] border border-[var(--color-brand-light)] rounded-xl text-xs text-[var(--color-ink)] placeholder-[var(--color-muted)] focus:outline-none focus:ring-2 focus:ring-[var(--color-brand)]/30 focus:border-[var(--color-brand)]"
              />
              <button
                type="submit"
                disabled={!input.trim() || loading}
                className="p-2.5 bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] text-white rounded-xl disabled:opacity-50 transition-all shadow-xs cursor-pointer"
              >
                <Send className="w-4 h-4" />
              </button>
            </form>
          </div>
        </div>
      )}
    </>
  );
};
