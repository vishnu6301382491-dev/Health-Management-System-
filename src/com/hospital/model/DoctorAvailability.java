package com.hospital.model;

public class DoctorAvailability {
    private int id;
    private int doctorId;
    private String doctorName;
    private String availableDays;
    private String startTime;
    private String endTime;
    private String breakStart;
    private String breakEnd;
    private int slotDurationMins;
    private int maxAppointments;

    public DoctorAvailability() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getAvailableDays() { return availableDays; }
    public void setAvailableDays(String availableDays) { this.availableDays = availableDays; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getBreakStart() { return breakStart; }
    public void setBreakStart(String breakStart) { this.breakStart = breakStart; }

    public String getBreakEnd() { return breakEnd; }
    public void setBreakEnd(String breakEnd) { this.breakEnd = breakEnd; }

    public int getSlotDurationMins() { return slotDurationMins; }
    public void setSlotDurationMins(int slotDurationMins) { this.slotDurationMins = slotDurationMins; }

    public int getMaxAppointments() { return maxAppointments; }
    public void setMaxAppointments(int maxAppointments) { this.maxAppointments = maxAppointments; }
}
