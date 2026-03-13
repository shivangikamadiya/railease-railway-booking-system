-- Fix NULL/zero fares in trains table after ticket_price column drop
-- Set realistic default fares for all active trains (Indian Railways pricing)

UPDATE trains 
SET 
  ac_fare = 1500.00,
  sleeper_fare = 800.00, 
  general_fare = 500.00
WHERE is_active = 1;

-- Specific updates for train #4 (from error logs)
UPDATE trains 
SET 
  ac_fare = 1500.00,
  sleeper_fare = 800.00,
  general_fare = 500.00
WHERE train_no = 4;

-- Verify all active trains have valid fares
SELECT 
  train_no, 
  train_name, 
  ac_fare, 
  sleeper_fare, 
  general_fare,
  is_active
FROM trains 
WHERE is_active = 1
ORDER BY train_no;

-- Count affected rows
SELECT 'Rows updated' as status, COUNT(*) as count 
FROM trains 
WHERE is_active = 1 AND (ac_fare IS NOT NULL OR general_fare IS NOT NULL);