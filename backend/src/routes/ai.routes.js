import { Router } from 'express';
import rateLimit from 'express-rate-limit';
import { z } from 'zod';
import { validate } from '../middleware/validate.js';
import { endpoint } from '../utils/errors.js';
import { db } from '../config/db.js';

const router = Router();

// Context-aware diagnosis helper for household problems
function getDiagnosticFallback(userMessage, services = []) {
  const query = (userMessage || '').toLowerCase();
  
  if (query.includes('ac') || query.includes('air conditioner') || query.includes('cool') || query.includes('filter')) {
    return {
      reply: "Based on your description, your AC likely requires routine jet servicing, filter cleaning, or refrigerant gas check. Regular maintenance improves cooling efficiency and lowers electricity bills. Would you like to book our certified AC technician?",
      action: { title: "View AC & Appliance Services", link: "/services?category=cat-appliances" }
    };
  }
  
  if (query.includes('leak') || query.includes('pipe') || query.includes('tap') || query.includes('faucet') || query.includes('drain') || query.includes('plumb')) {
    return {
      reply: "Water leakage or drainage issues should be inspected promptly to prevent water damage. Our verified plumbers carry standard replacement parts and tools for instant doorstep resolution.",
      action: { title: "View Plumbing Services", link: "/services?category=cat-plumbing" }
    };
  }

  if (query.includes('clean') || query.includes('dust') || query.includes('bhk') || query.includes('sofa') || query.includes('bathroom') || query.includes('kitchen')) {
    return {
      reply: "We offer professional deep cleaning using eco-friendly solutions, mechanized scrubbers, and high-pressure steam sanitization for homes, bathrooms, kitchens, and sofas.",
      action: { title: "View Cleaning Packages", link: "/services?category=cat-cleaning" }
    };
  }

  if (query.includes('salon') || query.includes('hair') || query.includes('facial') || query.includes('wax') || query.includes('makeup') || query.includes('spa') || query.includes('massage') || query.includes('pedicure') || query.includes('manicure')) {
    return {
      reply: "Our certified beauticians and therapists bring single-use hygiene kits and premium salon products right to your home for a relaxing grooming experience.",
      action: { title: "View Salon & Spa Services", link: "/services?category=cat-salon-women" }
    };
  }

  if (query.includes('laptop') || query.includes('computer') || query.includes('windows') || query.includes('mac') || query.includes('screen') || query.includes('keyboard') || query.includes('ram') || query.includes('ssd') || query.includes('slow')) {
    return {
      reply: "Hardware diagnosis, OS installations, SSD upgrades, and screen repairs are done doorstep or via secure lab pickup with 100% data safety guaranteed.",
      action: { title: "View Laptop & Computer Services", link: "/services?category=cat-laptop-repair" }
    };
  }

  if (query.includes('electric') || query.includes('switch') || query.includes('light') || query.includes('wire') || query.includes('fan') || query.includes('mcb') || query.includes('short')) {
    return {
      reply: "Electrical short-circuits, loose wiring, switchboard repairs, and appliance installations are safely handled by our licensed electricians.",
      action: { title: "View Electrician Services", link: "/services?category=cat-electrician" }
    };
  }

  if (query.includes('paint') || query.includes('wall') || query.includes('damp') || query.includes('waterproof')) {
    return {
      reply: "We offer laser measurement, moisture checks, and flawless wall painting with dust-free mechanical sanding and top-brand paints.",
      action: { title: "View Painting Services", link: "/services?category=cat-painting" }
    };
  }

  if (query.includes('pest') || query.includes('cockroach') || query.includes('termite') || query.includes('bedbug')) {
    return {
      reply: "Our certified pest control treatments use government-approved odorless chemicals that are completely safe for children and pets.",
      action: { title: "View Pest Control Services", link: "/services?category=cat-pest-control" }
    };
  }

  // Generic fallback referencing top service
  const matchedService = (services || []).find(s => 
    query.split(' ').some(w => w.length > 3 && (s.name?.toLowerCase().includes(w) || (s.slug && s.slug.includes(w))))
  ) || (services && services[0]);

  return {
    reply: `I recommend scheduling a doorstep inspection with our verified Service Assist professionals. We offer standard upfront pricing, background-verified technicians, and a 30-day service warranty.`,
    action: matchedService ? { title: `Book ${matchedService.name}`, link: `/services/${matchedService.slug}` } : { title: "Browse All Services", link: "/services" }
  };
}

