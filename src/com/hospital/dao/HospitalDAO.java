package com.hospital.dao;

import com.hospital.model.Hospital;
import com.hospital.model.HospitalBranch;
import com.hospital.util.DBConnection;
import com.hospital.util.IDGenerator;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HospitalDAO {

    static {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE INDEX idx_hospitals_perf ON hospitals(status, city, type, rating)");
        } catch (Exception ignored) {}
    }

    public List<Map<String, Object>> searchHospitalsFast(String query, int limit) {
        if (limit < 1 || limit > 50) limit = 20;
        List<Map<String, Object>> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT id, hospital_code, name, city, state, type, rating FROM hospitals WHERE status = 'ACTIVE' ");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (name LIKE ? OR city LIKE ? OR pincode LIKE ?) ");
            String term = "%" + query.trim() + "%";
            params.add(term); params.add(term); params.add(term);
        }

        sql.append("ORDER BY rating DESC LIMIT ?");
        params.add(limit);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("hospitalCode", rs.getString("hospital_code"));
                    map.put("name", rs.getString("name"));
                    map.put("city", rs.getString("city"));
                    map.put("state", rs.getString("state"));
                    map.put("type", rs.getString("type"));
                    map.put("rating", rs.getDouble("rating"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Hospital> getAllHospitals(String search, String city, String type, Double minRating, Boolean pharmacy, Boolean bloodBank, Boolean ambulance, Boolean insurance) {
        List<Hospital> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM hospitals WHERE status = 'ACTIVE' ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (name LIKE ? OR description LIKE ? OR city LIKE ? OR pincode LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term); params.add(term); params.add(term); params.add(term);
        }
        if (city != null && !city.trim().isEmpty()) {
            sql.append("AND city = ? ");
            params.add(city.trim());
        }
        if (type != null && !type.trim().isEmpty()) {
            sql.append("AND type = ? ");
            params.add(type.trim());
        }
        if (minRating != null && minRating > 0) {
            sql.append("AND rating >= ? ");
            params.add(minRating);
        }
        if (pharmacy != null && pharmacy) sql.append("AND pharmacy_avail = TRUE ");
        if (bloodBank != null && bloodBank) sql.append("AND blood_bank_avail = TRUE ");
        if (ambulance != null && ambulance) sql.append("AND ambulance_avail = TRUE ");
        if (insurance != null && insurance) sql.append("AND insurance_support = TRUE ");

        sql.append("ORDER BY rating DESC, review_count DESC LIMIT 50");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToHospital(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public Hospital getById(int id) {
        Hospital h = null;
        String sql = "SELECT * FROM hospitals WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    h = mapResultSetToHospital(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (h != null) {
            h.setBranches(getBranchesByHospitalId(h.getId()));
        }
        return h;
    }

    public boolean addHospital(Hospital h) {
        if (h.getHospitalCode() == null || h.getHospitalCode().trim().isEmpty()) {
            h.setHospitalCode(IDGenerator.generateId("hospitals", "hospital_code", "HOSP"));
        }

        String sql = "INSERT INTO hospitals (hospital_code, name, type, description, address, city, state, pincode, phone, email, website, emergency_phone, pharmacy_avail, blood_bank_avail, ambulance_avail, insurance_support, rating, image_url, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, h.getHospitalCode());
            pstmt.setString(2, h.getName());
            pstmt.setString(3, h.getType() != null ? h.getType() : "Multi-Speciality");
            pstmt.setString(4, h.getDescription());
            pstmt.setString(5, h.getAddress());
            pstmt.setString(6, h.getCity());
            pstmt.setString(7, h.getState());
            pstmt.setString(8, h.getPincode());
            pstmt.setString(9, h.getPhone());
            pstmt.setString(10, h.getEmail());
            pstmt.setString(11, h.getWebsite());
            pstmt.setString(12, h.getEmergencyPhone());
            pstmt.setBoolean(13, h.isPharmacyAvail());
            pstmt.setBoolean(14, h.isBloodBankAvail());
            pstmt.setBoolean(15, h.isAmbulanceAvail());
            pstmt.setBoolean(16, h.isInsuranceSupport());
            pstmt.setDouble(17, h.getRating() > 0 ? h.getRating() : 4.5);
            pstmt.setString(18, h.getImageUrl());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        h.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<HospitalBranch> getBranchesByHospitalId(int hospitalId) {
        List<HospitalBranch> list = new ArrayList<>();
        String sql = "SELECT b.*, h.name as hospital_name FROM hospital_branches b JOIN hospitals h ON b.hospital_id = h.id WHERE b.hospital_id = ? AND b.status = 'ACTIVE'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, hospitalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    HospitalBranch hb = new HospitalBranch();
                    hb.setId(rs.getInt("id"));
                    hb.setBranchCode(rs.getString("branch_code"));
                    hb.setHospitalId(rs.getInt("hospital_id"));
                    hb.setHospitalName(rs.getString("hospital_name"));
                    hb.setBranchName(rs.getString("branch_name"));
                    hb.setAddress(rs.getString("address"));
                    hb.setCity(rs.getString("city"));
                    hb.setState(rs.getString("state"));
                    hb.setPincode(rs.getString("pincode"));
                    hb.setPhone(rs.getString("phone"));
                    hb.setEmergencyPhone(rs.getString("emergency_phone"));
                    hb.setStatus(rs.getString("status"));
                    list.add(hb);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Hospital mapResultSetToHospital(ResultSet rs) throws SQLException {
        Hospital h = new Hospital();
        h.setId(rs.getInt("id"));
        h.setHospitalCode(rs.getString("hospital_code"));
        h.setName(rs.getString("name"));
        h.setType(rs.getString("type"));
        h.setAddress(rs.getString("address"));
        h.setCity(rs.getString("city"));
        h.setState(rs.getString("state"));
        h.setPincode(rs.getString("pincode"));
        h.setPhone(rs.getString("phone"));
        h.setEmail(rs.getString("email"));
        h.setWebsite(rs.getString("website"));
        h.setEmergencyPhone(rs.getString("emergency_phone"));
        h.setPharmacyAvail(rs.getBoolean("pharmacy_avail"));
        h.setBloodBankAvail(rs.getBoolean("blood_bank_avail"));
        h.setAmbulanceAvail(rs.getBoolean("ambulance_avail"));
        h.setInsuranceSupport(rs.getBoolean("insurance_support"));
        h.setRating(rs.getDouble("rating"));
        h.setReviewCount(rs.getInt("review_count"));
        h.setDescription(rs.getString("description"));
        h.setImageUrl(rs.getString("image_url"));
        h.setStatus(rs.getString("status"));
        h.setCreatedAt(rs.getTimestamp("created_at"));
        return h;
    }
}
