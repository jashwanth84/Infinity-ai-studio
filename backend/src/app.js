import express from 'express';
import cors from 'cors';
import { supabase } from './config/supabase.js';

const app = express();
app.use(cors());
app.use(express.json());

// Health Check Endpoint
app.get('/api/health', async (req, res) => {
  try {
    // Basic test query to verify PostgreSQL connection
    const { data, error } = await supabase.from('profiles').select('id').limit(1);
    
    if (error) {
      throw error;
    }

    res.status(200).json({
      backend: 'ok',
      supabase: 'connected',
      database: 'connected'
    });
  } catch (err) {
    console.error('Health check failed:', err.message);
    res.status(500).json({
      backend: 'ok',
      supabase: 'error',
      database: 'error',
      message: 'Failed to connect to Supabase database.'
    });
  }
});

// Middleware to extract user from Supabase JWT
const requireAuth = async (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader) return res.status(401).json({ error: 'Missing authorization header' });
  
  const token = authHeader.replace('Bearer ', '');
  const { data: { user }, error } = await supabase.auth.getUser(token);
  
  if (error || !user) {
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
  
  req.user = user;
  next();
};

// Example protected route using the backend client scoped to the user
app.get('/api/profile', requireAuth, async (req, res) => {
  const { data, error } = await supabase
    .from('profiles')
    .select('*')
    .eq('id', req.user.id)
    .single();

  if (error) return res.status(500).json({ error: error.message });
  res.json(data);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Backend server listening on port ${PORT}`);
});
