package com.hospital.util;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class ResetPasswords {
    public static void main(String[] args) throws Exception {
        String salt = PasswordUtil.DEFAULT_SALT;
        String adminHash = PasswordUtil.hashPassword("admin123", salt);
        String docHash = PasswordUtil.hashPassword("doc123", salt);
        String patHash = PasswordUtil.hashPassword("pat123", salt);
        String recHash = PasswordUtil.hashPassword("rec123", salt);

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE role IN ('SUPER_ADMIN', 'HOSPITAL_ADMIN')")) {
                pstmt.setString(1, adminHash);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE role = 'DOCTOR'")) {
                pstmt.setString(1, docHash);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE role = 'PATIENT'")) {
                pstmt.setString(1, patHash);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE role = 'RECEPTIONIST'")) {
                pstmt.setString(1, recHash);
                pstmt.executeUpdate();
            }
            System.out.println("Multi-hospital platform user passwords reset successfully!");
        }
    }
}
