package com.hospital.dao;

import com.hospital.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuditDAO {

    public static void log(Integer userId, String username, Integer hospitalId, String action, String module, String details) {
        String sql = "INSERT INTO audit_logs (user_id, username, hospital_id, action, module, details) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, userId, Types.INTEGER);
            pstmt.setString(2, username != null ? username : "SYSTEM");
            pstmt.setObject(3, hospitalId, Types.INTEGER);
            pstmt.setString(4, action);
            pstmt.setString(5, module);
            pstmt.setString(6, details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Map<String, Object>> getAuditLogs(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs ORDER BY id DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit > 0 ? limit : 50);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("userId", rs.getObject("user_id"));
                    map.put("username", rs.getString("username"));
                    map.put("hospitalId", rs.getObject("hospital_id"));
                    map.put("action", rs.getString("action"));
                    map.put("module", rs.getString("module"));
                    map.put("details", rs.getString("details"));
                    map.put("timestamp", rs.getTimestamp("timestamp") != null ? rs.getTimestamp("timestamp").toString() : "");
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Map<String, Object>> getRecentLogs(int limit) {
        return getAuditLogs(limit);
    }
}
