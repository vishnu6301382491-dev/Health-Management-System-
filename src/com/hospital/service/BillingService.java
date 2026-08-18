package com.hospital.service;

import com.hospital.dao.BillingDAO;
import com.hospital.model.Bill;
import com.hospital.model.Payment;

import java.util.List;

public class BillingService {
    private final BillingDAO billingDAO = new BillingDAO();

    public List<Bill> getAllBills(String search, Integer patientId, String status, String date) {
        return billingDAO.getAllBills(search, patientId, status, date);
    }

    public Bill getById(int id) {
        return billingDAO.getById(id);
    }

    public boolean createBill(Bill bill) {
        if (bill.getPatientId() <= 0) return false;
        return billingDAO.createBill(bill);
    }

    public boolean recordPayment(int billId, double amount, String mode, String ref) {
        if (billId <= 0 || amount <= 0 || mode == null) return false;
        return billingDAO.recordPayment(billId, amount, mode, ref);
    }

    public List<Payment> getPaymentsByBillId(int billId) {
        return billingDAO.getPaymentsByBillId(billId);
    }
}
