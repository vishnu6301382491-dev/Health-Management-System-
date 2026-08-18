package com.hospital.util;

import com.hospital.dao.HospitalDAO;
import com.hospital.model.Hospital;

import java.util.ArrayList;
import java.util.List;

public class CsvUtil {

    public static List<Hospital> parseHospitalsCsv(String csvContent) {
        List<Hospital> list = new ArrayList<>();
        if (csvContent == null || csvContent.trim().isEmpty()) return list;

        String[] lines = csvContent.split("\r?\n");
        if (lines.length <= 1) return list; // header only or empty

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] cols = parseCsvLine(line);
            if (cols.length >= 7) {
                Hospital h = new Hospital();
                h.setName(cols[0].trim());
                h.setType(cols.length > 1 ? cols[1].trim() : "Multi-Speciality");
                h.setAddress(cols.length > 2 ? cols[2].trim() : "Main Road");
                h.setCity(cols.length > 3 ? cols[3].trim() : "Metropolis");
                h.setState(cols.length > 4 ? cols[4].trim() : "State");
                h.setPincode(cols.length > 5 ? cols[5].trim() : "500001");
                h.setPhone(cols.length > 6 ? cols[6].trim() : "040-100200");
                h.setEmergencyPhone(cols.length > 7 ? cols[7].trim() : "108");
                h.setEmail(cols.length > 8 ? cols[8].trim() : "info@hospital.com");
                h.setWebsite(cols.length > 9 ? cols[9].trim() : "https://hospital.com");
                list.add(h);
            }
        }
        return list;
    }

    public static int importHospitalsFromCsv(String csvContent) {
        List<Hospital> hospitals = parseHospitalsCsv(csvContent);
        HospitalDAO dao = new HospitalDAO();
        int count = 0;
        for (Hospital h : hospitals) {
            if (dao.addHospital(h)) {
                count++;
            }
        }
        return count;
    }

    private static String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }
}
