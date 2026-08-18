package com.hospital.dao;

import com.hospital.model.BloodBank;
import com.hospital.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BloodBankDAO {

    public List<BloodBank> getBloodAvailability(String city, String bloodGroup) {
        List<BloodBank> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT bb.*, h.name as hospital_name FROM blood_bank bb JOIN hospitals h ON bb.hospital_id = h.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (city != null && !city.trim().isEmpty()) {
            sql.append("AND h.city = ? ");
            params.add(city.trim());
        }
        if (bloodGroup != null && !bloodGroup.trim().isEmpty()) {
            sql.append("AND bb.blood_group = ? ");
            params.add(bloodGroup.trim());
        }

        sql.append("ORDER BY bb.units_available DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BloodBank bb = new BloodBank();
                    bb.setId(rs.getInt("id"));
                    bb.setHospitalId(rs.getInt("hospital_id"));
                    bb.setHospitalName(rs.getString("hospital_name"));
                    bb.setBloodGroup(rs.getString("blood_group"));
                    bb.setUnitsAvailable(rs.getInt("units_available"));
                    bb.setLastUpdated(rs.getTimestamp("last_updated"));
                    list.add(bb);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
