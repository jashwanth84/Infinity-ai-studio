import express from 'express';
import cors from 'cors';
import { supabase } from './config/supabase.js';

const app = express();
app.use(cors());
app.use(express.json());

// Health Check Endpoint
app.get('/api/health', async (req, res) => {
  try {
    const { data, error } = await supabase.from('profiles').select('id').limit(1);
    if (error) throw error;
    res.status(200).json({ backend: 'ok', supabase: 'connected', database: 'connected' });
  } catch (err) {
    res.status(500).json({ backend: 'ok', supabase: 'error', database: 'error', message: err.message });
  }
});

app.post('/api/auth/signup', async (req, res) => {
  const { name, email, password } = req.body;
  try {
    const { data, error } = await supabase.auth.signUp({
      email, password, options: { data: { full_name: name } }
    });
    if (error) return res.status(400).json({ error: error.message });
    
    res.json({
      token: data.session?.access_token || '',
      user: {
        id: data.user.id, name: name, email: email, plan: 'Free',
        creditsRemaining: 5000, creditsUsed: 0, requests: 0, favoriteModel: 'GPT-4o'
      }
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/auth/login', async (req, res) => {
  const { email, password } = req.body;
  try {
    const { data, error } = await supabase.auth.signInWithPassword({ email, password });
    if (error) return res.status(401).json({ error: error.message });
    
    const { data: profile } = await supabase.from('profiles').select('*').eq('id', data.user.id).single();
    const { data: credits } = await supabase.from('user_credits').select('balance').eq('user_id', data.user.id).single();
    
    res.json({
      token: data.session.access_token,
      user: {
        id: data.user.id, name: profile?.name || email, email: email, plan: 'Free',
        creditsRemaining: credits?.balance || 0, creditsUsed: 0, requests: 0, favoriteModel: 'GPT-4o'
      }
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

const requireAuth = async (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader) return res.status(401).json({ error: 'Missing authorization header' });
  const token = authHeader.replace('Bearer ', '');
  const { data: { user }, error } = await supabase.auth.getUser(token);
  if (error || !user) return res.status(401).json({ error: 'Invalid or expired token' });
  req.user = user;
  next();
};

app.get('/api/user/profile', requireAuth, async (req, res) => {
  res.json({ id: req.user.id, name: req.user.email, email: req.user.email, plan: 'Free', creditsRemaining: 5000, creditsUsed: 0, requests: 0, favoriteModel: 'GPT-4o' });
});

app.get('/api/models', requireAuth, async (req, res) => {
  res.json([
    {id: "gpt-4", provider: "OpenAI", name: "GPT-4o", capabilities: ["Vision", "Reasoning", "Coding"], contextWindow: 128000, speed: "Fast", isFavorite: true},
    {id: "claude-3-opus", provider: "Anthropic", name: "Claude 3 Opus", capabilities: ["Reasoning", "Coding", "Writing"], contextWindow: 200000, speed: "Medium", isFavorite: false}
  ]);
});

app.get('/api/chats', requireAuth, async (req, res) => {
  const { data, error } = await supabase.from('conversations').select('*').eq('user_id', req.user.id).order('updated_at', { ascending: false });
  if (error) return res.status(500).json({ error: error.message });
  res.json(data.map(c => ({ id: c.id, title: c.title || 'New Chat', updatedAt: new Date(c.updated_at).getTime(), isPinned: false, folder: null })));
});

app.post('/api/chat/message', requireAuth, async (req, res) => {
  const { chatId, message, modelId } = req.body;
  
  try {
    // Check user credits
    const { data: credits, error: creditError } = await supabase
      .from('user_credits')
      .select('balance')
      .eq('user_id', req.user.id)
      .single();
      
    if (creditError && creditError.code !== 'PGRST116') {
      console.error('Credit fetch error:', creditError);
    }
    
    // In a real app we'd block if credits < 0, but for this demo if the table doesn't exist yet, we continue
    const balance = credits?.balance ?? 5000;
    if (balance <= 0) {
      return res.status(403).json({ error: 'Insufficient credits to generate a response.' });
    }

    // Determine the AI Provider
    // For this demonstration, we'll try Gemini if GEMINI_API_KEY is available,
    // otherwise fallback to a simulated response to unblock the Android UI.
    let replyText = "";
    
    if (process.env.GEMINI_API_KEY) {
      const providerUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-latest:generateContent?key=${process.env.GEMINI_API_KEY}`;
      const aiResponse = await fetch(providerUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ role: "user", parts: [{ text: message }] }]
        })
      });
      
      if (!aiResponse.ok) {
        throw new Error(`AI Provider returned error: ${await aiResponse.text()}`);
      }
      
      const aiData = await aiResponse.json();
      replyText = aiData.candidates?.[0]?.content?.parts?.[0]?.text || "No response generated.";
    } else {
      replyText = `This is a response generated by the real Node.js backend. You said: "${message}". (Add GEMINI_API_KEY to backend/.env to enable live AI responses)`;
    }

    // Deduct credits
    const cost = 10;
    if (credits) {
      await supabase.from('user_credits').update({ balance: balance - cost }).eq('user_id', req.user.id);
    }

    res.json({ 
      id: Date.now().toString(), 
      text: replyText, 
      timestamp: Date.now() 
    });
    
  } catch (err) {
    console.error('AI chat error:', err);
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/chat/message/:messageId/feedback', requireAuth, async (req, res) => {
  res.status(200).send();
});

app.get('/api/agents', requireAuth, async (req, res) => {
  res.json([{id: "a1", name: "Code Reviewer", persona: "Strict senior developer.", instructions: "Review code.", hasKnowledgeFiles: false}]);
});

app.post('/api/agents', requireAuth, async (req, res) => {
  const { name, persona, instructions } = req.body;
  res.json({ id: Date.now().toString(), name, persona, instructions, hasKnowledgeFiles: false });
});

const PORT = 3000;
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Backend server listening on port ${PORT}`);
});
