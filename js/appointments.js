/* ==========================================
   APPOINTMENTS & BOOKING MODULE (5-Section Form)
   ========================================== */

let bookingHospitals = [];
let bookingDoctors = [];
let addedMedications = [];
let selectedBookingSlotId = null;
let selectedBookingSlotObj = null;

async function loadAppointmentsTable() {
    try {
        const res = await fetch('/api/appointments');
        const list = await res.json();
        const tbl = document.getElementById('tbl-appointments-list');
        if (!tbl) return;

        if (list.length === 0) {
            tbl.innerHTML = `<tr><td colspan="7" class="text-center p-md text-muted">No appointments found.</td></tr>`;
            return;
        }

        tbl.innerHTML = list.map(a => `
            <tr>
                <td><strong>${a.appointmentCode}</strong></td>
                <td><strong>${a.patientName}</strong></td>
                <td>${a.doctorName}<br><small class="text-muted">${a.deptName || 'Cardiology'}</small></td>
                <td>${a.appointmentDate}<br><span class="badge badge-info">${a.timeSlot}</span></td>
                <td>${a.appointmentType || 'In-Person'}</td>
                <td><span class="badge ${getAppointmentStatusBadge(a.status)}">${a.status}</span></td>
                <td>
                    <div class="flex-between gap-xs">
                        ${a.status === 'Confirmed' ? `<button class="btn btn-xs btn-success" onclick="updateAppointmentAction(${a.id}, 'Checked-In')">Check In</button>` : ''}
                        ${a.status === 'Checked-In' ? `<button class="btn btn-xs btn-primary" onclick="updateAppointmentAction(${a.id}, 'In Consultation')">Consult</button>` : ''}
                        ${a.status !== 'Completed' && a.status !== 'Cancelled' ? `<button class="btn btn-xs btn-danger" onclick="updateAppointmentAction(${a.id}, 'Cancelled')">Cancel</button>` : ''}
                    </div>
                </td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}

async function initBookingFormDropdowns() {
    try {
        const resH = await fetch('/api/hospitals');
        bookingHospitals = await resH.json();

        const hSelect = document.getElementById('book-hospital-select');
        if (hSelect) {
            hSelect.innerHTML = `<option value="">Select Hospital Network</option>` + bookingHospitals.map(h => `<option value="${h.id}">${h.name} (${h.city})</option>`).join('');
            if (bookingHospitals.length > 0) {
                hSelect.value = bookingHospitals[0].id;
            }
        }

        const dateInput = document.getElementById('book-date-input');
        if (dateInput) {
            const today = new Date().toISOString().split('T')[0];
            dateInput.value = today;
            dateInput.min = today;
        }

        await filterDoctorsByHospital();
    } catch (e) { console.error(e); }
}

async function filterDoctorsByHospital() {
    selectedBookingSlotId = null;
    selectedBookingSlotObj = null;
    const inputSlot = document.getElementById('selected-booking-slot');
    if (inputSlot) inputSlot.value = '';

    const hSelect = document.getElementById('book-hospital-select');
    const hospitalId = hSelect ? hSelect.value : 1;

    try {
        const url = hospitalId ? `/api/doctors/search?hospitalId=${hospitalId}&limit=20` : '/api/doctors/search?limit=20';
        const res = await fetch(url);
        bookingDoctors = await res.json();

        const dSelect = document.getElementById('book-doctor-select');
        if (dSelect) {
            if (bookingDoctors.length === 0) {
                dSelect.innerHTML = `<option value="">No Doctors Available at Selected Hospital</option>`;
            } else {
                dSelect.innerHTML = bookingDoctors.map(d => `<option value="${d.id}">Dr. ${d.name} (${d.specialization}) - ₹${d.consultationFee}</option>`).join('');
            }
        }

        await fetchAvailableSlotsForBooking();
        await updateLiveBookingFee();
    } catch (e) { console.error(e); }
}

async function fetchAvailableSlotsForBooking() {
    const docSelect = document.getElementById('book-doctor-select');
    const docId = docSelect && docSelect.value && !isNaN(parseInt(docSelect.value)) ? parseInt(docSelect.value) : 1;
    const hospSelect = document.getElementById('book-hospital-select');
    const hospId = hospSelect && hospSelect.value ? parseInt(hospSelect.value) : 1;
    const date = document.getElementById('book-date-input')?.value || new Date().toISOString().split('T')[0];

    const alertBanner = document.getElementById('slot-conflict-alert');
    if (alertBanner) alertBanner.classList.add('hidden');

    const container = document.getElementById('booking-slot-container');
    if (!container) return;

    container.innerHTML = `<p class="text-muted p-sm"><i class="fa-solid fa-spinner fa-spin text-primary"></i> Loading real-time slot availability (${date})...</p>`;

    try {
        const res = await fetch(`/api/appointments/availability?doctorId=${docId}&hospitalId=${hospId}&branchId=1&date=${date}`);
        const data = await res.json();
        const slots = data.slots || [];

        if (!Array.isArray(slots) || slots.length === 0) {
            container.innerHTML = `<p class="text-muted p-sm">No appointment slots available for selected doctor & date.</p>`;
            return;
        }

        container.innerHTML = slots.map(s => {
            const slotId = s.slotId;
            const displayStr = s.displayTime || '08:00 AM - 08:30 AM';
            const status = s.status || 'AVAILABLE';
            const isAvail = (status === 'AVAILABLE');

            let badgeClass = 'available';
            let label = displayStr;

            if (status === 'BOOKED') {
                badgeClass = 'booked';
                label = `${displayStr} (Booked)`;
            } else if (status === 'HELD') {
                badgeClass = 'held';
                label = `${displayStr} (Held)`;
            } else if (status === 'PAST') {
                badgeClass = 'past';
                label = `${displayStr} (Passed)`;
            } else if (status === 'BLOCKED') {
                badgeClass = 'blocked';
                label = `${displayStr} (Blocked)`;
            }

            return `
                <button type="button" class="slot-btn ${badgeClass}" data-slot-id="${slotId}" data-display="${displayStr}" data-start="${s.startTime}" data-end="${s.endTime}" ${isAvail ? `onclick="selectBookingSlot('${slotId}', '${displayStr}', this)"` : 'disabled'}>
                    ${label}
                </button>
            `;
        }).join('');

        const firstAvail = container.querySelector('.slot-btn.available');
        if (firstAvail) {
            firstAvail.click();
        } else {
            selectedBookingSlotId = null;
            selectedBookingSlotObj = null;
            const inputSlot = document.getElementById('selected-booking-slot');
            if (inputSlot) inputSlot.value = '';
        }
    } catch (e) {
        console.error('Error fetching availability:', e);
        container.innerHTML = `<p class="text-danger p-sm">Unable to load appointment slots. <button type="button" class="btn btn-xs btn-outline-primary m-left-xs" onclick="fetchAvailableSlotsForBooking()">Retry</button></p>`;
    }
}

async function selectBookingSlot(slotId, displayTime, btn) {
    document.querySelectorAll('.slot-btn').forEach(b => b.classList.remove('selected'));
    btn.classList.add('selected');

    selectedBookingSlotId = slotId;
    selectedBookingSlotObj = {
        slotId: slotId,
        displayTime: displayTime,
        startTime: btn.getAttribute('data-start'),
        endTime: btn.getAttribute('data-end')
    };

    const inputSlot = document.getElementById('selected-booking-slot');
    if (inputSlot) inputSlot.value = displayTime;

    const patientIdVal = document.getElementById('book-patient-id-val')?.value || (currentUser ? currentUser.userId : '1');
    const docSelect = document.getElementById('book-doctor-select');
    const hospSelect = document.getElementById('book-hospital-select');
    const docId = docSelect && docSelect.value ? parseInt(docSelect.value) : 1;
    const hospId = hospSelect && hospSelect.value ? parseInt(hospSelect.value) : 1;
    const date = document.getElementById('book-date-input')?.value || '';

    // Hold slot reservation for 5 minutes
    try {
        fetch('/api/appointments/slots/hold', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                slotId: slotId,
                patientId: patientIdVal,
                doctorId: docId,
                hospitalId: hospId,
                date: date,
                displayTime: displayTime
            })
        });
    } catch (ignored) {}
}

function calculateAgeFromDOB() {
    const dobInput = document.getElementById('book-patient-dob')?.value;
    const ageDisplay = document.getElementById('book-patient-age-display');
    const ageInput = document.getElementById('book-patient-age');

    if (!dobInput) return;

    const dob = new Date(dobInput);
    const today = new Date();
    let age = today.getFullYear() - dob.getFullYear();
    const m = today.getMonth() - dob.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
        age--;
    }

    if (ageDisplay) ageDisplay.innerText = `${age} years`;
    if (ageInput) ageInput.value = age;
}

async function lookupExistingPatient() {
    const query = document.getElementById('book-patient-lookup')?.value.trim();
    if (!query) return;

    try {
        const res = await fetch(`/api/patients/lookup?query=${encodeURIComponent(query)}`);
        if (res.status === 200) {
            const p = await res.json();
            showToast(`Patient profile found: ${p.name}`, 'success');

            if (document.getElementById('book-patient-id-val')) document.getElementById('book-patient-id-val').value = p.id;
            if (document.getElementById('book-patient-fname')) document.getElementById('book-patient-fname').value = p.firstName || p.name.split(' ')[0] || '';
            if (document.getElementById('book-patient-lname')) document.getElementById('book-patient-lname').value = p.lastName || p.name.split(' ').slice(1).join(' ') || '';
            if (document.getElementById('book-patient-dob')) {
                document.getElementById('book-patient-dob').value = p.dob || '';
                calculateAgeFromDOB();
            }
            if (document.getElementById('book-patient-gender')) document.getElementById('book-patient-gender').value = p.gender || 'Male';
            if (document.getElementById('book-patient-phone')) document.getElementById('book-patient-phone').value = p.phone || '';
            if (document.getElementById('book-patient-email')) document.getElementById('book-patient-email').value = p.email || '';
            if (document.getElementById('book-patient-blood')) document.getElementById('book-patient-blood').value = p.bloodGroup || 'O+';
            if (document.getElementById('book-patient-city')) document.getElementById('book-patient-city').value = p.city || 'Hyderabad';
            if (document.getElementById('book-patient-address')) document.getElementById('book-patient-address').value = p.address || '';
        } else {
            showToast('No registered patient found matching code/phone.', 'info');
        }
    } catch (e) { console.error(e); }
}

function checkBloodPressureVitals() {
    const sys = parseInt(document.getElementById('book-bp-sys')?.value || '120');
    const dia = parseInt(document.getElementById('book-bp-dia')?.value || '80');
    const warningElem = document.getElementById('book-bp-warning');

    if (!warningElem) return;

    if (sys > 140 || dia > 90) {
        warningElem.className = 'alert alert-danger m-top-xs';
        warningElem.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> <strong>High Blood Pressure Alert (${sys}/${dia} mmHg):</strong> Immediate physician consultation recommended.`;
    } else if (sys < 90 || dia < 60) {
        warningElem.className = 'alert alert-warning m-top-xs';
        warningElem.innerHTML = `<i class="fa-solid fa-circle-exclamation"></i> <strong>Low Blood Pressure Alert (${sys}/${dia} mmHg):</strong> Advise patient to stay hydrated and rest.`;
    } else {
        warningElem.className = 'alert alert-success m-top-xs';
        warningElem.innerHTML = `<i class="fa-solid fa-check-circle"></i> <strong>Normal Blood Pressure Vitals (${sys}/${dia} mmHg).</strong>`;
    }
}

function addMedicineRow() {
    const name = document.getElementById('med-input-name')?.value.trim();
    const dosage = document.getElementById('med-input-dosage')?.value.trim() || '500mg';
    const freq = document.getElementById('med-input-freq')?.value.trim() || '1-0-1';
    const duration = document.getElementById('med-input-duration')?.value.trim() || '5 days';

    if (!name) {
        showToast('Please enter medicine name.', 'warning');
        return;
    }

    addedMedications.push({ name, dosage, frequency: freq, duration });

    if (document.getElementById('med-input-name')) document.getElementById('med-input-name').value = '';
    if (document.getElementById('med-input-dosage')) document.getElementById('med-input-dosage').value = '';

    renderMedicationsTable();
}

function renderMedicationsTable() {
    const tbl = document.getElementById('tbl-medications-list');
    if (!tbl) return;

    if (addedMedications.length === 0) {
        tbl.innerHTML = `<tr><td colspan="5" class="text-center text-muted p-xs">No medications added.</td></tr>`;
        return;
    }

    tbl.innerHTML = addedMedications.map((m, i) => `
        <tr>
            <td><strong>${m.name}</strong></td>
            <td>${m.dosage}</td>
            <td>${m.frequency}</td>
            <td>${m.duration}</td>
            <td><button type="button" class="btn btn-xs btn-danger" onclick="removeMedicineRow(${i})">&times;</button></td>
        </tr>
    `).join('');
}

function removeMedicineRow(idx) {
    addedMedications.splice(idx, 1);
    renderMedicationsTable();
}

async function updateLiveBookingFee() {
    const docSelect = document.getElementById('book-doctor-select');
    const hospSelect = document.getElementById('book-hospital-select');
    const typeSelect = document.getElementById('book-type-select');

    const docId = docSelect && docSelect.value ? parseInt(docSelect.value) : 1;
    const hospId = hospSelect && hospSelect.value ? parseInt(hospSelect.value) : 1;
    const type = typeSelect ? typeSelect.value : 'In-Person Consultation';

    try {
        const res = await fetch(`/api/appointments/calculate-fee?doctorId=${docId}&hospitalId=${hospId}&appointmentType=${encodeURIComponent(type)}`);
        const feeData = await res.json();

        const base = feeData.baseFee || 650;
        const service = feeData.serviceCharge || 50;
        const subtotal = feeData.subtotal || (base + service);
        const tax = feeData.taxAmount || Math.round((subtotal * 0.05) * 100.0) / 100.0;
        const discount = feeData.discount || 0;
        const total = feeData.totalAmount || (subtotal + tax - discount);

        if (document.getElementById('live-fee-base')) document.getElementById('live-fee-base').innerText = `₹${base.toFixed(2)}`;
        if (document.getElementById('live-fee-service')) document.getElementById('live-fee-service').innerText = `₹${service.toFixed(2)}`;
        if (document.getElementById('live-fee-subtotal')) document.getElementById('live-fee-subtotal').innerText = `₹${subtotal.toFixed(2)}`;
        if (document.getElementById('live-fee-tax')) document.getElementById('live-fee-tax').innerText = `₹${tax.toFixed(2)}`;
        if (document.getElementById('live-fee-discount')) document.getElementById('live-fee-discount').innerText = `₹${discount.toFixed(2)}`;
        if (document.getElementById('live-fee-total')) document.getElementById('live-fee-total').innerText = `₹${total.toFixed(2)}`;
    } catch (e) { console.error(e); }
}

async function handleBookAppointmentSubmit(e) {
    e.preventDefault();

    const submitBtn = document.querySelector('#book-appointment-form button[type="submit"]');
    const alertBanner = document.getElementById('slot-conflict-alert');
    const alertMsg = document.getElementById('slot-conflict-msg');

    const docSelect = document.getElementById('book-doctor-select');
    const hospSelect = document.getElementById('book-hospital-select');
    const slot = document.getElementById('selected-booking-slot')?.value;
    const fname = document.getElementById('book-patient-fname')?.value.trim();
    const lname = document.getElementById('book-patient-lname')?.value.trim();
    const phone = document.getElementById('book-patient-phone')?.value.trim();

    if (!fname || !lname) {
        showToast('Please enter Patient First Name and Last Name.', 'danger');
        return;
    }

    if (!phone || phone.length < 10) {
        showToast('Please enter a valid 10-digit mobile number.', 'danger');
        return;
    }

    if (!docSelect || !docSelect.value || isNaN(parseInt(docSelect.value))) {
        showToast('Please select a doctor for your appointment.', 'danger');
        return;
    }

    if (!slot || !selectedBookingSlotId) {
        showToast('Please select an available time slot.', 'danger');
        return;
    }

    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Booking...`;
    }

    const patientIdVal = document.getElementById('book-patient-id-val')?.value;
    const isEmergency = document.getElementById('book-is-emergency')?.value === 'true';

    const req = {
        userId: currentUser ? currentUser.userId : null,
        patientId: patientIdVal ? parseInt(patientIdVal) : (currentUser ? currentUser.userId : null),
        patientFirstName: fname,
        patientLastName: lname,
        patientPhone: phone,
        patientEmail: document.getElementById('book-patient-email')?.value.trim() || '',
        patientGender: document.getElementById('book-patient-gender')?.value || 'Male',
        patientDob: document.getElementById('book-patient-dob')?.value || '1995-01-01',
        patientBloodGroup: document.getElementById('book-patient-blood')?.value || 'O+',
        patientCity: document.getElementById('book-patient-city')?.value || 'Hyderabad',
        patientAddress: document.getElementById('book-patient-address')?.value || '',
        patientHeight: parseFloat(document.getElementById('book-height')?.value || '170'),
        patientWeight: parseFloat(document.getElementById('book-weight')?.value || '65'),
        emergencyContactName: document.getElementById('book-emg-name')?.value.trim() || '',
        emergencyContactRelationship: document.getElementById('book-emg-rel')?.value || 'Spouse',
        emergencyContactPhone: document.getElementById('book-emg-phone')?.value.trim() || '',
        doctorId: parseInt(docSelect.value),
        hospitalId: parseInt(hospSelect ? hospSelect.value : '1'),
        branchId: 1,
        deptId: 1,
        appointmentDate: document.getElementById('book-date-input').value,
        timeSlot: slot,
        slotId: selectedBookingSlotId,
        appointmentType: document.getElementById('book-type-select').value,
        healthProblemType: document.getElementById('book-problem-type')?.value || 'General Check-up',
        symptoms: getSelectedSymptoms(),
        problemDescription: document.getElementById('book-problem-desc')?.value.trim(),
        isEmergency: isEmergency,
        reason: document.getElementById('book-problem-desc')?.value.trim() || 'General Consultation'
    };

    try {
        const res = await fetch('/api/appointments', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });
        const data = await res.json();

        if (data.success) {
            if (submitBtn) {
                submitBtn.innerHTML = `<i class="fa-solid fa-check"></i> Appointment Confirmed ✓`;
            }
            showToast('Appointment booked successfully!', 'success');

            if (typeof loadAppointmentsTable === 'function') loadAppointmentsTable();
            if (typeof loadDashboardStats === 'function') loadDashboardStats();

            openAppointmentInvoiceModal({
                code: data.appointmentCode || 'APT' + Math.floor(10000 + Math.random() * 90000),
                patientName: `${fname} ${lname}`,
                doctorName: docSelect.options[docSelect.selectedIndex].text.split('-')[0],
                hospitalName: hospSelect.options[hospSelect.selectedIndex].text,
                date: req.appointmentDate,
                slot: req.timeSlot,
                type: req.appointmentType,
                totalAmount: document.getElementById('live-fee-total')?.innerText || '₹1,010.10'
            });
        } else {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = `<i class="fa-solid fa-check"></i> Confirm & Book Appointment`;
            }

            const errorMsg = data.message || 'Selected appointment slot is unavailable or invalid.';

            if (alertBanner && alertMsg) {
                alertMsg.innerText = errorMsg;
                alertBanner.classList.remove('hidden');
                alertBanner.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }

            showToast(errorMsg, 'danger');
            await fetchAvailableSlotsForBooking();
        }
    } catch (err) {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = `<i class="fa-solid fa-check"></i> Confirm & Book Appointment`;
        }
        showToast('Server communication error during booking.', 'danger');
    }
}

function getSelectedSymptoms() {
    const checkboxes = document.querySelectorAll('.symptom-checkbox:checked');
    return Array.from(checkboxes).map(c => c.value).join(', ');
}

function openAppointmentInvoiceModal(apt) {
    const modal = document.getElementById('modal-appointment-invoice');
    if (!modal) return;

    document.getElementById('inv-code').innerText = apt.code;
    document.getElementById('inv-patient-name').innerText = apt.patientName;
    document.getElementById('inv-doc-name').innerText = apt.doctorName;
    document.getElementById('inv-hosp-name').innerText = apt.hospitalName;
    document.getElementById('inv-date-slot').innerText = `${apt.date} | ${apt.slot}`;
    document.getElementById('inv-type').innerText = apt.type;
    document.getElementById('inv-total-fee').innerText = apt.totalAmount;

    modal.style.display = 'flex';
}

function closeAppointmentInvoiceModal() {
    const modal = document.getElementById('modal-appointment-invoice');
    if (modal) modal.style.display = 'none';
    switchView('appointments');
}

function printAppointmentInvoice() {
    window.print();
}
