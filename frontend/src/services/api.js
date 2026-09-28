export const API_URL = (
  import.meta.env.VITE_API_URL || 'http://localhost:3000/api'
).replace(/\/+$/, '');

export function buildApiUrl(path = '') {
  if (/^https?:\/\//i.test(path)) {
    return path;
  }
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  const base = API_URL.endsWith('/api') ? API_URL : `${API_URL}/api`;
  const relativePath = cleanPath.startsWith('/api/')
    ? cleanPath.slice(4)
    : cleanPath === '/api'
      ? ''
      : cleanPath;
  return `${base}${relativePath}`;
}

export async function apiFetch(path, options = {}) {
  let response;
  const targetUrl = buildApiUrl(path);
  try {
    response = await fetch(targetUrl, {
      ...options,
      credentials: 'include',
      signal: options.signal || AbortSignal.timeout(15000),
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });
  } catch {
    throw new Error('Unable to reach Service Assist. Please try again.');
  }
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    const error = new Error(
      data.details?.map((i) => i.message).join('. ') ||
        data.error ||
        'Request failed',
    );
    error.status = response.status;
    error.code = data.code;
    throw error;
  }
  return response;
}

export async function api(path, options = {}) {
  return (await apiFetch(path, options)).json();
}

export async function getAll(path, key) {
  let page = 1,
    result = [],
    data;
  do {
    data = await api(
      `${path}${path.includes('?') ? '&' : '?'}limit=100&page=${page++}`,
    );
    result = result.concat(data[key] || []);
  } while (data.pagination && page <= data.pagination.pages);
  return result;
}
