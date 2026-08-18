package com.hospital.service;

import com.hospital.dao.AppointmentDAO;
import com.hospital.dao.AppointmentDAO.BookingResult;
import com.hospital.model.Appointment;

import java.util.List;
import java.util.Map;

public class AppointmentService {
    private final AppointmentDAO appointmentDAO = new AppointmentDAO();

    public List<Appointment> getAllAppointments(Integer patientId, Integer doctorId, Integer hospitalId, String status, String date) {
        return appointmentDAO.getAllAppointments(patientId, doctorId, hospitalId, status, date);
    }

    public boolean updateAppointmentStatus(int appointmentId, String status) {
        return appointmentDAO.updateStatus(appointmentId, status);
    }

    public boolean bookAppointment(Appointment appointment) {
        return appointmentDAO.bookAppointment(appointment);
    }

    public BookingResult bookAppointmentTransaction(Appointment appointment) {
        return appointmentDAO.bookAppointmentTransaction(appointment);
    }

    public Map<String, Object> getAvailability(int doctorId, int hospitalId, int branchId, String date) {
        return appointmentDAO.getAvailability(doctorId, hospitalId, branchId, date);
    }

    public boolean holdSlot(String slotId, String patientId, int doctorId, int hospitalId, String date, String displayTime) {
        return appointmentDAO.holdSlot(slotId, patientId, doctorId, hospitalId, date, displayTime);
    }

    public List<Map<String, Object>> getAvailableSlots(int doctorId, String date) {
        return appointmentDAO.getAvailableSlots(doctorId, date);
    }

    public List<Map<String, Object>> getSlotDiagnostics(int doctorId, String date) {
        return appointmentDAO.getSlotDiagnostics(doctorId, date);
    }
}
