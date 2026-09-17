import express from 'express';
import path from 'path';
import dotenv from 'dotenv';
import { GoogleGenAI } from '@google/genai';
import { db } from './server/db';
import { createServer as createViteServer } from 'vite';

dotenv.config();

const app = express();
const PORT = 3000;

app.use(express.json());

// Initialize Google GenAI client lazily or when key exists
let genAI: GoogleGenAI | null = null;
function getGenAI() {
  if (!genAI && process.env.GEMINI_API_KEY) {
    genAI = new GoogleGenAI({
      apiKey: process.env.GEMINI_API_KEY,
      httpOptions: {
        headers: {
          'User-Agent': 'aistudio-build',
        },
      },
    });
  }
  return genAI;
}

// ==========================================
// 0. HEALTH CHECK
// ==========================================
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', service: 'Service Assist API', time: new Date().toISOString() });
});

// ==========================================
// 1. AUTHENTICATION APIS
// ==========================================
app.post('/api/auth/login', (req, res) => {
  const { email, role } = req.body;
  if (!email) {
    return res.status(400).json({ error: 'Email is required' });
  }

  let user = db.getUserByEmail(email);
  if (!user) {
    // For demo convenience, automatically create account with specified role
    user = db.createUser({
      email,
      name: email.split('@')[0].replace(/[._]/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()),
      role: role || 'CUSTOMER',
    });
  }

  res.json({
    user,
    token: `demo-jwt-token-${user.id}-${Date.now()}`,
  });
});

app.post('/api/auth/register', (req, res) => {
  const { name, email, phone, role } = req.body;
  if (!name || !email) {
    return res.status(400).json({ error: 'Name and email are required' });
  }

  const existing = db.getUserByEmail(email);
  if (existing) {
    return res.status(400).json({ error: 'User with this email already exists' });
  }

  const user = db.createUser({ name, email, phone, role: role || 'CUSTOMER' });
  res.status(201).json({
    user,
    token: `demo-jwt-token-${user.id}-${Date.now()}`,
  });
});

app.get('/api/auth/me', (req, res) => {
  const authHeader = req.headers.authorization;
  // Default to customer demo user
  const user = db.users[0];
  res.json({ user });
});

app.get('/api/auth/users', (req, res) => {
  res.json({ users: db.users });
});

app.post('/api/auth/addresses', (req, res) => {
  const { userId, address } = req.body;
  if (!userId || !address) {
    return res.status(400).json({ error: 'User ID and address are required' });
  }
  const newAddr = db.addAddress(userId, address);
  res.status(201).json({ address: newAddr });
});

// ==========================================
// 2. CATEGORIES & SERVICES APIS
// ==========================================
app.get('/api/categories', (req, res) => {
  res.json({ categories: db.getCategories() });
});

app.get('/api/services', (req, res) => {
  const { categoryId, search, city } = req.query;
  const services = db.getServices(categoryId as string, search as string);
  res.json({ services });
});

app.get('/api/services/:slug', (req, res) => {
  const service = db.getServiceBySlug(req.params.slug) || db.getServiceById(req.params.slug);
  if (!service) {
    return res.status(404).json({ error: 'Service not found' });
  }
  res.json({ service });
});

// Smart search API with intent matching
app.get('/api/search', (req, res) => {
  const query = (req.query.q as string || '').toLowerCase().trim();
  if (!query) {
    return res.json({ results: [], suggestions: [] });
  }

  // Synonym & natural language query mapping
  const synonyms: Record<string, string> = {
    'ac not cooling': 'ac',
    'water leak': 'plumbing',
    'fan stopped': 'electrician',
    'hair cut': 'salon',
    'cockroach': 'pest',
    'dirty sofa': 'sofa',
    'bathroom stain': 'bathroom',
  };

  let effectiveQuery = query;
  for (const [trigger, mapped] of Object.entries(synonyms)) {
    if (query.includes(trigger)) {
      effectiveQuery = mapped;
      break;
    }
  }

  const results = db.services.filter(
    (s) =>
      s.name.toLowerCase().includes(effectiveQuery) ||
      s.shortDesc.toLowerCase().includes(effectiveQuery) ||
      s.categoryName.toLowerCase().includes(effectiveQuery) ||
      s.description.toLowerCase().includes(effectiveQuery) ||
      s.whatIncluded.some((w) => w.toLowerCase().includes(effectiveQuery))
  );

  res.json({
    results,
    count: results.length,
    matchedQuery: effectiveQuery,
  });
});

// ==========================================
// 3. PROFESSIONALS APIS
// ==========================================
app.get('/api/professionals', (req, res) => {
  const { categoryId } = req.query;
  const professionals = db.getProfessionals(categoryId as string);
  res.json({ professionals });
});

