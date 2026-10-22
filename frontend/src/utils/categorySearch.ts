import { Category } from '../types';
import { SERVICE_CATEGORIES } from '../data/serviceCategories';

export interface CategoryMatch {
  id: string;
  name: string;
  slug?: string;
  description?: string;
  iconName?: string;
}

// Category keywords dictionary for fast, high-accuracy semantic routing
const CATEGORY_KEYWORDS: Record<string, string[]> = {
  'cat-carpenter': [
    'carpenter',
    'carpentry',
    'wood',
    'wooden',
    'woodwork',
    'furniture',
    'bed',
    'table',
    'chair',
    'wardrobe',
    'cupboard',
    'drawer',
    'door',
    'lock',
    'hinge',
    'curtain rod',
    'drill',
    'cabinet',
  ],
  'cat-plumber': [
    'plumber',
    'plumbing',
    'pipe',
    'pipes',
    'leak',
    'leakage',
    'tap',
    'taps',
    'faucet',
    'drain',
    'drainage',
    'flush',
    'cistern',
    'water tank',
    'blockage',
    'clog',
    'basin',
    'sink',
  ],
  'cat-electrician': [
    'electrician',
    'electrical',
    'electric',
    'wiring',
    'wire',
    'switch',
    'switches',
    'socket',
    'fan',
    'ceiling fan',
    'mcb',
    'fuse',
    'short circuit',
    'inverter',
    'light',
    'chandelier',
    'exhaust fan',
    'power',
  ],
  'cat-ac-appliances': [
    'ac',
    'air conditioner',
    'aircon',
    'split ac',
    'window ac',
    'jet service',
    'gas refill',
    'cooling',
    'ac repair',
    'ac installation',
    'ac uninstallation',
    'hvac',
  ],
  'cat-cleaning': [
    'cleaning',
    'clean',
    'deep cleaning',
    'home cleaning',
    'house cleaning',
    'dusting',
    'sanitization',
    'kitchen cleaning',
    'balcony cleaning',
    'floor scrub',
    'bhk',
    'move in clean',
  ],
  'cat-bathroom-cleaning': [
    'bathroom',
    'bathroom cleaning',
    'washroom',
    'toilet',
    'commode',
    'hard water',
    'tile scrub',
    'shower cleaning',
    'descaling',
  ],
  'cat-sofa-cleaning': [
    'sofa',
    'sofa cleaning',
    'couch',
    'cushion',
    'upholstery',
    'carpet',
    'mattress',
    'shampooing',
  ],
  'cat-beauty': [
    'beauty',
    'salon',
    'parlour',
    'parlor',
    'facial',
    'waxing',
    'wax',
    'haircut',
    'hair',
    'pedicure',
    'manicure',
    'threading',
    'bleach',
    'makeup',
    'spa',
    'massage',
  ],
  'cat-pest-control': [
    'pest',
    'pest control',
    'cockroach',
    'termite',
    'bedbug',
    'bedbugs',
    'ant',
    'ants',
    'mosquito',
    'rodent',
    'rat',
    'gel treatment',
  ],
  'cat-painting': [
    'painting',
    'paint',
    'painter',
    'whitewash',
    'wall paint',
    'texture',
    'waterproofing',
    'moisture',
    'distemper',
    'emulsion',
    'ceiling paint',
  ],
  'cat-water-purifier': [
    'water purifier',
    'ro',
    'purifier',
    'filter',
    'kent',
    'aquaguard',
    'livpure',
    'tds',
    'membrane',
    'water filter',
  ],
  'cat-laptop-computer': [
    'laptop',
    'computer',
    'pc',
    'desktop',
    'macbook',
    'windows',
    'screen replacement',
    'keyboard',
    'ram',
    'ssd',
    'virus',
    'formatting',
  ],
  'cat-appliance-repair': [
    'appliance',
    'washing machine',
    'refrigerator',
    'fridge',
    'microwave',
    'oven',
    'geyser',
    'water heater',
  ],
  'cat-packers-movers': [
    'packers',
    'movers',
    'shifting',
    'relocation',
    'transport',
    'packing',
    'moving',
  ],
  'cat-home-improvement': [
    'home improvement',
    'renovation',
    'interior',
    'false ceiling',
    'tile work',
    'flooring',
  ],
};

/**
 * Normalizes query string for comparisons
 */
function cleanTerm(str: string): string {
  return str.toLowerCase().replace(/[^a-z0-9\s]/g, ' ').replace(/\s+/g, ' ').trim();
}

/**
 * Resolves a search query to a matching Category ID if one exists.
 * E.g., "carpenter", "fix wooden bed", "need a plumber", "ac jet clean", etc.
 */
