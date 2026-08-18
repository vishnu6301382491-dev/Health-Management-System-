/* ==========================================
   PATIENTS MANAGEMENT MODULE
   ========================================== */

let patientsData = [];
let patientSearchDebounceTimer = null;

async function loadPatientsDirectory() {
    return loadPatientsTable();
}

function debouncedPatientSearch() {
    clearTimeout(patientSearchDebounceTimer);
    patientSearchDebounceTimer = setTimeout(() => {
        loadPatientsTable();
    }, 300);
}

async function loadPatientsTable() {
    const search = document.getElementById('patient-search-input')?.value || '';
    const blood = document.getElementById('patient-filter-blood')?.value || '';
    const gender = document.getElementById('patient-filter-gender')?.value || '';

    try {
        const url = `/api/patients?search=${encodeURIComponent(search)}&bloodGroup=${encodeURIComponent(blood)}&gender=${encodeURIComponent(gender)}`;
        const res = await fetch(url);
        patientsData = await res.json();

        const tbl = document.getElementById('tbl-patients-list');
        if (!tbl) return;

        if (!Array.isArray(patientsData) || patientsData.length === 0) {
            tbl.innerHTML = `<tr><td colspan="7" class="text-center p-md text-muted">No registered patient records found in MySQL.</td></tr>`;
            return;
        }

        tbl.innerHTML = patientsData.map(p => {
            const regDate = p.registrationDate ? (typeof p.registrationDate === 'string' ? p.registrationDate.substring(0, 10) : new Date(p.registrationDate).toISOString().split('T')[0]) : 'N/A';
            return `
                <tr>
                    <td><strong>${p.patientCode}</strong></td>
                    <td>
                        <strong>${p.name || (p.firstName + ' ' + p.lastName).trim()}</strong>
                        ${p.allergies ? `<br><small class="text-danger"><i class="fa-solid fa-triangle-exclamation"></i> ${p.allergies}</small>` : ''}
                    </td>
                    <td>${p.gender || 'Male'}, ${p.age || 28} yrs</td>
                    <td><span class="badge badge-info">${p.bloodGroup || 'O+'}</span></td>
                    <td>${p.phone || '9876543210'}<br><small class="text-muted">${p.email || 'patient@aura.com'}</small></td>
                    <td>${regDate}</td>
                    <td>
                        <div class="flex-wrap gap-xs">
                            <button class="btn btn-xs btn-outline-primary" onclick="editPatient(${p.id})" title="Edit Profile"><i class="fa-solid fa-pen"></i></button>
                            <button class="btn btn-xs btn-outline-danger" onclick="deletePatientRecord(${p.id})" title="Delete"><i class="fa-solid fa-trash"></i></button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (e) {
        console.error('Error loading patients directory:', e);
    }
}

function openPatientModal(patientObj = null) {
    const form = document.getElementById('form-patient');
    if (form) form.reset();

    if (patientObj) {
        if (document.getElementById('modal-patient-title')) document.getElementById('modal-patient-title').textContent = 'Edit Patient Record';
        if (document.getElementById('pat-id')) document.getElementById('pat-id').value = patientObj.id;
        if (document.getElementById('pat-name')) document.getElementById('pat-name').value = patientObj.name;
        if (document.getElementById('pat-gender')) document.getElementById('pat-gender').value = patientObj.gender;
        if (document.getElementById('pat-dob')) document.getElementById('pat-dob').value = patientObj.dob || '';
        if (document.getElementById('pat-age')) document.getElementById('pat-age').value = patientObj.age;
        if (document.getElementById('pat-blood')) document.getElementById('pat-blood').value = patientObj.bloodGroup || 'O+';
        if (document.getElementById('pat-phone')) document.getElementById('pat-phone').value = patientObj.phone;
        if (document.getElementById('pat-email')) document.getElementById('pat-email').value = patientObj.email;
        if (document.getElementById('pat-address')) document.getElementById('pat-address').value = patientObj.address || '';
        if (document.getElementById('pat-emergency')) document.getElementById('pat-emergency').value = patientObj.emergencyContactName || '';
        if (document.getElementById('pat-allergies')) document.getElementById('pat-allergies').value = patientObj.allergies || '';
    } else {
        if (document.getElementById('modal-patient-title')) document.getElementById('modal-patient-title').textContent = 'Add New Patient Record';
        if (document.getElementById('pat-id')) document.getElementById('pat-id').value = '';
    }

    if (typeof openModal === 'function') openModal('modal-patient');
}

function editPatient(id) {
    const p = patientsData.find(x => x.id === id);
    if (p) openPatientModal(p);
}

async function deletePatientRecord(id) {
    if (!confirm('Are you sure you want to delete this patient profile?')) return;
    try {
        const res = await fetch(`/api/patients?id=${id}`, { method: 'DELETE' });
        if (res.status === 200) {
            showToast('Patient record deleted.', 'info');
            loadPatientsTable();
        }
    } catch (e) { console.error(e); }
}

async function handlePatientSave(e) {
    e.preventDefault();
    const id = document.getElementById('pat-id')?.value;
    const req = {
        name: document.getElementById('pat-name')?.value.trim(),
        gender: document.getElementById('pat-gender')?.value,
        dob: document.getElementById('pat-dob')?.value,
        age: parseInt(document.getElementById('pat-age')?.value) || 25,
        bloodGroup: document.getElementById('pat-blood')?.value,
        phone: document.getElementById('pat-phone')?.value.trim(),
        email: document.getElementById('pat-email')?.value.trim(),
        address: document.getElementById('pat-address')?.value.trim(),
        emergencyContactName: document.getElementById('pat-emergency')?.value.trim(),
        allergies: document.getElementById('pat-allergies')?.value.trim()
    };

    try {
        let res;
        if (id) {
            req.id = parseInt(id);
            res = await fetch('/api/patients', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(req)
            });
        } else {
            res = await fetch('/api/patients', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(req)
            });
        }

        if (res.status === 200 || res.status === 201) {
            showToast('Patient record saved successfully.', 'success');
            if (typeof closeModal === 'function') closeModal('modal-patient');
            loadPatientsTable();
        }
    } catch (err) {
        showToast('Error saving patient record.', 'danger');
    }
}
