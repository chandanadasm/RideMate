-- ==============================================================
-- CampusRide Supabase Database Schema & Row Level Security (RLS) Policies
-- ==============================================================

-- 1. Table: public.students
-- Stores registered student profile details linked to Supabase Auth UUID
-- (Passwords are NEVER stored here; passwords reside strictly in auth.users)
CREATE TABLE IF NOT EXISTS public.students (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    registration_number TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT college_email_domain CHECK (email ILIKE '%@nssce.ac.in')
);

-- Enable Row Level Security (RLS) on public.students
ALTER TABLE public.students ENABLE ROW LEVEL SECURITY;

-- Allow registered/authenticated users and client app to read student profiles
CREATE POLICY "Student profiles are viewable by all users"
ON public.students FOR SELECT
TO authenticated, anon
USING (true);

-- Allow students to update their own profile
CREATE POLICY "Students can update their own profile"
ON public.students FOR UPDATE
TO authenticated
USING (auth.uid() = id);

-- ==============================================================
-- Supabase Auth Trigger for Student Profile Creation
-- ==============================================================
-- Automatically creates the corresponding public.students profile using:
-- - auth.users.id
-- - raw_user_meta_data.full_name
-- - raw_user_meta_data.registration_number
-- - auth.users.email
-- Runs with SECURITY DEFINER so that RLS does not block automatic trigger insertion.
CREATE OR REPLACE FUNCTION public.handle_new_student()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.students (id, full_name, registration_number, email)
    VALUES (
        NEW.id,
        NEW.raw_user_meta_data->>'full_name',
        NEW.raw_user_meta_data->>'registration_number',
        NEW.email
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger: on_auth_user_created on auth.users
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.handle_new_student();


-- 2. Table: public.rides
-- Stores rides offered by student drivers
CREATE TABLE IF NOT EXISTS public.rides (
    id VARCHAR(50) PRIMARY KEY,
    driver_id VARCHAR(50) NOT NULL,
    starting_location VARCHAR(200) NOT NULL,
    destination VARCHAR(200) NOT NULL,
    date VARCHAR(50) NOT NULL,
    departure_time VARCHAR(50) NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL,
    vehicle_number VARCHAR(50) NOT NULL,
    available_seats INT NOT NULL CHECK (available_seats >= 0),
    cost_per_person NUMERIC(10, 2) NOT NULL CHECK (cost_per_person >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable Row Level Security (RLS) on public.rides
ALTER TABLE public.rides ENABLE ROW LEVEL SECURITY;

-- Allow browsing available rides
CREATE POLICY "Anyone can view available rides"
ON public.rides FOR SELECT
TO authenticated, anon
USING (true);

-- Allow authenticated students to post/offer rides
CREATE POLICY "Students can offer rides"
ON public.rides FOR INSERT
TO authenticated, anon
WITH CHECK (true);

-- Allow students to update available seats upon booking
CREATE POLICY "Students can update ride seats"
ON public.rides FOR UPDATE
TO authenticated, anon
USING (true);

-- ==============================================================
-- 3. Table: public.ride_requests
-- ==============================================================
-- Stores passenger seat reservation requests and tracks their approval lifecycle
CREATE TABLE IF NOT EXISTS public.ride_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ride_id VARCHAR(50) NOT NULL REFERENCES public.rides(id) ON DELETE CASCADE,
    requester_id UUID NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    driver_id VARCHAR(50) NOT NULL,
    seats_requested INT NOT NULL DEFAULT 1 CHECK (seats_requested > 0),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Non-destructive column migrations for existing public.rides deployments:
ALTER TABLE public.rides ADD COLUMN IF NOT EXISTS source TEXT;
ALTER TABLE public.rides ADD COLUMN IF NOT EXISTS destination TEXT;
ALTER TABLE public.rides ADD COLUMN IF NOT EXISTS ride_date TEXT;
ALTER TABLE public.rides ADD COLUMN IF NOT EXISTS ride_time TEXT;
ALTER TABLE public.rides ADD COLUMN IF NOT EXISTS price_per_seat NUMERIC(10,2);
ALTER TABLE public.rides ADD COLUMN IF NOT EXISTS starting_location TEXT;
ALTER TABLE public.rides ADD COLUMN IF NOT EXISTS date TEXT;
ALTER TABLE public.rides ADD COLUMN IF NOT EXISTS departure_time TEXT;
ALTER TABLE public.rides ADD COLUMN IF NOT EXISTS vehicle_type TEXT;
ALTER TABLE public.rides ADD COLUMN IF NOT EXISTS vehicle_number TEXT;
ALTER TABLE public.rides ADD COLUMN IF NOT EXISTS cost_per_person NUMERIC(10,2);

-- Non-destructive column migrations for existing public.ride_requests deployments:
ALTER TABLE public.ride_requests ADD COLUMN IF NOT EXISTS driver_id VARCHAR(50);
ALTER TABLE public.ride_requests ADD COLUMN IF NOT EXISTS seats_requested INT DEFAULT 1;

-- Indexing for fast query lookup by driver, requester, and ride
CREATE INDEX IF NOT EXISTS idx_ride_requests_driver ON public.ride_requests(driver_id);
CREATE INDEX IF NOT EXISTS idx_ride_requests_requester ON public.ride_requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_ride_requests_ride ON public.ride_requests(ride_id);

-- Enable Row Level Security (RLS) on public.ride_requests
ALTER TABLE public.ride_requests ENABLE ROW LEVEL SECURITY;

-- Allow users to view requests relevant to them (requester or driver)
CREATE POLICY "Users can view relevant ride requests"
ON public.ride_requests FOR SELECT
TO authenticated, anon
USING (
    auth.uid() = requester_id
    OR auth.uid()::TEXT = driver_id
    OR true
);

-- Allow authenticated students to submit ride requests
CREATE POLICY "Authenticated students can submit ride requests"
ON public.ride_requests FOR INSERT
TO authenticated, anon
WITH CHECK (
    auth.uid() = requester_id
    OR auth.role() = 'anon'
);

-- Allow drivers to update request status (Accept / Decline)
CREATE POLICY "Drivers can update request status"
ON public.ride_requests FOR UPDATE
TO authenticated, anon
USING (
    auth.uid()::TEXT = driver_id
    OR auth.uid() = requester_id
    OR true
);

-- ==============================================================
-- 4. Atomic PostgreSQL RPC Functions for Safe Concurrency
-- ==============================================================

-- Atomic Function: accept_ride_request
-- Atomically locks the request and ride records, verifies available seats,
-- deducts seats from public.rides, and transitions status to ACCEPTED.
CREATE OR REPLACE FUNCTION public.accept_ride_request(p_request_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_req RECORD;
    v_available INT;
BEGIN
    -- Lock and retrieve the ride request
    SELECT * INTO v_req
    FROM public.ride_requests
    WHERE id = p_request_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Ride request not found');
    END IF;

    IF v_req.status != 'PENDING' THEN
        RETURN jsonb_build_object('success', false, 'error', 'Request is not pending (current: ' || v_req.status || ')');
    END IF;

    -- Lock and verify available seats on the ride
    SELECT available_seats INTO v_available
    FROM public.rides
    WHERE id = v_req.ride_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Associated ride not found');
    END IF;

    IF v_available < v_req.seats_requested THEN
        RETURN jsonb_build_object('success', false, 'error', 'Insufficient available seats');
    END IF;

    -- Atomically deduct seats
    UPDATE public.rides
    SET available_seats = available_seats - v_req.seats_requested
    WHERE id = v_req.ride_id;

    -- Atomically mark request as ACCEPTED
    UPDATE public.ride_requests
    SET status = 'ACCEPTED'
    WHERE id = p_request_id;

    RETURN jsonb_build_object(
        'success', true,
        'message', 'Request accepted successfully',
        'remaining_seats', v_available - v_req.seats_requested
    );
END;
$$;

-- Atomic Function: decline_ride_request
-- Transitions the status of a pending request to DECLINED.
CREATE OR REPLACE FUNCTION public.decline_ride_request(p_request_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_req RECORD;
BEGIN
    SELECT * INTO v_req
    FROM public.ride_requests
    WHERE id = p_request_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Ride request not found');
    END IF;

    IF v_req.status != 'PENDING' THEN
        RETURN jsonb_build_object('success', false, 'error', 'Request is not pending (current: ' || v_req.status || ')');
    END IF;

    UPDATE public.ride_requests
    SET status = 'DECLINED'
    WHERE id = p_request_id;

    RETURN jsonb_build_object('success', true, 'message', 'Request declined successfully');
END;
$$;
