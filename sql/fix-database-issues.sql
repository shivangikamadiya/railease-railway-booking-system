-- ============================================================================
-- RailEase Database Fix Script
-- This script removes the conflicting 'id' columns that were incorrectly 
-- added by the migration script and are causing JPA save operations to fail.
-- ============================================================================

USE railease_db;

-- ============================================================================
-- Fix 1: Remove conflicting 'id' column from trains table
-- ============================================================================
-- Check if the 'id' column exists in trains table
-- If it exists, we need to drop it (it's not the primary key, train_no is)

ALTER TABLE trains DROP COLUMN IF EXISTS id;

-- ============================================================================
-- Fix 2: Remove conflicting 'id' column from meals table  
-- ============================================================================
-- Check if the 'id' column exists in meals table
-- If it exists, we need to drop it (it's not the primary key, meal_id is)

ALTER TABLE meals DROP COLUMN IF EXISTS id;

-- ============================================================================
-- Fix 3: Remove conflicting 'id' column from user_profiles table
-- ============================================================================

ALTER TABLE user_profiles DROP COLUMN IF EXISTS id;

-- ============================================================================
-- Fix 4: Remove conflicting 'id' column from meal_orders table
-- ============================================================================

ALTER TABLE meal_orders DROP COLUMN IF EXISTS id;

-- ============================================================================
-- Fix 5: Remove conflicting 'id' column from train_meals table
-- ============================================================================

ALTER TABLE train_meals DROP COLUMN IF EXISTS id;

-- ============================================================================
-- Verify the changes
-- ============================================================================

SELECT 'Verifying trains table structure:' AS message;
DESCRIBE trains;

SELECT 'Verifying meals table structure:' AS message;
DESCRIBE meals;

SELECT 'Database fix completed successfully!' AS message;