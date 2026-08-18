package com.hospital.dao;

import com.hospital.model.Ambulance;
import com.hospital.model.BloodBank;
import com.hospital.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmergencyDAO {

    public List<Ambulance> getAmbulances(Integer hospitalId, String status) {
        List<Ambulance> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT a.*, h.name as hospital_name FROM ambulances a JOIN hospitals h ON a.hospital_id = h.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (hospitalId != null && hospitalId > 0) {
            sql.append("AND a.hospital_id = ? ");
            params.add(hospitalId);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND a.status = ? ");
            params.add(status.trim());
        }

        sql.append("ORDER BY a.id ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Ambulance amb = new Ambulance();
                    amb.setId(rs.getInt("id"));
                    amb.setAmbulanceCode(rs.getString("ambulance_code"));
                    amb.setHospitalId(rs.getInt("hospital_id"));
                    amb.setHospitalName(rs.getString("hospital_name"));
                    amb.setVehicleNumber(rs.getString("vehicle_number"));
                    amb.setDriverName(rs.getString("driver_name"));
                    amb.setDriverPhone(rs.getString("driver_phone"));
                    amb.setAmbulanceType(rs.getString("ambulance_type"));
                    amb.setStatus(rs.getString("status"));
                    amb.setCurrentLocation(rs.getString("current_location"));
                    list.add(amb);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Ambulance> getAmbulances(int hospitalId) {
        return getAmbulances(hospitalId, null);
    }

    public List<BloodBank> getBloodBankUnits(int hospitalId) {
        List<BloodBank> list = new ArrayList<>();
        String sql = "SELECT b.*, h.name as hospital_name FROM blood_bank b JOIN hospitals h ON b.hospital_id = h.id WHERE b.hospital_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, hospitalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BloodBank bb = new BloodBank();
                    bb.setId(rs.getInt("id"));
                    bb.setHospitalId(rs.getInt("hospital_id"));
                    bb.setHospitalName(rs.getString("hospital_name"));
                    bb.setBloodGroup(rs.getString("blood_group"));
                    bb.setUnitsAvailable(rs.getInt("units_available"));
                    list.add(bb);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean requestAmbulance(int patientId, int hospitalId, String pickupAddress, String phone) {
        String sql = "INSERT INTO ambulance_requests (patient_id, hospital_id, pickup_address, contact_phone, status) VALUES (?, ?, ?, ?, 'Requested')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, patientId);
            pstmt.setInt(2, hospitalId);
            pstmt.setString(3, pickupAddress);
            pstmt.setString(4, phone);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
