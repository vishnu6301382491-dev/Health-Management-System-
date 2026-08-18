package com.hospital.dao;

import com.hospital.model.LabTest;
import com.hospital.util.DBConnection;
import com.hospital.util.IDGenerator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LabDAO {

    public List<LabTest> getAllLabTests(String search, Integer doctorId, Integer patientId, String category, String status) {
        List<LabTest> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT lt.*, p.name as patient_name, p.patient_code, doc.name as doctor_name, lr.result_value, lr.reference_range, lr.remarks " +
            "FROM lab_tests lt " +
            "JOIN patients p ON lt.patient_id = p.id " +
            "JOIN doctors doc ON lt.doctor_id = doc.id " +
            "LEFT JOIN lab_results lr ON lt.id = lr.lab_test_id WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (lt.test_code LIKE ? OR lt.test_name LIKE ? OR p.name LIKE ? OR doc.name LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term); params.add(term); params.add(term); params.add(term);
        }
        if (doctorId != null && doctorId > 0) {
            sql.append("AND lt.doctor_id = ? ");
            params.add(doctorId);
        }
        if (patientId != null && patientId > 0) {
            sql.append("AND lt.patient_id = ? ");
            params.add(patientId);
        }
        if (category != null && !category.trim().isEmpty()) {
            sql.append("AND lt.category = ? ");
            params.add(category.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND lt.status = ? ");
            params.add(status.trim());
        }

        sql.append("ORDER BY lt.id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLabTest(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public LabTest getById(int id) {
        String sql = "SELECT lt.*, p.name as patient_name, p.patient_code, doc.name as doctor_name, lr.result_value, lr.reference_range, lr.remarks " +
                     "FROM lab_tests lt " +
                     "JOIN patients p ON lt.patient_id = p.id " +
                     "JOIN doctors doc ON lt.doctor_id = doc.id " +
                     "LEFT JOIN lab_results lr ON lt.id = lr.lab_test_id WHERE lt.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLabTest(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean requestLabTest(LabTest test) {
        String code = IDGenerator.generateId("lab_tests", "test_code", "LAB");
        test.setTestCode(code);

        String sql = "INSERT INTO lab_tests (test_code, patient_id, doctor_id, appointment_id, test_name, category, sample_type, test_date, status, lab_technician) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, test.getTestCode());
            pstmt.setInt(2, test.getPatientId());
            pstmt.setInt(3, test.getDoctorId());
            pstmt.setObject(4, test.getAppointmentId(), Types.INTEGER);
            pstmt.setString(5, test.getTestName());
            pstmt.setString(6, test.getCategory());
            pstmt.setString(7, test.getSampleType());
            pstmt.setString(8, test.getTestDate());
            pstmt.setString(9, test.getStatus() != null ? test.getStatus() : "Requested");
            pstmt.setString(10, test.getLabTechnician());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateStatus(int testId, String status, String technician) {
        String sql = "UPDATE lab_tests SET status = ?, lab_technician = COALESCE(?, lab_technician) WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, technician);
            pstmt.setInt(3, testId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addOrUpdateResult(int testId, String resultValue, String referenceRange, String remarks) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String checkSql = "SELECT id FROM lab_results WHERE lab_test_id = ?";
            boolean exists = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, testId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) exists = true;
                }
            }

            if (exists) {
                String updateSql = "UPDATE lab_results SET result_value = ?, reference_range = ?, remarks = ?, result_date = CURRENT_TIMESTAMP WHERE lab_test_id = ?";
                try (PreparedStatement uStmt = conn.prepareStatement(updateSql)) {
                    uStmt.setString(1, resultValue);
                    uStmt.setString(2, referenceRange);
                    uStmt.setString(3, remarks);
                    uStmt.setInt(4, testId);
                    uStmt.executeUpdate();
                }
            } else {
                String insertSql = "INSERT INTO lab_results (lab_test_id, result_value, reference_range, remarks) VALUES (?, ?, ?, ?)";
                try (PreparedStatement iStmt = conn.prepareStatement(insertSql)) {
                    iStmt.setInt(1, testId);
                    iStmt.setString(2, resultValue);
                    iStmt.setString(3, referenceRange);
                    iStmt.setString(4, remarks);
                    iStmt.executeUpdate();
                }
            }

            // Also mark test status as Completed
            String updateStatusSql = "UPDATE lab_tests SET status = 'Completed' WHERE id = ?";
            try (PreparedStatement statusStmt = conn.prepareStatement(updateStatusSql)) {
                statusStmt.setInt(1, testId);
                statusStmt.executeUpdate();
            }

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

    private LabTest mapResultSetToLabTest(ResultSet rs) throws SQLException {
        LabTest lt = new LabTest();
        lt.setId(rs.getInt("id"));
        lt.setTestCode(rs.getString("test_code"));
        lt.setPatientId(rs.getInt("patient_id"));
        lt.setPatientName(rs.getString("patient_name"));
        lt.setPatientCode(rs.getString("patient_code"));
        lt.setDoctorId(rs.getInt("doctor_id"));
        lt.setDoctorName(rs.getString("doctor_name"));
        lt.setAppointmentId((Integer) rs.getObject("appointment_id"));
        lt.setTestName(rs.getString("test_name"));
        lt.setCategory(rs.getString("category"));
        lt.setSampleType(rs.getString("sample_type"));
        lt.setTestDate(rs.getDate("test_date").toString());
        lt.setStatus(rs.getString("status"));
        lt.setLabTechnician(rs.getString("lab_technician"));
        lt.setCreatedAt(rs.getTimestamp("created_at"));
        lt.setResultValue(rs.getString("result_value"));
        lt.setReferenceRange(rs.getString("reference_range"));
        lt.setRemarks(rs.getString("remarks"));
        return lt;
    }
}
