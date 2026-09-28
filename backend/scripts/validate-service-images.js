import 'dotenv/config';
import { readFile, stat } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { db } from '../src/config/db.js';

const publicRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../frontend/public');
const strict = process.argv.includes('--strict');
const verbose = process.argv.includes('--verbose');
const results = { inspected: 0, valid: 0, broken: [], sharedTiles: [], remote: 0, uniqueLocalPhotos: 0 };

async function checkImage(service) {
  const reference = service.image?.trim();
  if (!reference) return { error: 'missing image field' };
  if (/^https:\/\//.test(reference)) {
    try {
      const response = await fetch(reference, { signal: AbortSignal.timeout(15000), headers: { Range: 'bytes=0-1023' } });
      if (!response.ok || !response.headers.get('content-type')?.startsWith('image/')) {
        return { error: `remote image returned ${response.status} ${response.headers.get('content-type') || ''}` };
      }
      return { remote: true };
    } catch (error) { return { error: `remote image failed: ${error.message}` }; }
  }
  if (!reference.startsWith('/') || reference.includes('?')) return { error: 'invalid local asset path' };
  const file = path.resolve(publicRoot, `.${reference}`);
  if (!file.startsWith(`${publicRoot}${path.sep}`)) return { error: 'asset path escapes public directory' };
  try {
    const info = await stat(file);
    if (!info.isFile() || info.size < 100) return { error: 'empty or missing local image' };
    const bytes = await readFile(file);
    const png = bytes.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]));
    const jpeg = bytes[0] === 0xff && bytes[1] === 0xd8;
    const svg = file.endsWith('.svg') && bytes.toString('utf8', 0, 300).includes('<svg');
    if (!png && !jpeg && !svg) return { error: 'unsupported or invalid image data' };
    return { sharedTile: svg && !path.basename(file).includes(service.id), uniquePhoto: (png || jpeg) && path.basename(file).includes(service.id) };
  } catch (error) { return { error: `local asset failed: ${error.code || error.message}` }; }
}

try {
  const services = await db.service.findMany({ where: { isActive: true }, select: { id: true, name: true, image: true, categoryId: true, subcategory: true }, orderBy: { id: 'asc' } });
  results.inspected = services.length;
  for (const service of services) {
    const check = await checkImage(service);
    if (check.error) results.broken.push({ id: service.id, name: service.name, reason: check.error });
    else {
      results.valid++;
      if (check.remote) results.remote++;
      if (check.sharedTile) results.sharedTiles.push({ id: service.id, name: service.name, image: service.image });
      if (check.uniquePhoto) results.uniqueLocalPhotos++;
    }
  }
  console.log(JSON.stringify({ ...results, sharedTiles: verbose ? results.sharedTiles : results.sharedTiles.length }, null, 2));
  if (results.broken.length || (strict && results.sharedTiles.length)) process.exitCode = 1;
} finally { await db.$disconnect(); }
