import { Router } from 'express';
import rateLimit from 'express-rate-limit';
import { z } from 'zod';
import { validate } from '../middleware/validate.js';
import { endpoint } from '../utils/errors.js';
import { db } from '../config/db.js';

const router = Router();

// Context-aware diagnosis helper for household problems
function getDiagnosticFallback(userMessage, services) {
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
  const matchedService = services.find(s => 
    query.split(' ').some(w => w.length > 3 && (s.name.toLowerCase().includes(w) || (s.slug && s.slug.includes(w))))
  ) || services[0];

  return {
    reply: `I recommend scheduling a doorstep inspection with our verified Service Assist professionals. We offer standard upfront pricing, background-verified technicians, and a 30-day service warranty.`,
    action: matchedService ? { title: `Book ${matchedService.name}`, link: `/services/${matchedService.slug}` } : { title: "Browse All Services", link: "/services" }
  };
}

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
