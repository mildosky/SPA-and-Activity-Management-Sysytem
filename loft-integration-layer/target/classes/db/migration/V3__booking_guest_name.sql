-- V3: capture the guest display name on the booking itself.
-- Kiosk and webshop guests type their name at booking time; staff need
-- to see WHO booked in the admin bookings screen without a directory
-- lookup round-trip (and for walk-ins there often is no profile to look
-- up). Nullable so existing rows, staff-created bookings, and
-- Opera-sourced bookings are unaffected.
ALTER TABLE booking ADD COLUMN guest_name VARCHAR(255);
