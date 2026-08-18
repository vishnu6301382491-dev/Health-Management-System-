package com.hospital.service;

import com.hospital.dao.DepartmentDAO;
import com.hospital.model.Department;

import java.util.List;

public class DepartmentService {
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    public List<Department> getAllDepartments() {
        return departmentDAO.getAllDepartments();
    }

    public Department getById(int id) {
        return departmentDAO.getById(id);
    }

    public boolean addDepartment(Department dept) {
        if (dept.getName() == null || dept.getName().trim().isEmpty()) return false;
        if (dept.getDeptCode() == null || dept.getDeptCode().trim().isEmpty()) {
            dept.setDeptCode("DEP-" + dept.getName().toUpperCase().replaceAll("[^A-Z]", "").substring(0, Math.min(4, dept.getName().length())));
        }
        return departmentDAO.addDepartment(dept);
    }

    public boolean updateDepartment(Department dept) {
        return departmentDAO.updateDepartment(dept);
    }

    public boolean deleteDepartment(int id) {
        return departmentDAO.deleteDepartment(id);
    }
}
