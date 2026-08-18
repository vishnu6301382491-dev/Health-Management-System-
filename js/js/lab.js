/* ==========================================
   LABORATORY MANAGEMENT MODULE
   ========================================== */

let labTestsData = [];

async function loadLabTestsTable() {
    const search = document.getElementById('lab-search-input')?.value || '';
    const status = document.getElementById('lab-filter-status')?.value || '';

    try {
        const url = `/api/lab?search=${encodeURIComponent(search)}&status=${encodeURIComponent(status)}`;
        const res = await fetch(url);
        labTestsData = await res.json();

        const tbl = document.getElementById('tbl-lab-tests-list');
        if (!tbl) return;

        if (labTestsData.length === 0) {
            tbl.innerHTML = `<tr><td colspan="7" class="text-center p-md text-muted">No lab test records found.</td></tr>`;
            return;
        }

        tbl.innerHTML = labTestsData.map(lt => `
            <tr>
                <td><strong>${lt.testCode}</strong></td>
                <td><strong>${lt.patientName}</strong><br><small class="text-muted">${lt.patientCode}</small></td>
                <td><strong>${lt.testName}</strong><br><small class="text-info">${lt.category}</small></td>
                <td>${lt.sampleType || 'N/A'}</td>
                <td>${lt.testDate}</td>
                <td><span class="badge ${getStatusBadgeClass(lt.status)}">${lt.status}</span></td>
                <td>
                    <div class="flex-wrap gap-xs">
                        ${lt.status === 'Requested' ? `<button class="btn btn-xs btn-info" onclick="updateLabStatus(${lt.id}, 'Sample Collected')">Collect Sample</button>` : ''}
                        ${lt.status === 'Sample Collected' ? `<button class="btn btn-xs btn-warning" onclick="updateLabStatus(${lt.id}, 'Processing')">Process</button>` : ''}
                        ${lt.status === 'Processing' || lt.status === 'Completed' ? `<button class="btn btn-xs btn-primary" onclick="openLabResultModal(${lt.id})">${lt.status === 'Completed' ? 'View/Edit Result' : 'Enter Result'}</button>` : ''}
                    </div>
                </td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}

async function openLabTestModal() {
    const pRes = await fetch('/api/patients');
    const pats = await pRes.json();
    document.getElementById('lab-patient-id').innerHTML = pats.map(p => `<option value="${p.id}">${p.name} (${p.patientCode})</option>`).join('');

    const dRes = await fetch('/api/doctors');
    const docs = await dRes.json();
    document.getElementById('lab-doctor-id').innerHTML = docs.map(d => `<option value="${d.id}">${d.name} (${d.specialization})</option>`).join('');

    openModal('modal-lab-order');
}

async function handleLabOrderSave(e) {
    e.preventDefault();
    const req = {
        patientId: parseInt(document.getElementById('lab-patient-id').value),
        doctorId: parseInt(document.getElementById('lab-doctor-id').value),
        testName: document.getElementById('lab-test-name').value.trim(),
        category: document.getElementById('lab-category').value,
        sampleType: document.getElementById('lab-sample').value.trim(),
        status: 'Requested'
    };

    try {
        const res = await fetch('/api/lab', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });
        const data = await res.json();
        if (data.success) {
            showToast('Lab test ordered!', 'success');
            closeModal('modal-lab-order');
            loadLabTestsTable();
        }
    } catch (err) { showToast('Order test failed', 'danger'); }
}

async function updateLabStatus(id, newStatus) {
    try {
        const res = await fetch('/api/lab', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id, status: newStatus })
        });
        const data = await res.json();
        if (data.success) {
            showToast(`Status updated to ${newStatus}`, 'success');
            loadLabTestsTable();
        }
    } catch (e) { showToast('Failed to update status', 'danger'); }
}

function openLabResultModal(id) {
    const test = labTestsData.find(x => x.id === id);
    if (!test) return;

    document.getElementById('lab-result-test-id').value = id;
    document.getElementById('lab-result-val').value = test.resultValue || '';
    document.getElementById('lab-ref-range').value = test.referenceRange || '';
    document.getElementById('lab-remarks').value = test.remarks || '';

    openModal('modal-lab-result');
}

async function handleLabResultSave(e) {
    e.preventDefault();
    const req = {
        testId: parseInt(document.getElementById('lab-result-test-id').value),
        resultValue: document.getElementById('lab-result-val').value.trim(),
        referenceRange: document.getElementById('lab-ref-range').value.trim(),
        remarks: document.getElementById('lab-remarks').value.trim()
    };

    try {
        const res = await fetch('/api/lab/result', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });
        const data = await res.json();
        if (data.success) {
            showToast('Lab test results published!', 'success');
            closeModal('modal-lab-result');
            loadLabTestsTable();
        }
    } catch (err) { showToast('Failed to save lab results', 'danger'); }
}
