-- Civic Connect: Full Database Reset Script
-- Drops and recreates all tables from scratch

DROP DATABASE IF EXISTS civics;
CREATE DATABASE civics;
USE civics;

-- 1. Users Table
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(100),
    reward_points INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Departments Table
CREATE TABLE departments (
    department_id INT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL,
    contact_email VARCHAR(150),
    contact_phone VARCHAR(20)
);

-- 3. Categories Table
CREATE TABLE categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    department_id INT,
    FOREIGN KEY (department_id) REFERENCES departments(department_id)
);

-- 4. Complaints Table
CREATE TABLE complaints (
    complaint_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    category_id INT,
    department_id INT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    image_url VARCHAR(500),
    status VARCHAR(50) DEFAULT 'Open',
    priority VARCHAR(20) DEFAULT 'Medium',
    escalation_level INT DEFAULT 0,
    is_verified BOOLEAN DEFAULT FALSE,
    assigned_officer VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (category_id) REFERENCES categories(category_id),
    FOREIGN KEY (department_id) REFERENCES departments(department_id)
);

-- 5. Services Table
CREATE TABLE services (
    service_id INT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(150) NOT NULL,
    category VARCHAR(50),
    fee DECIMAL(10,2) DEFAULT 0.00,
    description TEXT
);

-- 6. Service Applications Table
CREATE TABLE service_applications (
    application_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    service_id INT NOT NULL,
    status VARCHAR(50) DEFAULT 'Pending',
    application_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verification_date TIMESTAMP NULL,
    responsible_person VARCHAR(100),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (service_id) REFERENCES services(service_id)
);

-- =============================================
-- SEED DATA: Departments
-- =============================================
INSERT INTO departments (department_id, department_name, contact_email) VALUES
(1, 'Public Works', 'dreameranji527@gmail.com'),
(2, 'Water Board', 'dreameranji527@gmail.com'),
(3, 'Electrical Dept', 'dreameranji527@gmail.com'),
(4, 'Health & Sanitation', 'dreameranji527@gmail.com'),
(5, 'Lokayukta / Anti-Corruption', 'dreameranji527@gmail.com');

-- =============================================
-- SEED DATA: Categories
-- =============================================
INSERT INTO categories (category_id, category_name, department_id) VALUES
(1, 'Roads & Transport', 1),
(2, 'Water Supply', 2),
(3, 'Street Lights', 3),
(4, 'Sanitation & Garbage', 4),
(5, 'Electricity', 3),
(6, 'Corruption / Bribe', 5);

-- =============================================
-- SEED DATA: Services
-- =============================================
INSERT INTO services (service_name, category, fee, description) VALUES
('Income Certificate', 'Income', 25.00, 'Apply for an income certificate from the revenue department.'),
('Caste Certificate', 'Caste', 25.00, 'Apply for a caste certificate for reservation benefits.'),
('Residence Certificate', 'Residence', 20.00, 'Apply for a residence/domicile certificate.'),
('Birth Certificate', 'Other', 0.00, 'Apply for a birth certificate.'),
('Death Certificate', 'Other', 0.00, 'Apply for a death certificate.'),
('Property Tax Payment', 'Other', 0.00, 'Pay property tax online.');

SELECT 'Database reset complete! All tables created and seeded.' AS status;
