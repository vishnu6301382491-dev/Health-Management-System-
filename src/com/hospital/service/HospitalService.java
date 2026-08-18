package com.hospital.service;

import com.hospital.dao.HospitalDAO;
import com.hospital.model.Hospital;
import com.hospital.model.HospitalBranch;

import java.util.List;

public class HospitalService {
    private HospitalDAO hospitalDAO = new HospitalDAO();

    public List<Hospital> searchHospitals(String search, String city, String type, Double minRating, Boolean pharmacy, Boolean bloodBank, Boolean ambulance, Boolean insurance) {
        return hospitalDAO.getAllHospitals(search, city, type, minRating, pharmacy, bloodBank, ambulance, insurance);
    }

    public Hospital getHospitalById(int id) {
        return hospitalDAO.getById(id);
    }

    public List<HospitalBranch> getBranches(int hospitalId) {
        return hospitalDAO.getBranchesByHospitalId(hospitalId);
    }

    public boolean registerHospital(Hospital h) {
        return hospitalDAO.addHospital(h);
    }
}
