-- Enable Row Level Security
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE providers ENABLE ROW LEVEL SECURITY;
ALTER TABLE models ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_credits ENABLE ROW LEVEL SECURITY;
ALTER TABLE usage_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- Profiles: Users can read and update their own profile
CREATE POLICY "Users can view own profile" ON profiles FOR SELECT USING (auth.uid() = id);
CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE USING (auth.uid() = id);

-- Providers: Everyone can read, only backend (service role) can modify
CREATE POLICY "Anyone can view providers" ON providers FOR SELECT USING (true);

-- Models: Everyone can read, only backend (service role) can modify
CREATE POLICY "Anyone can view models" ON models FOR SELECT USING (true);

-- User Credits: Users can read their own, cannot modify
CREATE POLICY "Users can view own credits" ON user_credits FOR SELECT USING (auth.uid() = user_id);
-- (No insert/update/delete policies for users -> they can't modify credits)

-- Usage Logs: Users can read their own
CREATE POLICY "Users can view own usage logs" ON usage_logs FOR SELECT USING (auth.uid() = user_id);

-- Conversations: Users can manage their own
CREATE POLICY "Users can view own conversations" ON conversations FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert own conversations" ON conversations FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update own conversations" ON conversations FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete own conversations" ON conversations FOR DELETE USING (auth.uid() = user_id);

-- Messages: Users can manage their own
CREATE POLICY "Users can view own messages" ON messages FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert own messages" ON messages FOR INSERT WITH CHECK (auth.uid() = user_id);
-- (Typically messages aren't editable, but if needed, users can update their own)
CREATE POLICY "Users can delete own messages" ON messages FOR DELETE USING (auth.uid() = user_id);

-- Notifications: Users can view and update their own
CREATE POLICY "Users can view own notifications" ON notifications FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can update own notifications" ON notifications FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete own notifications" ON notifications FOR DELETE USING (auth.uid() = user_id);

-- Audit Logs: Only service role can insert/view
-- (No policies for normal users, effectively blocking them)

-- Storage Policies
-- Assuming buckets: 'avatars', 'chat-files', 'images', 'documents'
-- Note: Buckets need to be created first via Supabase UI or SQL.
-- Example SQL for creating buckets (requires superuser/postgres role)
INSERT INTO storage.buckets (id, name, public) VALUES ('avatars', 'avatars', true) ON CONFLICT DO NOTHING;
INSERT INTO storage.buckets (id, name, public) VALUES ('chat-files', 'chat-files', false) ON CONFLICT DO NOTHING;
INSERT INTO storage.buckets (id, name, public) VALUES ('images', 'images', false) ON CONFLICT DO NOTHING;
INSERT INTO storage.buckets (id, name, public) VALUES ('documents', 'documents', false) ON CONFLICT DO NOTHING;

-- Avatars: Public read, authenticated users can upload their own
CREATE POLICY "Avatar images are publicly accessible." ON storage.objects FOR SELECT USING (bucket_id = 'avatars');
CREATE POLICY "Anyone can upload an avatar." ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'avatars' AND auth.role() = 'authenticated');
CREATE POLICY "Users can update their own avatars." ON storage.objects FOR UPDATE USING (bucket_id = 'avatars' AND auth.uid() = owner);

-- Chat Files: Private read/write to the owner
CREATE POLICY "Users can read own chat files" ON storage.objects FOR SELECT USING (bucket_id = 'chat-files' AND auth.uid() = owner);
CREATE POLICY "Users can upload own chat files" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'chat-files' AND auth.role() = 'authenticated');
CREATE POLICY "Users can delete own chat files" ON storage.objects FOR DELETE USING (bucket_id = 'chat-files' AND auth.uid() = owner);

-- Images: Private read/write to the owner
CREATE POLICY "Users can read own images" ON storage.objects FOR SELECT USING (bucket_id = 'images' AND auth.uid() = owner);
CREATE POLICY "Users can upload own images" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'images' AND auth.role() = 'authenticated');
CREATE POLICY "Users can delete own images" ON storage.objects FOR DELETE USING (bucket_id = 'images' AND auth.uid() = owner);

-- Documents: Private read/write to the owner
CREATE POLICY "Users can read own documents" ON storage.objects FOR SELECT USING (bucket_id = 'documents' AND auth.uid() = owner);
CREATE POLICY "Users can upload own documents" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'documents' AND auth.role() = 'authenticated');
CREATE POLICY "Users can delete own documents" ON storage.objects FOR DELETE USING (bucket_id = 'documents' AND auth.uid() = owner);

-- Enable Realtime for selected tables
ALTER PUBLICATION supabase_realtime ADD TABLE notifications;
ALTER PUBLICATION supabase_realtime ADD TABLE user_credits;