// Smart Heuristic structured diagnosis for Landing Page AI widget
function generateStructuredDiagnosis(query) {
  const q = (query || '').toLowerCase();
  
  if (q.includes('ac') || q.includes('air conditioner') || q.includes('cool') || q.includes('gas') || (q.includes('leak') && (q.includes('water') || q.includes('indoor') || q.includes('unit')))) {
    return {
      category: 'AC & Appliance Repair',
      serviceId: 'ac-repair',
      serviceTitle: 'Split AC Deep Clean & Jet Servicing',
      issueDetected: 'Clogged condensate drain line & evaporator coil dust buildup',
      urgency: 'Medium - Book within 24 hours to prevent wall dampness',
      estimatedPrice: '₹499 - ₹799',
      estimatedDuration: '45 - 60 mins',
      recommendedPackage: 'AC Foam Jet Servicing + Gas Pressure Check',
      proSkillRequired: 'Certified HVAC & Refrigeration Technician',
      guarantee: '30-Day Service Guarantee with Re-work Warranty',
      steps: [
        'High-pressure water jet cleaning of indoor cooling coils',
        'Unclogging and sanitization of water drainage pipe',
        'Refrigerant gas pressure & current draw diagnostic',
        'Disinfection of filter mesh and blower fan'
      ]
    };
  }

  if (q.includes('leak') || q.includes('pipe') || q.includes('tap') || q.includes('flush') || q.includes('plumb') || q.includes('drain') || q.includes('choke')) {
    return {
      category: 'Electrician & Plumber',
      serviceId: 'plumber-repair',
      serviceTitle: 'Plumbing Leakage & Pipe Repair Service',
      issueDetected: 'Loose joint seal, valve cartridge wear, or waste pipe obstruction',
      urgency: 'High - Immediate repair advised to avoid floor damage',
      estimatedPrice: '₹249 - ₹499',
      estimatedDuration: '30 - 45 mins',
      recommendedPackage: 'Pipe Leakage Repair & Seal Replacement',
      proSkillRequired: 'Licensed Doorstep Plumber',
      guarantee: '30-Day Leak-Proof Guarantee',
      steps: [
        'Isolate water line and pressure test the affected fitting',
        'Replace degraded Teflon tape, washers, or connector hoses',
        'Clear minor sediment debris in traps and drain lines',
        'Verify zero seepage under operating water pressure'
      ]
    };
  }

  if (q.includes('spark') || q.includes('trip') || q.includes('mcb') || q.includes('electric') || q.includes('switch') || q.includes('wire') || q.includes('shock')) {
    return {
      category: 'Electrician & Plumber',
      serviceId: 'electrician-repair',
      serviceTitle: 'Electrical Inspection & Short Circuit Diagnostic',
      issueDetected: 'Phase overload, corroded switchboard terminal, or tripping MCB',
      urgency: 'Urgent - Safety hazard, resolve promptly',
      estimatedPrice: '₹199 - ₹449',
      estimatedDuration: '30 - 50 mins',
      recommendedPackage: 'MCB & Switchboard Safety Overhaul',
      proSkillRequired: 'Certified High-Voltage & Residential Electrician',
      guarantee: '30-Day Safety Assurance Guarantee',
      steps: [
        'Multimeter resistance & voltage line testing',
        'Tightening loose terminal lugs and replacing blown switches',
        'Load balancing check across active circuit breakers',
        'Insulation test to confirm zero ground leakage'
      ]
    };
  }

  if (q.includes('clean') || q.includes('dust') || q.includes('sofa') || q.includes('bathroom') || q.includes('kitchen') || q.includes('stain')) {
    return {
      category: 'Home Deep Cleaning',
      serviceId: 'deep-clean',
      serviceTitle: 'Intense Home Deep Cleaning & Sanitization',
      issueDetected: 'Hard water scale, grout discoloration, and deep fabric dust accumulation',
      urgency: 'Standard - Convenient weekend or same-day slot',
      estimatedPrice: '₹1,299 - ₹2,499',
      estimatedDuration: '2 - 3.5 hours',
      recommendedPackage: 'Full Bathroom & Kitchen Degreasing Deep Clean',
      proSkillRequired: 'Trained Cleaning Crew with Hospital-Grade Disinfectants',
      guarantee: '100% Inspection Satisfaction or Instant Re-clean',
      steps: [
        'Industrial machine buffing of bathroom tiles and chrome fixtures',
        'Eco-friendly kitchen chimney and tile degreasing',
        'High-suction HEPA vacuuming and sanitization',
        'Walkthrough inspection with home owner before handover'
      ]
    };
  }

  if (q.includes('cockroach') || q.includes('pest') || q.includes('termite') || q.includes('bedbug') || q.includes('ant') || q.includes('bug')) {
    return {
      category: 'Pest Control',
      serviceId: 'pest-control',
      serviceTitle: 'Advanced Herbal Pest Eradication',
      issueDetected: 'Insect harborage in wall cracks, kitchen cabinets, and drains',
      urgency: 'Medium - Book within 48 hours to stop breeding cycles',
      estimatedPrice: '₹799 - ₹1,499',
      estimatedDuration: '45 - 60 mins',
      recommendedPackage: 'Herbal Gel Baiting & Odorless Spray Treatment',
      proSkillRequired: 'Government Certified Pest Management Professional',
      guarantee: '60-Day Pest-Free Warranty with Free Booster',
      steps: [
        'Inspection of infestation hotspots behind appliances',
        'Application of child-safe, pet-friendly herbal gel dots',
        'Odorless barrier spray along skirting and drainage traps',
        'Preventative tips and moisture reduction guidance'
      ]
    };
  }

  return {
    category: 'Carpentry & Handyman',
    serviceId: 'general-repair',
    serviceTitle: 'Comprehensive Home Inspection & Handyman Diagnostic',
    issueDetected: `Assessment of: "${query.slice(0, 60)}"`,
    urgency: 'Medium - Flexible slot available within 15 mins',
    estimatedPrice: '₹299 - ₹599',
    estimatedDuration: '45 mins',
    recommendedPackage: 'Multi-Task Handyman Diagnostic Visit',
    proSkillRequired: 'Multi-Skilled Background-Verified Technician',
    guarantee: '30-Day Service Guarantee',
    steps: [
      'Comprehensive on-site physical inspection by verified technician',
      'Exact upfront quote before starting any work',
      'Execution with genuine replacement parts and standard tools',
      'Post-repair cleanup and safety verification'
    ]
  };
}

