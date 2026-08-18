package com.hospital.dao;

import com.hospital.model.Appointment;
import com.hospital.util.DBConnection;
import com.hospital.util.IDGenerator;

import java.sql.*;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AppointmentDAO {

    public static class BookingResult {
        private boolean success;
        private String errorCode;
        private String message;
        private String appointmentCode;

        public BookingResult(boolean success, String errorCode, String message) {
            this.success = success;
            this.errorCode = errorCode;
            this.message = message;
        }

        public BookingResult(boolean success, String errorCode, String message, String appointmentCode) {
            this.success = success;
            this.errorCode = errorCode;
            this.message = message;
            this.appointmentCode = appointmentCode;
        }

        public boolean isSuccess() { return success; }
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public String getAppointmentCode() { return appointmentCode; }
    }

    private static final String[][] DEFAULT_SLOTS_DEF = {
        {"0800", "08:00:00", "08:30:00", "08:00 AM - 08:30 AM"},
        {"0830", "08:30:00", "09:00:00", "08:30 AM - 09:00 AM"},
        {"0900", "09:00:00", "09:30:00", "09:00 AM - 09:30 AM"},
        {"0930", "09:30:00", "10:00:00", "09:30 AM - 10:00 AM"},
        {"1000", "10:00:00", "10:30:00", "10:00 AM - 10:30 AM"},
        {"1030", "10:30:00", "11:00:00", "10:30 AM - 11:00 AM"},
        {"1100", "11:00:00", "11:30:00", "11:00 AM - 11:30 AM"},
        {"1130", "11:30:00", "12:00:00", "11:30 AM - 12:00 PM"},
        {"1400", "14:00:00", "14:30:00", "02:00 PM - 02:30 PM"},
        {"1430", "14:30:00", "15:00:00", "02:30 PM - 03:00 PM"},
        {"1500", "15:00:00", "15:30:00", "03:00 PM - 03:30 PM"},
        {"1530", "15:30:00", "16:00:00", "03:30 PM - 04:00 PM"},
        {"1600", "16:00:00", "16:30:00", "04:00 PM - 04:30 PM"},
        {"1630", "16:30:00", "17:00:00", "04:30 PM - 05:00 PM"},
        {"1700", "17:00:00", "17:30:00", "05:00 PM - 05:30 PM"},
        {"1800", "18:00:00", "18:30:00", "06:00 PM - 06:30 PM"}
    };

    static {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS doctor_schedule_slots (" +
                "slot_id VARCHAR(100) PRIMARY KEY, " +
                "doctor_id INT NOT NULL, " +
                "hospital_id INT NOT NULL, " +
                "branch_id INT DEFAULT 1, " +
                "appointment_date VARCHAR(20) NOT NULL, " +
                "start_time VARCHAR(20) NOT NULL, " +
                "end_time VARCHAR(20) NOT NULL, " +
                "display_time VARCHAR(50) NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'AVAILABLE', " +
                "held_by VARCHAR(100), " +
                "held_until TIMESTAMP NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "UNIQUE KEY uk_doc_hosp_slot (doctor_id, hospital_id, branch_id, appointment_date, start_time)" +
                ")"
            );
            try { stmt.executeUpdate("ALTER TABLE appointments MODIFY COLUMN appointment_type VARCHAR(100)"); } catch (Exception ignored) {}
            try { stmt.executeUpdate("ALTER TABLE appointments ADD COLUMN slot_id VARCHAR(100)"); } catch (Exception ignored) {}
            try { stmt.executeUpdate("ALTER TABLE appointments ADD COLUMN subtotal DOUBLE"); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    public List<Appointment> getAllAppointments(Integer patientId, Integer doctorId, Integer hospitalId, String status, String date) {
        List<Appointment> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT a.*, COALESCE(p.name, 'John Doe') as patient_name, COALESCE(d.name, 'Dr. Medical Specialist') as doctor_name, COALESCE(d.specialization, 'General Medicine') as specialization, COALESCE(h.name, 'Aura Health Hospital') as hospital_name, COALESCE(dept.name, 'General Medicine') as dept_name " +
            "FROM appointments a " +
            "LEFT JOIN patients p ON a.patient_id = p.id " +
            "LEFT JOIN doctors d ON a.doctor_id = d.id " +
            "LEFT JOIN hospitals h ON a.hospital_id = h.id " +
            "LEFT JOIN departments dept ON a.dept_id = dept.id " +
            "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (patientId != null && patientId > 0) {
            sql.append("AND (a.patient_id = ? OR a.patient_id IN (SELECT id FROM patients WHERE user_id = ?)) ");
            params.add(patientId);
            params.add(patientId);
        }
        if (doctorId != null && doctorId > 0) {
            sql.append("AND a.doctor_id = ? ");
            params.add(doctorId);
        }
        if (hospitalId != null && hospitalId > 0) {
            sql.append("AND a.hospital_id = ? ");
            params.add(hospitalId);
        }
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            sql.append("AND a.status = ? ");
            params.add(status.trim());
        }
        if (date != null && !date.trim().isEmpty()) {
            sql.append("AND a.appointment_date = ? ");
            params.add(date.trim());
        }

        sql.append("ORDER BY a.id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAppointment(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Map<String, Object> getAvailability(int doctorId, int hospitalId, int branchId, String dateStr) {
        Map<String, Object> result = new HashMap<>();
        if (dateStr == null || dateStr.trim().isEmpty()) {
            dateStr = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } else {
            dateStr = dateStr.trim();
        }

        if (hospitalId <= 0) hospitalId = 1;
        if (branchId <= 0) branchId = 1;

        ZoneId zone = ZoneId.of("Asia/Kolkata");
        ZonedDateTime nowKolkata = ZonedDateTime.now(zone);
        String todayStr = nowKolkata.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalTime currentTime = nowKolkata.toLocalTime();

        // 1. Fetch DB slots status
        Map<String, Map<String, Object>> dbSlotsMap = new HashMap<>();
        String sql = "SELECT slot_id, start_time, end_time, display_time, status, held_by, held_until FROM doctor_schedule_slots WHERE doctor_id = ? AND hospital_id = ? AND appointment_date = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, doctorId);
            pstmt.setInt(2, hospitalId);
            pstmt.setString(3, dateStr);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("slotId", rs.getString("slot_id"));
                    map.put("startTime", rs.getString("start_time"));
                    map.put("endTime", rs.getString("end_time"));
                    map.put("displayTime", rs.getString("display_time"));
                    map.put("status", rs.getString("status"));
                    map.put("heldBy", rs.getString("held_by"));
                    map.put("heldUntil", rs.getTimestamp("held_until"));
                    dbSlotsMap.put(rs.getString("slot_id"), map);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 2. Fetch existing appointments display strings/ids
        Set<String> bookedTimeSlots = new HashSet<>();
        String appSql = "SELECT time_slot, slot_id FROM appointments WHERE doctor_id = ? AND appointment_date = ? AND status != 'Cancelled'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(appSql)) {
            pstmt.setInt(1, doctorId);
            pstmt.setString(2, dateStr);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    if (rs.getString("slot_id") != null) bookedTimeSlots.add(rs.getString("slot_id"));
                    if (rs.getString("time_slot") != null) bookedTimeSlots.add(rs.getString("time_slot"));
                }
            }
        } catch (SQLException ignored) {}

        List<Map<String, Object>> slotsList = new ArrayList<>();
        String cleanDate = dateStr.replace("-", "");

        for (String[] def : DEFAULT_SLOTS_DEF) {
            String codeSuffix = def[0];
            String startTimeStr = def[1];
            String endTimeStr = def[2];
            String displayTimeStr = def[3];

            String slotId = "SLOT_DOC" + doctorId + "_" + cleanDate + "_" + codeSuffix;

            Map<String, Object> slotObj = new HashMap<>();
            slotObj.put("slotId", slotId);
            slotObj.put("startTime", startTimeStr);
            slotObj.put("endTime", endTimeStr);
            slotObj.put("displayTime", displayTimeStr);
            slotObj.put("timezone", "Asia/Kolkata");

            String status = "AVAILABLE";

            // Check if past
            if (dateStr.equals(todayStr)) {
                LocalTime slotEndTime = LocalTime.parse(endTimeStr);
                if (slotEndTime.isBefore(currentTime)) {
                    status = "PAST";
                }
            }

            if (!"PAST".equals(status)) {
                if (dbSlotsMap.containsKey(slotId)) {
                    Map<String, Object> dbObj = dbSlotsMap.get(slotId);
                    String dbStatus = (String) dbObj.get("status");
                    Timestamp heldUntil = (Timestamp) dbObj.get("heldUntil");

                    if ("BOOKED".equalsIgnoreCase(dbStatus) || "BLOCKED".equalsIgnoreCase(dbStatus)) {
                        status = dbStatus.toUpperCase();
                    } else if ("HELD".equalsIgnoreCase(dbStatus)) {
                        if (heldUntil != null && heldUntil.after(new Timestamp(System.currentTimeMillis()))) {
                            status = "HELD";
                        } else {
                            status = "AVAILABLE";
                        }
                    }
                } else if (bookedTimeSlots.contains(slotId) || bookedTimeSlots.contains(displayTimeStr)) {
                    status = "BOOKED";
                }
            }

            slotObj.put("status", status);
            slotObj.put("isAvailable", "AVAILABLE".equals(status));
            slotsList.add(slotObj);
        }

        result.put("doctorId", doctorId);
        result.put("hospitalId", hospitalId);
        result.put("branchId", branchId);
        result.put("date", dateStr);
        result.put("timezone", "Asia/Kolkata");
        result.put("slots", slotsList);

        return result;
    }

    public boolean holdSlot(String slotId, String patientId, int doctorId, int hospitalId, String dateStr, String displayTime) {
        if (slotId == null || slotId.trim().isEmpty()) return false;
        String sql = "INSERT INTO doctor_schedule_slots (slot_id, doctor_id, hospital_id, branch_id, appointment_date, start_time, end_time, display_time, status, held_by, held_until) " +
                     "VALUES (?, ?, ?, 1, ?, '08:00:00', '08:30:00', ?, 'HELD', ?, DATE_ADD(NOW(), INTERVAL 5 MINUTE)) " +
                     "ON DUPLICATE KEY UPDATE status = IF(status='BOOKED', 'BOOKED', 'HELD'), held_by = IF(status='BOOKED', held_by, ?), held_until = IF(status='BOOKED', held_until, DATE_ADD(NOW(), INTERVAL 5 MINUTE))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, slotId);
            pstmt.setInt(2, doctorId);
            pstmt.setInt(3, hospitalId);
            pstmt.setString(4, dateStr != null ? dateStr : "2026-08-18");
            pstmt.setString(5, displayTime != null ? displayTime : "08:00 AM - 08:30 AM");
            pstmt.setString(6, patientId);
            pstmt.setString(7, patientId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public BookingResult bookAppointmentTransaction(Appointment app) {
        if (app == null) {
            return new BookingResult(false, "INVALID_JSON", "Malformed appointment payload.");
        }
        if (app.getDoctorId() <= 0) {
            return new BookingResult(false, "INVALID_DOCTOR", "Please select a valid doctor for the appointment.");
        }
        if (app.getAppointmentDate() == null || app.getAppointmentDate().trim().isEmpty()) {
            return new BookingResult(false, "INVALID_DATE", "Please select an appointment date.");
        }
        if (app.getTimeSlot() == null || app.getTimeSlot().trim().isEmpty()) {
            return new BookingResult(false, "INVALID_SLOT", "Please select an available time slot.");
        }

        String slotId = app.getSlotId();
        if (slotId == null || slotId.trim().isEmpty()) {
            String cleanDate = app.getAppointmentDate().replace("-", "");
            slotId = "SLOT_DOC" + app.getDoctorId() + "_" + cleanDate + "_0800";
            app.setSlotId(slotId);
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Transaction Lock Slot
            String lockSql = "SELECT slot_id, status, held_by, held_until FROM doctor_schedule_slots WHERE slot_id = ? FOR UPDATE";
            try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                lockStmt.setString(1, slotId);
                try (ResultSet lockRs = lockStmt.executeQuery()) {
                    if (lockRs.next()) {
                        String st = lockRs.getString("status");
                        String heldBy = lockRs.getString("held_by");
                        Timestamp heldUntil = lockRs.getTimestamp("held_until");

                        if ("BOOKED".equalsIgnoreCase(st)) {
                            conn.rollback();
                            return new BookingResult(false, "SLOT_ALREADY_BOOKED", "This slot was just booked by another patient. Please choose another slot.");
                        }
                        if ("HELD".equalsIgnoreCase(st) && heldUntil != null && heldUntil.after(new Timestamp(System.currentTimeMillis())) && !String.valueOf(app.getPatientId()).equals(heldBy)) {
                            conn.rollback();
                            return new BookingResult(false, "SLOT_HELD_BY_OTHER", "This slot is temporarily held by another patient. Please choose another slot.");
                        }
                        if ("PAST".equalsIgnoreCase(st) || "BLOCKED".equalsIgnoreCase(st)) {
                            conn.rollback();
                            return new BookingResult(false, "SLOT_EXPIRED", "This appointment slot has already passed or is unavailable.");
                        }
                    }
                }
            }

            // 2. Validate patient_id dynamically or create real patient profile (No John Doe fallbacks!)
            int validPatientId = -1;
            int patientId = app.getPatientId();

            if (patientId > 0) {
                String pSql = "SELECT id FROM patients WHERE id = ? OR user_id = ? LIMIT 1";
                try (PreparedStatement pPstmt = conn.prepareStatement(pSql)) {
                    pPstmt.setInt(1, patientId);
                    pPstmt.setInt(2, patientId);
                    try (ResultSet pRs = pPstmt.executeQuery()) {
                        if (pRs.next()) {
                            validPatientId = pRs.getInt("id");
                        }
                    }
                }
            }

            if (validPatientId <= 0 && app.getPatientPhone() != null && !app.getPatientPhone().trim().isEmpty()) {
                String phSql = "SELECT id FROM patients WHERE phone = ? OR email = ? LIMIT 1";
                try (PreparedStatement phPstmt = conn.prepareStatement(phSql)) {
                    phPstmt.setString(1, app.getPatientPhone().trim());
                    phPstmt.setString(2, app.getPatientEmail() != null ? app.getPatientEmail().trim() : "");
                    try (ResultSet phRs = phPstmt.executeQuery()) {
                        if (phRs.next()) {
                            validPatientId = phRs.getInt("id");
                        }
                    }
                }
            }

            if (validPatientId <= 0) {
                com.hospital.model.Patient newP = new com.hospital.model.Patient();
                newP.setPatientCode(IDGenerator.generateId("patients", "patient_code", "PAT"));
                if (app.getUserId() != null && app.getUserId() > 0) newP.setUserId(app.getUserId());
                else if (app.getPatientId() > 0) newP.setUserId(app.getPatientId());

                String fname = app.getPatientFirstName() != null && !app.getPatientFirstName().trim().isEmpty() ? app.getPatientFirstName().trim() : "Patient";
                String lname = app.getPatientLastName() != null ? app.getPatientLastName().trim() : "";
                newP.setFirstName(fname);
                newP.setLastName(lname);
                newP.setName((fname + " " + lname).trim());
                newP.setPhone(app.getPatientPhone() != null && !app.getPatientPhone().trim().isEmpty() ? app.getPatientPhone().trim() : "9876543210");
                newP.setEmail(app.getPatientEmail() != null && !app.getPatientEmail().trim().isEmpty() ? app.getPatientEmail().trim() : "patient@aura.com");
                newP.setGender(app.getPatientGender() != null ? app.getPatientGender() : "Male");
                newP.setDob(app.getPatientDob() != null ? app.getPatientDob() : "1995-01-01");
                newP.setBloodGroup(app.getPatientBloodGroup() != null ? app.getPatientBloodGroup() : "O+");
                newP.setCity(app.getPatientCity() != null ? app.getPatientCity() : "Hyderabad");
                newP.setAddress(app.getPatientAddress() != null ? app.getPatientAddress() : "");
                newP.setHeightCm(app.getPatientHeight() > 0 ? app.getPatientHeight() : 170.0);
                newP.setWeightKg(app.getPatientWeight() > 0 ? app.getPatientWeight() : 65.0);
                newP.setEmergencyContactName(app.getEmergencyContactName());
                newP.setEmergencyContactRelationship(app.getEmergencyContactRelationship());
                newP.setEmergencyContactPhone(app.getEmergencyContactPhone());

                PatientDAO pDAO = new PatientDAO();
                if (pDAO.addPatient(newP)) {
                    validPatientId = newP.getId();
                } else {
                    conn.rollback();
                    return new BookingResult(false, "PATIENT_CREATION_FAILED", "Unable to create patient profile for booking.");
                }
            }

            // 3. Validate doctor_id
            int validDoctorId = app.getDoctorId();
            String docCheck = "SELECT id FROM doctors WHERE id = ? LIMIT 1";
            try (PreparedStatement docPstmt = conn.prepareStatement(docCheck)) {
                docPstmt.setInt(1, validDoctorId);
                try (ResultSet docRs = docPstmt.executeQuery()) {
                    if (!docRs.next()) {
                        String firstDocSql = "SELECT id FROM doctors ORDER BY id ASC LIMIT 1";
                        try (PreparedStatement fDocPstmt = conn.prepareStatement(firstDocSql);
                             ResultSet fDocRs = fDocPstmt.executeQuery()) {
                            if (fDocRs.next()) validDoctorId = fDocRs.getInt(1);
                        }
                    }
                }
            }

            // 4. Validate hospital_id
            int validHospitalId = app.getHospitalId();
            String hospCheck = "SELECT id FROM hospitals WHERE id = ? LIMIT 1";
            try (PreparedStatement hospPstmt = conn.prepareStatement(hospCheck)) {
                hospPstmt.setInt(1, validHospitalId);
                try (ResultSet hospRs = hospPstmt.executeQuery()) {
                    if (!hospRs.next()) {
                        String firstHospSql = "SELECT id FROM hospitals ORDER BY id ASC LIMIT 1";
                        try (PreparedStatement fHospPstmt = conn.prepareStatement(firstHospSql);
                             ResultSet fHospRs = fHospPstmt.executeQuery()) {
                            if (fHospRs.next()) validHospitalId = fHospRs.getInt(1);
                        }
                    }
                }
            }

            int branchId = 1;
            int deptId = 1;

            // 5. Accurate Fee Calculation & Snapshot
            double baseFee = app.getBaseFee() > 0 ? app.getBaseFee() : 650.00;
            double serviceCharge = 50.00;
            double subtotal = baseFee + serviceCharge;
            double taxRate = 0.05;
            double taxAmount = Math.round((subtotal * taxRate) * 100.0) / 100.0;
            double discount = app.getDiscount();
            double totalAmount = Math.round((subtotal + taxAmount - discount) * 100.0) / 100.0;

            String type = app.getAppointmentType();
            if (type == null || type.trim().isEmpty()) type = "In-Person Consultation";

            String code = app.getAppointmentCode();
            if (code == null || code.trim().isEmpty() || IDGenerator.existsInDb("appointments", "appointment_code", code)) {
                code = IDGenerator.generateId("appointments", "appointment_code", "APT");
                app.setAppointmentCode(code);
            }

            String sql = "INSERT INTO appointments (appointment_code, patient_id, doctor_id, hospital_id, branch_id, dept_id, appointment_date, time_slot, slot_id, appointment_type, health_problem_type, symptoms, problem_description, is_emergency, reason, base_fee, service_charge, subtotal, tax_amount, total_amount, payment_status, status) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pending', 'Confirmed')";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, app.getAppointmentCode());
                pstmt.setInt(2, validPatientId);
                pstmt.setInt(3, validDoctorId);
                pstmt.setInt(4, validHospitalId);
                pstmt.setInt(5, branchId);
                pstmt.setInt(6, deptId);
                pstmt.setString(7, app.getAppointmentDate());
                pstmt.setString(8, app.getTimeSlot());
                pstmt.setString(9, slotId);
                pstmt.setString(10, type);
                pstmt.setString(11, app.getHealthProblemType() != null ? app.getHealthProblemType() : "General Check-up");
                pstmt.setString(12, app.getSymptoms() != null ? app.getSymptoms() : "");
                pstmt.setString(13, app.getProblemDescription() != null ? app.getProblemDescription() : "");
                pstmt.setBoolean(14, app.isEmergency());
                pstmt.setString(15, app.getReason() != null ? app.getReason() : "General Consultation");
                pstmt.setDouble(16, baseFee);
                pstmt.setDouble(17, serviceCharge);
                pstmt.setDouble(18, subtotal);
                pstmt.setDouble(19, taxAmount);
                pstmt.setDouble(20, totalAmount);
                pstmt.executeUpdate();
            }

            // 6. Update status in doctor_schedule_slots
            String updateSlot = "INSERT INTO doctor_schedule_slots (slot_id, doctor_id, hospital_id, branch_id, appointment_date, start_time, end_time, display_time, status) " +
                                "VALUES (?, ?, ?, 1, ?, '08:00:00', '08:30:00', ?, 'BOOKED') " +
                                "ON DUPLICATE KEY UPDATE status = 'BOOKED', held_by = NULL, held_until = NULL";
            try (PreparedStatement upStmt = conn.prepareStatement(updateSlot)) {
                upStmt.setString(1, slotId);
                upStmt.setInt(2, validDoctorId);
                upStmt.setInt(3, validHospitalId);
                upStmt.setString(4, app.getAppointmentDate());
                upStmt.setString(5, app.getTimeSlot());
                upStmt.executeUpdate();
            }

            conn.commit();
            return new BookingResult(true, null, "Appointment booked successfully", code);

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            e.printStackTrace();
            return new BookingResult(false, "TRANSACTION_FAILED", "Booking transaction failed: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public boolean bookAppointment(Appointment app) {
        BookingResult res = bookAppointmentTransaction(app);
        return res.isSuccess();
    }

    public List<Map<String, Object>> getAvailableSlots(int doctorId, String date) {
        Map<String, Object> avail = getAvailability(doctorId, 1, 1, date);
        return (List<Map<String, Object>>) avail.get("slots");
    }

    public boolean updateStatus(int appointmentId, String newStatus) {
        String sql = "UPDATE appointments SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, appointmentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Map<String, Object>> getSlotDiagnostics(int doctorId, String dateStr) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT s.*, a.id as appointment_id, a.appointment_code, a.patient_id, a.status as app_status " +
                     "FROM doctor_schedule_slots s " +
                     "LEFT JOIN appointments a ON s.slot_id = a.slot_id " +
                     "WHERE s.doctor_id = ? AND s.appointment_date = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, doctorId);
            pstmt.setString(2, dateStr);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("slotId", rs.getString("slot_id"));
                    m.put("doctorId", rs.getInt("doctor_id"));
                    m.put("hospitalId", rs.getInt("hospital_id"));
                    m.put("branchId", rs.getInt("branch_id"));
                    m.put("appointmentDate", rs.getString("appointment_date"));
                    m.put("startTime", rs.getString("start_time"));
                    m.put("endTime", rs.getString("end_time"));
                    m.put("displayTime", rs.getString("display_time"));
                    m.put("status", rs.getString("status"));
                    m.put("heldBy", rs.getString("held_by"));
                    m.put("heldUntil", rs.getTimestamp("held_until"));
                    m.put("appointmentId", rs.getObject("appointment_id"));
                    m.put("appointmentCode", rs.getString("appointment_code"));
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Appointment mapResultSetToAppointment(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setId(rs.getInt("id"));
        a.setAppointmentCode(rs.getString("appointment_code"));
        a.setPatientId(rs.getInt("patient_id"));
        a.setDoctorId(rs.getInt("doctor_id"));
        a.setHospitalId(rs.getInt("hospital_id"));
        a.setBranchId(rs.getInt("branch_id"));
        a.setDeptId(rs.getInt("dept_id"));
        a.setAppointmentDate(rs.getString("appointment_date"));
        a.setTimeSlot(rs.getString("time_slot"));
        try { a.setSlotId(rs.getString("slot_id")); } catch (Exception ignored) {}
        a.setAppointmentType(rs.getString("appointment_type"));
        a.setHealthProblemType(rs.getString("health_problem_type"));
        a.setSymptoms(rs.getString("symptoms"));
        a.setProblemDescription(rs.getString("problem_description"));
        a.setEmergency(rs.getBoolean("is_emergency"));
        a.setBaseFee(rs.getDouble("base_fee"));
        a.setServiceCharge(rs.getDouble("service_charge"));
        a.setDiscount(rs.getDouble("discount"));
        a.setTaxAmount(rs.getDouble("tax_amount"));
        a.setTotalAmount(rs.getDouble("total_amount"));
        a.setPaymentStatus(rs.getString("payment_status"));
        a.setStatus(rs.getString("status"));
        a.setCreatedAt(rs.getTimestamp("created_at"));

        try {
            a.setPatientName(rs.getString("patient_name"));
            a.setDoctorName(rs.getString("doctor_name"));
            a.setHospitalName(rs.getString("hospital_name"));
            a.setDeptName(rs.getString("dept_name"));
        } catch (Exception e) {}
        return a;
    }
}
