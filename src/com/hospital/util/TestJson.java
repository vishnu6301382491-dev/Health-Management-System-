package com.hospital.util;

import com.hospital.dao.HospitalDAO;
import com.hospital.model.Hospital;
import java.util.List;

public class TestJson {
    public static void main(String[] args) {
        try {
            HospitalDAO dao = new HospitalDAO();
            List<Hospital> list = dao.getAllHospitals(null, null, null, null, null, null, null, null);
            System.out.println("Hospitals count: " + list.size());
            String json = JsonUtil.toJson(list);
            System.out.println("JSON length: " + json.length());
            System.out.println("JSON preview: " + json.substring(0, Math.min(200, json.length())));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
