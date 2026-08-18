/* ==========================================
   PRESCRIPTION MANAGEMENT MODULE
   ========================================== */

let rxData = [];

async function loadPrescriptionsTable() {
    const search = document.getElementById('rx-search-input')?.value || '';
    try {
        const res = await fetch(`/api/prescriptions?search=${encodeURIComponent(search)}`);
        rxData = await res.json();

        const tbl = document.getElementById('tbl-prescriptions-list');
        if (!tbl) return;

        if (rxData.length === 0) {
            tbl.innerHTML = `<tr><td colspan="7" class="text-center p-md text-muted">No prescriptions found.</td></tr>`;
            return;
        }

        tbl.innerHTML = rxData.map(pr => `
            <tr>
                <td><strong>${pr.prescriptionCode}</strong></td>
                <td><strong>${pr.patientName}</strong><br><small class="text-muted">${pr.patientCode}</small></td>
                <td>${pr.doctorName}</td>
                <td>${pr.visitDate}</td>
                <td>${pr.diagnosis}</td>
                <td><span class="badge badge-info">${pr.items ? pr.items.length : 0} Medicines</span></td>
                <td>
                    <button class="btn btn-xs btn-outline-primary" onclick="viewPrescriptionPrintModal(${pr.id})"><i class="fa-solid fa-print"></i> View / Print</button>
                </td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}

async function openPrescriptionModal(aptObj = null) {
    const form = document.getElementById('form-prescription');
    form.reset();

    // Load Patient and Doctor Dropdowns
    const pRes = await fetch('/api/patients');
    const pats = await pRes.json();
    document.getElementById('rx-patient-id').innerHTML = pats.map(p => `<option value="${p.id}">${p.name} (${p.patientCode})</option>`).join('');

    const dRes = await fetch('/api/doctors');
    const docs = await dRes.json();
    document.getElementById('rx-doctor-id').innerHTML = docs.map(d => `<option value="${d.id}">${d.name} (${d.specialization})</option>`).join('');

    const container = document.getElementById('rx-medicines-container');
    container.innerHTML = '';
    addRxMedicineRow(); // Add default initial row
    addRxMedicineRow();

    openModal('modal-prescription');
}

function addRxMedicineRow() {
    const container = document.getElementById('rx-medicines-container');
    if (!container) return;

    const row = document.createElement('div');
    row.className = 'form-row rx-med-row m-bottom-sm';
    row.innerHTML = `
        <div class="form-group col-3"><input type="text" class="form-control rx-med-name" placeholder="Medicine Name" required></div>
        <div class="form-group col-2"><input type="text" class="form-control rx-med-dosage" placeholder="500mg" required></div>
        <div class="form-group col-2"><input type="text" class="form-control rx-med-freq" placeholder="1-0-1" required></div>
        <div class="form-group col-2"><input type="text" class="form-control rx-med-dur" placeholder="5 days" required></div>
        <div class="form-group col-2"><input type="text" class="form-control rx-med-inst" placeholder="After food"></div>
        <div class="form-group col-1"><button type="button" class="btn btn-xs btn-outline-danger" onclick="this.closest('.rx-med-row').remove()">&times;</button></div>
    `;
    container.appendChild(row);
}

async function handlePrescriptionSave(e) {
    e.preventDefault();

    const items = [];
    document.querySelectorAll('.rx-med-row').forEach(row => {
        const name = row.querySelector('.rx-med-name').value.trim();
        const dosage = row.querySelector('.rx-med-dosage').value.trim();
        const freq = row.querySelector('.rx-med-freq').value.trim();
        const dur = row.querySelector('.rx-med-dur').value.trim();
        const inst = row.querySelector('.rx-med-inst').value.trim();
        if (name) {
            items.push({ medicineName: name, dosage, frequency: freq, duration: dur, instructions: inst });
        }
    });

    if (items.length === 0) {
        showToast('Please add at least one medicine.', 'danger');
        return;
    }

    const req = {
        patientId: parseInt(document.getElementById('rx-patient-id').value),
        doctorId: parseInt(document.getElementById('rx-doctor-id').value),
        diagnosis: document.getElementById('rx-diagnosis').value.trim(),
        items: items
    };

    try {
        const res = await fetch('/api/prescriptions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });
        const data = await res.json();
        if (data.success) {
            showToast('Prescription created successfully!', 'success');
            closeModal('modal-prescription');
            loadPrescriptionsTable();
        }
    } catch (err) {
        showToast('Failed to create prescription', 'danger');
    }
}

async function viewPrescriptionPrintModal(id) {
    try {
        const res = await fetch(`/api/prescriptions?id=${id}`);
        const pr = await res.json();

        const content = document.getElementById('rx-print-content');
        if (content) {
            content.innerHTML = `
                <div class="rx-header flex-between border-bottom p-bottom-md">
                    <div>
                        <h2>AURA HEALTH 3D CLINIC</h2>
                        <p class="text-muted">108 Medical Center Plaza, Digital Avenue</p>
                    </div>
                    <div class="text-end">
                        <h3>RX: ${pr.prescriptionCode}</h3>
                        <p>Date: ${pr.visitDate}</p>
                    </div>
                </div>

                <div class="rx-info-box grid-2 m-top-md bg-light p-md border-radius-md">
                    <div>
                        <strong>Patient Name:</strong> ${pr.patientName} (${pr.patientCode})<br>
                        <strong>Diagnosis:</strong> ${pr.diagnosis}
                    </div>
                    <div>
                        <strong>Attending Doctor:</strong> ${pr.doctorName}<br>
                        <strong>Clinic Room:</strong> Cabin 101
                    </div>
                </div>

                <table class="table m-top-lg">
                    <thead>
                        <tr>
                            <th>Medicine Name</th>
                            <th>Dosage</th>
                            <th>Frequency</th>
                            <th>Duration</th>
                            <th>Instructions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${pr.items ? pr.items.map(it => `
                            <tr>
                                <td><strong>${it.medicineName}</strong></td>
                                <td>${it.dosage}</td>
                                <td><span class="badge badge-info">${it.frequency}</span></td>
                                <td>${it.duration}</td>
                                <td>${it.instructions || '-'}</td>
                            </tr>
                        `).join('') : ''}
                    </tbody>
                </table>

                <div class="rx-footer m-top-lg p-top-lg border-top flex-between">
                    <div><small>Take medicines as prescribed. Signature valid digitally.</small></div>
                    <div class="text-center">
                        <br>
                        <strong>_______________________</strong><br>
                        <small>Doctor Signature & Seal</small>
                    </div>
                </div>
            `;
        }
        openModal('modal-rx-detail');
    } catch (e) { showToast('Error opening prescription view', 'danger'); }
}
