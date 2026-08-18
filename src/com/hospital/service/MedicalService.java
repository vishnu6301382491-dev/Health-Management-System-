package com.hospital.service;

import com.hospital.dao.MedicalHistoryDAO;
import com.hospital.model.MedicalHistory;

import java.util.List;

public class MedicalService {
    private final MedicalHistoryDAO medicalHistoryDAO = new MedicalHistoryDAO();

    public List<MedicalHistory> getByPatientId(int patientId) {
        return medicalHistoryDAO.getByPatientId(patientId);
    }

    public MedicalHistory getById(int id) {
        return medicalHistoryDAO.getById(id);
    }

    public boolean addMedicalRecord(MedicalHistory record) {
        if (record.getPatientId() <= 0 || record.getDoctorId() <= 0 || record.getDiagnosis() == null) return false;
        return medicalHistoryDAO.addMedicalRecord(record);
    }
}
