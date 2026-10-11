-- ==============================================================
-- CampusRide Minimal Non-Destructive Migration
-- ==============================================================
-- Adds pickup and dropoff locations to public.ride_requests,
-- ensures driver_id and seats_requested exist, and adds optional
-- notes to public.rides without altering or dropping existing data.
-- ==============================================================

-- 1. Ensure driver_id and seats_requested exist on public.ride_requests
ALTER TABLE public.ride_requests ADD COLUMN IF NOT EXISTS driver_id VARCHAR(50);
ALTER TABLE public.ride_requests ADD COLUMN IF NOT EXISTS seats_requested INT DEFAULT 1;

-- 2. Add rider pickup_location and dropoff_location to public.ride_requests
ALTER TABLE public.ride_requests ADD COLUMN IF NOT EXISTS pickup_location TEXT;
ALTER TABLE public.ride_requests ADD COLUMN IF NOT EXISTS dropoff_location TEXT;

-- 3. Add optional notes column to public.rides (if not already existing)
ALTER TABLE public.rides ADD COLUMN IF NOT EXISTS notes TEXT;

-- 4. Create performance indexes for fast query lookups
CREATE INDEX IF NOT EXISTS idx_ride_requests_driver ON public.ride_requests(driver_id);
CREATE INDEX IF NOT EXISTS idx_ride_requests_requester ON public.ride_requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_ride_requests_ride ON public.ride_requests(ride_id);
