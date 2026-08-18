package com.hospital.model;

public class Ambulance {
    private int id;
    private String ambulanceCode;
    private int hospitalId;
    private String hospitalName;
    private String vehicleNumber;
    private String driverName;
    private String driverPhone;
    private String ambulanceType; // Basic Life Support, Advanced Life Support, Patient Transport
    private String status; // Available, Dispatched, Maintenance
    private String currentLocation;

    public Ambulance() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAmbulanceCode() { return ambulanceCode; }
    public void setAmbulanceCode(String ambulanceCode) { this.ambulanceCode = ambulanceCode; }

    public int getHospitalId() { return hospitalId; }
    public void setHospitalId(int hospitalId) { this.hospitalId = hospitalId; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }

    public String getAmbulanceType() { return ambulanceType; }
    public void setAmbulanceType(String ambulanceType) { this.ambulanceType = ambulanceType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
}
