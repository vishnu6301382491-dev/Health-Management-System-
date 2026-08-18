package com.hospital.dao;

import com.hospital.model.Patient;
import com.hospital.model.User;
import com.hospital.service.AuthService;
import com.hospital.util.DBConnection;
import com.hospital.util.IDGenerator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    public List<Patient> getAllPatients(String search) {
        return getAllPatients(search, null, null);
    }

    public List<Patient> getAllPatients(String search, String bloodGroup, String gender) {
        List<Patient> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM patients WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (name LIKE ? OR patient_code LIKE ? OR phone LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term); params.add(term); params.add(term);
        }
        if (bloodGroup != null && !bloodGroup.trim().isEmpty() && !bloodGroup.equalsIgnoreCase("ALL")) {
            sql.append("AND blood_group = ? ");
            params.add(bloodGroup.trim());
        }
        if (gender != null && !gender.trim().isEmpty() && !gender.equalsIgnoreCase("ALL")) {
            sql.append("AND gender = ? ");
            params.add(gender.trim());
        }

        sql.append("ORDER BY registration_date DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPatient(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Patient getById(int id) {
        String sql = "SELECT * FROM patients WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToPatient(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Patient getByUserId(int userId) {
        String sql = "SELECT * FROM patients WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToPatient(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Patient getByCodeOrPhone(String query) {
        if (query == null || query.trim().isEmpty()) return null;
        String q = query.trim();
        String sql = "SELECT * FROM patients WHERE patient_code = ? OR phone = ? OR email = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, q);
            pstmt.setString(2, q);
            pstmt.setString(3, q);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPatient(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addPatient(Patient p) {
        return addPatient(p, null, null);
    }

    public boolean addPatient(Patient p, String username, String password) {
        String code = IDGenerator.generateId("patients", "patient_code", "PAT");
        p.setPatientCode(code);

        Integer userId = p.getUserId();
        if (userId == null && username != null && password != null) {
            User u = new User();
            u.setUsername(username);
            u.setPasswordHash(com.hospital.util.PasswordUtil.hashPassword(password, "HOSPITAL_SALT_2026"));
            u.setSalt("HOSPITAL_SALT_2026");
            u.setRole("PATIENT");
            u.setEmail(p.getEmail() != null ? p.getEmail() : username + "@patient.com");
            u.setPhone(p.getPhone());
            try {
                UserDAO udao = new UserDAO();
                int newUid = udao.createUser(u, password);
                if (newUid > 0) userId = newUid;
            } catch (SQLException ignored) {}
        }

        String sql = "INSERT INTO patients (patient_code, user_id, first_name, middle_name, last_name, name, gender, dob, age, blood_group, phone, alternate_phone, email, door_no, street, locality, address, city, state, pincode, height_cm, weight_kg, emergency_contact_name, emergency_contact_relationship, emergency_contact_phone, medical_history_summary, allergies, insurance_provider, policy_no) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, p.getPatientCode());
            pstmt.setObject(2, userId);
            pstmt.setString(3, p.getFirstName());
            pstmt.setString(4, p.getMiddleName());
            pstmt.setString(5, p.getLastName());
            pstmt.setString(6, p.getName());
            pstmt.setString(7, p.getGender() != null ? p.getGender() : "Male");
            pstmt.setString(8, p.getDob());
            pstmt.setInt(9, p.getAge());
            pstmt.setString(10, p.getBloodGroup() != null ? p.getBloodGroup() : "O+");
            pstmt.setString(11, p.getPhone());
            pstmt.setString(12, p.getAlternatePhone());
            pstmt.setString(13, p.getEmail());
            pstmt.setString(14, p.getDoorNo());
            pstmt.setString(15, p.getStreet());
            pstmt.setString(16, p.getLocality());
            pstmt.setString(17, p.getAddress());
            pstmt.setString(18, p.getCity() != null ? p.getCity() : "Hyderabad");
            pstmt.setString(19, p.getState() != null ? p.getState() : "Telangana");
            pstmt.setString(20, p.getPincode());
            pstmt.setDouble(21, p.getHeightCm() > 0 ? p.getHeightCm() : 170.00);
            pstmt.setDouble(22, p.getWeightKg() > 0 ? p.getWeightKg() : 65.00);
            pstmt.setString(23, p.getEmergencyContactName());
            pstmt.setString(24, p.getEmergencyContactRelationship());
            pstmt.setString(25, p.getEmergencyContactPhone());
            pstmt.setString(26, p.getMedicalHistorySummary());
            pstmt.setString(27, p.getAllergies());
            pstmt.setString(28, p.getInsuranceProvider());
            pstmt.setString(29, p.getPolicyNo());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        p.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updatePatient(Patient p) {
        String sql = "UPDATE patients SET name = ?, gender = ?, dob = ?, age = ?, blood_group = ?, phone = ?, email = ?, address = ?, city = ?, state = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getName());
            pstmt.setString(2, p.getGender());
            pstmt.setString(3, p.getDob());
            pstmt.setInt(4, p.getAge());
            pstmt.setString(5, p.getBloodGroup());
            pstmt.setString(6, p.getPhone());
            pstmt.setString(7, p.getEmail());
            pstmt.setString(8, p.getAddress());
            pstmt.setString(9, p.getCity());
            pstmt.setString(10, p.getState());
            pstmt.setInt(11, p.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deletePatient(int id) {
        String sql = "DELETE FROM patients WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Patient mapResultSetToPatient(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setId(rs.getInt("id"));
        p.setPatientCode(rs.getString("patient_code"));
        Object uid = rs.getObject("user_id");
        if (uid != null) p.setUserId((Integer) uid);

        p.setFirstName(rs.getString("first_name"));
        p.setMiddleName(rs.getString("middle_name"));
        p.setLastName(rs.getString("last_name"));
        p.setName(rs.getString("name"));
        p.setGender(rs.getString("gender"));
        p.setDob(rs.getString("dob"));
        p.setAge(rs.getInt("age"));
        p.setBloodGroup(rs.getString("blood_group"));
        p.setPhone(rs.getString("phone"));
        p.setAlternatePhone(rs.getString("alternate_phone"));
        p.setEmail(rs.getString("email"));
        p.setDoorNo(rs.getString("door_no"));
        p.setStreet(rs.getString("street"));
        p.setLocality(rs.getString("locality"));
        p.setAddress(rs.getString("address"));
        p.setCity(rs.getString("city"));
        p.setState(rs.getString("state"));
        p.setPincode(rs.getString("pincode"));
        p.setHeightCm(rs.getDouble("height_cm"));
        p.setWeightKg(rs.getDouble("weight_kg"));
        p.setEmergencyContactName(rs.getString("emergency_contact_name"));
        p.setEmergencyContactRelationship(rs.getString("emergency_contact_relationship"));
        p.setEmergencyContactPhone(rs.getString("emergency_contact_phone"));
        p.setMedicalHistorySummary(rs.getString("medical_history_summary"));
        p.setAllergies(rs.getString("allergies"));
        p.setRegistrationDate(rs.getTimestamp("registration_date"));

        return p;
    }
}
