const API_URL = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '');
export async function apiFetch(path, options = {}) {
  let response;
  try {
    response = await fetch(`${API_URL}${path.replace(/^\/api/, '')}`, {
      ...options,
      credentials: 'include',
      signal: options.signal || AbortSignal.timeout(15000),
      headers: { 'Content-Type': 'application/json', ...options.headers },
    });
  } catch {
    throw new Error('Unable to reach Service Assist. Please try again.');
  }
  if (!response.ok) {
    const data = await response.json().catch(() => ({}));
    throw new Error(
      data.details?.map((i) => i.message).join('. ') ||
        data.error ||
        'Request failed',
    );
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
    result = result.concat(data[key]);
  } while (data.pagination && page <= data.pagination.pages);
  return result;
}
