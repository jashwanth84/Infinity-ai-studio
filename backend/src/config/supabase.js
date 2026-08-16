import { createClient } from '@supabase/supabase-js';
import dotenv from 'dotenv';

// Load environment variables
dotenv.config();

const supabaseUrl = process.env.SUPABASE_URL;
const supabaseServiceKey = process.env.SUPABASE_SECRET_KEY; // Only used on the backend

if (!supabaseUrl || !supabaseServiceKey) {
  console.error('CRITICAL ERROR: Supabase environment variables are missing!');
  console.error('Please check SUPABASE_URL and SUPABASE_SECRET_KEY in your .env file.');
  process.exit(1);
}

// Create the Supabase client with the service role key to bypass RLS for administrative backend tasks.
// User context (if needed for RLS) can be passed explicitly via auth headers on requests.
export const supabase = createClient(supabaseUrl, supabaseServiceKey, {
  auth: {
    autoRefreshToken: false,
    persistSession: false
  }
});

// For passing client requests acting on behalf of a user using their JWT
export const createScopedClient = (authHeader) => {
  const token = authHeader?.replace('Bearer ', '');
  if (!token) throw new Error('Missing Auth token');
  
  return createClient(supabaseUrl, process.env.SUPABASE_PUBLISHABLE_KEY, {
    global: { headers: { Authorization: `Bearer ${token}` } },
    auth: { persistSession: false, autoRefreshToken: false }
  });
};
