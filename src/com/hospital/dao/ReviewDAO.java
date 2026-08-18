package com.hospital.dao;

import com.hospital.model.Review;
import com.hospital.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    public List<Review> getReviewsByHospital(int hospitalId) {
        List<Review> list = new ArrayList<>();
        String sql = "SELECT r.*, p.name as patient_name, doc.name as doctor_name, h.name as hospital_name " +
                     "FROM reviews r " +
                     "JOIN patients p ON r.patient_id = p.id " +
                     "JOIN doctors doc ON r.doctor_id = doc.id " +
                     "JOIN hospitals h ON r.hospital_id = h.id " +
                     "WHERE r.hospital_id = ? ORDER BY r.id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, hospitalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Review rev = new Review();
                    rev.setId(rs.getInt("id"));
                    rev.setPatientId(rs.getInt("patient_id"));
                    rev.setPatientName(rs.getString("patient_name"));
                    rev.setHospitalId(rs.getInt("hospital_id"));
                    rev.setHospitalName(rs.getString("hospital_name"));
                    rev.setDoctorId(rs.getInt("doctor_id"));
                    rev.setDoctorName(rs.getString("doctor_name"));
                    rev.setAppointmentId((Integer) rs.getObject("appointment_id"));
                    rev.setRating(rs.getInt("rating"));
                    rev.setDoctorRating(rs.getInt("doctor_rating"));
                    rev.setStaffRating(rs.getInt("staff_rating"));
                    rev.setCleanlinessRating(rs.getInt("cleanliness_rating"));
                    rev.setReviewText(rs.getString("review_text"));
                    rev.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(rev);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addReview(Review r) {
        String sql = "INSERT INTO reviews (patient_id, hospital_id, doctor_id, appointment_id, rating, doctor_rating, staff_rating, cleanliness_rating, review_text) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, r.getPatientId());
            pstmt.setInt(2, r.getHospitalId());
            pstmt.setInt(3, r.getDoctorId());
            pstmt.setObject(4, r.getAppointmentId(), Types.INTEGER);
            pstmt.setInt(5, r.getRating());
            pstmt.setInt(6, r.getDoctorRating() > 0 ? r.getDoctorRating() : r.getRating());
            pstmt.setInt(7, r.getStaffRating() > 0 ? r.getStaffRating() : r.getRating());
            pstmt.setInt(8, r.getCleanlinessRating() > 0 ? r.getCleanlinessRating() : r.getRating());
            pstmt.setString(9, r.getReviewText());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
