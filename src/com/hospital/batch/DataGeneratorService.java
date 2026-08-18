package com.hospital.batch;

import com.hospital.util.DBConnection;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DataGeneratorService {

    private static final AtomicBoolean isRunning = new AtomicBoolean(false);
    private static final AtomicInteger hospitalsGenerated = new AtomicInteger(0);
    private static final AtomicInteger branchesGenerated = new AtomicInteger(0);
    private static final AtomicInteger doctorsGenerated = new AtomicInteger(0);
    private static int targetHospitals = 1000;
    private static int targetDoctors = 100000;
    private static String statusMessage = "Idle";

    private static final String[] FIRST_NAMES = {"Aarav", "Aditi", "Ananya", "Arjun", "Bhavya", "Dev", "Diya", "Gaurav", "Isha", "Kavya", "Karan", "Manish", "Neha", "Pooja", "Pranav", "Rahul", "Rohan", "Sanjay", "Shreya", "Sneha", "Tanvi", "Varun", "Vikram", "Yash"};
    private static final String[] LAST_NAMES = {"Sharma", "Verma", "Patel", "Reddy", "Nair", "Deshmukh", "Singh", "Gupta", "Rao", "Joshi", "Kulkarni", "Mehta", "Babu", "Chowdhury", "Mukherjee", "Pillai", "Agarwal", "Bhat", "Iyer", "Malhotra"};
    private static final String[] SPECIALIZATIONS = {"Cardiologist", "Neurologist", "Orthopedic", "Oncologist", "Pediatrician", "Gynecologist", "Dermatologist", "General Physician", "ENT Specialist", "Ophthalmologist", "Dentist", "Psychiatrist", "Urologist", "Nephrologist", "Radiologist", "Pulmonologist", "Gastroenterologist", "Endocrinologist", "Rheumatologist", "General Surgeon", "Plastic Surgeon", "Anesthesiologist"};
    private static final String[] QUALIFICATIONS = {"MBBS, MD", "MBBS, MS", "MBBS, DNB", "MD, DM (Super-Speciality)", "MS, MCh (Super-Speciality)", "MBBS, FRCS (UK)", "MBBS, FACC (USA)"};
    private static final String[] CITIES = {"Hyderabad", "Bangalore", "New Delhi", "Mumbai", "Kolkata", "Chennai", "Visakhapatnam", "Ahmedabad", "Noida", "Jaipur", "Pune", "Lucknow", "Chandigarh", "Kochi", "Indore"};
    private static final String[] STATES = {"Telangana", "Karnataka", "Delhi", "Maharashtra", "West Bengal", "Tamil Nadu", "Andhra Pradesh", "Gujarat", "Uttar Pradesh", "Rajasthan", "Punjab", "Kerala", "Madhya Pradesh"};
    private static final String[] HOSP_PREFIXES = {"Apex", "Care", "City", "Global", "Heritage", "LifeLine", "MaxCare", "Metro", "National", "Nova", "Pacific", "Prime", "St. Jude", "Sunrise", "Trust", "Universal", "Zenith"};
    private static final String[] HOSP_SUFFIXES = {"Hospital", "Super-Speciality Hospital", "Medical Center", "Institute of Health Sciences", "Healthcare City"};

    public static Map<String, Object> getProgressStatus() {
        Map<String, Object> map = new HashMap<>();
        map.put("isRunning", isRunning.get());
        map.put("hospitalsGenerated", hospitalsGenerated.get());
        map.put("branchesGenerated", branchesGenerated.get());
        map.put("doctorsGenerated", doctorsGenerated.get());
        map.put("targetHospitals", targetHospitals);
        map.put("targetDoctors", targetDoctors);
        map.put("statusMessage", statusMessage);
        return map;
    }

    public static void startGenerationAsync(int countHospitals, int countDoctors) {
        if (isRunning.get()) return;

        targetHospitals = countHospitals > 0 ? countHospitals : 1000;
        targetDoctors = countDoctors > 0 ? countDoctors : 100000;

        isRunning.set(true);
        hospitalsGenerated.set(0);
        branchesGenerated.set(0);
        doctorsGenerated.set(0);
        statusMessage = "Starting dataset generation task...";

        new Thread(() -> {
            try {
                generateDataset(targetHospitals, targetDoctors);
            } catch (Exception e) {
                e.printStackTrace();
                statusMessage = "Error during generation: " + e.getMessage();
            } finally {
                isRunning.set(false);
            }
        }).start();
    }

    private static void generateDataset(int targetHospCount, int targetDocCount) throws SQLException {
        Random rand = new Random();

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            int startHospIndex = 11;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(CAST(SUBSTRING(hospital_code, 5) AS UNSIGNED)), 0) + 1 FROM hospitals")) {
                if (rs.next() && rs.getInt(1) > 0) startHospIndex = rs.getInt(1);
            }

            // 1. Generate Hospitals
            statusMessage = "Generating " + targetHospCount + " Hospitals...";
            String hospSql = "INSERT INTO hospitals (hospital_code, name, type, description, address, city, state, pincode, phone, emergency_phone, email, website, google_maps_url, image_url, opening_hours, total_beds, icu_beds, rating, review_count, established_year, status, data_source) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', 'DEMO')";

            try (PreparedStatement hospPstmt = conn.prepareStatement(hospSql)) {
                for (int i = startHospIndex; i < startHospIndex + targetHospCount; i++) {
                    String code = String.format("HOSP%06d", i);
                    String name = HOSP_PREFIXES[rand.nextInt(HOSP_PREFIXES.length)] + " " + HOSP_SUFFIXES[rand.nextInt(HOSP_SUFFIXES.length)];
                    String city = CITIES[rand.nextInt(CITIES.length)];
                    String state = STATES[rand.nextInt(STATES.length)];
                    String type = (i % 3 == 0) ? "Super-Speciality" : ((i % 2 == 0) ? "Multi-Speciality" : "Private");
                    int totalBeds = 50 + rand.nextInt(750);
                    int icuBeds = 10 + rand.nextInt(80);
                    double rating = 4.0 + (rand.nextDouble() * 1.0);

                    hospPstmt.setString(1, code);
                    hospPstmt.setString(2, name);
                    hospPstmt.setString(3, type);
                    hospPstmt.setString(4, "Premier healthcare institution offering advanced care in " + city);
                    hospPstmt.setString(5, "Sector " + (1 + rand.nextInt(50)) + ", Main Ring Road");
                    hospPstmt.setString(6, city);
                    hospPstmt.setString(7, state);
                    hospPstmt.setString(8, String.format("%06d", 500000 + rand.nextInt(99999)));
                    hospPstmt.setString(9, "0" + (40 + rand.nextInt(50)) + "-" + (2000000 + rand.nextInt(7000000)));
                    hospPstmt.setString(10, "0" + (40 + rand.nextInt(50)) + "-1066");
                    hospPstmt.setString(11, "contact@" + code.toLowerCase() + ".org");
                    hospPstmt.setString(12, "https://www." + code.toLowerCase() + ".org");
                    hospPstmt.setString(13, "https://maps.google.com/?q=" + name.replace(" ", "+") + "+" + city);
                    hospPstmt.setString(14, "https://images.unsplash.com/photo-1587351021759-3e566b6af7cc?w=800&q=80");
                    hospPstmt.setString(15, "24/7 Open");
                    hospPstmt.setInt(16, totalBeds);
                    hospPstmt.setInt(17, icuBeds);
                    hospPstmt.setDouble(18, Math.round(rating * 10.0) / 10.0);
                    hospPstmt.setInt(19, 50 + rand.nextInt(1500));
                    hospPstmt.setInt(20, 1990 + rand.nextInt(34));
                    hospPstmt.addBatch();

                    hospitalsGenerated.incrementAndGet();

                    if (i % 500 == 0) {
                        hospPstmt.executeBatch();
                        conn.commit();
                    }
                }
                hospPstmt.executeBatch();
                conn.commit();
            }

            // Fetch all valid hospital IDs
            List<Integer> validHospIds = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id FROM hospitals")) {
                while (rs.next()) {
                    validHospIds.add(rs.getInt(1));
                }
            }

            if (validHospIds.isEmpty()) validHospIds.add(1);

            int defaultBranchId = 1;
            int defaultDeptId = 1;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id FROM hospital_branches LIMIT 1")) {
                if (rs.next()) defaultBranchId = rs.getInt(1);
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id FROM departments LIMIT 1")) {
                if (rs.next()) defaultDeptId = rs.getInt(1);
            }

            int startDocIndex = 11;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(CAST(SUBSTRING(doctor_code, 4) AS UNSIGNED)), 0) + 1 FROM doctors")) {
                if (rs.next() && rs.getInt(1) > 0) startDocIndex = rs.getInt(1);
            }

            // 2. Generate Doctors via Fast Batch Insertion
            statusMessage = "Generating " + targetDocCount + " Synthetic Doctors in Batches...";
            String docSql = "INSERT INTO doctors (doctor_code, name, gender, dob, phone, email, specialization, sub_specialization, qualification, experience_years, consultation_fee, followup_fee, video_consultation_fee, emergency_consultation_fee, city, state, languages, license_no, rating, review_count, bio, image_url, verification_status, data_source) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DEMO', 'DEMO')";
            String dhSql = "INSERT INTO doctor_hospitals (doctor_id, hospital_id, branch_id, dept_id, room_no, consultation_fee) VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement docPstmt = conn.prepareStatement(docSql);
                 PreparedStatement dhPstmt = conn.prepareStatement(dhSql)) {

                int batchSize = 5000;
                for (int d = startDocIndex; d < startDocIndex + targetDocCount; d++) {
                    String dCode = String.format("DOC%07d", d);
                    String fname = FIRST_NAMES[rand.nextInt(FIRST_NAMES.length)];
                    String lname = LAST_NAMES[rand.nextInt(LAST_NAMES.length)];
                    String name = "Dr. " + fname + " " + lname;
                    String gender = (d % 2 == 0) ? "Female" : "Male";
                    String spec = SPECIALIZATIONS[rand.nextInt(SPECIALIZATIONS.length)];
                    String qual = QUALIFICATIONS[rand.nextInt(QUALIFICATIONS.length)];
                    String city = CITIES[rand.nextInt(CITIES.length)];
                    String state = STATES[rand.nextInt(STATES.length)];
                    int exp = 1 + rand.nextInt(35);

                    double baseFee = 300.0 + (exp * 25.0) + rand.nextInt(200);
                    double followupFee = Math.round(baseFee * 0.6);
                    double videoFee = Math.round(baseFee * 0.8);
                    double emergencyFee = Math.round(baseFee * 1.8);
                    double rating = 4.2 + (rand.nextDouble() * 0.8);

                    docPstmt.setString(1, dCode);
                    docPstmt.setString(2, name);
                    docPstmt.setString(3, gender);
                    docPstmt.setString(4, (1965 + rand.nextInt(35)) + "-05-15");
                    docPstmt.setString(5, "9" + String.format("%09d", 100000000 + rand.nextInt(899999999)));
                    docPstmt.setString(6, fname.toLowerCase() + "." + lname.toLowerCase() + "@aurahealth-demo.com");
                    docPstmt.setString(7, spec);
                    docPstmt.setString(8, "Senior " + spec);
                    docPstmt.setString(9, qual);
                    docPstmt.setInt(10, exp);
                    docPstmt.setDouble(11, baseFee);
                    docPstmt.setDouble(12, followupFee);
                    docPstmt.setDouble(13, videoFee);
                    docPstmt.setDouble(14, emergencyFee);
                    docPstmt.setString(15, city);
                    docPstmt.setString(16, state);
                    docPstmt.setString(17, "English, Hindi");
                    docPstmt.setString(18, "LIC-DEMO-" + dCode);
                    docPstmt.setDouble(19, Math.round(rating * 10.0) / 10.0);
                    docPstmt.setInt(20, 10 + rand.nextInt(400));
                    docPstmt.setString(21, "Experienced " + spec + " practicing in " + city + " with " + exp + " years of clinical experience.");
                    docPstmt.setString(22, gender.equals("Female") ? "https://images.unsplash.com/photo-1594824813566-88855ce78961?w=400&q=80" : "https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=400&q=80");
                    docPstmt.addBatch();

                    doctorsGenerated.incrementAndGet();

                    if (d % batchSize == 0) {
                        docPstmt.executeBatch();
                        conn.commit();
                        statusMessage = "Generated " + String.format("%,d", doctorsGenerated.get()) + " / " + String.format("%,d", targetDocCount) + " Doctors...";
                    }
                }
                docPstmt.executeBatch();
                conn.commit();

                // Map doctors to Hospitals
                statusMessage = "Mapping Doctor-Hospital Relationships...";
                for (int docId = startDocIndex; docId < Math.min(startDocIndex + targetDocCount, startDocIndex + 50000); docId++) {
                    int assignedHospId = validHospIds.get(rand.nextInt(validHospIds.size()));
                    dhPstmt.setInt(1, docId);
                    dhPstmt.setInt(2, assignedHospId);
                    dhPstmt.setInt(3, defaultBranchId);
                    dhPstmt.setInt(4, defaultDeptId);
                    dhPstmt.setString(5, "OPD " + (100 + rand.nextInt(400)));
                    dhPstmt.setDouble(6, 500.00 + rand.nextInt(500));
                    dhPstmt.addBatch();

                    if (docId % batchSize == 0) {
                        dhPstmt.executeBatch();
                        conn.commit();
                    }
                }
                dhPstmt.executeBatch();
                conn.commit();
            }

            statusMessage = "Successfully generated dataset with " + targetHospCount + " Hospitals & " + String.format("%,d", targetDocCount) + " Doctors!";
        }
    }
}
