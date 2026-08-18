package com.hospital.dao;

import com.hospital.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsuranceDAO {

    public List<Map<String, Object>> getClaims(Integer hospitalId) {
        List<Map<String, Object>> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT c.*, p.name as patient_name, h.name as hospital_name FROM insurance_claims c JOIN patients p ON c.patient_id = p.id JOIN hospitals h ON c.hospital_id = h.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (hospitalId != null && hospitalId > 0) {
            sql.append("AND c.hospital_id = ? ");
            params.add(hospitalId);
        }
        sql.append("ORDER BY c.id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) pstmt.setObject(i + 1, params.get(i));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("claimCode", rs.getString("claim_code"));
                    map.put("patientId", rs.getInt("patient_id"));
                    map.put("patientName", rs.getString("patient_name"));
                    map.put("hospitalId", rs.getInt("hospital_id"));
                    map.put("hospitalName", rs.getString("hospital_name"));
                    map.put("providerName", rs.getString("provider_name"));
                    map.put("claimAmount", rs.getDouble("claim_amount"));
                    map.put("status", rs.getString("status"));
                    map.put("submittedAt", rs.getTimestamp("submitted_at") != null ? rs.getTimestamp("submitted_at").toString() : "");
                    list.add(map);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Map<String, Object>> getAllClaims() {
        return getClaims(null);
    }
}
