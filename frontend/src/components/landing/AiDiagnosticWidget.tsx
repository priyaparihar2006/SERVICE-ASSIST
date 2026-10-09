import React, { useState } from 'react';
import { 
  Sparkles, 
  Search, 
  ArrowRight, 
  AlertCircle, 
  CheckCircle2, 
  Wrench,
  Loader2,
  ShieldCheck
} from 'lucide-react';
import { DiagnosticResult, ServiceItem, UserProfile } from '../../types/landing';
import { SERVICES } from '../../data/landing/servicesData';
import { Tilt3DCard } from './Tilt3DCard';

interface AiDiagnosticWidgetProps {
  onBookService: (service: ServiceItem) => void;
  currentUser?: UserProfile | null;
}

const COMMON_ISSUES = [
  { label: 'AC Issue', query: 'AC is not cooling properly and making a strange buzzing noise' },
  { label: 'Water Leak', query: 'Water pipe is leaking underneath the bathroom sink' },
  { label: 'Electrical Problem', query: 'Switchboard sparked and tripped the main power circuit breaker' },
  { label: 'Cleaning & Repairs', query: 'Need deep cleaning for living room sofa and door hinge repair' },
];

export const AiDiagnosticWidget: React.FC<AiDiagnosticWidgetProps> = ({ 
  onBookService,
  currentUser,
}) => {
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<DiagnosticResult | null>(null);

  const runDiagnosis = async (textToDiagnose: string) => {
    if (!textToDiagnose.trim()) return;
    setLoading(true);

    try {
      const response = await fetch('/api/diagnose', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ problemDescription: textToDiagnose }),
      });

      if (!response.ok) {
        throw new Error('Diagnosis failed.');
      }

      const data = await response.json();
      if (data.diagnosis) {
        setResult(data.diagnosis);
      } else {
        throw new Error('No diagnostic data returned.');
      }
    } catch {
      // Fallback matching query intent
      const q = textToDiagnose.toLowerCase();
      if (q.includes('leak') || q.includes('water') || q.includes('pipe') || q.includes('tap')) {
        setResult({
          category: 'Plumbing & Pipe Repair',
          probableCause: 'Corroded valve stem, worn washer seal, or excess hydrostatic line pressure.',
          recommendedPackage: 'Pipe Leakage & Tap Repair',
          urgency: 'Medium Priority (Dispatch in 15 mins)',
          actionSteps: [
            'Turn off the main quarter-turn angle stop valve under the fixture.',
            'Keep a bucket or dry cloth underneath the leaking fitting.',
            'Our certified plumber will replace washers and pressure seal the joints.',
          ],
        });
      } else if (q.includes('spark') || q.includes('switch') || q.includes('mcb') || q.includes('electric') || q.includes('power')) {
        setResult({
          category: 'Electrical & MCB Diagnostics',
          probableCause: 'Overloaded terminal connection, carbon arcing, or burnt modular switch contacts.',
          recommendedPackage: 'Switchboard & Socket Repair',
          urgency: 'High Priority (Immediate Dispatch)',
          actionSteps: [
            'Avoid touching the switchboard or operating high-wattage appliances.',
            'Keep the main MCB breaker switched off if smoking or buzzing.',
            'Our licensed electrician will test phase voltage and fit fire-retardant parts.',
          ],
        });
      } else if (q.includes('clean') || q.includes('dust') || q.includes('sofa') || q.includes('bathroom') || q.includes('home')) {
        setResult({
          category: 'Home Deep Cleaning',
          probableCause: 'Deep embedded allergens, stubborn hard water scaling, and fabric dust mites.',
          recommendedPackage: 'Home Deep Cleaning (2 BHK)',
          urgency: 'Standard Priority',
          actionSteps: [
            'Clear delicate or valuable countertop items before arrival.',
            'Our cleaning crew will sanitize surfaces with industrial single-disc scrubbers.',
            'Includes full 30-day satisfaction rework warranty.',
          ],
        });
      } else {
        setResult({
          category: 'Air Conditioning (AC) Service',
          probableCause: 'Blocked evaporator cooling coil, low refrigerant gas, or clogged drain pipe.',
          recommendedPackage: 'AC Foam Jet Deep Cleaning',
          urgency: 'Medium Priority (Dispatch in 15 mins)',
          actionSteps: [
            'Turn off the AC power isolator switch to prevent compressor strain.',
            'Check that outdoor air intake vents are clear of obstructions.',
            'Certified AC technician will clean the indoor coil with pressure jet wash.',
          ],
        });
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    runDiagnosis(query);
  };

  return (
    <section id="ai-diagnostic" className="py-12 sm:py-16 bg-gradient-to-r from-emerald-50/80 via-teal-50/40 to-slate-50 border-y border-emerald-100/60 relative overflow-hidden">
      
      {/* Subtle ambient lighting */}
      <div className="absolute top-1/2 right-1/4 -translate-y-1/2 w-[500px] h-[500px] bg-emerald-500/10 rounded-full blur-[140px] pointer-events-none" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        
        {/* Main 2-Column Showcase matching mockup */}
        <div className="grid grid-cols-1 lg:grid-cols-12 items-center gap-8 lg:gap-12">
          
          {/* Left Column: Heading, Subtitle, and Pill Input Bar */}
          <div className="lg:col-span-7 text-left space-y-4">
            
            {/* Tagline: AI HOME DIAGNOSTIC (No Box) */}
            <div className="inline-flex items-center gap-1.5 text-emerald-700 text-xs font-bold uppercase tracking-wider">
              <Sparkles className="w-3.5 h-3.5 text-emerald-600" />
              <span>AI HOME DIAGNOSTIC</span>
            </div>

            {/* Headline */}
            <h2 className="text-3xl sm:text-4xl lg:text-[42px] font-black text-slate-900 tracking-tight leading-[1.15]">
              Not Sure What&apos;s <br />
              <span className="text-emerald-600">Wrong at Home?</span>
            </h2>

            {/* Subtitle */}
            <p className="text-xs sm:text-sm text-slate-600 font-normal max-w-lg leading-relaxed">
              Tell us the issue in simple words. Our AI quickly identifies the problem and connects you with a verified expert.
            </p>

            {/* Pill Search & Diagnostic Input Bar */}
            <form onSubmit={handleSubmit} className="pt-2 max-w-xl">
              <div className="bg-white rounded-full p-1.5 border border-slate-200 shadow-lg shadow-emerald-500/5 flex items-center justify-between gap-2">
                <div className="flex items-center gap-2.5 pl-4 flex-1">
                  <Search className="w-4 h-4 text-slate-400 shrink-0" />
                  <input
                    type="text"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="e.g. AC is leaking water"
                    className="w-full bg-transparent text-slate-800 placeholder-slate-400 text-xs sm:text-sm font-medium focus:outline-none"
                  />
                </div>

                <button
                  type="submit"
                  disabled={loading || !query.trim()}
                  className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 disabled:opacity-50 text-white font-extrabold text-xs sm:text-sm rounded-full shadow-md shadow-emerald-600/25 transition-all flex items-center gap-1.5 cursor-pointer shrink-0"
                >
                  {loading ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      <span>Diagnosing...</span>
                    </>
                  ) : (
                    <>
                      <span>Diagnose Now</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </>
                  )}
                </button>
              </div>
            </form>

            {/* Quick Symptoms Chips */}
            <div className="pt-1 flex flex-wrap items-center gap-2 text-xs text-slate-500">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Try:</span>
              {COMMON_ISSUES.map((issue, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => {
                    setQuery(issue.query);
                    runDiagnosis(issue.query);
                  }}
                  className="px-3 py-1 rounded-full bg-white hover:bg-emerald-50 text-slate-700 hover:text-emerald-700 border border-slate-200 text-xs font-semibold shadow-xs transition-colors cursor-pointer"
                >
                  {issue.label}
                </button>
              ))}
            </div>

          </div>

          {/* Right Column: 3D Robot & Smartphone Video with 3D Motion */}
          <div className="lg:col-span-5 flex justify-center items-center">
            <div className="relative w-full max-w-[380px] sm:max-w-[420px] animate-float-slow">
              {/* Dynamic 3D ambient glow beneath the video */}
              <div className="absolute -inset-4 bg-gradient-to-tr from-emerald-500/25 to-teal-400/25 rounded-[36px] blur-2xl -z-10 animate-pulse-glow pointer-events-none" />

              <Tilt3DCard maxTilt={14} perspective={1200} glare={true} className="w-full">
                <div className="relative w-full rounded-3xl overflow-hidden shadow-2xl border border-white/80 bg-white/40 backdrop-blur-sm group">
                  <video
                    src="/gemini_generated_video_24731f82.mp4"
                    poster="/robot-video-poster.jpg"
                    autoPlay
                    loop
                    muted
                    playsInline
                    className="w-full h-auto object-cover rounded-3xl block shadow-inner"
                  />
                </div>
              </Tilt3DCard>
            </div>
          </div>

        </div>

        {/* Diagnostic Result Card (Appears dynamically upon running diagnosis) */}
        {result && (
          <div className="mt-10 bg-white border border-emerald-500/40 rounded-3xl p-6 sm:p-8 shadow-xl shadow-emerald-500/10 animate-in fade-in slide-in-from-bottom-3 duration-200 text-left">
            
            <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-5 border-b border-slate-100 gap-3">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center border border-emerald-100">
                  <Wrench className="w-6 h-6" />
                </div>
                <div>
                  <span className="text-xs font-bold uppercase tracking-wider text-emerald-600">
                    {result.category}
                  </span>
                  <h3 className="text-xl font-extrabold text-slate-900">
                    {result.recommendedPackage}
                  </h3>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <span className="px-3.5 py-1 rounded-full text-xs font-bold bg-amber-50 text-amber-800 border border-amber-200 flex items-center gap-1.5">
                  <AlertCircle className="w-3.5 h-3.5 text-amber-600" />
                  {result.urgency}
                </span>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 py-6 border-b border-slate-100">
              <div>
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
                  Probable Cause Identified
                </h4>
                <p className="text-sm text-slate-700 leading-relaxed font-medium">
                  {result.probableCause || result.issueDetected}
                </p>
              </div>

              <div>
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
                  Recommended Immediate Steps
                </h4>
                <ul className="space-y-1.5 text-xs text-slate-600">
                  {(result.actionSteps || result.steps || []).map((step, idx) => (
                    <li key={idx} className="flex items-start gap-2">
                      <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
                      <span>{step}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="pt-6 border-t border-slate-100 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-emerald-600 shrink-0" />
                <div>
                  <span className="text-xs font-bold text-slate-900 block">30-Day Service Guarantee</span>
                  <span className="text-[11px] text-slate-500">Includes genuine manufacturer-approved parts & post-service testing</span>
                </div>
              </div>

              <div className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-3 py-1.5 rounded-full border border-emerald-200/60 shrink-0">
                <span>Verified Diagnostic Solution</span>
              </div>
            </div>

          </div>
        )}

      </div>
    </section>
  );
};

