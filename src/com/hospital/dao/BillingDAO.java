package com.hospital.dao;

import com.hospital.model.Bill;
import com.hospital.model.Payment;
import com.hospital.util.DBConnection;
import com.hospital.util.IDGenerator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BillingDAO {

    public List<Bill> getAllBills(String search, Integer patientId, String status, String date) {
        List<Bill> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT b.*, p.name as patient_name, p.patient_code " +
            "FROM bills b " +
            "JOIN patients p ON b.patient_id = p.id WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (b.invoice_code LIKE ? OR p.name LIKE ? OR p.patient_code LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term); params.add(term); params.add(term);
        }
        if (patientId != null && patientId > 0) {
            sql.append("AND b.patient_id = ? ");
            params.add(patientId);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND b.payment_status = ? ");
            params.add(status.trim());
        }
        if (date != null && !date.trim().isEmpty()) {
            sql.append("AND b.invoice_date = ? ");
            params.add(date.trim());
        }

        sql.append("ORDER BY b.id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBill(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Bill getById(int id) {
        String sql = "SELECT b.*, p.name as patient_name, p.patient_code FROM bills b JOIN patients p ON b.patient_id = p.id WHERE b.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBill(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean createBill(Bill bill) {
        String code = IDGenerator.generateId("bills", "invoice_code", "INV");
        bill.setInvoiceCode(code);

        double subtotal = bill.getConsultationFee() + bill.getLabCharges() + bill.getMedicineCharges() + bill.getRoomCharges() + bill.getOtherCharges();
        double total = subtotal + bill.getTaxAmount() - bill.getDiscount();
        if (total < 0) total = 0;
        bill.setTotalAmount(total);

        double remaining = total - bill.getPaidAmount();
        if (remaining <= 0) {
            remaining = 0;
            bill.setPaymentStatus("Paid");
        } else if (bill.getPaidAmount() > 0) {
            bill.setPaymentStatus("Partially Paid");
        } else {
            bill.setPaymentStatus("Pending");
        }
        bill.setRemainingAmount(remaining);

        String sql = "INSERT INTO bills (invoice_code, patient_id, appointment_id, consultation_fee, lab_charges, medicine_charges, room_charges, other_charges, discount, tax_amount, total_amount, paid_amount, remaining_amount, payment_status, invoice_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bill.getInvoiceCode());
            pstmt.setInt(2, bill.getPatientId());
            pstmt.setObject(3, bill.getAppointmentId(), Types.INTEGER);
            pstmt.setDouble(4, bill.getConsultationFee());
            pstmt.setDouble(5, bill.getLabCharges());
            pstmt.setDouble(6, bill.getMedicineCharges());
            pstmt.setDouble(7, bill.getRoomCharges());
            pstmt.setDouble(8, bill.getOtherCharges());
            pstmt.setDouble(9, bill.getDiscount());
            pstmt.setDouble(10, bill.getTaxAmount());
            pstmt.setDouble(11, bill.getTotalAmount());
            pstmt.setDouble(12, bill.getPaidAmount());
            pstmt.setDouble(13, bill.getRemainingAmount());
            pstmt.setString(14, bill.getPaymentStatus());
            pstmt.setString(15, bill.getInvoiceDate() != null ? bill.getInvoiceDate() : new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean recordPayment(int billId, double paymentAmount, String mode, String transactionRef) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            Bill bill = getById(billId);
            if (bill == null) return false;

            double newPaid = bill.getPaidAmount() + paymentAmount;
            double newRemaining = bill.getTotalAmount() - newPaid;
            String newStatus = "Pending";
            if (newRemaining <= 0) {
                newRemaining = 0;
                newStatus = "Paid";
            } else if (newPaid > 0) {
                newStatus = "Partially Paid";
            }

            String updateBillSql = "UPDATE bills SET paid_amount = ?, remaining_amount = ?, payment_status = ? WHERE id = ?";
            try (PreparedStatement uStmt = conn.prepareStatement(updateBillSql)) {
                uStmt.setDouble(1, newPaid);
                uStmt.setDouble(2, newRemaining);
                uStmt.setString(3, newStatus);
                uStmt.setInt(4, billId);
                uStmt.executeUpdate();
            }

            String insertPaySql = "INSERT INTO payments (bill_id, amount, payment_mode, transaction_ref) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pStmt = conn.prepareStatement(insertPaySql)) {
                pStmt.setInt(1, billId);
                pStmt.setDouble(2, paymentAmount);
                pStmt.setString(3, mode);
                pStmt.setString(4, transactionRef);
                pStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
        return false;
    }

    public List<Payment> getPaymentsByBillId(int billId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM payments WHERE bill_id = ? ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, billId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Payment p = new Payment();
                    p.setId(rs.getInt("id"));
                    p.setBillId(rs.getInt("bill_id"));
                    p.setAmount(rs.getDouble("amount"));
                    p.setPaymentMode(rs.getString("payment_mode"));
                    p.setTransactionRef(rs.getString("transaction_ref"));
                    p.setPaymentDate(rs.getTimestamp("payment_date"));
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Bill mapResultSetToBill(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setId(rs.getInt("id"));
        b.setInvoiceCode(rs.getString("invoice_code"));
        b.setPatientId(rs.getInt("patient_id"));
        b.setPatientName(rs.getString("patient_name"));
        b.setPatientCode(rs.getString("patient_code"));
        b.setAppointmentId((Integer) rs.getObject("appointment_id"));
        b.setConsultationFee(rs.getDouble("consultation_fee"));
        b.setLabCharges(rs.getDouble("lab_charges"));
        b.setMedicineCharges(rs.getDouble("medicine_charges"));
        b.setRoomCharges(rs.getDouble("room_charges"));
        b.setOtherCharges(rs.getDouble("other_charges"));
        b.setDiscount(rs.getDouble("discount"));
        b.setTaxAmount(rs.getDouble("tax_amount"));
        b.setTotalAmount(rs.getDouble("total_amount"));
        b.setPaidAmount(rs.getDouble("paid_amount"));
        b.setRemainingAmount(rs.getDouble("remaining_amount"));
        b.setPaymentStatus(rs.getString("payment_status"));
        b.setInvoiceDate(rs.getDate("invoice_date").toString());
        b.setCreatedAt(rs.getTimestamp("created_at"));
        return b;
    }
}
