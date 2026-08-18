package com.hospital.service;

import com.hospital.dao.ReportDAO;
import com.hospital.model.DashboardStats;
import com.hospital.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class ReportService {

    private final ReportDAO reportDAO = new ReportDAO();

    public DashboardStats getAdminDashboardStats() {
        DashboardStats stats = reportDAO.getAdminDashboardStats();
        // Enrich stats with extra KPI metrics
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM hospital_branches WHERE status = 'ACTIVE'")) {
                try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) stats.setTotalBranches(rs.getInt(1)); }
            }
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM appointments WHERE appointment_date = CURRENT_DATE()")) {
                try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) stats.setTodayAppointments(rs.getInt(1)); }
            }
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM appointments WHERE status = 'Pending'")) {
                try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) stats.setPendingAppointments(rs.getInt(1)); }
            }
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM appointments WHERE status = 'Completed'")) {
                try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) stats.setCompletedAppointments(rs.getInt(1)); }
            }
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM appointments WHERE status = 'Cancelled'")) {
                try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) stats.setCancelledAppointments(rs.getInt(1)); }
            }
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COALESCE(SUM(total_amount), 0) FROM bills WHERE payment_status = 'Paid'")) {
                try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) stats.setTotalRevenue(rs.getDouble(1)); }
            }
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COALESCE(SUM(remaining_amount), 0) FROM bills WHERE payment_status != 'Paid'")) {
                try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) stats.setPendingPayments(rs.getDouble(1)); }
            }
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM doctors WHERE status = 'ACTIVE'")) {
                try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) stats.setAvailableDoctors(rs.getInt(1)); }
            }
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM hospitals WHERE emergency_phone IS NOT NULL AND status = 'ACTIVE'")) {
                try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) stats.setEmergencyHospitals(rs.getInt(1)); }
            }
        } catch (Exception e) { e.printStackTrace(); }

        return stats;
    }

    public DashboardStats getDashboardStats(Integer hospitalId) {
        return getAdminDashboardStats();
    }

    public Map<String, Object> getRevenueReport(String startDate, String endDate) {
        return reportDAO.getRevenueReport(startDate, endDate);
    }
}
