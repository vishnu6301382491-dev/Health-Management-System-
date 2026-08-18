package com.hospital.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Hospital {
    private int id;
    private String hospitalCode;
    private String name;
    private String type;
    private String description;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String phone;
    private String emergencyPhone;
    private String email;
    private String website;
    private String googleMapsUrl;
    private String imageUrl;
    private String openingHours;
    private int totalBeds;
    private int icuBeds;
    private boolean pharmacyAvail;
    private boolean labAvail;
    private boolean bloodBankAvail;
    private boolean ambulanceAvail;
    private boolean insuranceSupport;
    private double rating;
    private int reviewCount;
    private int establishedYear;
    private String status;
    private Date createdAt;

    private List<HospitalBranch> branches = new ArrayList<>();

    public Hospital() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getHospitalCode() { return hospitalCode; }
    public void setHospitalCode(String hospitalCode) { this.hospitalCode = hospitalCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getGoogleMapsUrl() { return googleMapsUrl; }
    public void setGoogleMapsUrl(String googleMapsUrl) { this.googleMapsUrl = googleMapsUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }

    public int getTotalBeds() { return totalBeds; }
    public void setTotalBeds(int totalBeds) { this.totalBeds = totalBeds; }

    public int getIcuBeds() { return icuBeds; }
    public void setIcuBeds(int icuBeds) { this.icuBeds = icuBeds; }

    public boolean isPharmacyAvail() { return pharmacyAvail; }
    public void setPharmacyAvail(boolean pharmacyAvail) { this.pharmacyAvail = pharmacyAvail; }

    public boolean isLabAvail() { return labAvail; }
    public void setLabAvail(boolean labAvail) { this.labAvail = labAvail; }

    public boolean isBloodBankAvail() { return bloodBankAvail; }
    public void setBloodBankAvail(boolean bloodBankAvail) { this.bloodBankAvail = bloodBankAvail; }

    public boolean isAmbulanceAvail() { return ambulanceAvail; }
    public void setAmbulanceAvail(boolean ambulanceAvail) { this.ambulanceAvail = ambulanceAvail; }

    public boolean isInsuranceSupport() { return insuranceSupport; }
    public void setInsuranceSupport(boolean insuranceSupport) { this.insuranceSupport = insuranceSupport; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public int getEstablishedYear() { return establishedYear; }
    public void setEstablishedYear(int establishedYear) { this.establishedYear = establishedYear; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public List<HospitalBranch> getBranches() { return branches; }
    public void setBranches(List<HospitalBranch> branches) { this.branches = branches; }
}