export function findMatchingCategory(
  query: string,
  categories?: Category[]
): CategoryMatch | null {
  if (!query || !query.trim()) return null;

  const normalized = cleanTerm(query);
  if (!normalized) return null;

  const words = normalized.split(' ');

  // 1. Direct match in provided categories list
  if (categories && categories.length > 0) {
    for (const cat of categories) {
      const cId = cleanTerm(cat.id);
      const cName = cleanTerm(cat.name);
      const cSlug = cleanTerm(cat.slug || '');

      if (normalized === cName || normalized === cSlug || normalized === cId) {
        return { id: cat.id, name: cat.name, slug: cat.slug, description: cat.description };
      }
      if (cName.includes(normalized) || (normalized.length > 3 && normalized.includes(cName))) {
        return { id: cat.id, name: cat.name, slug: cat.slug, description: cat.description };
      }
    }
  }

  // 2. Direct match in SERVICE_CATEGORIES catalog
  for (const cat of SERVICE_CATEGORIES) {
    const cName = cleanTerm(cat.name);
    const cSlug = cleanTerm(cat.slug);
    const cId = cleanTerm(cat.id);

    if (normalized === cName || normalized === cSlug || normalized === cId) {
      return { id: cat.id, name: cat.name, slug: cat.slug, description: cat.description };
    }
    if (cName.includes(normalized) || (normalized.length > 3 && normalized.includes(cName))) {
      return { id: cat.id, name: cat.name, slug: cat.slug, description: cat.description };
    }
  }

  // 3. Keyword / Synonym mapping lookup
  let bestCategoryId: string | null = null;
  let maxScore = 0;

  for (const [catId, keywords] of Object.entries(CATEGORY_KEYWORDS)) {
    for (const kw of keywords) {
      // Exact keyword match
      if (normalized === kw) {
        bestCategoryId = catId;
        maxScore = 100;
        break;
      }
      // Word boundaries
      if (words.includes(kw)) {
        if (50 > maxScore) {
          bestCategoryId = catId;
          maxScore = 50;
        }
      } else if (normalized.includes(kw)) {
        const score = kw.length;
        if (score > maxScore) {
          bestCategoryId = catId;
          maxScore = score;
        }
      }
    }
    if (maxScore === 100) break;
  }

  if (bestCategoryId) {
    // Standardize cat-home-cleaning to cat-cleaning or vice-versa
    const foundInService = SERVICE_CATEGORIES.find(
      (c) => c.id === bestCategoryId || (bestCategoryId === 'cat-cleaning' && c.id === 'cat-home-cleaning')
    );
    const foundInProvided = categories?.find(
      (c) => c.id === bestCategoryId || (bestCategoryId === 'cat-cleaning' && c.id === 'cat-home-cleaning')
    );

    const resolved = foundInProvided || foundInService;
    if (resolved) {
      return {
        id: resolved.id,
        name: resolved.name,
        slug: resolved.slug,
        description: resolved.description,
      };
    }

    // Fallback if not found in array
    const nameMap: Record<string, string> = {
      'cat-carpenter': 'Carpentry',
      'cat-plumber': 'Plumbing',
      'cat-electrician': 'Electrician',
      'cat-ac-appliances': 'AC & Appliances',
      'cat-cleaning': 'Home Cleaning',
      'cat-beauty': 'Beauty & Salon',
      'cat-pest-control': 'Pest Control',
      'cat-painting': 'Painting & Walls',
      'cat-sofa-cleaning': 'Sofa Cleaning',
      'cat-bathroom-cleaning': 'Bathroom Cleaning',
      'cat-water-purifier': 'Water Purifier',
      'cat-laptop-computer': 'Laptop & Computer',
      'cat-appliance-repair': 'Appliance Repair',
      'cat-packers-movers': 'Packers & Movers',
      'cat-home-improvement': 'Home Improvement',
    };

    return {
      id: bestCategoryId,
      name: nameMap[bestCategoryId] || bestCategoryId,
    };
  }

  return null;
}

/**
 * Returns matching categories for autocomplete / instant suggestions
 */
export function getCategorySuggestions(
  query: string,
  categories?: Category[],
  maxResults = 5
): CategoryMatch[] {
  if (!query || !query.trim()) {
    // Default popular suggestions
    return [
      { id: 'cat-carpenter', name: 'Carpentry & Woodwork' },
      { id: 'cat-electrician', name: 'Electrician & Wiring' },
      { id: 'cat-plumber', name: 'Plumber & Leak Fix' },
      { id: 'cat-ac-appliances', name: 'AC & Appliances' },
      { id: 'cat-cleaning', name: 'Home Deep Cleaning' },
      { id: 'cat-beauty', name: 'Beauty & Salon at Home' },
    ].slice(0, maxResults);
  }

  const normalized = cleanTerm(query);
  const results: CategoryMatch[] = [];
  const seenIds = new Set<string>();

  // Check keywords for matching categories
  for (const [catId, keywords] of Object.entries(CATEGORY_KEYWORDS)) {
    const hasMatch = keywords.some(
      (kw) => kw.includes(normalized) || normalized.includes(kw)
    );
    if (hasMatch && !seenIds.has(catId)) {
      seenIds.add(catId);
      const matched = SERVICE_CATEGORIES.find(
        (c) => c.id === catId || (catId === 'cat-cleaning' && c.id === 'cat-home-cleaning')
      );
      results.push({
        id: matched?.id || catId,
        name: matched?.name || catId.replace('cat-', '').replace('-', ' ').toUpperCase(),
        slug: matched?.slug,
        description: matched?.description,
      });
    }
  }

  return results.slice(0, maxResults);
}
