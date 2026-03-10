-- ============================================================================
-- RailEase Database Schema
-- ============================================================================

-- Create database
CREATE DATABASE IF NOT EXISTS railease_db;
USE railease_db;

-- ============================================================================
-- Users table
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    mobile_number VARCHAR(10),
    role VARCHAR(20) NOT NULL,
    is_enabled BOOLEAN DEFAULT FALSE,
    profile_photo LONGBLOB,
    profile_photo_content_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    session_id VARCHAR(100),
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- ============================================================================
-- User profiles table
-- ============================================================================
CREATE TABLE IF NOT EXISTS user_profiles (
    profile_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NOT NULL,
    address VARCHAR(50),
    city VARCHAR(50),
    state VARCHAR(50),
    pincode VARCHAR(6),
    date_of_birth DATE,
    gender VARCHAR(10),
    occupation VARCHAR(100),
    id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ============================================================================
-- Trains table
-- ============================================================================
CREATE TABLE IF NOT EXISTS trains (
    train_no INT PRIMARY KEY,
    train_name VARCHAR(100) NOT NULL,
    source VARCHAR(50),
    source_station VARCHAR(50) NOT NULL,
    destination VARCHAR(50),
    destination_station VARCHAR(50) NOT NULL,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    travel_date DATE,
    available_seats INT DEFAULT 0,
    ticket_price DOUBLE DEFAULT 0,
    ac_seats INT DEFAULT 0,
    sleeper_seats INT DEFAULT 0,
    general_seats INT DEFAULT 0,
    ac_fare DOUBLE DEFAULT 0,
    sleeper_fare DOUBLE DEFAULT 0,
    general_fare DOUBLE DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    id BIGINT NOT NULL,
    INDEX idx_source (source_station),
    INDEX idx_destination (destination_station),
    INDEX idx_journey_date (travel_date)
);

-- ============================================================================
-- Meals table (E-Pantry)
-- ============================================================================
CREATE TABLE IF NOT EXISTS meals (
    meal_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    meal_name VARCHAR(100) NOT NULL,
    meal_type VARCHAR(50) NOT NULL,
    price DOUBLE NOT NULL,
    description VARCHAR(500),
    availability_status TINYINT(1) DEFAULT 1,
    is_available TINYINT(1) DEFAULT 1,
    image_url VARCHAR(255),
    image LONGBLOB,
    image_content_type VARCHAR(50),
    preparation_time INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    id BIGINT NOT NULL,
    INDEX idx_meal_type (meal_type)
);

-- ============================================================================
-- Train-Meals junction table
-- ============================================================================
CREATE TABLE IF NOT EXISTS train_meals (
    train_no INT,
    meal_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    train_id BIGINT NOT NULL,
    PRIMARY KEY (train_no, meal_id),
    FOREIGN KEY (train_no) REFERENCES trains(train_no) ON DELETE CASCADE,
    FOREIGN KEY (meal_id) REFERENCES meals(meal_id) ON DELETE CASCADE
);

-- ============================================================================
-- Tickets table
-- ============================================================================
CREATE TABLE IF NOT EXISTS tickets (
    ticket_id VARCHAR(20) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    train_no INT NOT NULL,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    journey_date DATE NOT NULL,
    source_station VARCHAR(50) NOT NULL,
    destination_station VARCHAR(50) NOT NULL,
    passenger_name VARCHAR(100) NOT NULL,
    passenger_age INT NOT NULL,
    passenger_gender VARCHAR(10) NOT NULL,
    class_type VARCHAR(20) NOT NULL,
    number_of_seats INT NOT NULL,
    total_fare DOUBLE NOT NULL,
    ticket_status VARCHAR(20) NOT NULL,
    booking_status VARCHAR(20) DEFAULT 'CONFIRMED',
    payment_status VARCHAR(20) DEFAULT 'PAID',
    payment_id VARCHAR(50),
    payment_method VARCHAR(20),
    cancellation_date TIMESTAMP NULL,
    cancellation_reason VARCHAR(500),
    refund_amount DOUBLE,
    refund_percentage DOUBLE,
    refund_status VARCHAR(20),
    refund_processed_date DATETIME,
    cancellation_requested_date DATETIME,
    refund_date DATETIME,
    refund_transaction_id VARCHAR(50),
    cancellation_charges DOUBLE,
    meal_id BIGINT,
    meal_quantity INT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (train_no) REFERENCES trains(train_no) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_train_no (train_no),
    INDEX idx_journey_date (journey_date),
    INDEX idx_ticket_status (ticket_status),
    INDEX idx_refund_status (refund_status)
);

-- ============================================================================
-- Meal Orders table
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
-- Verification tokens table
-- ============================================================================
CREATE TABLE IF NOT EXISTS verification_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expiry_date (expiry_date)
);

-- ============================================================================
-- Password reset tokens table
-- ============================================================================
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expiry_date (expiry_date)
);

-- ============================================================================
-- Payments table
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