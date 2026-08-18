package com.hospital.service;

import com.hospital.dao.DoctorDAO;
import com.hospital.model.Doctor;
import com.hospital.model.PaginatedResult;

import java.util.List;
import java.util.Map;

public class DoctorService {
    private DoctorDAO doctorDAO = new DoctorDAO();

    public List<Doctor> getAllDoctors(String search, Integer deptId, String specialization, Integer hospitalId, String status) {
        return doctorDAO.getAllDoctors(search, deptId, specialization, hospitalId, status);
    }

    public PaginatedResult<Doctor> getDoctorsPaginated(String search, String city, String state, String specialization, Integer hospitalId, Double minFee, Double maxFee, Double minRating, Integer minExp, String gender, int page, int pageSize, String sortBy, String sortOrder) {
        return doctorDAO.getDoctorsPaginated(search, city, state, specialization, hospitalId, minFee, maxFee, minRating, minExp, gender, page, pageSize, sortBy, sortOrder);
    }

    public List<Map<String, Object>> searchDoctorsFast(String query, Integer hospitalId, String spec, int limit) {
        return doctorDAO.searchDoctorsFast(query, hospitalId, spec, limit);
    }

    public Map<String, Object> getDoctorFees(int doctorId) {
        return doctorDAO.getDoctorFees(doctorId);
    }

    public Doctor getDoctorById(int id) {
        return doctorDAO.getById(id);
    }

    public boolean addDoctor(Doctor doc, int hospitalId, int deptId, int branchId) {
        return doctorDAO.addDoctor(doc, hospitalId, deptId, branchId);
    }
}
