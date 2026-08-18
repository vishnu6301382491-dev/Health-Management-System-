package com.hospital.model;

import java.util.Date;

public class Review {
    private int id;
    private int patientId;
    private String patientName;
    private int hospitalId;
    private String hospitalName;
    private int doctorId;
    private String doctorName;
    private Integer appointmentId;
    private int rating;
    private int doctorRating;
    private int staffRating;
    private int cleanlinessRating;
    private String reviewText;
    private Date createdAt;

    public Review() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public int getHospitalId() { return hospitalId; }
    public void setHospitalId(int hospitalId) { this.hospitalId = hospitalId; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public Integer getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Integer appointmentId) { this.appointmentId = appointmentId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public int getDoctorRating() { return doctorRating; }
    public void setDoctorRating(int doctorRating) { this.doctorRating = doctorRating; }

    public int getStaffRating() { return staffRating; }
    public void setStaffRating(int staffRating) { this.staffRating = staffRating; }

    public int getCleanlinessRating() { return cleanlinessRating; }
    public void setCleanlinessRating(int cleanlinessRating) { this.cleanlinessRating = cleanlinessRating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
