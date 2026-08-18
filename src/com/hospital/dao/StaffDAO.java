package com.hospital.dao;

import com.hospital.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffDAO {

    public List<Map<String, Object>> getStaffList(Integer hospitalId, String role) {
        List<Map<String, Object>> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT s.*, h.name as hospital_name FROM staff s JOIN hospitals h ON s.hospital_id = h.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (hospitalId != null && hospitalId > 0) {
            sql.append("AND s.hospital_id = ? ");
            params.add(hospitalId);
        }
        if (role != null && !role.trim().isEmpty()) {
            sql.append("AND s.staff_role = ? ");
            params.add(role.trim());
        }
        sql.append("ORDER BY s.id ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) pstmt.setObject(i + 1, params.get(i));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("staffCode", rs.getString("staff_code"));
                    map.put("hospitalId", rs.getInt("hospital_id"));
                    map.put("hospitalName", rs.getString("hospital_name"));
                    map.put("name", rs.getString("name"));
                    map.put("gender", rs.getString("gender"));
                    map.put("phone", rs.getString("phone"));
                    map.put("email", rs.getString("email"));
                    map.put("staffRole", rs.getString("staff_role"));
                    map.put("designation", rs.getString("designation"));
                    map.put("salary", rs.getDouble("salary"));
                    map.put("status", rs.getString("status"));
                    list.add(map);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Map<String, Object>> getAllStaff() {
        return getStaffList(null, null);
    }
}
