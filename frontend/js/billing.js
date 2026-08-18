/* ==========================================
   BILLING & INVOICE PAYMENT MODULE
   ========================================== */

let billsData = [];

async function loadBillsTable() {
    const search = document.getElementById('bill-search-input')?.value || '';
    const status = document.getElementById('bill-filter-status')?.value || '';

    try {
        const url = `/api/billing?search=${encodeURIComponent(search)}&status=${encodeURIComponent(status)}`;
        const res = await fetch(url);
        billsData = await res.json();

        const tbl = document.getElementById('tbl-bills-list');
        if (!tbl) return;

        if (billsData.length === 0) {
            tbl.innerHTML = `<tr><td colspan="7" class="text-center p-md text-muted">No billing invoices found.</td></tr>`;
            return;
        }

        tbl.innerHTML = billsData.map(b => `
            <tr>
                <td><strong>${b.invoiceCode}</strong></td>
                <td><strong>${b.patientName}</strong><br><small class="text-muted">${b.patientCode}</small></td>
                <td><strong>₹${b.totalAmount.toFixed(2)}</strong></td>
                <td class="text-success">₹${b.paidAmount.toFixed(2)}</td>
                <td class="text-danger">₹${b.remainingAmount.toFixed(2)}</td>
                <td><span class="badge ${getStatusBadgeClass(b.paymentStatus)}">${b.paymentStatus}</span></td>
                <td>
                    <div class="flex-wrap gap-xs">
                        ${b.paymentStatus !== 'Paid' ? `<button class="btn btn-xs btn-success" onclick="openPaymentModal(${b.id}, ${b.remainingAmount})"><i class="fa-solid fa-credit-card"></i> Pay</button>` : ''}
                        <button class="btn btn-xs btn-outline-primary" onclick="viewInvoicePrintModal(${b.id})"><i class="fa-solid fa-print"></i> Invoice</button>
                    </div>
                </td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}

async function openCreateBillModal() {
    const pRes = await fetch('/api/patients');
    const pats = await pRes.json();
    document.getElementById('bill-patient-id').innerHTML = pats.map(p => `<option value="${p.id}">${p.name} (${p.patientCode})</option>`).join('');

    document.getElementById('bill-consult-fee').value = '500.00';
    document.getElementById('bill-lab-fee').value = '0.00';
    document.getElementById('bill-med-fee').value = '0.00';
    document.getElementById('bill-room-fee').value = '0.00';
    document.getElementById('bill-discount').value = '0.00';
    document.getElementById('bill-tax').value = '25.00';

    calculateInvoiceTotal();
    openModal('modal-create-bill');
}

function calculateInvoiceTotal() {
    const c = parseFloat(document.getElementById('bill-consult-fee').value) || 0;
    const l = parseFloat(document.getElementById('bill-lab-fee').value) || 0;
    const m = parseFloat(document.getElementById('bill-med-fee').value) || 0;
    const r = parseFloat(document.getElementById('bill-room-fee').value) || 0;
    const d = parseFloat(document.getElementById('bill-discount').value) || 0;
    const t = parseFloat(document.getElementById('bill-tax').value) || 0;

    let total = (c + l + m + r + t) - d;
    if (total < 0) total = 0;

    document.getElementById('bill-calculated-total').textContent = `₹${total.toFixed(2)}`;
}

async function handleBillSave(e) {
    e.preventDefault();
    const req = {
        patientId: parseInt(document.getElementById('bill-patient-id').value),
        consultationFee: parseFloat(document.getElementById('bill-consult-fee').value) || 0,
        labCharges: parseFloat(document.getElementById('bill-lab-fee').value) || 0,
        medicineCharges: parseFloat(document.getElementById('bill-med-fee').value) || 0,
        roomCharges: parseFloat(document.getElementById('bill-room-fee').value) || 0,
        discount: parseFloat(document.getElementById('bill-discount').value) || 0,
        taxAmount: parseFloat(document.getElementById('bill-tax').value) || 0,
        paidAmount: 0.0
    };

    try {
        const res = await fetch('/api/billing', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });
        const data = await res.json();
        if (data.success) {
            showToast('Invoice generated successfully!', 'success');
            closeModal('modal-create-bill');
            loadBillsTable();
        }
    } catch (err) { showToast('Invoice generation failed', 'danger'); }
}

function openPaymentModal(billId, remaining) {
    document.getElementById('pay-bill-id').value = billId;
    document.getElementById('pay-amount').value = remaining.toFixed(2);
    openModal('modal-payment');
}

async function handlePaymentSave(e) {
    e.preventDefault();
    const req = {
        billId: parseInt(document.getElementById('pay-bill-id').value),
        amount: parseFloat(document.getElementById('pay-amount').value) || 0,
        paymentMode: document.getElementById('pay-mode').value,
        transactionRef: document.getElementById('pay-ref').value.trim()
    };

    try {
        const res = await fetch('/api/billing/payment', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });
        const data = await res.json();
        if (data.success) {
            showToast('Payment recorded successfully!', 'success');
            closeModal('modal-payment');
            loadBillsTable();
            loadDashboardStats();
        }
    } catch (err) { showToast('Payment processing error', 'danger'); }
}

async function viewInvoicePrintModal(id) {
    try {
        const res = await fetch(`/api/billing?id=${id}`);
        const bill = await res.json();

        const content = document.getElementById('invoice-print-content');
        if (content) {
            content.innerHTML = `
                <div class="invoice-header flex-between border-bottom p-bottom-md">
                    <div>
                        <h2>AURA HEALTHCARE SYSTEM</h2>
                        <p class="text-muted">GSTIN: 07AAAAB1234C1Z9 | Reg # 9811/2026</p>
                    </div>
                    <div class="text-end">
                        <h3>INVOICE: ${bill.invoiceCode}</h3>
                        <p>Date: ${bill.invoiceDate}</p>
                        <span class="badge ${getStatusBadgeClass(bill.paymentStatus)}">${bill.paymentStatus}</span>
                    </div>
                </div>

                <div class="invoice-patient p-md bg-light m-top-md border-radius-md flex-between">
                    <div>
                        <strong>Billed To:</strong> ${bill.patientName} (${bill.patientCode})<br>
                        <strong>Payment Status:</strong> ${bill.paymentStatus}
                    </div>
                    <div class="text-end">
                        <strong>Billing Date:</strong> ${bill.invoiceDate}
                    </div>
                </div>

                <table class="table m-top-lg">
                    <thead>
                        <tr>
                            <th>Item Description</th>
                            <th class="text-end">Amount (₹)</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr><td>Consultation Charges</td><td class="text-end">₹${bill.consultationFee.toFixed(2)}</td></tr>
                        <tr><td>Laboratory & Diagnostics</td><td class="text-end">₹${bill.labCharges.toFixed(2)}</td></tr>
                        <tr><td>Pharmacy / Medicines</td><td class="text-end">₹${bill.medicineCharges.toFixed(2)}</td></tr>
                        <tr><td>Hospital Room & Facilities</td><td class="text-end">₹${bill.roomCharges.toFixed(2)}</td></tr>
                        <tr><td>Taxes & Service Surcharge</td><td class="text-end">₹${bill.taxAmount.toFixed(2)}</td></tr>
                        <tr><td>Discount Applied</td><td class="text-end text-danger">-₹${bill.discount.toFixed(2)}</td></tr>
                        <tr style="border-top: 2px solid var(--border-color); font-size: 1.1rem;">
                            <td><strong>TOTAL INVOICE AMOUNT</strong></td>
                            <td class="text-end text-primary"><strong>₹${bill.totalAmount.toFixed(2)}</strong></td>
                        </tr>
                        <tr>
                            <td><strong>Amount Paid to Date</strong></td>
                            <td class="text-end text-success"><strong>₹${bill.paidAmount.toFixed(2)}</strong></td>
                        </tr>
                        <tr>
                            <td><strong>Balance Due</strong></td>
                            <td class="text-end text-danger"><strong>₹${bill.remainingAmount.toFixed(2)}</strong></td>
                        </tr>
                    </tbody>
                </table>

                <div class="invoice-footer m-top-lg p-top-lg border-top flex-between">
                    <div><small>Computer generated invoice. No signature required.</small></div>
                    <div><strong>Thank you for choosing AURA Health!</strong></div>
                </div>
            `;
        }
        openModal('modal-invoice-detail');
    } catch (e) { showToast('Failed to open invoice', 'danger'); }
}
