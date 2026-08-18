package com.hospital.dao;

import com.hospital.model.Doctor;
import com.hospital.model.PaginatedResult;
import com.hospital.util.DBConnection;
import com.hospital.util.IDGenerator;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoctorDAO {

    static {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE INDEX idx_doctors_perf ON doctors(status, specialization, city, rating)");
            stmt.executeUpdate("CREATE INDEX idx_dh_perf ON doctor_hospitals(hospital_id, doctor_id)");
        } catch (Exception ignored) {}
    }

    public List<Map<String, Object>> searchDoctorsFast(String query, Integer hospitalId, String spec, int limit) {
        if (limit < 1 || limit > 50) limit = 20;
        List<Map<String, Object>> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT d.id, d.doctor_code, d.name, d.specialization, d.experience_years, d.consultation_fee, d.rating, d.city, " +
            "(SELECT h.name FROM doctor_hospitals dh JOIN hospitals h ON dh.hospital_id = h.id WHERE dh.doctor_id = d.id LIMIT 1) as hospital_name " +
            "FROM doctors d WHERE d.status = 'ACTIVE' "
        );
        List<Object> params = new ArrayList<>();

        if (hospitalId != null && hospitalId > 0) {
            sql.append("AND (d.id IN (SELECT doctor_id FROM doctor_hospitals WHERE hospital_id = ?)) ");
            params.add(hospitalId);
        }
        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (d.name LIKE ? OR d.specialization LIKE ? OR d.city LIKE ?) ");
            String term = "%" + query.trim() + "%";
            params.add(term); params.add(term); params.add(term);
        }
        if (spec != null && !spec.trim().isEmpty() && !spec.equalsIgnoreCase("ALL")) {
            sql.append("AND d.specialization = ? ");
            params.add(spec.trim());
        }

        sql.append("ORDER BY d.rating DESC LIMIT ?");
        params.add(limit);

        long start = System.currentTimeMillis();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("doctorCode", rs.getString("doctor_code"));
                    map.put("name", rs.getString("name"));
                    map.put("specialization", rs.getString("specialization"));
                    map.put("experienceYears", rs.getInt("experience_years"));
                    map.put("consultationFee", rs.getDouble("consultation_fee"));
                    map.put("rating", rs.getDouble("rating"));
                    map.put("city", rs.getString("city"));
                    map.put("hospitalName", rs.getString("hospital_name"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > 500) {
            System.err.println("WARNING: Slow doctor search query (" + elapsed + " ms), returned " + list.size() + " rows");
        }
        return list;
    }

    public Map<String, Object> getDoctorFees(int doctorId) {
        Map<String, Object> res = new HashMap<>();
        Doctor d = getById(doctorId);
        double base = (d != null && d.getConsultationFee() > 0) ? d.getConsultationFee() : 650.0;
        res.put("doctorId", doctorId);
        res.put("doctorName", d != null ? d.getName() : "Doctor");
        res.put("inPersonFee", base);
        res.put("followUpFee", Math.round(base * 0.65));
        res.put("videoFee", Math.round(base * 0.85));
        res.put("emergencyFee", Math.round(base * 1.5));
        res.put("homeVisitFee", Math.round(base * 2.2));
        res.put("serviceCharge", 50.0);
        res.put("taxRate", 0.05);
        return res;
    }

    public PaginatedResult<Doctor> getDoctorsPaginated(String search, String city, String state, String specialization, Integer hospitalId, Double minFee, Double maxFee, Double minRating, Integer minExp, String gender, int page, int pageSize, String sortBy, String sortOrder) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;

        List<Doctor> list = new ArrayList<>();
        int totalRecords = 0;

        StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (hospitalId != null && hospitalId > 0) {
            whereClause.append("AND (d.id IN (SELECT doctor_id FROM doctor_hospitals WHERE hospital_id = ?)) ");
            params.add(hospitalId);
        }
        if (search != null && !search.trim().isEmpty()) {
            whereClause.append("AND (d.name LIKE ? OR d.specialization LIKE ? OR d.city LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term); params.add(term); params.add(term);
        }
        if (specialization != null && !specialization.trim().isEmpty() && !specialization.equalsIgnoreCase("ALL")) {
            whereClause.append("AND d.specialization = ? ");
            params.add(specialization.trim());
        }
        if (city != null && !city.trim().isEmpty() && !city.equalsIgnoreCase("ALL")) {
            whereClause.append("AND d.city = ? ");
            params.add(city.trim());
        }
        if (state != null && !state.trim().isEmpty() && !state.equalsIgnoreCase("ALL")) {
            whereClause.append("AND d.state = ? ");
            params.add(state.trim());
        }
        if (minFee != null && minFee > 0) {
            whereClause.append("AND d.consultation_fee >= ? ");
            params.add(minFee);
        }
        if (maxFee != null && maxFee > 0) {
            whereClause.append("AND d.consultation_fee <= ? ");
            params.add(maxFee);
        }
        if (minRating != null && minRating > 0) {
            whereClause.append("AND d.rating >= ? ");
            params.add(minRating);
        }
        if (minExp != null && minExp > 0) {
            whereClause.append("AND d.experience_years >= ? ");
            params.add(minExp);
        }
        if (gender != null && !gender.trim().isEmpty() && !gender.equalsIgnoreCase("ALL")) {
            whereClause.append("AND d.gender = ? ");
            params.add(gender.trim());
        }
        whereClause.append("AND d.status = 'ACTIVE' ");

        // 1. Count Total Matching Records
        String countSql = "SELECT COUNT(*) FROM doctors d " + whereClause.toString();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement countPstmt = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) {
                countPstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = countPstmt.executeQuery()) {
                if (rs.next()) {
                    totalRecords = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 2. Fetch Paginated Slice
        String sortCol = "d.rating DESC";
        if ("fee_asc".equalsIgnoreCase(sortBy)) sortCol = "d.consultation_fee ASC";
        else if ("fee_desc".equalsIgnoreCase(sortBy)) sortCol = "d.consultation_fee DESC";
        else if ("exp_desc".equalsIgnoreCase(sortBy)) sortCol = "d.experience_years DESC";
        else if ("rating_desc".equalsIgnoreCase(sortBy)) sortCol = "d.rating DESC";

        int offset = (page - 1) * pageSize;
        String dataSql = "SELECT d.*, (SELECT h.name FROM doctor_hospitals dh JOIN hospitals h ON dh.hospital_id = h.id WHERE dh.doctor_id = d.id LIMIT 1) as hospital_name " +
                         "FROM doctors d " +
                         whereClause.toString() +
                         "ORDER BY " + sortCol + " LIMIT ? OFFSET ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement dataPstmt = conn.prepareStatement(dataSql)) {
            int idx = 1;
            for (Object p : params) {
                dataPstmt.setObject(idx++, p);
            }
            dataPstmt.setInt(idx++, pageSize);
            dataPstmt.setInt(idx++, offset);

            try (ResultSet rs = dataPstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDoctor(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new PaginatedResult<>(totalRecords, page, pageSize, list);
    }

    public List<Doctor> getAllDoctors(String search, Integer deptId, String specialization, Integer hospitalId, String status) {
        // Capped safety limit to prevent memory overflow if page params omitted
        PaginatedResult<Doctor> result = getDoctorsPaginated(search, null, null, specialization, hospitalId, null, null, null, null, null, 1, 50, "rating_desc", "DESC");
        return result.getData();
    }

    public Doctor getById(int id) {
        Doctor doc = null;
        String sql = "SELECT d.*, (SELECT h.name FROM doctor_hospitals dh JOIN hospitals h ON dh.hospital_id = h.id WHERE dh.doctor_id = d.id LIMIT 1) as hospital_name FROM doctors d WHERE d.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    doc = mapResultSetToDoctor(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return doc;
    }

    public boolean addDoctor(Doctor doc, int hospitalId, int deptId, int branchId) {
        String code = IDGenerator.generateId("doctors", "doctor_code", "DOC");
        doc.setDoctorCode(code);

        String sql = "INSERT INTO doctors (doctor_code, user_id, name, gender, dob, phone, email, specialization, sub_specialization, qualification, experience_years, consultation_fee, languages, license_no, bio, image_url, data_source) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DEMO')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, doc.getDoctorCode());
            pstmt.setObject(2, doc.getUserId());
            pstmt.setString(3, doc.getName());
            pstmt.setString(4, doc.getGender() != null ? doc.getGender() : "Male");
            pstmt.setString(5, doc.getDob() != null ? doc.getDob() : "1985-01-01");
            pstmt.setString(6, doc.getPhone());
            pstmt.setString(7, doc.getEmail());
            pstmt.setString(8, doc.getSpecialization());
            pstmt.setString(9, doc.getSubSpecialization());
            pstmt.setString(10, doc.getQualification());
            pstmt.setInt(11, doc.getExperienceYears());
            pstmt.setDouble(12, doc.getConsultationFee() > 0 ? doc.getConsultationFee() : 600.00);
            pstmt.setString(13, doc.getLanguages() != null ? doc.getLanguages() : "English, Hindi");
            pstmt.setString(14, doc.getLicenseNo() != null ? doc.getLicenseNo() : "LIC-" + System.currentTimeMillis());
            pstmt.setString(15, doc.getBio());
            pstmt.setString(16, doc.getImageUrl() != null ? doc.getImageUrl() : "https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=400&q=80");

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int docId = rs.getInt(1);
                        mapDoctorToHospital(docId, hospitalId, branchId, deptId, "Cabin 101");
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean mapDoctorToHospital(int doctorId, int hospitalId, int branchId, int deptId, String roomNo) {
        String sql = "INSERT INTO doctor_hospitals (doctor_id, hospital_id, branch_id, dept_id, room_no) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, doctorId);
            pstmt.setInt(2, hospitalId);
            pstmt.setInt(3, branchId);
            pstmt.setInt(4, deptId);
            pstmt.setString(5, roomNo);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Doctor mapResultSetToDoctor(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.setId(rs.getInt("id"));
        d.setDoctorCode(rs.getString("doctor_code"));
        Object uid = rs.getObject("user_id");
        if (uid != null) d.setUserId((Integer) uid);
        d.setName(rs.getString("name"));
        d.setGender(rs.getString("gender"));
        d.setDob(rs.getString("dob"));
        d.setPhone(rs.getString("phone"));
        d.setEmail(rs.getString("email"));
        d.setSpecialization(rs.getString("specialization"));
        d.setSubSpecialization(rs.getString("sub_specialization"));
        d.setQualification(rs.getString("qualification"));
        d.setExperienceYears(rs.getInt("experience_years"));
        d.setConsultationFee(rs.getDouble("consultation_fee"));
        d.setLanguages(rs.getString("languages"));
        d.setLicenseNo(rs.getString("license_no"));
        d.setRating(rs.getDouble("rating"));
        d.setReviewCount(rs.getInt("review_count"));
        d.setBio(rs.getString("bio"));
        d.setImageUrl(rs.getString("image_url"));
        d.setVerificationStatus(rs.getString("verification_status"));
        d.setStatus(rs.getString("status"));
        d.setCreatedAt(rs.getTimestamp("created_at"));

        try {
            d.setHospitalId(rs.getInt("hospital_id"));
            d.setHospitalName(rs.getString("hospital_name"));
        } catch (Exception e) {}
        return d;
    }
}