// Landing Page AI Diagnostic API endpoint: /api/diagnose
router.post(
  '/diagnose',
  rateLimit({ windowMs: 60000, limit: 30 }),
  validate(z.object({ problemDescription: z.string().trim().min(1).max(2000) })),
  endpoint(async (req, res) => {
    const { problemDescription } = req.validated;
    const apiKey = process.env.GEMINI_API_KEY || process.env.GOOGLE_API_KEY || process.env.GOOGLE_AI_KEY || '';
    const model = process.env.GEMINI_MODEL || 'gemini-1.5-flash';

    if (!apiKey || apiKey === 'MY_GEMINI_API_KEY') {
      const fallback = generateStructuredDiagnosis(problemDescription);
      return res.json({ diagnosis: fallback, source: 'smart-heuristic' });
    }

    try {
      const response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'x-goog-api-key': apiKey,
          },
          body: JSON.stringify({
            contents: [
              {
                role: 'user',
                parts: [
                  {
                    text: `You are the master technical diagnostic AI for "Service Assist", an on-demand home services platform.
Analyze this user home problem report: "${problemDescription}".
Diagnose what is broken, provide practical insights, cost estimation in INR (₹), and return STRICTLY a JSON object with this exact shape:
{
  "category": "one of: AC & Appliance Repair | Home Deep Cleaning | Electrician & Plumber | Carpentry & Handyman | Pest Control | Painting & Waterproofing | Water Purifier (RO) | Salon & Spa at Home",
  "serviceId": "short-slug",
  "serviceTitle": "Accurate service name",
  "issueDetected": "Concise 1-sentence technical diagnosis of the root cause",
  "urgency": "Urgent | High | Medium | Standard with brief reasoning",
  "estimatedPrice": "₹XXX - ₹YYY",
  "estimatedDuration": "XX mins or X hours",
  "recommendedPackage": "Exact package title recommended",
  "proSkillRequired": "Skill of professional to dispatch",
  "guarantee": "30-Day Service Guarantee",
  "steps": [
    "Step 1 pro will perform",
    "Step 2 pro will perform",
    "Step 3 pro will perform",
    "Step 4 pro will perform"
  ]
}
Return only valid JSON without markdown formatting.`,
                  },
                ],
              },
            ],
          }),
          signal: AbortSignal.timeout(10000),
        },
      );

      if (response.ok) {
        const data = await response.json();
        const reply = data.candidates?.[0]?.content?.parts?.map((p) => p.text || '').join('').trim();
        if (reply) {
          const cleanJson = reply.replace(/```json/g, '').replace(/```/g, '').trim();
          const parsed = JSON.parse(cleanJson);
          return res.json({ diagnosis: parsed, source: 'gemini' });
        }
      }
      const fallback = generateStructuredDiagnosis(problemDescription);
      return res.json({ diagnosis: fallback, source: 'smart-heuristic' });
    } catch {
      const fallback = generateStructuredDiagnosis(problemDescription);
      return res.json({ diagnosis: fallback, source: 'smart-heuristic' });
    }
  }),
);

