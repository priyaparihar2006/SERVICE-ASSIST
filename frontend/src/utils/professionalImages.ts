/**
 * Professional Image Mapping and Fallback Utility
 * Provides verified local assets matching each profession and service category
 */

export const PROFESSION_IMAGES: Record<string, string[]> = {
  plumber: [
    '/images/professionals/plumber-1.jpg',
    '/images/professionals/plumber-2.jpg',
  ],
  electrician: [
    '/images/professionals/electrician-1.jpg',
    '/images/professionals/electrician-2.jpg',
  ],
  ac: [
    '/images/professionals/ac-technician-1.jpg',
    '/images/professionals/ac-technician-2.jpg',
  ],
  cleaner: [
    '/images/professionals/cleaner-1.jpg',
    '/images/professionals/cleaner-2.jpg',
    '/images/professionals/cleaner-3.jpg',
  ],
  beauty: [
    '/images/professionals/beauty-1.jpg',
    '/images/professionals/beauty-2.jpg',
    '/images/professionals/beauty-3.jpg',
  ],
  nail: [
    '/images/professionals/nail-tech-1.jpg',
    '/images/professionals/nail-tech-2.jpg',
  ],
  carpenter: [
    '/images/professionals/carpenter-1.jpg',
    '/images/professionals/carpenter-2.jpg',
  ],
  painter: [
    '/images/professionals/painter-1.jpg',
    '/images/professionals/painter-2.jpg',
  ],
  appliance: [
    '/images/professionals/appliance-1.jpg',
    '/images/professionals/appliance-2.jpg',
  ],
  laptop: [
    '/images/professionals/laptop-tech-1.jpg',
    '/images/professionals/laptop-tech-2.jpg',
  ],
  electronics: [
    '/images/professionals/electronics-1.jpg',
    '/images/professionals/electronics-2.jpg',
  ],
  pest: [
    '/images/professionals/pest-control-1.jpg',
    '/images/professionals/pest-control-2.jpg',
  ],
  waterPurifier: [
    '/images/professionals/water-purifier-1.jpg',
    '/images/professionals/water-purifier-2.jpg',
  ],
  movers: [
    '/images/professionals/movers-1.jpg',
    '/images/professionals/movers-2.jpg',
  ],
  mechanic: [
    '/images/professionals/mechanic-1.jpg',
  ],
  general: [
    '/images/professionals/general-pro-1.jpg',
    '/images/professionals/general-pro-2.jpg',
    '/images/professionals/general-pro-3.jpg',
    '/images/professionals/general-pro-4.jpg',
  ],
};

// Explicit mappings for known demo professionals to ensure unique, perfect photos
export const NAMED_PRO_IMAGES: Record<string, string> = {
  // By ID
  'pro-rahul': '/images/professionals/ac-technician-1.jpg',
  'pro-priya': '/images/professionals/beauty-1.jpg',
  'pro-amit': '/images/professionals/cleaner-1.jpg',
  'pro-vikram': '/images/professionals/electrician-1.jpg',
  'pro-manoj': '/images/professionals/plumber-1.jpg',
  'pro-sanjay': '/images/professionals/appliance-1.jpg',
  'pro-arjun': '/images/professionals/laptop-tech-1.jpg',
  'pro-rohan': '/images/professionals/electronics-1.jpg',
  'pro-neha': '/images/professionals/nail-tech-1.jpg',
  'pro-demo-nails': '/images/professionals/nail-tech-1.jpg',
  'pro-demo-computers': '/images/professionals/laptop-tech-1.jpg',
  'pro-demo-electronics': '/images/professionals/electronics-1.jpg',
  'pro-demo-appliances': '/images/professionals/appliance-1.jpg',
  'pro-dinesh': '/images/professionals/carpenter-1.jpg',
  'pro-karan': '/images/professionals/painter-1.jpg',
  'pro-manish': '/images/professionals/pest-control-1.jpg',
  'pro-deepak': '/images/professionals/water-purifier-1.jpg',
  'pro-harish': '/images/professionals/movers-1.jpg',
  'pro-pooja': '/images/professionals/cleaner-2.jpg',
  'pro-sameer': '/images/professionals/cleaner-3.jpg',

  // By Name key
  'rahul sharma': '/images/professionals/ac-technician-1.jpg',
  'priya verma': '/images/professionals/beauty-1.jpg',
  'amit rawat': '/images/professionals/cleaner-1.jpg',
  'vikram singh': '/images/professionals/electrician-1.jpg',
  'manoj kumar': '/images/professionals/plumber-1.jpg',
  'manoj plumber': '/images/professionals/plumber-1.jpg',
  'sanjay verma': '/images/professionals/appliance-1.jpg',
  'arjun mehta': '/images/professionals/laptop-tech-1.jpg',
  'rohan kapoor': '/images/professionals/electronics-1.jpg',
  'neha singh': '/images/professionals/nail-tech-1.jpg',
  'dinesh lohar': '/images/professionals/carpenter-1.jpg',
  'karan verma': '/images/professionals/painter-1.jpg',
  'manish tiwari': '/images/professionals/pest-control-1.jpg',
  'deepak gupta': '/images/professionals/water-purifier-1.jpg',
  'harish negi': '/images/professionals/movers-1.jpg',
  'pooja rajput': '/images/professionals/cleaner-2.jpg',
  'sameer khan': '/images/professionals/cleaner-3.jpg',
  'workflow professional': '/images/professionals/general-pro-1.jpg',
  
  // Hero slide names
  'rahul s.': '/images/professionals/ac-technician-1.jpg',
  'anjali m.': '/images/professionals/cleaner-1.jpg',
  'neha s.': '/images/professionals/beauty-2.jpg',
  'amit p.': '/images/professionals/electrician-1.jpg',
  'suresh y.': '/images/professionals/plumber-2.jpg',
  'dinesh l.': '/images/professionals/carpenter-1.jpg',
  'manish t.': '/images/professionals/pest-control-1.jpg',
  'karan v.': '/images/professionals/painter-1.jpg',
  'vikram j.': '/images/professionals/appliance-1.jpg',
  'pooja r.': '/images/professionals/cleaner-2.jpg',
  'sameer k.': '/images/professionals/cleaner-3.jpg',
  'harish n.': '/images/professionals/movers-1.jpg',
  'arjun b.': '/images/professionals/carpenter-2.jpg',
  'deepak g.': '/images/professionals/water-purifier-1.jpg',
  'prashant m.': '/images/professionals/laptop-tech-2.jpg',
};

