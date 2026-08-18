-- Hospital Management System Database Schema & Seed Data
DROP DATABASE IF EXISTS hospital_db;
CREATE DATABASE hospital_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital_db;

-- 1. Users Table
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    role ENUM('ADMIN', 'DOCTOR', 'PATIENT', 'RECEPTIONIST') NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Departments Table
CREATE TABLE departments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dept_code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    head_doctor_name VARCHAR(100),
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. Doctors Table
CREATE TABLE doctors (
    id INT AUTO_INCREMENT PRIMARY KEY,
    doctor_code VARCHAR(20) NOT NULL UNIQUE,
    user_id INT UNIQUE,
    dept_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    dob DATE,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    qualification VARCHAR(100) NOT NULL,
    experience_years INT DEFAULT 0,
    consultation_fee DECIMAL(10, 2) NOT NULL DEFAULT 500.00,
    room_no VARCHAR(20),
    license_no VARCHAR(50) UNIQUE,
    status ENUM('ACTIVE', 'ON_LEAVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (dept_id) REFERENCES departments(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 4. Doctor Availability Configuration Table
CREATE TABLE doctor_availability (
    id INT AUTO_INCREMENT PRIMARY KEY,
    doctor_id INT NOT NULL,
    available_days VARCHAR(100) NOT NULL DEFAULT 'Monday,Tuesday,Wednesday,Thursday,Friday',
    start_time TIME NOT NULL DEFAULT '09:00:00',
    end_time TIME NOT NULL DEFAULT '17:00:00',
    break_start TIME DEFAULT '13:00:00',
    break_end TIME DEFAULT '14:00:00',
    slot_duration_mins INT NOT NULL DEFAULT 30,
    max_appointments INT NOT NULL DEFAULT 16,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. Patients Table
CREATE TABLE patients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_code VARCHAR(20) NOT NULL UNIQUE,
    user_id INT UNIQUE,
    name VARCHAR(100) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    dob DATE,
    age INT,
    blood_group VARCHAR(10),
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    address TEXT,
    emergency_contact VARCHAR(100),
    medical_history_summary TEXT,
    allergies TEXT,
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 6. Appointments Table
CREATE TABLE appointments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    appointment_code VARCHAR(20) NOT NULL UNIQUE,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    dept_id INT NOT NULL,
    appointment_date DATE NOT NULL,
    time_slot VARCHAR(20) NOT NULL,
    reason TEXT,
    status ENUM('Pending', 'Confirmed', 'Checked-In', 'In Consultation', 'Completed', 'Cancelled', 'No Show') DEFAULT 'Pending',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    FOREIGN KEY (dept_id) REFERENCES departments(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 7. Medical Histories Table
CREATE TABLE medical_histories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    appointment_id INT,
    doctor_id INT NOT NULL,
    visit_date DATE NOT NULL,
    symptoms TEXT,
    diagnosis TEXT NOT NULL,
    treatment_plan TEXT,
    bp VARCHAR(20),
    heart_rate INT,
    temp_c DECIMAL(4,1),
    oxygen_sat INT,
    weight_kg DECIMAL(5,2),
    height_cm DECIMAL(5,2),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 8. Prescriptions Table
CREATE TABLE prescriptions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    prescription_code VARCHAR(20) NOT NULL UNIQUE,
    appointment_id INT,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    visit_date DATE NOT NULL,
    diagnosis TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 9. Prescription Items Table
CREATE TABLE prescription_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    prescription_id INT NOT NULL,
    medicine_name VARCHAR(100) NOT NULL,
    dosage VARCHAR(50) NOT NULL,
    frequency VARCHAR(50) NOT NULL,
    duration VARCHAR(50) NOT NULL,
    instructions TEXT,
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 10. Lab Tests Table
CREATE TABLE lab_tests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    test_code VARCHAR(20) NOT NULL UNIQUE,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    appointment_id INT,
    test_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    sample_type VARCHAR(50),
    test_date DATE NOT NULL,
    status ENUM('Requested', 'Sample Collected', 'Processing', 'Completed') DEFAULT 'Requested',
    lab_technician VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 11. Lab Results Table
CREATE TABLE lab_results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    lab_test_id INT NOT NULL UNIQUE,
    result_value TEXT NOT NULL,
    reference_range VARCHAR(100),
    remarks TEXT,
    result_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lab_test_id) REFERENCES lab_tests(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 12. Bills Table
CREATE TABLE bills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_code VARCHAR(20) NOT NULL UNIQUE,
    patient_id INT NOT NULL,
    appointment_id INT,
    consultation_fee DECIMAL(10, 2) DEFAULT 0.00,
    lab_charges DECIMAL(10, 2) DEFAULT 0.00,
    medicine_charges DECIMAL(10, 2) DEFAULT 0.00,
    room_charges DECIMAL(10, 2) DEFAULT 0.00,
    other_charges DECIMAL(10, 2) DEFAULT 0.00,
    discount DECIMAL(10, 2) DEFAULT 0.00,
    tax_amount DECIMAL(10, 2) DEFAULT 0.00,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    paid_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    remaining_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    payment_status ENUM('Pending', 'Partially Paid', 'Paid', 'Cancelled') DEFAULT 'Pending',
    invoice_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 13. Payments Table
CREATE TABLE payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    bill_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_mode ENUM('Cash', 'Card', 'UPI', 'Insurance', 'NetBanking') NOT NULL,
    transaction_ref VARCHAR(100),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 14. Notifications Table
CREATE TABLE notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 15. Audit Logs Table
CREATE TABLE audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ========================================================
-- SEED DATA
-- ========================================================

-- Password hashes (Salt: "HOSPITAL_SALT_2026")
-- admin123 -> SHA-256("HOSPITAL_SALT_2026admin123") = "d572776c53fa65239e2467d02516428c9a3bb92db01cfd74659b977be415f340"
-- doc123   -> SHA-256("HOSPITAL_SALT_2026doc123")   = "fcf0a473f1d8ef3f24bf7c541764658145bd9ce64df5a0248ad67b36f1ca8575"
-- pat123   -> SHA-256("HOSPITAL_SALT_2026pat123")   = "6cf83d2ff98be5c3b9b47e224e75d50bd86756858a74ec4eb119dfcb8ef8a42f"
-- rec123   -> SHA-256("HOSPITAL_SALT_2026rec123")   = "04b5033c467a9cf5ff81525bb16611fbbe32f65a1ea0aa1ee87f549a1d954605"

INSERT INTO users (id, username, password_hash, salt, role, email, phone, status) VALUES
(1, 'admin', 'd572776c53fa65239e2467d02516428c9a3bb92db01cfd74659b977be415f340', 'HOSPITAL_SALT_2026', 'ADMIN', 'admin@hospital.com', '9876543210', 'ACTIVE'),
(2, 'dr_sharma', 'fcf0a473f1d8ef3f24bf7c541764658145bd9ce64df5a0248ad67b36f1ca8575', 'HOSPITAL_SALT_2026', 'DOCTOR', 'sharma@hospital.com', '9876543211', 'ACTIVE'),
(3, 'dr_ananya', 'fcf0a473f1d8ef3f24bf7c541764658145bd9ce64df5a0248ad67b36f1ca8575', 'HOSPITAL_SALT_2026', 'DOCTOR', 'ananya@hospital.com', '9876543212', 'ACTIVE'),
(4, 'dr_vikram', 'fcf0a473f1d8ef3f24bf7c541764658145bd9ce64df5a0248ad67b36f1ca8575', 'HOSPITAL_SALT_2026', 'DOCTOR', 'vikram@hospital.com', '9876543213', 'ACTIVE'),
(5, 'patient1', '6cf83d2ff98be5c3b9b47e224e75d50bd86756858a74ec4eb119dfcb8ef8a42f', 'HOSPITAL_SALT_2026', 'PATIENT', 'john.doe@gmail.com', '9811122233', 'ACTIVE'),
(6, 'patient2', '6cf83d2ff98be5c3b9b47e224e75d50bd86756858a74ec4eb119dfcb8ef8a42f', 'HOSPITAL_SALT_2026', 'PATIENT', 'priya.singh@gmail.com', '9822233344', 'ACTIVE'),
(7, 'receptionist1', '04b5033c467a9cf5ff81525bb16611fbbe32f65a1ea0aa1ee87f549a1d954605', 'HOSPITAL_SALT_2026', 'RECEPTIONIST', 'frontdesk@hospital.com', '9899988877', 'ACTIVE');

INSERT INTO departments (id, dept_code, name, description, head_doctor_name, status) VALUES
(1, 'DEP-CARD', 'Cardiology', 'Comprehensive heart care and cardiovascular surgical procedures.', 'Dr. Rajesh Sharma', 'ACTIVE'),
(2, 'DEP-NEUR', 'Neurology', 'Brain, spinal cord, and nerve diagnosis and rehabilitation.', 'Dr. Ananya Roy', 'ACTIVE'),
(3, 'DEP-ORTH', 'Orthopedics', 'Bone, joint, spine and sports injury specialized care.', 'Dr. Vikram Patel', 'ACTIVE'),
(4, 'DEP-PED', 'Pediatrics', 'Childhood healthcare, infant wellness, and pediatric therapy.', 'Dr. Sunita Gupta', 'ACTIVE'),
(5, 'DEP-GEN', 'General Medicine', 'Primary care, diagnostic assessments, and general health management.', 'Dr. Rajesh Sharma', 'ACTIVE'),
(6, 'DEP-ENT', 'ENT', 'Ear, Nose, Throat, and Head/Neck consultation and treatment.', 'Dr. Ramesh Kumar', 'ACTIVE');

INSERT INTO doctors (id, doctor_code, user_id, dept_id, name, gender, dob, phone, email, specialization, qualification, experience_years, consultation_fee, room_no, license_no, status) VALUES
(1, 'DOC00001', 2, 1, 'Dr. Rajesh Sharma', 'Male', '1980-05-15', '9876543211', 'sharma@hospital.com', 'Cardiologist', 'MD, DM (Cardiology)', 15, 800.00, 'Cabin 101', 'LIC-IND-90812', 'ACTIVE'),
(2, 'DOC00002', 3, 2, 'Dr. Ananya Roy', 'Female', '1984-08-22', '9876543212', 'ananya@hospital.com', 'Neurologist', 'MD, DNB (Neurology)', 12, 900.00, 'Cabin 204', 'LIC-IND-88123', 'ACTIVE'),
(3, 'DOC00003', 4, 3, 'Dr. Vikram Patel', 'Male', '1978-11-03', '9876543213', 'vikram@hospital.com', 'Orthopedic', 'MS (Orthopedics)', 18, 750.00, 'Cabin 305', 'LIC-IND-77341', 'ACTIVE');

INSERT INTO doctor_availability (id, doctor_id, available_days, start_time, end_time, break_start, break_end, slot_duration_mins, max_appointments) VALUES
(1, 1, 'Monday,Tuesday,Wednesday,Thursday,Friday', '09:00:00', '17:00:00', '13:00:00', '14:00:00', 30, 16),
(2, 2, 'Monday,Wednesday,Friday', '10:00:00', '18:00:00', '13:30:00', '14:30:00', 30, 14),
(3, 3, 'Tuesday,Thursday,Saturday', '08:30:00', '16:30:00', '12:30:00', '13:30:00', 30, 16);

INSERT INTO patients (id, patient_code, user_id, name, gender, dob, age, blood_group, phone, email, address, emergency_contact, medical_history_summary, allergies, registration_date) VALUES
(1, 'PAT00001', 5, 'John Doe', 'Male', '1990-04-12', 36, 'O+', '9811122233', 'john.doe@gmail.com', '42 Park Avenue, Sector 15, New Delhi', 'Wife: Jane Doe (9811122234)', 'Mild Hypertension diagnosed in 2024', 'Penicillin', '2026-01-10 10:00:00'),
(2, 'PAT00002', 6, 'Priya Singh', 'Female', '1995-09-25', 30, 'B+', '9822233344', 'priya.singh@gmail.com', '108 Metro Green, Indiranagar, Bangalore', 'Father: R.P. Singh (9822233345)', 'No prior chronic condition', 'Dust, Dust Mites', '2026-02-01 11:30:00');

INSERT INTO appointments (id, appointment_code, patient_id, doctor_id, dept_id, appointment_date, time_slot, reason, status, notes) VALUES
(1, 'APT00001', 1, 1, 1, CURRENT_DATE(), '10:00 - 10:30', 'Routine Cardiac Checkup & Chest Tightness', 'Confirmed', 'Patient asked for ECG test'),
(2, 'APT00002', 2, 2, 2, CURRENT_DATE(), '11:00 - 11:30', 'Persistent Migraine and dizziness', 'Completed', 'Consultation finished, prescribed medication');

INSERT INTO medical_histories (id, patient_id, appointment_id, doctor_id, visit_date, symptoms, diagnosis, treatment_plan, bp, heart_rate, temp_c, oxygen_sat, weight_kg, height_cm, notes) VALUES
(1, 2, 2, 2, CURRENT_DATE(), 'Chronic left-side headache, light sensitivity', 'Vascular Migraine Stage 1', 'Hydration, Stress reduction, oral triptan course', '120/80', 74, 36.8, 99, 62.0, 165.0, 'Advised follow-up after 2 weeks');

INSERT INTO prescriptions (id, prescription_code, appointment_id, patient_id, doctor_id, visit_date, diagnosis, notes) VALUES
(1, 'PRE00001', 2, 2, 2, CURRENT_DATE(), 'Vascular Migraine', 'Take medicines after food with plenty of water.');

INSERT INTO prescription_items (id, prescription_id, medicine_name, dosage, frequency, duration, instructions) VALUES
(1, 1, 'Paracetamol', '500mg', '1-0-1', '5 days', 'After meal'),
(2, 1, 'Sumatriptan', '50mg', '1-0-0 (as needed)', '3 days', 'At onset of aura/headache');

INSERT INTO lab_tests (id, test_code, patient_id, doctor_id, appointment_id, test_name, category, sample_type, test_date, status, lab_technician) VALUES
(1, 'LAB00001', 1, 1, 1, 'Complete Blood Count (CBC)', 'Haematology', 'Blood', CURRENT_DATE(), 'Completed', 'Tech. Robert Vance'),
(2, 'LAB00002', 2, 2, 2, 'Brain MRI Scan', 'Radiology', 'Imaging', CURRENT_DATE(), 'Processing', 'Tech. Lisa Ray');

INSERT INTO lab_results (id, lab_test_id, result_value, reference_range, remarks) VALUES
(1, 1, 'Hemoglobin: 14.5 g/dL, WBC: 7,200 /mcL, Platelets: 250,000 /mcL', 'Hb: 13.5-17.5 g/dL, WBC: 4500-11000', 'All blood counts are within normal physiological range.');

INSERT INTO bills (id, invoice_code, patient_id, appointment_id, consultation_fee, lab_charges, medicine_charges, room_charges, other_charges, discount, tax_amount, total_amount, paid_amount, remaining_amount, payment_status, invoice_date) VALUES
(1, 'INV00001', 2, 2, 900.00, 1500.00, 350.00, 0.00, 0.00, 100.00, 132.50, 2682.50, 2682.50, 0.00, 'Paid', CURRENT_DATE());

INSERT INTO payments (id, bill_id, amount, payment_mode, transaction_ref) VALUES
(1, 1, 2682.50, 'Card', 'TXN-99881122');

INSERT INTO notifications (id, user_id, title, message, is_read) VALUES
(1, 5, 'Appointment Confirmed', 'Your appointment APT00001 with Dr. Rajesh Sharma is confirmed for today.', FALSE),
(2, 6, 'Prescription Created', 'Dr. Ananya Roy created prescription PRE00001 for your visit.', TRUE),
(3, 1, 'New Registration', 'Patient Priya Singh registered in system.', TRUE);