// Partner Application Lead Endpoint: /api/partners/apply
router.post(
  '/partners/apply',
  rateLimit({ windowMs: 60000, limit: 15 }),
  validate(
    z.object({
      name: z.string().optional(),
      phone: z.string().min(10),
      city: z.string().optional(),
      skill: z.string().optional(),
      experience: z.string().optional(),
    }),
  ),
  endpoint(async (req, res) => {
    const { name, phone, city } = req.validated;
    res.json({
      success: true,
      message: `Thank you ${name || 'Partner'}! Our Pro Onboarding Team in ${city || 'your city'} will call you at +91 ${phone} within 2 hours for verification.`,
    });
  }),
);

router.post(
  '/ai/chat',
  rateLimit({ windowMs: 60000, limit: 15 }),
  validate(z.object({ message: z.string().trim().min(1).max(2000) })),
  endpoint(async (req, res) => {
    const apiKey = process.env.GEMINI_API_KEY || process.env.GOOGLE_API_KEY || process.env.GOOGLE_AI_KEY || process.env.GOOGLE_GENAI_API_KEY || '';
    const model = process.env.GEMINI_MODEL || 'gemini-1.5-flash';

    let services = [];
    try {
      services = await db.service.findMany({
        where: { isActive: true },
        select: { name: true, slug: true, startingPrice: true, categoryId: true },
        take: 100,
      });
    } catch {
      services = [];
    }

    if (!apiKey) {
      const fallback = getDiagnosticFallback(req.validated.message, services);
      return res.json(fallback);
    }

    try {
      const response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'x-goog-api-key': apiKey,
          },
          body: JSON.stringify({
            systemInstruction: {
              parts: [
                {
                  text: `You are HomeAI, Service Assist's friendly smart home advisor in India. Provide concise, professional advice (2-3 sentences max) diagnosing the user's household or appliance problem and recommending relevant catalog services. Always maintain a helpful, welcoming tone. Catalog: ${JSON.stringify(services.slice(0, 50))}`,
                },
              ],
            },
            contents: [{ role: 'user', parts: [{ text: req.validated.message }] }],
          }),
          signal: AbortSignal.timeout(10000),
        },
      );

      if (!response.ok) {
        const fallback = getDiagnosticFallback(req.validated.message, services);
        return res.json(fallback);
      }

      const data = await response.json();
      const reply = data.candidates?.[0]?.content?.parts?.map((p) => p.text || '').join('');
      if (!reply) {
        const fallback = getDiagnosticFallback(req.validated.message, services);
        return res.json(fallback);
      }

      // Check if we can attach an action link
      const fallback = getDiagnosticFallback(req.validated.message, services);
      res.json({ reply, action: fallback.action });
    } catch (err) {
      const fallback = getDiagnosticFallback(req.validated.message, services);
      res.json(fallback);
    }
  }),
);

export default router;
