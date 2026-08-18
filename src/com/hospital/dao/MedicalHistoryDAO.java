package com.hospital.dao;

import com.hospital.model.MedicalHistory;
import com.hospital.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicalHistoryDAO {

    public List<MedicalHistory> getByPatientId(int patientId) {
        List<MedicalHistory> list = new ArrayList<>();
        String sql = "SELECT mh.*, p.name as patient_name, doc.name as doctor_name " +
                     "FROM medical_histories mh " +
                     "JOIN patients p ON mh.patient_id = p.id " +
                     "JOIN doctors doc ON mh.doctor_id = doc.id " +
                     "WHERE mh.patient_id = ? ORDER BY mh.visit_date DESC, mh.id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, patientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMedicalHistory(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public MedicalHistory getById(int id) {
        String sql = "SELECT mh.*, p.name as patient_name, doc.name as doctor_name " +
                     "FROM medical_histories mh " +
                     "JOIN patients p ON mh.patient_id = p.id " +
                     "JOIN doctors doc ON mh.doctor_id = doc.id WHERE mh.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMedicalHistory(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addMedicalRecord(MedicalHistory record) {
        String sql = "INSERT INTO medical_histories (patient_id, appointment_id, doctor_id, visit_date, symptoms, diagnosis, treatment_plan, bp, heart_rate, temp_c, oxygen_sat, weight_kg, height_cm, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, record.getPatientId());
            pstmt.setObject(2, record.getAppointmentId(), Types.INTEGER);
            pstmt.setInt(3, record.getDoctorId());
            pstmt.setString(4, record.getVisitDate());
            pstmt.setString(5, record.getSymptoms());
            pstmt.setString(6, record.getDiagnosis());
            pstmt.setString(7, record.getTreatmentPlan());
            pstmt.setString(8, record.getBp());
            pstmt.setObject(9, record.getHeartRate(), Types.INTEGER);
            pstmt.setObject(10, record.getTempC(), Types.DECIMAL);
            pstmt.setObject(11, record.getOxygenSat(), Types.INTEGER);
            pstmt.setObject(12, record.getWeightKg(), Types.DECIMAL);
            pstmt.setObject(13, record.getHeightCm(), Types.DECIMAL);
            pstmt.setString(14, record.getNotes());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private MedicalHistory mapResultSetToMedicalHistory(ResultSet rs) throws SQLException {
        MedicalHistory mh = new MedicalHistory();
        mh.setId(rs.getInt("id"));
        mh.setPatientId(rs.getInt("patient_id"));
        mh.setPatientName(rs.getString("patient_name"));
        mh.setAppointmentId((Integer) rs.getObject("appointment_id"));
        mh.setDoctorId(rs.getInt("doctor_id"));
        mh.setDoctorName(rs.getString("doctor_name"));
        mh.setVisitDate(rs.getDate("visit_date").toString());
        mh.setSymptoms(rs.getString("symptoms"));
        mh.setDiagnosis(rs.getString("diagnosis"));
        mh.setTreatmentPlan(rs.getString("treatment_plan"));
        mh.setBp(rs.getString("bp"));
        mh.setHeartRate((Integer) rs.getObject("heart_rate"));
        mh.setTempC((Double) rs.getObject("temp_c"));
        mh.setOxygenSat((Integer) rs.getObject("oxygen_sat"));
        mh.setWeightKg((Double) rs.getObject("weight_kg"));
        mh.setHeightCm((Double) rs.getObject("height_cm"));
        mh.setNotes(rs.getString("notes"));
        mh.setCreatedAt(rs.getTimestamp("created_at"));
        return mh;
    }
}
