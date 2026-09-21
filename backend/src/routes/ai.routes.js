import { Router } from 'express';
import rateLimit from 'express-rate-limit';
import { z } from 'zod';
import { validate } from '../middleware/validate.js';
import { endpoint, ensure } from '../utils/errors.js';
import { db } from '../config/db.js';
const router = Router();
router.post(
  '/ai/chat',
  rateLimit({ windowMs: 60000, limit: 10 }),
  validate(z.object({ message: z.string().trim().min(1).max(2000) })),
  endpoint(async (req, res) => {
    ensure(
      process.env.GEMINI_API_KEY && process.env.GEMINI_MODEL,
      503,
      'HomeAI is currently unavailable. Please browse or search our services.',
    );
    const services = await db.service.findMany({
      where: { isActive: true },
      select: { name: true, slug: true, startingPrice: true },
      take: 100,
    });
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(process.env.GEMINI_MODEL)}:generateContent`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-goog-api-key': process.env.GEMINI_API_KEY,
        },
        body: JSON.stringify({
          systemInstruction: {
            parts: [
              {
                text: `You are Service Assist's service discovery assistant. Recommend only catalog services. Never claim to have booked or paid. Catalog: ${JSON.stringify(services)}`,
              },
            ],
          },
          contents: [{ role: 'user', parts: [{ text: req.validated.message }] }],
        }),
        signal: AbortSignal.timeout(20000),
      },
    );
    ensure(response.ok, 503, 'HomeAI is temporarily unavailable');
    const data = await response.json();
    const reply = data.candidates?.[0]?.content?.parts?.map((p) => p.text || '').join('');
    ensure(reply, 503, 'HomeAI could not answer this request');
    res.json({ reply });
  }),
);
export default router;
