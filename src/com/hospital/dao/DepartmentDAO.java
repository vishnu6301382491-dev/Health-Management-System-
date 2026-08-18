package com.hospital.dao;

import com.hospital.model.Department;
import com.hospital.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDAO {

    public List<Department> getAllDepartments() {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT d.*, (SELECT COUNT(*) FROM doctors doc WHERE doc.dept_id = d.id AND doc.status = 'ACTIVE') as doctor_count " +
                     "FROM departments d ORDER BY d.name ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Department dept = new Department();
                dept.setId(rs.getInt("id"));
                dept.setDeptCode(rs.getString("dept_code"));
                dept.setName(rs.getString("name"));
                dept.setDescription(rs.getString("description"));
                dept.setHeadDoctorName(rs.getString("head_doctor_name"));
                dept.setStatus(rs.getString("status"));
                dept.setCreatedAt(rs.getTimestamp("created_at"));
                dept.setDoctorCount(rs.getInt("doctor_count"));
                list.add(dept);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Department getById(int id) {
        String sql = "SELECT d.*, (SELECT COUNT(*) FROM doctors doc WHERE doc.dept_id = d.id) as doctor_count " +
                     "FROM departments d WHERE d.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Department dept = new Department();
                    dept.setId(rs.getInt("id"));
                    dept.setDeptCode(rs.getString("dept_code"));
                    dept.setName(rs.getString("name"));
                    dept.setDescription(rs.getString("description"));
                    dept.setHeadDoctorName(rs.getString("head_doctor_name"));
                    dept.setStatus(rs.getString("status"));
                    dept.setCreatedAt(rs.getTimestamp("created_at"));
                    dept.setDoctorCount(rs.getInt("doctor_count"));
                    return dept;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addDepartment(Department dept) {
        String sql = "INSERT INTO departments (dept_code, name, description, head_doctor_name, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, dept.getDeptCode());
            pstmt.setString(2, dept.getName());
            pstmt.setString(3, dept.getDescription());
            pstmt.setString(4, dept.getHeadDoctorName());
            pstmt.setString(5, dept.getStatus() != null ? dept.getStatus() : "ACTIVE");
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateDepartment(Department dept) {
        String sql = "UPDATE departments SET name = ?, description = ?, head_doctor_name = ?, status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, dept.getName());
            pstmt.setString(2, dept.getDescription());
            pstmt.setString(3, dept.getHeadDoctorName());
            pstmt.setString(4, dept.getStatus());
            pstmt.setInt(5, dept.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteDepartment(int id) {
        String sql = "DELETE FROM departments WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
