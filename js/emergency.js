/* ==========================================
   24/7 EMERGENCY, AMBULANCE & BLOOD BANK MODULE
   ========================================== */

async function loadEmergencyHub() {
    loadAmbulancesTable();
    loadBloodBankGrid();
}

async function loadAmbulancesTable() {
    try {
        const res = await fetch('/api/emergency');
        const list = await res.json();
        const tbl = document.getElementById('tbl-ambulances-list');
        if (!tbl) return;

        tbl.innerHTML = list.map(a => `
            <tr>
                <td><strong>${a.ambulanceCode}</strong></td>
                <td>${a.hospitalName}</td>
                <td><strong>${a.vehicleNumber}</strong></td>
                <td><span class="badge badge-info">${a.ambulanceType}</span></td>
                <td>${a.driverName}<br><small class="text-muted">${a.driverPhone}</small></td>
                <td><span class="badge ${a.status === 'Available' ? 'badge-success' : 'badge-warning'}">${a.status}</span></td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}

async function loadBloodBankGrid() {
    try {
        const res = await fetch('/api/bloodbank');
        const list = await res.json();
        const grid = document.getElementById('blood-bank-grid');
        if (!grid) return;

        grid.innerHTML = list.map(b => `
            <div class="blood-card">
                <h2>${b.bloodGroup}</h2>
                <strong class="text-main">${b.unitsAvailable} Units</strong><br>
                <small class="text-muted" style="font-size:0.75rem;">${b.hospitalName}</small>
            </div>
        `).join('');
    } catch (e) { console.error(e); }
}

function openAmbulanceRequestModal() {
    openModal('modal-ambulance-req');
}

async function handleAmbulanceRequestSubmit(e) {
    e.preventDefault();
    const req = {
        pickupAddress: document.getElementById('amb-pickup').value.trim(),
        phone: document.getElementById('amb-phone').value.trim(),
        hospitalId: 1,
        patientId: 1
    };

    try {
        const res = await fetch('/api/emergency/request', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });
        const data = await res.json();
        if (data.success) {
            showToast('EMERGENCY AMBULANCE DISPATCHED! Driver has been notified.', 'success');
            closeModal('modal-ambulance-req');
            loadAmbulancesTable();
        }
    } catch (err) { showToast('Emergency request failed', 'danger'); }
}
