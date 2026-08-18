package com.hospital.model;

public class DashboardStats {
    private int totalHospitals;
    private int totalBranches;
    private int totalDoctors;
    private int totalPatients;
    private int todayAppointments;
    private int pendingAppointments;
    private int completedAppointments;
    private int cancelledAppointments;
    private double totalRevenue;
    private double pendingPayments;
    private int availableDoctors;
    private int emergencyHospitals;

    public DashboardStats() {}

    public int getTotalHospitals() { return totalHospitals; }
    public void setTotalHospitals(int totalHospitals) { this.totalHospitals = totalHospitals; }

    public int getTotalBranches() { return totalBranches; }
    public void setTotalBranches(int totalBranches) { this.totalBranches = totalBranches; }

    public int getTotalDoctors() { return totalDoctors; }
    public void setTotalDoctors(int totalDoctors) { this.totalDoctors = totalDoctors; }

    public int getTotalPatients() { return totalPatients; }
    public void setTotalPatients(int totalPatients) { this.totalPatients = totalPatients; }

    public int getTodayAppointments() { return todayAppointments; }
    public void setTodayAppointments(int todayAppointments) { this.todayAppointments = todayAppointments; }

    public int getPendingAppointments() { return pendingAppointments; }
    public void setPendingAppointments(int pendingAppointments) { this.pendingAppointments = pendingAppointments; }

    public int getCompletedAppointments() { return completedAppointments; }
    public void setCompletedAppointments(int completedAppointments) { this.completedAppointments = completedAppointments; }

    public int getCancelledAppointments() { return cancelledAppointments; }
    public void setCancelledAppointments(int cancelledAppointments) { this.cancelledAppointments = cancelledAppointments; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }

    public double getPendingPayments() { return pendingPayments; }
    public void setPendingPayments(double pendingPayments) { this.pendingPayments = pendingPayments; }

    public int getAvailableDoctors() { return availableDoctors; }
    public void setAvailableDoctors(int availableDoctors) { this.availableDoctors = availableDoctors; }

    public int getEmergencyHospitals() { return emergencyHospitals; }
    public void setEmergencyHospitals(int emergencyHospitals) { this.emergencyHospitals = emergencyHospitals; }
}