app.get('/api/professionals/:id', (req, res) => {
  const pro = db.getProfessionalById(req.params.id);
  if (!pro) {
    return res.status(404).json({ error: 'Professional not found' });
  }
  const reviews = db.getReviews(undefined, pro.id);
  res.json({ professional: pro, reviews });
});

app.patch('/api/professionals/:id/availability', (req, res) => {
  const { isAvailableToday } = req.body;
  const pro = db.updateProfessionalAvailability(req.params.id, Boolean(isAvailableToday));
  if (!pro) {
    return res.status(404).json({ error: 'Professional not found' });
  }
  res.json({ professional: pro });
});

// ==========================================
// 4. BOOKINGS APIS
// ==========================================
app.post('/api/bookings', (req, res) => {
  try {
    const booking = db.createBooking(req.body);
    res.status(201).json({ booking });
  } catch (err: any) {
    res.status(500).json({ error: err.message || 'Failed to create booking' });
  }
});

app.get('/api/bookings', (req, res) => {
  const { customerId, professionalId } = req.query;
  let bookings = db.getAllBookings();
  if (customerId) {
    bookings = db.getBookingsByCustomerId(customerId as string);
  } else if (professionalId) {
    bookings = db.getBookingsByProfessionalId(professionalId as string);
  }
  res.json({ bookings });
});

app.get('/api/bookings/:id', (req, res) => {
  const booking = db.getBookingById(req.params.id);
  if (!booking) {
    return res.status(404).json({ error: 'Booking not found' });
  }
  res.json({ booking });
});

app.patch('/api/bookings/:id/status', (req, res) => {
  const { status, note } = req.body;
  if (!status) {
    return res.status(400).json({ error: 'Status is required' });
  }
  const booking = db.updateBookingStatus(req.params.id, status, note);
  if (!booking) {
    return res.status(404).json({ error: 'Booking not found' });
  }
  res.json({ booking });
});

// ==========================================
// 5. COUPONS & OFFERS APIS
// ==========================================
app.get('/api/coupons', (req, res) => {
  res.json({ coupons: db.getCoupons() });
});

app.post('/api/coupons/validate', (req, res) => {
  const { code, amount, categoryId } = req.body;
  if (!code || amount === undefined) {
    return res.status(400).json({ error: 'Code and amount are required' });
  }
  const result = db.validateCoupon(code, Number(amount), categoryId);
  res.json(result);
});

// ==========================================
// 6. REVIEWS APIS
// ==========================================
app.get('/api/reviews', (req, res) => {
  const { serviceId, professionalId } = req.query;
  const reviews = db.getReviews(serviceId as string, professionalId as string);
  res.json({ reviews });
});

app.post('/api/reviews', (req, res) => {
  const review = db.addReview(req.body);
  res.status(201).json({ review });
});

// ==========================================
// 7. NOTIFICATIONS & SUPPORT APIS
// ==========================================
app.get('/api/notifications', (req, res) => {
  res.json({ notifications: db.getNotifications() });
});

app.patch('/api/notifications/:id/read', (req, res) => {
  const notif = db.markNotificationRead(req.params.id);
  res.json({ notification: notif });
});

app.get('/api/support/tickets', (req, res) => {
  const { userId } = req.query;
  res.json({ tickets: db.getTickets(userId as string) });
});

app.post('/api/support/tickets', (req, res) => {
  const ticket = db.createSupportTicket(req.body);
  res.status(201).json({ ticket });
});

// ==========================================
// 8. ADMIN DASHBOARD APIS
// ==========================================
app.get('/api/admin/metrics', (req, res) => {
  const metrics = db.getAdminMetrics();
  res.json(metrics);
});

