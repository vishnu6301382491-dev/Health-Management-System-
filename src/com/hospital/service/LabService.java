package com.hospital.service;

import com.hospital.dao.LabDAO;
import com.hospital.model.LabTest;

import java.util.List;

public class LabService {
    private final LabDAO labDAO = new LabDAO();

    public List<LabTest> getAllLabTests(String search, Integer doctorId, Integer patientId, String category, String status) {
        return labDAO.getAllLabTests(search, doctorId, patientId, category, status);
    }

    public LabTest getById(int id) {
        return labDAO.getById(id);
    }

    public boolean requestLabTest(LabTest test) {
        if (test.getPatientId() <= 0 || test.getDoctorId() <= 0 || test.getTestName() == null) return false;
        return labDAO.requestLabTest(test);
    }

    public boolean updateStatus(int testId, String status, String technician) {
        return labDAO.updateStatus(testId, status, technician);
    }

    public boolean addOrUpdateResult(int testId, String resultValue, String referenceRange, String remarks) {
        return labDAO.addOrUpdateResult(testId, resultValue, referenceRange, remarks);
    }
}
