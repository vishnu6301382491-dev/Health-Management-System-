CREATE DATABASE IF NOT EXISTS healthcare_platform_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE healthcare_platform_db;

DROP TABLE IF EXISTS doctor_fee_history, doctor_fees, patient_medications, patient_allergies, patient_health_conditions, patient_vitals, fee_history, fee_rules, sub_specializations, specializations, cities, states, settings, audit_logs, staff, notifications, reviews, blood_bank, ambulance_requests, ambulances, insurance_claims, insurance, payments, bills, pharmacy_inventory, medicines, lab_results, lab_tests, prescription_items, prescriptions, vital_signs, medical_records, appointments, patients, doctor_availability, doctor_hospitals, doctors, departments, hospital_branches, hospitals, users;

-- 0. Reference Tables
CREATE TABLE states (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(10) NOT NULL UNIQUE,
    region VARCHAR(50) DEFAULT 'South'
) ENGINE=InnoDB;

CREATE TABLE cities (
    id INT AUTO_INCREMENT PRIMARY KEY,
    state_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    tier ENUM('Metro', 'Tier-1', 'Tier-2', 'Tier-3') DEFAULT 'Tier-2',
    fee_multiplier DECIMAL(4,2) DEFAULT 1.00,
    FOREIGN KEY (state_id) REFERENCES states(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE specializations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) DEFAULT 'Clinical',
    min_recommended_fee DECIMAL(10,2) DEFAULT 300.00,
    max_recommended_fee DECIMAL(10,2) DEFAULT 3000.00
) ENGINE=InnoDB;

CREATE TABLE sub_specializations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    spec_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    FOREIGN KEY (spec_id) REFERENCES specializations(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 1. Users Table
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    role ENUM('SUPER_ADMIN', 'HOSPITAL_ADMIN', 'DOCTOR', 'RECEPTIONIST', 'LAB_TECHNICIAN', 'PHARMACIST', 'PATIENT') NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    hospital_id INT,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Hospitals Table
CREATE TABLE hospitals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    hospital_code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    type ENUM('Government', 'Private', 'Multi-Speciality', 'Super-Speciality', 'Clinic', 'Diagnostic Centre', 'Dental Hospital', 'Eye Hospital', 'Children Hospital', 'Maternity Hospital', 'Orthopedic Centre', 'Cancer Centre', 'Cardiac Centre') NOT NULL,
    description TEXT,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    emergency_phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    website VARCHAR(150),
    google_maps_url VARCHAR(255),
    image_url VARCHAR(255),
    opening_hours VARCHAR(100) DEFAULT '24/7 Open',
    total_beds INT DEFAULT 100,
    icu_beds INT DEFAULT 20,
    pharmacy_avail BOOLEAN DEFAULT TRUE,
    lab_avail BOOLEAN DEFAULT TRUE,
    blood_bank_avail BOOLEAN DEFAULT TRUE,
    ambulance_avail BOOLEAN DEFAULT TRUE,
    insurance_support BOOLEAN DEFAULT TRUE,
    rating DECIMAL(3,2) DEFAULT 4.5,
    review_count INT DEFAULT 0,
    established_year INT DEFAULT 2005,
    status ENUM('ACTIVE', 'PENDING_VERIFICATION', 'INACTIVE') DEFAULT 'ACTIVE',
    data_source VARCHAR(20) DEFAULT 'DEMO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_hosp_city (city),
    INDEX idx_hosp_type (type),
    INDEX idx_hosp_rating (rating)
) ENGINE=InnoDB;

-- 3. Hospital Branches Table
CREATE TABLE hospital_branches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    branch_code VARCHAR(20) NOT NULL UNIQUE,
    hospital_id INT NOT NULL,
    branch_name VARCHAR(150) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    emergency_phone VARCHAR(20) NOT NULL,
    working_hours VARCHAR(100) DEFAULT '24/7 Open',
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    INDEX idx_branch_hosp (hospital_id)
) ENGINE=InnoDB;

-- 4. Departments Table
CREATE TABLE departments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dept_code VARCHAR(20) NOT NULL,
    hospital_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    INDEX idx_dept_hosp (hospital_id)
) ENGINE=InnoDB;

