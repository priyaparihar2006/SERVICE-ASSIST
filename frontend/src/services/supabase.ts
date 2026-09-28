/**
 * Supabase Web Client for ServiceAssist Website
 * Connects directly to the user's Supabase account with the dedicated 'website' content schema.
 */

export const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL || 'https://biaequumqtdtugjqqkqs.supabase.co';
export const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJpYWVxdXVtcXRkdHVnanFxa3FzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk2MzczNzQsImV4cCI6MjEwNTIxMzM3NH0.tGTWfuuF9fUzUXcLwXd_I0sRbXm4TcxjMKev3rETmX4';

/**
 * Fetch website content directly from Supabase PostgREST API using the 'website' schema.
 */
export async function fetchWebsiteContent(tableName: string, queryParams: string = '') {
  try {
    const url = `${SUPABASE_URL}/rest/v1/${tableName}${queryParams ? `?${queryParams}` : ''}`;
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'apikey': SUPABASE_ANON_KEY,
        'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
        'Accept-Profile': 'website', // Directs PostgREST to the 'website' schema
        'Content-Profile': 'website',
        'Content-Type': 'application/json'
      }
    });
    if (!response.ok) {
      return null;
    }
    return await response.json();
  } catch (err) {
    console.warn(`[Supabase website.${tableName}] fetch error:`, err);
    return null;
  }
}
