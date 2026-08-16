# Supabase Setup for Infinity AI Studio

This guide explains how to properly configure the Supabase backend for Infinity AI Studio.

## 1. Create a Supabase Project

1. Go to [Supabase](https://supabase.com/) and create a new project.
2. Wait for the database provisioning to complete.

## 2. Obtain Credentials

1. Go to **Project Settings -> API**.
2. Copy the **Project URL** (`SUPABASE_URL`).
3. Copy the **anon / public key** (`SUPABASE_PUBLISHABLE_KEY`).
4. Copy the **service_role / secret key** (`SUPABASE_SECRET_KEY`).
   > **WARNING**: The `SUPABASE_SECRET_KEY` bypasses all Row Level Security (RLS) policies. NEVER expose it to the Android application or place it in the Android source code. It must only be used securely on the Node.js backend.

## 3. Configure the Backend Environment

1. In the `backend/` directory, copy `.env.example` to `.env`:
   ```bash
   cd backend
   cp .env.example .env
   ```
2. Open `.env` and fill in the Supabase credentials obtained above.
3. Also configure any required AI Provider API keys (e.g., `OPENAI_API_KEY`). These keys are kept completely isolated on the backend.

## 4. Run SQL Migrations

1. Go to the **SQL Editor** in your Supabase Dashboard.
2. Copy and paste the contents of `backend/supabase/migrations/00001_initial_schema.sql` and run it. This establishes the database schema and authentication triggers.
3. Copy and paste the contents of `backend/supabase/migrations/00002_rls_and_storage.sql` and run it. This secures the tables with Row Level Security (RLS), sets up Storage buckets, and configures Realtime.

## 5. Configure Authentication

1. Go to **Authentication -> Settings** in the Supabase Dashboard.
2. Enable Email/Password authentication (or any OAuth providers you wish to support).
3. (Optional) Configure email templates and SMTP settings for production.

## 6. Configure Storage

The migration script (`00002_rls_and_storage.sql`) automatically provisions the necessary storage buckets (`avatars` and `chat-files`) and applies proper security policies so users can only access their own private files. 

If for some reason the buckets were not created, go to **Storage -> Buckets** in the Supabase dashboard and create them manually:
- `avatars` (Public)
- `chat-files` (Private)

## 7. Start the Backend

1. Install dependencies:
   ```bash
   cd backend
   npm install
   ```
2. Start the server:
   ```bash
   npm start
   ```
   *The server will start on port 3000 by default.*

## 8. Test the Setup

You can verify the backend connection to Supabase by hitting the health check endpoint:

```bash
curl http://localhost:3000/api/health
```

**Expected Response**:
```json
{
  "backend": "ok",
  "supabase": "connected",
  "database": "connected"
}
```

## Architecture Summary

- **Android App**: Communicates exclusively with the Node.js backend via HTTPS REST/GraphQL. Stores NO secret keys.
- **Node.js Backend**: Validates JWTs, manages credits, and interfaces with AI providers using secure API keys. Connects to Supabase using the service_role key to perform administrative operations where necessary, but mostly relies on User JWTs for scoped RLS access.
- **Supabase**: Handles Auth, PostgreSQL Data, Storage, and Realtime events safely gated by RLS.
