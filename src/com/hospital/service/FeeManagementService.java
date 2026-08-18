package com.hospital.service;

import com.hospital.util.DBConnection;

import java.sql.*;
import java.util.*;

public class FeeManagementService {

    public Map<String, Object> calculateAppointmentFee(int doctorId, int hospitalId, String appointmentType) {
        Map<String, Object> feeMap = new HashMap<>();
        double baseFee = 600.00;
        double followupFee = 400.00;
        double videoFee = 500.00;
        double emergencyFee = 1200.00;
        String docName = "Doctor";
        String spec = "General Physician";

        String sql = "SELECT d.name, d.specialization, d.consultation_fee, d.followup_fee, d.video_consultation_fee, d.emergency_consultation_fee, dh.consultation_fee as dh_fee " +
                     "FROM doctors d LEFT JOIN doctor_hospitals dh ON d.id = dh.doctor_id AND dh.hospital_id = ? " +
                     "WHERE d.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, hospitalId);
            pstmt.setInt(2, doctorId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    docName = rs.getString("name");
                    spec = rs.getString("specialization");
                    double cFee = rs.getDouble("consultation_fee");
                    if (cFee > 0) baseFee = cFee;

                    double dhFee = rs.getDouble("dh_fee");
                    if (dhFee > 0) baseFee = dhFee;

                    followupFee = rs.getDouble("followup_fee") > 0 ? rs.getDouble("followup_fee") : Math.round(baseFee * 0.6);
                    videoFee = rs.getDouble("video_consultation_fee") > 0 ? rs.getDouble("video_consultation_fee") : Math.round(baseFee * 0.85);
                    emergencyFee = rs.getDouble("emergency_consultation_fee") > 0 ? rs.getDouble("emergency_consultation_fee") : Math.round(baseFee * 1.75);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        double selectedFee = baseFee;
        if (appointmentType != null) {
            if (appointmentType.contains("Video")) selectedFee = videoFee;
            else if (appointmentType.contains("Follow")) selectedFee = followupFee;
            else if (appointmentType.contains("Emergency")) selectedFee = emergencyFee;
        }

        double serviceCharge = 50.00;
        double subtotal = selectedFee + serviceCharge;
        double taxRate = 0.05;
        double taxAmount = Math.round((subtotal * taxRate) * 100.0) / 100.0;
        double discount = 0.00;
        double totalAmount = Math.round((subtotal + taxAmount - discount) * 100.0) / 100.0;

        feeMap.put("doctorId", doctorId);
        feeMap.put("doctorName", docName);
        feeMap.put("specialization", spec);
        feeMap.put("appointmentType", appointmentType != null ? appointmentType : "In-Person");
        feeMap.put("baseFee", selectedFee);
        feeMap.put("serviceCharge", serviceCharge);
        feeMap.put("subtotal", subtotal);
        feeMap.put("taxRate", taxRate);
        feeMap.put("taxAmount", taxAmount);
        feeMap.put("discount", discount);
        feeMap.put("totalAmount", totalAmount);

        return feeMap;
    }

    public int bulkUpdateFees(String specialization, String city, Double newFee, Double multiplier) {
        StringBuilder sql = new StringBuilder("UPDATE doctors SET ");
        List<Object> params = new ArrayList<>();

        if (newFee != null && newFee > 0) {
            sql.append("consultation_fee = ?, followup_fee = ? * 0.6, video_consultation_fee = ? * 0.8, emergency_consultation_fee = ? * 1.75 ");
            params.add(newFee);
            params.add(newFee);
            params.add(newFee);
            params.add(newFee);
        } else if (multiplier != null && multiplier > 0) {
            sql.append("consultation_fee = consultation_fee * ?, followup_fee = followup_fee * ?, video_consultation_fee = video_consultation_fee * ?, emergency_consultation_fee = emergency_consultation_fee * ? ");
            params.add(multiplier);
            params.add(multiplier);
            params.add(multiplier);
            params.add(multiplier);
        } else {
            return 0;
        }

        sql.append("WHERE status = 'ACTIVE' ");
        if (specialization != null && !specialization.trim().isEmpty() && !specialization.equalsIgnoreCase("ALL")) {
            sql.append("AND specialization = ? ");
            params.add(specialization.trim());
        }
        if (city != null && !city.trim().isEmpty() && !city.equalsIgnoreCase("ALL")) {
            sql.append("AND city = ? ");
            params.add(city.trim());
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
