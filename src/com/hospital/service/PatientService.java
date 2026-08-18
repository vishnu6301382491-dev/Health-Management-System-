package com.hospital.service;

import com.hospital.dao.PatientDAO;
import com.hospital.model.Patient;

import java.util.List;

public class PatientService {
    private final PatientDAO patientDAO = new PatientDAO();

    public List<Patient> getAllPatients(String search, String bloodGroup, String gender) {
        return patientDAO.getAllPatients(search, bloodGroup, gender);
    }

    public List<Patient> getAllPatients(String search) {
        return patientDAO.getAllPatients(search, null, null);
    }

    public Patient getById(int id) {
        return patientDAO.getById(id);
    }

    public Patient getByUserId(int userId) {
        return patientDAO.getByUserId(userId);
    }

    public boolean addPatient(Patient patient, String username, String password) {
        if (patient.getName() == null || patient.getName().trim().isEmpty()) return false;
        if (patient.getPhone() == null || patient.getPhone().trim().isEmpty()) return false;
        return patientDAO.addPatient(patient, username, password);
    }

    public boolean updatePatient(Patient patient) {
        return patientDAO.updatePatient(patient);
    }

    public boolean deletePatient(int id) {
        return patientDAO.deletePatient(id);
    }
}
