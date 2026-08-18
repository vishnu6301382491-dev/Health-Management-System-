# AURA Health - Professional Multi-Hospital Healthcare Platform

A production-style, enterprise **Multi-Hospital Healthcare & Doctor Appointment Platform** built with **Core Java**, **JDBC**, **MySQL 9.7**, **HTML5/CSS3/JavaScript**, and **Three.js 3D Animations**.

---

## 🌟 Key Architecture & Capabilities

### 1. Multi-Hospital & Multi-Branch Management
- Centralized discovery directory for hospitals across cities (Hyderabad, Bangalore, New Delhi, Visakhapatnam, etc.).
- Detailed hospital profile pages with overview, facilities (ICU Beds, Pharmacy, Blood Bank, Ambulance, Scans), multi-branch mapping, affiliated doctors, and patient reviews.
- Role-based data isolation for Super Admins, Hospital Admins, Doctors, Receptionists, Lab Technicians, Pharmacists, and Patients.

### 2. Centralized Doctor Discovery & Scheduling
- Search doctors by specialization (Cardiologist, Neurologist, Orthopedic, Pediatrics, ENT, etc.), city, consultation fee, rating, and hospital affiliation.
- Real-time time slot generation (`09:00 - 09:30`, `09:30 - 10:00`), break time exclusion, slot locking, and double-booking prevention.
- Telemedicine video consultation launcher.

### 3. Emergency 24/7 & Blood Bank Dispatch
- Live blood bank unit tracker for all blood groups (`A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, `O+`, `O-`).
- 24/7 emergency ambulance dispatch with status tracking (Basic Life Support, Advanced Life Support).

### 4. Digital Prescriptions, Lab Automation & Billing
- Electronic prescription builder supporting multi-medicine dosage, frequency, and instructions with printable RX layout.
- Laboratory test ordering, status progression (`Requested` -> `Sample Collected` -> `Processing` -> `Completed`), and result entry with reference ranges.
- Pharmacy catalog with medicine search & inventory tracking.
- Itemized billing (Consultation, Lab, Pharmacy, Room, Taxes, Discounts) with payment status (Paid, Partially Paid, Pending) and printable tax invoices.

### 5. Section-Specific 3D Animations & Themes
Powered by Three.js WebGL rendering with distinct 3D visual environments per section:
- **Hospital Directory**: 3D DNA Double Helix model & particle cluster.
- **Doctor Search & Profiles**: Holographic Cyan 3D Floating Pulse Rings & Medical Symbol Node Network.
- **Emergency & Blood Bank**: Deep Crimson 3D Bouncing Blood Cell Spheres with ambient lighting.
- **Pharmacy & Lab**: Glowing 3D Chemical Molecular Crystal Lattice (atoms & bonds).
- **Dashboards & Analytics**: Tumbling 3D Neumorphic Polyhedrons (Cubes, Octahedrons, Torus Knots) with mouse tilt.

### 6. CSV Batch Data Import System
- Super Admin utility to bulk import hospital networks, branches, doctors, and departments from CSV datasets into MySQL.

---

## 🔑 Demo Login Credentials

| Role | Username | Password | Default Scope & Access |
| :--- | :--- | :--- | :--- |
| **Super Admin** | `superadmin` | `admin123` | Platform-wide control, hospital network management, CSV batch importer |
| **Hospital Admin** | `apollo_admin` | `admin123` | Apollo Health City hospital management, branches, staff, revenue |
| **Doctor** | `dr_sharma` | `doc123` | Consultation queue, patient records, digital RX, lab test orders |
| **Patient** | `patient1` | `pat123` | Hospital discovery, doctor booking, prescriptions, lab reports, bills |
| **Receptionist** | `receptionist1` | `rec123` | Front-desk queue, doctor slot matrix, walk-in registration |

---

## 🛠️ Technology Stack

- **Frontend**: HTML5, CSS3 (CSS Variables, Flexbox, Grid, Glassmorphism, Neumorphism), JavaScript ES6 (SPA), Three.js (WebGL 3D).
- **Backend**: Core Java 25 (`com.sun.net.httpserver.HttpServer`), RESTful JSON API.
- **Database**: MySQL 9.7 Server (`healthcare_platform_db` schema with 26+ normalized tables).
- **Database Connectivity**: JDBC (`com.mysql.cj.jdbc.Driver`).
- **Security**: SHA-256 password hashing with salt, session token management.

---

## 🚀 How to Setup & Run

### Step 1: Initialize MySQL Database
Make sure MySQL Server 9.7 is running on `localhost:3306` with user `root` and password `root`.

```powershell
Get-Content database\multi_hospital_platform.sql -Raw | & "C:\Program Files\MySQL\MySQL Server 9.7\bin\mysql.exe" -u root -proot
```

### Step 2: Reset Password Hashes (Optional)
```powershell
javac -d bin -cp "bin;lib/*" src/com/hospital/util/ResetPasswords.java
java -cp "bin;lib/*" com.hospital.util.ResetPasswords
```

### Step 3: Compile Java Codebase
```powershell
javac -d bin -cp "lib/*" (Get-ChildItem -Recurse src/*.java | Select-Object -ExpandProperty FullName)
```

### Step 4: Launch Backend Server
```powershell
java -cp "bin;lib/*" com.hospital.controller.ServerMain
```

### Step 5: Open Web Platform
Open your web browser at: **[http://localhost:8080](http://localhost:8080)**

---

## 📊 End-to-End Workflow Verification

```text
Register Patient → Sign In → Search Hospital in City → Select Hospital Profile → View Branches & Facilities → Search Doctor → View Schedule → Book Time Slot → Hospital Reception Check-In → Doctor Consultation & Vitals Entry → Create Prescription → Request Lab Test → Technician Adds Results → Generate Itemized Bill → Patient Pays Bill → Submit Rating & Review
```
