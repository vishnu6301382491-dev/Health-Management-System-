package com.hospital.dao;

import com.hospital.model.Prescription;
import com.hospital.model.PrescriptionItem;
import com.hospital.util.DBConnection;
import com.hospital.util.IDGenerator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionDAO {

    public List<Prescription> getByPatientId(int patientId) {
        List<Prescription> list = new ArrayList<>();
        String sql = "SELECT pr.*, p.name as patient_name, p.patient_code, doc.name as doctor_name " +
                     "FROM prescriptions pr " +
                     "JOIN patients p ON pr.patient_id = p.id " +
                     "JOIN doctors doc ON pr.doctor_id = doc.id " +
                     "WHERE pr.patient_id = ? ORDER BY pr.id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, patientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Prescription p = mapResultSetToPrescription(rs);
                    p.setItems(getPrescriptionItems(p.getId()));
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Prescription> getAllPrescriptions(String search, Integer doctorId, Integer patientId) {
        List<Prescription> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT pr.*, p.name as patient_name, p.patient_code, doc.name as doctor_name " +
            "FROM prescriptions pr " +
            "JOIN patients p ON pr.patient_id = p.id " +
            "JOIN doctors doc ON pr.doctor_id = doc.id WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (pr.prescription_code LIKE ? OR p.name LIKE ? OR doc.name LIKE ? OR pr.diagnosis LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term); params.add(term); params.add(term); params.add(term);
        }
        if (doctorId != null && doctorId > 0) {
            sql.append("AND pr.doctor_id = ? ");
            params.add(doctorId);
        }
        if (patientId != null && patientId > 0) {
            sql.append("AND pr.patient_id = ? ");
            params.add(patientId);
        }
        sql.append("ORDER BY pr.id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Prescription p = mapResultSetToPrescription(rs);
                    p.setItems(getPrescriptionItems(p.getId()));
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Prescription getById(int id) {
        String sql = "SELECT pr.*, p.name as patient_name, p.patient_code, doc.name as doctor_name " +
                     "FROM prescriptions pr " +
                     "JOIN patients p ON pr.patient_id = p.id " +
                     "JOIN doctors doc ON pr.doctor_id = doc.id WHERE pr.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Prescription p = mapResultSetToPrescription(rs);
                    p.setItems(getPrescriptionItems(p.getId()));
                    return p;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean createPrescription(Prescription prescription) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String code = IDGenerator.generateId("prescriptions", "prescription_code", "PRE");
            prescription.setPrescriptionCode(code);

            String pSql = "INSERT INTO prescriptions (prescription_code, appointment_id, patient_id, doctor_id, visit_date, diagnosis, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(pSql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, prescription.getPrescriptionCode());
            pstmt.setObject(2, prescription.getAppointmentId(), Types.INTEGER);
            pstmt.setInt(3, prescription.getPatientId());
            pstmt.setInt(4, prescription.getDoctorId());
            pstmt.setString(5, prescription.getVisitDate());
            pstmt.setString(6, prescription.getDiagnosis());
            pstmt.setString(7, prescription.getNotes());

            pstmt.executeUpdate();
            ResultSet rsKeys = pstmt.getGeneratedKeys();
            int pId = -1;
            if (rsKeys.next()) {
                pId = rsKeys.getInt(1);
            }

            if (pId <= 0) {
                conn.rollback();
                return false;
            }

            String itemSql = "INSERT INTO prescription_items (prescription_id, medicine_name, dosage, frequency, duration, instructions) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement itemStmt = conn.prepareStatement(itemSql);
            for (PrescriptionItem item : prescription.getItems()) {
                itemStmt.setInt(1, pId);
                itemStmt.setString(2, item.getMedicineName());
                itemStmt.setString(3, item.getDosage());
                itemStmt.setString(4, item.getFrequency());
                itemStmt.setString(5, item.getDuration());
                itemStmt.setString(6, item.getInstructions());
                itemStmt.addBatch();
            }
            itemStmt.executeBatch();

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
        return false;
    }

    private List<PrescriptionItem> getPrescriptionItems(int prescriptionId) {
        List<PrescriptionItem> list = new ArrayList<>();
        String sql = "SELECT * FROM prescription_items WHERE prescription_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, prescriptionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PrescriptionItem item = new PrescriptionItem();
                    item.setId(rs.getInt("id"));
                    item.setPrescriptionId(rs.getInt("prescription_id"));
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setDosage(rs.getString("dosage"));
                    item.setFrequency(rs.getString("frequency"));
                    item.setDuration(rs.getString("duration"));
                    item.setInstructions(rs.getString("instructions"));
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Prescription mapResultSetToPrescription(ResultSet rs) throws SQLException {
        Prescription p = new Prescription();
        p.setId(rs.getInt("id"));
        p.setPrescriptionCode(rs.getString("prescription_code"));
        p.setAppointmentId((Integer) rs.getObject("appointment_id"));
        p.setPatientId(rs.getInt("patient_id"));
        p.setPatientName(rs.getString("patient_name"));
        p.setPatientCode(rs.getString("patient_code"));
        p.setDoctorId(rs.getInt("doctor_id"));
        p.setDoctorName(rs.getString("doctor_name"));
        p.setVisitDate(rs.getDate("visit_date").toString());
        p.setDiagnosis(rs.getString("diagnosis"));
        p.setNotes(rs.getString("notes"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        return p;
    }
}
