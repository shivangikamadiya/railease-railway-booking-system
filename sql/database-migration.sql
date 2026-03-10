-- ============================================================================
-- RailEase Database Migration Script
-- Run this script in MySQL Workbench to sync your database with the codebase
-- ============================================================================

USE railease_db;

-- ============================================================================
-- 1. TRAINS TABLE - Add missing columns
-- ============================================================================

ALTER TABLE trains 
ADD COLUMN IF NOT EXISTS available_seats INT DEFAULT 0 AFTER general_fare,
ADD COLUMN IF NOT EXISTS ticket_price DOUBLE DEFAULT 0 AFTER available_seats,
ADD COLUMN IF NOT EXISTS source VARCHAR(50) AFTER train_name,
ADD COLUMN IF NOT EXISTS destination VARCHAR(50) AFTER source,
ADD COLUMN IF NOT EXISTS travel_date DATE AFTER arrival_time,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER is_active,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
ADD COLUMN IF NOT EXISTS id BIGINT NOT NULL AFTER updated_at;

-- ============================================================================
-- 2. MEALS TABLE - Add missing columns
-- ============================================================================

ALTER TABLE meals 
ADD COLUMN IF NOT EXISTS preparation_time INT AFTER meal_type,
ADD COLUMN IF NOT EXISTS availability_status TINYINT(1) DEFAULT 1 AFTER preparation_time,
ADD COLUMN IF NOT EXISTS is_available TINYINT(1) DEFAULT 1 AFTER availability_status,
ADD COLUMN IF NOT EXISTS image_content_type VARCHAR(50) AFTER image,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER image_content_type,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
ADD COLUMN IF NOT EXISTS id BIGINT NOT NULL AFTER updated_at;

-- ============================================================================
-- 3. TICKETS TABLE - Add missing columns
-- ============================================================================

ALTER TABLE tickets 
ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500) AFTER cancellation_date,
ADD COLUMN IF NOT EXISTS refund_percentage DOUBLE AFTER refund_amount,
ADD COLUMN IF NOT EXISTS refund_status VARCHAR(20) AFTER refund_percentage,
ADD COLUMN IF NOT EXISTS refund_processed_date DATETIME AFTER refund_status,
ADD COLUMN IF NOT EXISTS cancellation_requested_date DATETIME AFTER refund_processed_date,
ADD COLUMN IF NOT EXISTS refund_date DATETIME AFTER cancellation_requested_date,
ADD COLUMN IF NOT EXISTS refund_transaction_id VARCHAR(50) AFTER refund_date,
ADD COLUMN IF NOT EXISTS cancellation_charges DOUBLE AFTER refund_transaction_id,
ADD COLUMN IF NOT EXISTS meal_id BIGINT AFTER cancellation_charges,
ADD COLUMN IF NOT EXISTS meal_quantity INT AFTER meal_id,
ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20) AFTER payment_id;

-- ============================================================================
-- 4. TRAIN_MEALS TABLE - Add missing column
-- ============================================================================

ALTER TABLE train_meals 
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER meal_id,
ADD COLUMN IF NOT EXISTS train_id BIGINT NOT NULL AFTER created_at;

-- ============================================================================
-- 5. USER_PROFILES TABLE - Add missing column (id)
-- ============================================================================

ALTER TABLE user_profiles 
ADD COLUMN IF NOT EXISTS id BIGINT NOT NULL AFTER occupation;

-- ============================================================================
-- 6. CREATE meal_orders TABLE (Missing)
-- ============================================================================

CREATE TABLE IF NOT EXISTS meal_orders (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    ticket_id VARCHAR(20) NOT NULL,
    meal_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    total_price DOUBLE NOT NULL,
    order_date DATETIME NOT NULL,
    delivery_status VARCHAR(20) NOT NULL,
    delivery_station VARCHAR(50),
    special_instructions VARCHAR(500),
    id BIGINT NOT NULL,
    cancellation_date DATETIME,
    cancellation_reason VARCHAR(500),
    refund_amount DOUBLE,
    refund_status VARCHAR(20),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE CASCADE,
    FOREIGN KEY (meal_id) REFERENCES meals(meal_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_ticket_id (ticket_id),
    INDEX idx_meal_id (meal_id),
    INDEX idx_order_date (order_date),
    INDEX idx_delivery_status (delivery_status)
);

-- ============================================================================
-- 7. CREATE password_reset_tokens TABLE (Missing)
-- ============================================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used TINYINT(1) DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expiry_date (expiry_date)
);

-- ============================================================================
-- 8. CREATE payments TABLE (Missing)
-- ============================================================================

CREATE TABLE IF NOT EXISTS payments (
    payment_id VARCHAR(20) PRIMARY KEY,
    ticket_id VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    amount DOUBLE NOT NULL,
    payment_method VARCHAR(20),
    payment_status VARCHAR(20),
    transaction_id VARCHAR(50),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_ticket_id (ticket_id),
    INDEX idx_user_id (user_id),
    INDEX idx_payment_status (payment_status)
);

-- ============================================================================
-- 9. UPDATE schema.sql to match (run this part separately if needed)
-- ============================================================================

-- This section is for updating the schema.sql file in the project
-- (Will be handled by code updates)

-- ============================================================================
-- VERIFICATION - Check if all tables exist
-- ============================================================================

SELECT 'Checking tables...' AS status;

SELECT TABLE_NAME 
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'railease_db'
ORDER BY TABLE_NAME;

-- ============================================================================
-- Display results
-- ============================================================================

SELECT 'Migration completed successfully!' AS message;

-- To verify the changes, run:
-- DESCRIBE trains;
-- DESCRIBE meals;
-- DESCRIBE tickets;
-- DESCRIBE meal_orders;
-- DESCRIBE password_reset_tokens;
-- DESCRIBE payments;