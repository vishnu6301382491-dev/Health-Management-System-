package com.hospital.service;

import com.hospital.dao.PrescriptionDAO;
import com.hospital.model.Prescription;

import java.util.List;

public class PrescriptionService {
    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAO();

    public List<Prescription> getByPatientId(int patientId) {
        return prescriptionDAO.getByPatientId(patientId);
    }

    public List<Prescription> getAllPrescriptions(String search, Integer doctorId, Integer patientId) {
        return prescriptionDAO.getAllPrescriptions(search, doctorId, patientId);
    }

    public Prescription getById(int id) {
        return prescriptionDAO.getById(id);
    }

    public boolean createPrescription(Prescription prescription) {
        if (prescription.getPatientId() <= 0 || prescription.getDoctorId() <= 0 || prescription.getItems().isEmpty()) {
            return false;
        }
        return prescriptionDAO.createPrescription(prescription);
    }
}
