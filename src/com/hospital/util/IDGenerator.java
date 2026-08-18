package com.hospital.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class IDGenerator {

    public static String generateId(String tableName, String idColumnName, String prefix) {
        int nextNum = 1;
        int prefixLen = prefix != null ? prefix.length() : 0;
        String query = "SELECT COALESCE(MAX(CAST(SUBSTRING(" + idColumnName + ", " + (prefixLen + 1) + ") AS UNSIGNED)), 0) FROM " + tableName + " WHERE " + idColumnName + " LIKE '" + prefix + "%'";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                nextNum = rs.getInt(1) + 1;
                if (nextNum <= 0) nextNum = 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String code = String.format("%s%05d", prefix, nextNum);

        // Ensure collision safety by verifying existence
        int safetyCounter = 0;
        while (existsInDb(tableName, idColumnName, code) && safetyCounter < 10000) {
            nextNum++;
            safetyCounter++;
            code = String.format("%s%05d", prefix, nextNum);
        }

        return code;
    }

    public static boolean existsInDb(String tableName, String columnName, String value) {
        if (value == null || value.trim().isEmpty()) return false;
        String query = "SELECT 1 FROM " + tableName + " WHERE " + columnName + " = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, value.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