// ==========================================
// 9. AI HOME SERVICE ASSISTANT (GEMINI)
// ==========================================
app.post('/api/ai/chat', async (req, res) => {
  const { message, history } = req.body;
  if (!message) {
    return res.status(400).json({ error: 'Message is required' });
  }

  const ai = getGenAI();
  const availableServicesContext = db.services
    .map((s) => `• ${s.name} (Slug: ${s.slug}, Category: ${s.categoryName}, Starting Price: ₹${s.startingPrice})`)
    .join('\n');

  const systemInstruction = `You are "HomeAI", an expert on-demand home services advisor for Service Assist, a premium home-service marketplace operating in major Indian cities (Agra, Delhi NCR, Lucknow, Jaipur, Noida, Gurgaon, Kanpur, Mathura, Mumbai, Bengaluru, Hyderabad, Pune, Chennai, Kolkata).
Your goal is to politely diagnose household problems (e.g., AC cooling issues, water leaks, electrical trips, bathroom scaling, pest problems, salon needs), provide practical troubleshooting tips, and recommend matching Service Assist services with estimated pricing in INR (₹).

Available Service Assist Services on the platform:
${availableServicesContext}

Guidelines:
1. Be warm, professional, trustworthy, and concise (2-4 brief paragraphs).
2. Directly recommend the exact service name and specify the starting price in ₹.
3. If relevant, provide 1-2 immediate safety/troubleshooting tips (e.g. "turn off MCB switch before checking", "clear the outer AC filter").
4. Include a suggested action in format: [ACTION: Book <Service Name> | /services/<slug>].
5. Never invent non-existent services outside of home maintenance, cleaning, beauty, electrical, plumbing, AC, and carpentry.`;

  if (!ai) {
    // Graceful fallback if GEMINI_API_KEY is not configured yet
    const query = message.toLowerCase();
    let reply = "Hello! I am HomeAI, your Service Assist home care assistant. ";
    let actionSlug = "ac-jet-service";
    let actionName = "Foam Jet Power AC Servicing";

    if (query.includes('ac') || query.includes('cool') || query.includes('air condition')) {
      reply += "If your AC is not cooling adequately, common reasons include clogged cooling fins, blocked air filters, or refrigerant pressure loss. We recommend our Foam Jet Power AC Servicing (starting at ₹599) which washes deep coil deposits with a high-pressure jet pump.";
      actionSlug = "ac-jet-service";
      actionName = "Foam Jet Power AC Servicing";
    } else if (query.includes('clean') || query.includes('bath') || query.includes('stain')) {
      reply += "Hard water marks and soap scum on tiles require specialized descaling. Our Intense Bathroom Deep Cleansing (starting at ₹449) removes stubborn mineral deposits and sanitizes fittings without damaging chrome.";
      actionSlug = "bathroom-deep-cleaning";
      actionName = "Intense Bathroom Deep Cleansing";
    } else if (query.includes('salon') || query.includes('hair') || query.includes('facial') || query.includes('beauty')) {
      reply += "Looking for pampering at home? Our Glow & Relax Salon Package for Women (₹899) and Gentlemen’s Haircut & Styling (₹299) bring certified cosmetologists with 100% sterilized single-use kits to your doorstep.";
      actionSlug = "salon-at-home-women";
      actionName = "Glow & Relax Salon Package for Women";
    } else if (query.includes('leak') || query.includes('tap') || query.includes('pipe') || query.includes('plumb')) {
      reply += "A dripping tap or blocked drain can waste hundreds of liters of water weekly. Our verified plumbers can replace internal spindles, angle valves, or clear sink waste lines rapidly (starting at ₹149).";
      actionSlug = "plumbing-tap-leakage-fix";
      actionName = "Tap, Mixer & Drain Pipe Repair";
    } else if (query.includes('fan') || query.includes('switch') || query.includes('electric') || query.includes('mcb')) {
      reply += "Electrical faults should always be inspected by licensed electricians. For tripping MCBs or fan installations, our technicians arrive equipped with multimeters and genuine parts (starting at ₹199).";
      actionSlug = "electrician-on-demand";
      actionName = "Electrician On-Demand & Diagnostics";
    } else {
      reply += "I'd be glad to assist you with booking verified home professionals in your city! Whether it's AC servicing, deep cleaning, pest control, plumbing, or electrical repairs, our background-verified experts are ready.";
      actionSlug = "ac-jet-service";
      actionName = "Foam Jet Power AC Servicing";
    }

    return res.json({
      reply: `${reply}\n\n[ACTION: Book ${actionName} | /services/${actionSlug}]`,
      action: {
        title: `Book ${actionName}`,
        link: `/services/${actionSlug}`,
      },
    });
  }

  try {
    const response = await ai.models.generateContent({
      model: 'gemini-3.8-flash',
      contents: message,
      config: {
        systemInstruction,
        temperature: 0.7,
      },
    });

    const reply = response.text || "I am here to help you find the right Service Assist service for your home.";
    
    // Parse action if present
    let action = undefined;
    const actionMatch = reply.match(/\[ACTION:\s*([^\|\]]+)\s*\|\s*([^\]]+)\]/);
    if (actionMatch) {
      action = {
        title: actionMatch[1].trim(),
        link: actionMatch[2].trim(),
      };
    }

    res.json({
      reply,
      action,
    });
  } catch (err: any) {
    console.error('Gemini error:', err);
    res.json({
      reply: "I can help diagnose your home service issue! Please choose from our popular categories like AC servicing, bathroom cleaning, electrical, or plumbing.",
      action: {
        title: 'Explore Services',
        link: '/services',
      },
    });
  }
});

// ==========================================
// 10. VITE INTEGRATION & SERVER BOOTSTRAP
// ==========================================
async function startServer() {
  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 Service Assist Full-Stack Server running on port ${PORT}`);
  });
}

startServer();
