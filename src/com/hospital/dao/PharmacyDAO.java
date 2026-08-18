package com.hospital.dao;

import com.hospital.model.Medicine;
import com.hospital.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PharmacyDAO {

    public List<Medicine> getAllMedicines(String search, String category) {
        List<Medicine> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT m.*, COALESCE(SUM(pi.stock_quantity), 100) as stock FROM medicines m LEFT JOIN pharmacy_inventory pi ON m.id = pi.medicine_id WHERE m.status = 'ACTIVE' ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (m.name LIKE ? OR m.manufacturer LIKE ? OR m.medicine_code LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term); params.add(term); params.add(term);
        }
        if (category != null && !category.trim().isEmpty()) {
            sql.append("AND m.category = ? ");
            params.add(category.trim());
        }

        sql.append("GROUP BY m.id ORDER BY m.name ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Medicine m = new Medicine();
                    m.setId(rs.getInt("id"));
                    m.setMedicineCode(rs.getString("medicine_code"));
                    m.setName(rs.getString("name"));
                    m.setCategory(rs.getString("category"));
                    m.setManufacturer(rs.getString("manufacturer"));
                    m.setUnitPrice(rs.getDouble("unit_price"));
                    m.setRequiresPrescription(rs.getBoolean("requires_prescription"));
                    m.setStatus(rs.getString("status"));
                    m.setStockQuantity(rs.getInt("stock"));
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Medicine> getMedicines(String search) {
        return getAllMedicines(search, null);
    }
}
