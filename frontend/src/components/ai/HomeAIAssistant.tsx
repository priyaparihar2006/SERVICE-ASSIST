import React, { useState, useRef, useEffect } from 'react';
import { Bot, Send, X, Sparkles, ArrowRight, ShieldCheck, CheckCircle2 } from 'lucide-react';

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
      const res = await fetch('/api/ai/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: textToSend }),
      });
      const data = await res.json();

      const botMsg: ChatMessage = {
        id: `bot-${Date.now()}`,
        sender: 'bot',
        text: data.reply?.replace(/\[ACTION:.*?\]/g, '').trim() || "Here's what I recommend for your home.",
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        action: data.action,
      };

      setMessages((prev) => [...prev, botMsg]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          id: `bot-${Date.now()}`,
          sender: 'bot',
          text: "I can assist with AC jet cleaning, plumbing repairs, salon packages, and electrical safety inspections. Which service would you like to book?",
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          action: { title: 'View All Services', link: '/services' },
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
            className="group flex items-center gap-2.5 px-4 py-3 bg-gradient-to-r from-[#0B9F6E] to-[#087F5B] hover:from-[#087F5B] hover:to-[#103C35] text-white rounded-full shadow-xl shadow-[#0B9F6E]/30 hover:scale-105 active:scale-95 transition-all cursor-pointer border border-[#19C995]/40"
          >
            <div className="relative">
              <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center">
                <Bot className="w-5 h-5 text-white" />
              </div>
              <span className="absolute -top-0.5 -right-0.5 w-3 h-3 bg-[#19C995] rounded-full border-2 border-[#103C35] animate-pulse" />
            </div>
            <div className="text-left hidden sm:block">
              <span className="text-xs font-black block leading-tight font-['Outfit']">Ask HomeAI</span>
              <span className="text-[10px] text-[#DDF7EC] block">Instant Diagnosis & Booking</span>
            </div>
          </button>
        )}
      </div>

      {/* Assistant Modal / Drawer */}
      {isOpen && (
        <div className="fixed bottom-6 right-4 sm:right-6 z-50 w-[calc(100vw-2rem)] sm:w-[420px] h-[580px] bg-white rounded-3xl shadow-2xl border border-[#DDF7EC] flex flex-col overflow-hidden animate-in slide-in-from-bottom-5 duration-200">
          {/* Header */}
          <div className="p-4 bg-gradient-to-r from-[#103C35] via-[#087F5B] to-[#0B9F6E] text-white flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-2xl bg-white/20 backdrop-blur-sm flex items-center justify-center text-white border border-[#19C995]/30">
                <Sparkles className="w-5 h-5 text-[#19C995]" />
              </div>
              <div>
                <h4 className="font-bold text-sm leading-tight flex items-center gap-1.5 font-['Outfit']">
                  HomeAI Advisor
                  <span className="text-[10px] bg-[#DDF7EC]/20 text-[#DDF7EC] px-2 py-0.5 rounded-full font-medium border border-white/20">
                    Service Assist AI
                  </span>
                </h4>
                <p className="text-[11px] text-[#DDF7EC]/80">Troubleshoot household issues & get instant booking</p>
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
          <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-[#F2FCF7]/40 text-xs">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex flex-col ${msg.sender === 'user' ? 'items-end' : 'items-start'}`}
              >
                <div
                  className={`max-w-[85%] rounded-2xl p-3 shadow-2xs leading-relaxed ${
                    msg.sender === 'user'
                      ? 'bg-[#087F5B] text-white rounded-tr-none'
                      : 'bg-white text-[#142D2A] border border-[#DDF7EC] rounded-tl-none'
                  }`}
                >
                  <p>{msg.text}</p>
                  <span
                    className={`text-[9px] mt-1 block ${
                      msg.sender === 'user' ? 'text-[#DDF7EC]/80 text-right' : 'text-[#6B817C]'
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
                      className="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-[#0B9F6E] hover:bg-[#087F5B] text-white rounded-xl text-xs font-bold shadow-xs cursor-pointer"
                    >
                      <span>{msg.action.title}</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}
              </div>
            ))}

            {loading && (
              <div className="flex items-center gap-2 text-xs text-[#6B817C] p-2 bg-white rounded-2xl w-fit border border-[#DDF7EC]">
                <div className="w-2 h-2 rounded-full bg-[#0B9F6E] animate-ping" />
                <span>HomeAI is analyzing your query...</span>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Quick Prompts */}
          <div className="px-4 py-2 bg-white border-t border-[#DDF7EC] flex items-center gap-1.5 overflow-x-auto text-[11px] no-scrollbar">
            {quickPrompts.map((prompt) => (
              <button
                key={prompt}
                onClick={() => handleSend(prompt)}
                className="whitespace-nowrap px-2.5 py-1 rounded-lg bg-[#F2FCF7] hover:bg-[#DDF7EC] hover:text-[#087F5B] text-[#142D2A] transition-colors cursor-pointer font-medium border border-[#DDF7EC]"
              >
                {prompt}
              </button>
            ))}
          </div>

          {/* Input Area */}
          <div className="p-3 bg-white border-t border-[#DDF7EC]">
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
                className="flex-1 px-3.5 py-2.5 bg-[#F2FCF7] border border-[#DDF7EC] rounded-xl text-xs text-[#142D2A] placeholder-[#6B817C] focus:outline-none focus:ring-2 focus:ring-[#0B9F6E]/30 focus:border-[#0B9F6E]"
              />
              <button
                type="submit"
                disabled={!input.trim() || loading}
                className="p-2.5 bg-[#0B9F6E] hover:bg-[#087F5B] text-white rounded-xl disabled:opacity-50 transition-all shadow-xs cursor-pointer"
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
