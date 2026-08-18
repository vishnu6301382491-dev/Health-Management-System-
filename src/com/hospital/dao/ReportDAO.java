package com.hospital.dao;

import com.hospital.model.DashboardStats;
import com.hospital.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportDAO {

    public DashboardStats getAdminDashboardStats() {
        DashboardStats stats = new DashboardStats();
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM hospitals WHERE status = 'ACTIVE'")) {
                try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) stats.setTotalHospitals(rs.getInt(1)); }
            }
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM doctors WHERE status = 'ACTIVE'")) {
                try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) stats.setTotalDoctors(rs.getInt(1)); }
            }
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM patients")) {
                try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) stats.setTotalPatients(rs.getInt(1)); }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return stats;
    }

    public Map<String, Object> getRevenueReport(String startDate, String endDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("totalRevenue", 1250000.00);
        map.put("consultationRevenue", 450000.00);
        map.put("labRevenue", 350000.00);
        map.put("pharmacyRevenue", 280000.00);
        map.put("roomRevenue", 170000.00);
        return map;
    }
}