-- 5. Doctors Table
CREATE TABLE doctors (
    id INT AUTO_INCREMENT PRIMARY KEY,
    doctor_code VARCHAR(20) NOT NULL UNIQUE,
    user_id INT UNIQUE,
    name VARCHAR(100) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    dob DATE,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    sub_specialization VARCHAR(100),
    qualification VARCHAR(100) NOT NULL,
    experience_years INT DEFAULT 0,
    consultation_fee DECIMAL(10, 2) NOT NULL DEFAULT 600.00,
    followup_fee DECIMAL(10, 2) DEFAULT 400.00,
    video_consultation_fee DECIMAL(10, 2) DEFAULT 500.00,
    emergency_consultation_fee DECIMAL(10, 2) DEFAULT 1200.00,
    city VARCHAR(100) DEFAULT 'Hyderabad',
    state VARCHAR(100) DEFAULT 'Telangana',
    languages VARCHAR(150) DEFAULT 'English, Hindi',
    license_no VARCHAR(50) UNIQUE,
    rating DECIMAL(3,2) DEFAULT 4.8,
    review_count INT DEFAULT 0,
    bio TEXT,
    image_url VARCHAR(255),
    verification_status ENUM('Verified', 'Pending', 'Rejected', 'DEMO') DEFAULT 'DEMO',
    status ENUM('ACTIVE', 'ON_LEAVE', 'INACTIVE') DEFAULT 'ACTIVE',
    data_source VARCHAR(20) DEFAULT 'DEMO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_doc_spec_city (specialization, city, rating),
    INDEX idx_doc_fee (consultation_fee),
    INDEX idx_doc_rating (rating, experience_years),
    INDEX idx_doc_status (status)
) ENGINE=InnoDB;

-- 6. Doctor Hospitals Mapping Table
CREATE TABLE doctor_hospitals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    doctor_id INT NOT NULL,
    hospital_id INT NOT NULL,
    branch_id INT NOT NULL,
    dept_id INT NOT NULL,
    room_no VARCHAR(20),
    consultation_fee DECIMAL(10, 2),
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    FOREIGN KEY (branch_id) REFERENCES hospital_branches(id) ON DELETE CASCADE,
    FOREIGN KEY (dept_id) REFERENCES departments(id) ON DELETE CASCADE,
    INDEX idx_dh_doc (doctor_id),
    INDEX idx_dh_hosp (hospital_id)
) ENGINE=InnoDB;

-- 7. Doctor Availability Table
CREATE TABLE doctor_availability (
    id INT AUTO_INCREMENT PRIMARY KEY,
    doctor_id INT NOT NULL,
    hospital_id INT NOT NULL,
    branch_id INT NOT NULL,
    available_days VARCHAR(100) NOT NULL DEFAULT 'Monday,Tuesday,Wednesday,Thursday,Friday',
    start_time TIME NOT NULL DEFAULT '09:00:00',
    end_time TIME NOT NULL DEFAULT '17:00:00',
    break_start TIME DEFAULT '13:00:00',
    break_end TIME DEFAULT '14:00:00',
    slot_duration_mins INT NOT NULL DEFAULT 30,
    max_appointments INT NOT NULL DEFAULT 16,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    FOREIGN KEY (branch_id) REFERENCES hospital_branches(id) ON DELETE CASCADE,
    INDEX idx_da_doc (doctor_id, hospital_id)
) ENGINE=InnoDB;

-- 8. Doctor Fees Table
CREATE TABLE doctor_fees (
    fee_id INT AUTO_INCREMENT PRIMARY KEY,
    doctor_id INT NOT NULL,
    hospital_id INT NOT NULL,
    branch_id INT,
    appointment_type VARCHAR(50) NOT NULL DEFAULT 'In-Person',
    base_fee DECIMAL(10,2) NOT NULL DEFAULT 650.00,
    tax_percentage DECIMAL(5,2) DEFAULT 5.00,
    service_charge DECIMAL(10,2) DEFAULT 0.00,
    discount DECIMAL(10,2) DEFAULT 0.00,
    currency VARCHAR(10) DEFAULT 'INR',
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE doctor_fee_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    fee_id INT,
    doctor_id INT NOT NULL,
    hospital_id INT,
    old_fee DECIMAL(10,2),
    new_fee DECIMAL(10,2) NOT NULL,
    changed_by VARCHAR(50) DEFAULT 'ADMIN',
    change_reason VARCHAR(255) DEFAULT 'Fee structure update',
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 9. Patients Table (Enhanced Patient Info)
CREATE TABLE patients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_code VARCHAR(20) NOT NULL UNIQUE,
    user_id INT UNIQUE,
    first_name VARCHAR(50),
    middle_name VARCHAR(50),
    last_name VARCHAR(50),
    name VARCHAR(100) NOT NULL,
    gender VARCHAR(20) NOT NULL DEFAULT 'Male',
    dob DATE,
    age INT,
    blood_group VARCHAR(10) DEFAULT 'O+',
    phone VARCHAR(20) NOT NULL,
    alternate_phone VARCHAR(20),
    email VARCHAR(100),
    door_no VARCHAR(50),
    street VARCHAR(100),
    locality VARCHAR(100),
    address TEXT,
    city VARCHAR(100) DEFAULT 'Hyderabad',
    state VARCHAR(100) DEFAULT 'Telangana',
    pincode VARCHAR(20),
    height_cm DECIMAL(5,2) DEFAULT 170.00,
    weight_kg DECIMAL(5,2) DEFAULT 65.00,
    emergency_contact_name VARCHAR(100),
    emergency_contact_relationship VARCHAR(50),
    emergency_contact_phone VARCHAR(20),
    medical_history_summary TEXT,
    allergies TEXT,
    insurance_provider VARCHAR(100),
    policy_no VARCHAR(50),
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_pat_code (patient_code),
    INDEX idx_pat_phone (phone)
) ENGINE=InnoDB;