/**
 * Returns the best profession-specific fallback image URL based on profession, category, or role text.
 */
export function getProfessionFallbackImage(
  professionOrCategory: string = '',
  seedKey: string = ''
): string {
  const text = (professionOrCategory || '').toLowerCase();
  let pool = PROFESSION_IMAGES.general;

  if (/nail|manicure|pedicure/.test(text)) {
    pool = PROFESSION_IMAGES.nail;
  } else if (/salon|beauty|hair|aesthetic|facial|makeup|spa|waxing/.test(text)) {
    pool = PROFESSION_IMAGES.beauty;
  } else if (/plumb|pipe|tap|drain|leak|sanitary|geyser|cat-plumber/.test(text)) {
    pool = PROFESSION_IMAGES.plumber;
  } else if (/electric|wire|wiring|switch|circuit|mcb|cat-electrician/.test(text)) {
    pool = PROFESSION_IMAGES.electrician;
  } else if (/hvac|ac |air condition|cooling|compressor|cat-ac/.test(text)) {
    pool = PROFESSION_IMAGES.ac;
  } else if (/appliance|washing machine|refrigerator|fridge|microwave|chimney|cat-appliance/.test(text)) {
    pool = PROFESSION_IMAGES.appliance;
  } else if (/laptop|computer|macbook|pc |screen repair|chipset/.test(text)) {
    pool = PROFESSION_IMAGES.laptop;
  } else if (/electronic|tv |television|motherboard|cat-laptop-electronics/.test(text)) {
    pool = PROFESSION_IMAGES.electronics;
  } else if (/clean|sanitiz|bath|deep clean|maid|hygiene|cat-cleaning|cat-bathroom/.test(text)) {
    pool = PROFESSION_IMAGES.cleaner;
  } else if (/sofa|upholstery|carpet|fabric|cat-sofa/.test(text)) {
    pool = PROFESSION_IMAGES.cleaner;
  } else if (/carpen|wood|furniture|door|hinge|lock|cat-carpenter/.test(text)) {
    pool = PROFESSION_IMAGES.carpenter;
  } else if (/paint|whitewash|wall|waterproof|coat|cat-painting/.test(text)) {
    pool = PROFESSION_IMAGES.painter;
  } else if (/pest|termite|cockroach|bedbug|insect|cat-pest/.test(text)) {
    pool = PROFESSION_IMAGES.pest;
  } else if (/water purifier|ro |filtration|membrane|tds|cat-water-purifier/.test(text)) {
    pool = PROFESSION_IMAGES.waterPurifier;
  } else if (/mover|pack|relocat|shifting|cargo|cat-moving/.test(text)) {
    pool = PROFESSION_IMAGES.movers;
  } else if (/mechanic|car |vehicle|automobile|motor/.test(text)) {
    pool = PROFESSION_IMAGES.mechanic;
  }

  // Use seed key to pick a deterministic index from the pool
  if (seedKey && pool.length > 1) {
    let hash = 0;
    for (let i = 0; i < seedKey.length; i++) {
      hash = (hash << 5) - hash + seedKey.charCodeAt(i);
      hash |= 0;
    }
    const index = Math.abs(hash) % pool.length;
    return pool[index];
  }

  return pool[0];
}

/**
 * Resolves the final image URL for a professional object or parameters.
 * Guarantees a valid, non-empty, profession-matched photo URL.
 */
export function getProfessionalImage(pro?: {
  id?: string;
  name?: string;
  avatar?: string;
  profession?: string;
  categoryId?: string;
} | null): string {
  if (!pro) return PROFESSION_IMAGES.general[0];

  const idKey = (pro.id || '').toLowerCase();
  const nameKey = (pro.name || '').toLowerCase();

  // Check explicit named/ID mapping
  if (idKey && NAMED_PRO_IMAGES[idKey]) {
    return NAMED_PRO_IMAGES[idKey];
  }
  if (nameKey && NAMED_PRO_IMAGES[nameKey]) {
    return NAMED_PRO_IMAGES[nameKey];
  }

  // If pro already has a valid avatar URL
  if (pro.avatar && pro.avatar.trim() && !pro.avatar.includes('broken')) {
    return pro.avatar.trim();
  }

  // Fallback to profession/category matching
  return getProfessionFallbackImage(
    `${pro.profession || ''} ${pro.categoryId || ''} ${pro.name || ''}`,
    pro.id || pro.name || ''
  );
}