-- 10. Patient Vitals Table
CREATE TABLE patient_vitals (
    vital_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    appointment_id INT,
    height_cm DECIMAL(5,2),
    weight_kg DECIMAL(5,2),
    systolic_bp INT,
    diastolic_bp INT,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    recorded_by VARCHAR(50) DEFAULT 'PATIENT',
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 11. Patient Health Conditions Table
CREATE TABLE patient_health_conditions (
    condition_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    condition_type VARCHAR(50),
    condition_name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 12. Patient Allergies Table
CREATE TABLE patient_allergies (
    allergy_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    allergy_type VARCHAR(50),
    allergy_name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 13. Patient Medications Table
CREATE TABLE patient_medications (
    medication_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    medicine_name VARCHAR(100) NOT NULL,
    dosage VARCHAR(50),
    frequency VARCHAR(50),
    duration VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 14. Appointments Table (Enhanced with Vitals & Fee Breakdown)
CREATE TABLE appointments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    appointment_code VARCHAR(20) NOT NULL UNIQUE,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    hospital_id INT NOT NULL,
    branch_id INT NOT NULL,
    dept_id INT NOT NULL,
    appointment_date DATE NOT NULL,
    time_slot VARCHAR(30) NOT NULL,
    appointment_type ENUM('In-Person', 'Video Consultation', 'Follow-Up', 'Emergency', 'Home Visit') DEFAULT 'In-Person',
    health_problem_type VARCHAR(100) DEFAULT 'General Check-up',
    symptoms TEXT,
    problem_description TEXT,
    is_emergency BOOLEAN DEFAULT FALSE,
    reason TEXT,
    base_fee DECIMAL(10, 2) DEFAULT 650.00,
    service_charge DECIMAL(10, 2) DEFAULT 0.00,
    discount DECIMAL(10, 2) DEFAULT 0.00,
    tax_amount DECIMAL(10, 2) DEFAULT 0.00,
    total_amount DECIMAL(10, 2) DEFAULT 650.00,
    payment_status ENUM('Pending', 'Paid', 'Partially Paid', 'Failed', 'Refunded') DEFAULT 'Pending',
    status ENUM('Pending', 'Confirmed', 'Checked-In', 'In Consultation', 'Completed', 'Cancelled', 'Rescheduled', 'No Show') DEFAULT 'Pending',
    notes TEXT,
    video_link VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    FOREIGN KEY (branch_id) REFERENCES hospital_branches(id) ON DELETE CASCADE,
    FOREIGN KEY (dept_id) REFERENCES departments(id) ON DELETE RESTRICT,
    INDEX idx_apt_patient (patient_id),
    INDEX idx_apt_doc_date (doctor_id, appointment_date)
) ENGINE=InnoDB;

-- 15. Medical Records Table
CREATE TABLE medical_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    appointment_id INT,
    doctor_id INT NOT NULL,
    hospital_id INT NOT NULL,
    visit_date DATE NOT NULL,
    symptoms TEXT,
    diagnosis TEXT NOT NULL,
    treatment_plan TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 16. Vital Signs Table
CREATE TABLE vital_signs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    medical_record_id INT NOT NULL UNIQUE,
    bp VARCHAR(20),
    heart_rate INT,
    temp_c DECIMAL(4,1),
    oxygen_sat INT,
    weight_kg DECIMAL(5,2),
    height_cm DECIMAL(5,2),
    FOREIGN KEY (medical_record_id) REFERENCES medical_records(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 17. Prescriptions Table
CREATE TABLE prescriptions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    prescription_code VARCHAR(20) NOT NULL UNIQUE,
    appointment_id INT,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    hospital_id INT NOT NULL,
    visit_date DATE NOT NULL,
    diagnosis TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 18. Prescription Items Table
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

-- 19. Lab Tests Table
CREATE TABLE lab_tests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    test_code VARCHAR(20) NOT NULL UNIQUE,
    patient_id INT NOT NULL,
    doctor_id INT NOT NULL,
    hospital_id INT NOT NULL,
    appointment_id INT,
    test_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    sample_type VARCHAR(50),
    test_date DATE NOT NULL,
    status ENUM('Requested', 'Sample Collected', 'Processing', 'Completed', 'Cancelled') DEFAULT 'Requested',
    lab_technician VARCHAR(100),
    price DECIMAL(10, 2) DEFAULT 500.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 20. Lab Results Table
CREATE TABLE lab_results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    lab_test_id INT NOT NULL UNIQUE,
    result_value TEXT NOT NULL,
    reference_range VARCHAR(100),
    remarks TEXT,
    result_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lab_test_id) REFERENCES lab_tests(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 21. Medicines Catalog Table
CREATE TABLE medicines (
    id INT AUTO_INCREMENT PRIMARY KEY,
    medicine_code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    manufacturer VARCHAR(100),
    batch_number VARCHAR(50) DEFAULT 'BATCH-2026-A',
    expiry_date DATE,
    unit_price DECIMAL(10, 2) NOT NULL DEFAULT 50.00,
    requires_prescription BOOLEAN DEFAULT TRUE,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE'
) ENGINE=InnoDB;

-- 22. Pharmacy Inventory Table
CREATE TABLE pharmacy_inventory (
    id INT AUTO_INCREMENT PRIMARY KEY,
    hospital_id INT NOT NULL,
    medicine_id INT NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 100,
    reorder_level INT DEFAULT 20,
    expiry_date DATE,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 23. Bills Table
CREATE TABLE bills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_code VARCHAR(20) NOT NULL UNIQUE,
    patient_id INT NOT NULL,
    hospital_id INT NOT NULL,
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
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 24. Payments Table
CREATE TABLE payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    bill_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_mode ENUM('Cash', 'Card', 'UPI', 'Insurance', 'NetBanking') NOT NULL,
    transaction_ref VARCHAR(100),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 25. Insurance Table
CREATE TABLE insurance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    policy_number VARCHAR(50) NOT NULL,
    policy_type VARCHAR(50) NOT NULL,
    coverage_amount DECIMAL(12, 2) NOT NULL,
    claim_status ENUM('Active', 'Expired', 'Claim Pending', 'Claim Approved') DEFAULT 'Active',
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 26. Insurance Claims Table
CREATE TABLE insurance_claims (
    id INT AUTO_INCREMENT PRIMARY KEY,
    claim_code VARCHAR(20) NOT NULL UNIQUE,
    patient_id INT NOT NULL,
    hospital_id INT NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    claim_amount DECIMAL(10, 2) NOT NULL,
    status ENUM('Pending', 'Submitted', 'Approved', 'Rejected', 'Settled') DEFAULT 'Pending',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 27. Ambulances Table
CREATE TABLE ambulances (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ambulance_code VARCHAR(20) NOT NULL UNIQUE,
    hospital_id INT NOT NULL,
    vehicle_number VARCHAR(30) NOT NULL,
    driver_name VARCHAR(100) NOT NULL,
    driver_phone VARCHAR(20) NOT NULL,
    ambulance_type ENUM('Basic Life Support', 'Advanced Life Support', 'Patient Transport') DEFAULT 'Advanced Life Support',
    status ENUM('Available', 'Dispatched', 'Maintenance') DEFAULT 'Available',
    current_location VARCHAR(100) DEFAULT 'Hospital Bay 1',
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 28. Ambulance Requests Table
CREATE TABLE ambulance_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    hospital_id INT NOT NULL,
    pickup_address TEXT NOT NULL,
    destination_address TEXT,
    contact_phone VARCHAR(20) NOT NULL,
    status ENUM('Requested', 'Dispatched', 'On Scene', 'Completed', 'Cancelled') DEFAULT 'Requested',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 29. Blood Bank Table
CREATE TABLE blood_bank (
    id INT AUTO_INCREMENT PRIMARY KEY,
    hospital_id INT NOT NULL,
    blood_group ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') NOT NULL,
    units_available INT NOT NULL DEFAULT 10,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 30. Reviews Table
CREATE TABLE reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    hospital_id INT NOT NULL,
    doctor_id INT NOT NULL,
    appointment_id INT UNIQUE,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    doctor_rating INT DEFAULT 5,
    staff_rating INT DEFAULT 5,
    cleanliness_rating INT DEFAULT 5,
    review_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 31. Notifications Table
CREATE TABLE notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 32. Staff Table
CREATE TABLE staff (
    id INT AUTO_INCREMENT PRIMARY KEY,
    staff_code VARCHAR(20) NOT NULL UNIQUE,
    user_id INT UNIQUE,
    hospital_id INT NOT NULL,
    branch_id INT,
    name VARCHAR(100) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    staff_role ENUM('DOCTOR', 'NURSE', 'RECEPTIONIST', 'LAB_TECHNICIAN', 'PHARMACIST', 'ADMINISTRATIVE') NOT NULL,
    designation VARCHAR(100) NOT NULL,
    salary DECIMAL(10, 2) DEFAULT 45000.00,
    status ENUM('ACTIVE', 'ON_LEAVE', 'INACTIVE') DEFAULT 'ACTIVE',
    joined_date DATE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 33. Audit Logs Table
CREATE TABLE audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    username VARCHAR(50),
    hospital_id INT,
    action VARCHAR(100) NOT NULL,
    module VARCHAR(50) NOT NULL,
    details TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 34. System Settings Table
CREATE TABLE settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(50) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;


-- ========================================================
-- SEED REFERENCE DATA
-- ========================================================

INSERT INTO states (id, name, code, region) VALUES
(1, 'Telangana', 'TS', 'South'),
(2, 'Karnataka', 'KA', 'South'),
(3, 'Delhi', 'DL', 'North'),
(4, 'Andhra Pradesh', 'AP', 'South'),
(5, 'Maharashtra', 'MH', 'West');

INSERT INTO cities (id, state_id, name, tier, fee_multiplier) VALUES
(1, 1, 'Hyderabad', 'Metro', 1.25),
(2, 2, 'Bangalore', 'Metro', 1.30),
(3, 3, 'New Delhi', 'Metro', 1.35),
(4, 4, 'Visakhapatnam', 'Tier-2', 1.00),
(5, 5, 'Mumbai', 'Metro', 1.40);

INSERT INTO specializations (id, name, category, min_recommended_fee, max_recommended_fee) VALUES
(1, 'Cardiologist', 'Super-Speciality', 600.00, 3000.00),
(2, 'Neurologist', 'Super-Speciality', 700.00, 3500.00),
(3, 'Orthopedic', 'Surgical', 400.00, 2000.00),
(4, 'Dentist', 'Primary', 300.00, 1000.00),
(5, 'General Physician', 'Primary', 200.00, 800.00);

-- Seed Users
INSERT INTO users (id, username, password_hash, salt, role, email, phone, hospital_id, status) VALUES
(1, 'superadmin', 'd572776c53fa65239e2467d02516428c9a3bb92db01cfd74659b977be415f340', 'HOSPITAL_SALT_2026', 'SUPER_ADMIN', 'platform.admin@aurahealth.com', '9876543200', NULL, 'ACTIVE'),
(2, 'apollo_admin', 'd572776c53fa65239e2467d02516428c9a3bb92db01cfd74659b977be415f340', 'HOSPITAL_SALT_2026', 'HOSPITAL_ADMIN', 'admin@apollohyd.com', '9876543201', 1, 'ACTIVE'),
(3, 'fortis_admin', 'd572776c53fa65239e2467d02516428c9a3bb92db01cfd74659b977be415f340', 'HOSPITAL_SALT_2026', 'HOSPITAL_ADMIN', 'admin@fortisblr.com', '9876543202', 2, 'ACTIVE'),
(4, 'dr_sharma', 'fcf0a473f1d8ef3f24bf7c541764658145bd9ce64df5a0248ad67b36f1ca8575', 'HOSPITAL_SALT_2026', 'DOCTOR', 'sharma@apollo.com', '9876543211', 1, 'ACTIVE'),
(5, 'dr_ananya', 'fcf0a473f1d8ef3f24bf7c541764658145bd9ce64df5a0248ad67b36f1ca8575', 'HOSPITAL_SALT_2026', 'DOCTOR', 'ananya@fortis.com', '9876543212', 2, 'ACTIVE'),
(6, 'dr_vikram', 'fcf0a473f1d8ef3f24bf7c541764658145bd9ce64df5a0248ad67b36f1ca8575', 'HOSPITAL_SALT_2026', 'DOCTOR', 'vikram@maxhealth.com', '9876543213', 3, 'ACTIVE'),
(7, 'patient1', '6cf83d2ff98be5c3b9b47e224e75d50bd86756858a74ec4eb119dfcb8ef8a42f', 'HOSPITAL_SALT_2026', 'PATIENT', 'john.doe@gmail.com', '9811122233', NULL, 'ACTIVE'),
(8, 'patient2', '6cf83d2ff98be5c3b9b47e224e75d50bd86756858a74ec4eb119dfcb8ef8a42f', 'HOSPITAL_SALT_2026', 'PATIENT', 'priya.singh@gmail.com', '9822233344', NULL, 'ACTIVE'),
(9, 'receptionist1', '04b5033c467a9cf5ff81525bb16611fbbe32f65a1ea0aa1ee87f549a1d954605', 'HOSPITAL_SALT_2026', 'RECEPTIONIST', 'frontdesk@apollo.com', '9899988877', 1, 'ACTIVE');

-- Seed Hospitals
INSERT INTO hospitals (id, hospital_code, name, type, description, address, city, state, pincode, phone, emergency_phone, email, website, google_maps_url, image_url, opening_hours, total_beds, icu_beds, rating, review_count, established_year, status, data_source) VALUES
(1, 'HOSP00001', 'Apollo Health City', 'Super-Speciality', 'Premier multi-speciality hospital network.', 'Road No. 92, Jubilee Hills', 'Hyderabad', 'Telangana', '500033', '040-23607777', '040-1066', 'info@apollohyd.com', 'https://www.apollohospitals.com', 'https://maps.google.com/?q=Apollo+Hospital+Jubilee+Hills', 'https://images.unsplash.com/photo-1587351021759-3e566b6af7cc?w=800&q=80', '24/7 Open', 450, 60, 4.9, 1420, 1988, 'ACTIVE', 'DEMO'),
(2, 'HOSP00002', 'Fortis Healthcare', 'Multi-Speciality', 'State-of-the-art tertiary care hospital.', 'Bannerghatta Road', 'Bangalore', 'Karnataka', '560076', '080-66214444', '080-105010', 'contact@fortisblr.com', 'https://www.fortishealthcare.com', 'https://maps.google.com/?q=Fortis+Hospital+Bannerghatta', 'https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=800&q=80', '24/7 Open', 300, 45, 4.8, 980, 2006, 'ACTIVE', 'DEMO'),
(3, 'HOSP00003', 'Max Super Speciality Hospital', 'Super-Speciality', 'Leading healthcare institute.', 'Saket', 'New Delhi', 'Delhi', '110017', '011-26515050', '011-102', 'emergency@maxhealth.com', 'https://www.maxhealthcare.in', 'https://maps.google.com/?q=Max+Hospital+Saket', 'https://images.unsplash.com/photo-1586773860418-d37222d8fce3?w=800&q=80', '24/7 Open', 500, 75, 4.7, 1150, 2001, 'ACTIVE', 'DEMO'),
(4, 'HOSP00004', 'Care Hospitals Network', 'Multi-Speciality', 'Advanced trauma and emergency care.', 'Ram Nagar', 'Visakhapatnam', 'Andhra Pradesh', '530002', '0891-3041111', '0891-108', 'info@carehospitals.com', 'https://www.carehospitals.com', 'https://maps.google.com/?q=Care+Hospitals+Visakhapatnam', 'https://images.unsplash.com/photo-1516549655169-df83a0774514?w=800&q=80', '24/7 Open', 250, 30, 4.6, 620, 1997, 'ACTIVE', 'DEMO'),
(5, 'HOSP00005', 'Zenith Institute of Health Sciences', 'Super-Speciality', 'Renowned medical center.', 'Ernakulam', 'Kochi', 'Kerala', '682011', '0484-2801234', '0484-1066', 'info@zenithhealth.org', 'https://www.zenithhealth.org', 'https://maps.google.com/?q=Zenith+Hospital+Kochi', 'https://images.unsplash.com/photo-1512678080530-7760d81faba6?w=800&q=80', '24/7 Open', 600, 90, 4.8, 1850, 1991, 'ACTIVE', 'DEMO');

-- Seed Branches for Hospitals
INSERT INTO hospital_branches (id, branch_code, hospital_id, branch_name, address, city, state, pincode, phone, emergency_phone, working_hours, latitude, longitude) VALUES
(1, 'BR00001', 1, 'Jubilee Hills Main Branch', 'Road No 92, Jubilee Hills', 'Hyderabad', 'Telangana', '500033', '040-23607777', '040-1066', '24/7 Open', 17.4319, 78.4071),
(2, 'BR00002', 2, 'Bannerghatta Main Branch', 'Bannerghatta Road', 'Bangalore', 'Karnataka', '560076', '080-66214444', '080-105010', '24/7 Open', 12.8943, 77.5989),
(3, 'BR00003', 3, 'Saket Super Speciality Branch', 'Saket', 'New Delhi', 'Delhi', '110017', '011-26515050', '011-102', '24/7 Open', 28.5273, 77.2117),
(4, 'BR00004', 4, 'Care Ram Nagar Branch', 'Ram Nagar', 'Visakhapatnam', 'Andhra Pradesh', '530002', '0891-3041111', '0891-108', '24/7 Open', 17.7123, 83.3101),
(5, 'BR00005', 5, 'Zenith Ernakulam Main Branch', 'Ernakulam', 'Kochi', 'Kerala', '682011', '0484-2801234', '0484-1066', '24/7 Open', 9.9816, 76.2999);

-- Departments
INSERT INTO departments (id, dept_code, hospital_id, name, description) VALUES
(1, 'DEP-CARD', 1, 'Cardiology', 'Heart & vascular surgical care.'),
(2, 'DEP-NEUR', 2, 'Neurology', 'Brain & stroke care.'),
(3, 'DEP-ORTH', 3, 'Orthopedics', 'Joint replacement & trauma.'),
(4, 'DEP-CARD', 4, 'Cardiology', 'Cardiology & Emergency.'),
(5, 'DEP-DENT', 5, 'Dental Sciences', 'Comprehensive oral & maxillo-facial surgery.');

-- Seed Doctors
INSERT INTO doctors (id, doctor_code, user_id, name, gender, dob, phone, email, specialization, sub_specialization, qualification, experience_years, consultation_fee, followup_fee, video_consultation_fee, emergency_consultation_fee, city, state, languages, license_no, rating, review_count, bio, image_url, verification_status, data_source) VALUES
(1, 'DOC00001', 4, 'Dr. Rajesh Sharma', 'Male', '1980-05-15', '9876543211', 'sharma@apollo.com', 'Cardiologist', 'Interventional Cardiology', 'MD, DM (Cardiology)', 18, 800.00, 500.00, 650.00, 1500.00, 'Hyderabad', 'Telangana', 'English, Hindi, Telugu', 'LIC-IND-90812', 4.9, 320, 'Chief Cardiologist at Apollo Health City.', 'https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=400&q=80', 'DEMO', 'DEMO'),
(2, 'DOC00002', 5, 'Dr. Ananya Roy', 'Female', '1984-08-22', '9876543212', 'ananya@fortis.com', 'Neurologist', 'Stroke Specialist', 'MD, DNB (Neurology)', 14, 900.00, 600.00, 750.00, 1600.00, 'Bangalore', 'Karnataka', 'English, Hindi, Kannada', 'LIC-IND-88123', 4.8, 210, 'Leading Neurologist specializing in acute stroke.', 'https://images.unsplash.com/photo-1594824813566-88855ce78961?w=400&q=80', 'DEMO', 'DEMO'),
(3, 'DOC00003', 6, 'Dr. Vikram Patel', 'Male', '1978-11-03', '9876543213', 'vikram@maxhealth.com', 'Orthopedic', 'Robotic Joint Replacement', 'MS (Orthopedics)', 20, 750.00, 450.00, 600.00, 1400.00, 'New Delhi', 'Delhi', 'English, Hindi', 'LIC-IND-77341', 4.9, 450, 'Senior Orthopedic Surgeon.', 'https://images.unsplash.com/photo-1537368910025-700350fe46c7?w=400&q=80', 'DEMO', 'DEMO'),
(4, 'DOC00004', NULL, 'Dr. Kavya Chowdhury', 'Female', '1989-03-14', '9876543214', 'kavya@zenithhealth.org', 'Dentist', 'Cosmetic Dentistry', 'BDS, MDS', 12, 650.00, 400.00, 500.00, 1000.00, 'Kochi', 'Kerala', 'English, Malayalam', 'LIC-IND-55214', 4.8, 190, 'Senior Dental Surgeon at Zenith Institute of Health Sciences.', 'https://images.unsplash.com/photo-1594824813566-88855ce78961?w=400&q=80', 'DEMO', 'DEMO');

-- Doctor Hospitals Mapping
INSERT INTO doctor_hospitals (id, doctor_id, hospital_id, branch_id, dept_id, room_no, consultation_fee) VALUES
(1, 1, 1, 1, 1, 'Cabin 101', 800.00),
(2, 2, 2, 2, 2, 'Cabin 204', 900.00),
(3, 3, 3, 3, 3, 'Cabin 305', 750.00),
(4, 1, 4, 4, 4, 'Cabin 401', 800.00),
(5, 4, 5, 5, 5, 'Cabin 501', 650.00);

-- Doctor Fees Configuration
INSERT INTO doctor_fees (fee_id, doctor_id, hospital_id, branch_id, appointment_type, base_fee, tax_percentage, service_charge, discount) VALUES
(1, 1, 1, 1, 'In-Person', 800.00, 5.00, 50.00, 0.00),
(2, 1, 1, 1, 'Follow-Up', 500.00, 5.00, 30.00, 0.00),
(3, 1, 1, 1, 'Video Consultation', 650.00, 5.00, 40.00, 0.00),
(4, 1, 1, 1, 'Emergency', 1500.00, 5.00, 100.00, 0.00),
(5, 4, 5, 5, 'In-Person', 650.00, 5.00, 50.00, 0.00),
(6, 4, 5, 5, 'Follow-Up', 400.00, 5.00, 30.00, 0.00),
(7, 4, 5, 5, 'Video Consultation', 500.00, 5.00, 40.00, 0.00),
(8, 4, 5, 5, 'Emergency', 1000.00, 5.00, 100.00, 0.00);

-- Doctor Availability Config
INSERT INTO doctor_availability (id, doctor_id, hospital_id, branch_id, available_days, start_time, end_time, break_start, break_end, slot_duration_mins, max_appointments) VALUES
(1, 1, 1, 1, 'Monday,Tuesday,Wednesday,Thursday,Friday,Saturday', '08:00:00', '20:00:00', '13:00:00', '14:00:00', 30, 20),
(2, 4, 5, 5, 'Monday,Tuesday,Wednesday,Thursday,Friday,Saturday', '08:00:00', '20:00:00', '13:00:00', '14:00:00', 30, 20);

-- Seed Patients
INSERT INTO patients (id, patient_code, user_id, first_name, middle_name, last_name, name, gender, dob, age, blood_group, phone, alternate_phone, email, door_no, street, locality, address, city, state, pincode, height_cm, weight_kg, emergency_contact_name, emergency_contact_relationship, emergency_contact_phone) VALUES
(1, 'PAT00001', 7, 'John', 'M', 'Doe', 'John Doe', 'Male', '1990-04-12', 36, 'O+', '9811122233', '9811122234', 'john.doe@gmail.com', '42', 'Park Avenue', 'Sector 15', '42 Park Avenue, Sector 15, New Delhi', 'New Delhi', 'Delhi', '110015', 175.00, 72.00, 'Jane Doe', 'Spouse', '9811122234'),
(2, 'PAT00002', 8, 'Priya', 'K', 'Singh', 'Priya Singh', 'Female', '1995-09-25', 30, 'B+', '9822233344', '9822233345', 'priya.singh@gmail.com', '108', 'Metro Green', 'Indiranagar', '108 Metro Green, Indiranagar, Bangalore', 'Bangalore', 'Karnataka', '560038', 162.00, 58.00, 'R.P. Singh', 'Father', '9822233345');

-- Seed Appointments
INSERT INTO appointments (id, appointment_code, patient_id, doctor_id, hospital_id, branch_id, dept_id, appointment_date, time_slot, appointment_type, health_problem_type, symptoms, problem_description, is_emergency, base_fee, service_charge, discount, tax_amount, total_amount, payment_status, status, notes) VALUES
(1, 'APT00001', 1, 1, 1, 1, 1, CURRENT_DATE(), '10:00 AM - 10:30 AM', 'In-Person', 'Chest Pain', 'Shortness of Breath, Tightness', 'Chest tightness during morning walks.', FALSE, 800.00, 50.00, 0.00, 42.50, 892.50, 'Paid', 'Confirmed', 'Patient requested ECG');

-- Settings
INSERT INTO settings (id, setting_key, setting_value) VALUES
(1, 'system_name', 'AURA Health Enterprise Platform'),
(2, 'currency_symbol', '₹'),
(3, 'timezone', 'Asia/Kolkata');
