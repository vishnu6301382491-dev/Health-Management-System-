/* ==========================================
   APPOINTMENTS & SLOT BOOKING ENGINE
   ========================================== */

let selectedBookingSlotId = null;
let selectedBookingSlotObj = null;
let bookingHospitals = [];
let bookingDoctors = [];

async function loadAppointmentsTable() {
    let list = [];

    if (typeof DEMO_MODE !== 'undefined' && !DEMO_MODE) {
        const res = await apiRequest('/api/appointments');
        if (res && res.status === 200) {
            try {
                list = await res.json();
            } catch (e) {}
        }
    }

    if (!Array.isArray(list) || list.length === 0) {
        list = [
            { id: 1, appointmentCode: "APT00130", patientName: "Anjali Sharma", doctorName: "Dr. Dev Rao", deptName: "Cardiology", appointmentDate: "2026-08-21", timeSlot: "10:00 AM - 10:30 AM", appointmentType: "In-Person Consultation", status: "Confirmed" },
            { id: 2, appointmentCode: "APT00129", patientName: "Vishnu Reddy", doctorName: "Dr. Dev Rao", deptName: "Cardiology", appointmentDate: "2026-08-20", timeSlot: "09:00 AM - 09:30 AM", appointmentType: "In-Person Consultation", status: "Confirmed" },
            { id: 3, appointmentCode: "APT00128", patientName: "Priya Singh", doctorName: "Dr. Ananya Sharma", deptName: "Neurology", appointmentDate: "2026-08-19", timeSlot: "11:00 AM - 11:30 AM", appointmentType: "Video Consultation", status: "Checked-In" }
        ];
    }

    const tbl = document.getElementById('tbl-appointments-list');
    if (!tbl) return;

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
}

function getAppointmentStatusBadge(status) {
    if (status === 'Confirmed') return 'badge-success';
    if (status === 'Checked-In') return 'badge-info';
    if (status === 'In Consultation') return 'badge-warning';
    if (status === 'Completed') return 'badge-secondary';
    if (status === 'Cancelled') return 'badge-danger';
    return 'badge-primary';
}

async function initBookingFormDropdowns() {
    try {
        let hospitalsData = null;
        if (typeof DEMO_MODE !== 'undefined' && !DEMO_MODE) {
            const resH = await apiRequest('/api/hospitals');
            if (resH && resH.status === 200) {
                try { hospitalsData = await resH.json(); } catch(e){}
            }
        }

        if (Array.isArray(hospitalsData) && hospitalsData.length > 0) {
            bookingHospitals = hospitalsData;
        } else {
            bookingHospitals = [
                { id: 1, name: "AURA Medical Centre", city: "Hyderabad" },
                { id: 2, name: "Apollo Health City", city: "Bengaluru" },
                { id: 3, name: "KIMS Global Hospital", city: "Hyderabad" },
                { id: 4, name: "Yashoda Super Specialty", city: "Secunderabad" }
            ];
        }

        const hSelect = document.getElementById('book-hospital-select');
        if (hSelect) {
            hSelect.innerHTML = `<option value="">Select Hospital Network</option>` + 
                bookingHospitals.map(h => `<option value="${h.id}">${h.name} (${h.city})</option>`).join('');
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
    } catch (e) {
        console.error('Error initializing booking dropdowns:', e);
    }
}

async function filterDoctorsByHospital() {
    selectedBookingSlotId = null;
    selectedBookingSlotObj = null;
    const inputSlot = document.getElementById('selected-booking-slot');
    if (inputSlot) inputSlot.value = '';

    const hSelect = document.getElementById('book-hospital-select');
    const hospitalId = hSelect && hSelect.value ? parseInt(hSelect.value) : 1;

    try {
        let doctorsData = null;
        if (typeof DEMO_MODE !== 'undefined' && !DEMO_MODE) {
            const url = hospitalId ? `/api/doctors/search?hospitalId=${hospitalId}&limit=20` : '/api/doctors/search?limit=20';
            const res = await apiRequest(url);
            if (res && res.status === 200) {
                try { doctorsData = await res.json(); } catch(e){}
            }
        }

        if (Array.isArray(doctorsData) && doctorsData.length > 0) {
            bookingDoctors = doctorsData;
        } else {
            const allDemoDoctors = [
                { id: 4506, hospitalId: 1, name: "Dev Rao", specialization: "Cardiologist", consultationFee: 1356 },
                { id: 101, hospitalId: 1, name: "Rohan Reddy", specialization: "Cardiologist", consultationFee: 912 },
                { id: 102, hospitalId: 2, name: "Ananya Sharma", specialization: "Neurologist", consultationFee: 1100 },
                { id: 103, hospitalId: 3, name: "Vikram Chowdhury", specialization: "Orthopedic", consultationFee: 850 },
                { id: 104, hospitalId: 4, name: "Priya Menon", specialization: "Pediatrician", consultationFee: 750 }
            ];
            bookingDoctors = allDemoDoctors.filter(d => d.hospitalId === hospitalId);
            if (bookingDoctors.length === 0) {
                bookingDoctors = allDemoDoctors.filter(d => d.hospitalId === 1);
            }
        }

        const dSelect = document.getElementById('book-doctor-select');
        if (dSelect) {
            if (bookingDoctors.length === 0) {
                dSelect.innerHTML = `<option value="">No Doctors Available at Selected Hospital</option>`;
            } else {
                dSelect.innerHTML = bookingDoctors.map(d => `<option value="${d.id}">Dr. ${d.name} (${d.specialization}) - ₹${d.consultationFee}</option>`).join('');
                dSelect.value = bookingDoctors[0].id;
            }
        }

        await fetchAvailableSlotsForBooking();
        await updateLiveBookingFee();
    } catch (e) {
        console.error('Error filtering doctors by hospital:', e);
    }
}

async function fetchAvailableSlotsForBooking() {
    const docSelect = document.getElementById('book-doctor-select');
    const docId = docSelect && docSelect.value && !isNaN(parseInt(docSelect.value)) ? parseInt(docSelect.value) : 4506;
    const hospSelect = document.getElementById('book-hospital-select');
    const hospId = hospSelect && hospSelect.value ? parseInt(hospSelect.value) : 1;
    const date = document.getElementById('book-date-input')?.value || new Date().toISOString().split('T')[0];

    const alertBanner = document.getElementById('slot-conflict-alert');
    if (alertBanner) alertBanner.classList.add('hidden');

    const container = document.getElementById('booking-slot-container');
    if (!container) return;

    let slots = [];

    if (typeof DEMO_MODE !== 'undefined' && !DEMO_MODE) {
        const res = await apiRequest(`/api/appointments/availability?doctorId=${docId}&hospitalId=${hospId}&branchId=1&date=${date}`);
        if (res && res.status === 200) {
            try {
                const data = await res.json();
                slots = data.slots || [];
            } catch(e){}
        }
    }

    if (!Array.isArray(slots) || slots.length === 0) {
        slots = [
            { slotId: "SLOT_0800", displayTime: "08:00 AM - 08:30 AM", startTime: "08:00:00", endTime: "08:30:00", status: "AVAILABLE" },
            { slotId: "SLOT_0830", displayTime: "08:30 AM - 09:00 AM", startTime: "08:30:00", endTime: "09:00:00", status: "AVAILABLE" },
            { slotId: "SLOT_0900", displayTime: "09:00 AM - 09:30 AM", startTime: "09:00:00", endTime: "09:30:00", status: "AVAILABLE" },
            { slotId: "SLOT_0930", displayTime: "09:30 AM - 10:00 AM", startTime: "09:30:00", endTime: "10:00:00", status: "AVAILABLE" },
            { slotId: "SLOT_1000", displayTime: "10:00 AM - 10:30 AM", startTime: "10:00:00", endTime: "10:30:00", status: "AVAILABLE" },
            { slotId: "SLOT_1030", displayTime: "10:30 AM - 11:00 AM", startTime: "10:30:00", endTime: "11:00:00", status: "AVAILABLE" }
        ];
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
}

async function updateLiveBookingFee() {
    const docSelect = document.getElementById('book-doctor-select');
    const hospSelect = document.getElementById('book-hospital-select');
    const typeSelect = document.getElementById('book-type-select');

    const docId = docSelect && docSelect.value ? parseInt(docSelect.value) : 4506;
    const hospId = hospSelect && hospSelect.value ? parseInt(hospSelect.value) : 1;
    const type = typeSelect ? typeSelect.value : 'In-Person Consultation';

    let feeData = null;
    if (typeof DEMO_MODE !== 'undefined' && !DEMO_MODE) {
        const res = await apiRequest(`/api/appointments/calculate-fee?doctorId=${docId}&hospitalId=${hospId}&appointmentType=${encodeURIComponent(type)}`);
        if (res && res.status === 200) {
            try { feeData = await res.json(); } catch(e){}
        }
    }

    let base = 912;
    if (bookingDoctors && bookingDoctors.length > 0) {
        const selDoc = bookingDoctors.find(d => d.id === docId);
        if (selDoc && selDoc.consultationFee) base = selDoc.consultationFee;
    }

    if (feeData) {
        base = feeData.baseFee || base;
    }

    const service = 50.00;
    const subtotal = base + service;
    const tax = Math.round((subtotal * 0.05) * 100.0) / 100.0;
    const discount = 0;
    const total = subtotal + tax - discount;

    if (document.getElementById('live-fee-base')) document.getElementById('live-fee-base').innerText = `₹${base.toFixed(2)}`;
    if (document.getElementById('live-fee-service')) document.getElementById('live-fee-service').innerText = `₹${service.toFixed(2)}`;
    if (document.getElementById('live-fee-subtotal')) document.getElementById('live-fee-subtotal').innerText = `₹${subtotal.toFixed(2)}`;
    if (document.getElementById('live-fee-tax')) document.getElementById('live-fee-tax').innerText = `₹${tax.toFixed(2)}`;
    if (document.getElementById('live-fee-discount')) document.getElementById('live-fee-discount').innerText = `₹${discount.toFixed(2)}`;
    if (document.getElementById('live-fee-total')) document.getElementById('live-fee-total').innerText = `₹${total.toFixed(2)}`;
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

    if (!slot) {
        showToast('Please select an available time slot for your appointment.', 'danger');
        return;
    }

    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Confirming Booking...`;
    }

    const req = {
        patientId: document.getElementById('book-patient-id-val')?.value || (currentUser ? currentUser.userId : '1'),
        doctorId: docSelect ? parseInt(docSelect.value) : 4506,
        hospitalId: hospSelect ? parseInt(hospSelect.value) : 1,
        branchId: 1,
        appointmentDate: document.getElementById('book-date-input')?.value || new Date().toISOString().split('T')[0],
        timeSlot: slot,
        slotId: selectedBookingSlotId || 'SLOT_0800',
        appointmentType: document.getElementById('book-type-select')?.value || 'In-Person Consultation',
        patientName: `${fname} ${lname}`,
        patientPhone: phone || '9876543210',
        patientEmail: document.getElementById('book-patient-email')?.value.trim() || 'patient@aura.com',
        reason: document.getElementById('book-problem-desc')?.value.trim() || 'General Consultation'
    };

    if (typeof DEMO_MODE !== 'undefined' && !DEMO_MODE) {
        const res = await apiRequest('/api/appointments', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });
        if (res && res.status === 200) {
            try {
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
                        doctorName: docSelect.options[docSelect.selectedIndex]?.text.split('-')[0] || 'Dr. Dev Rao',
                        hospitalName: hospSelect.options[hospSelect.selectedIndex]?.text || 'AURA Medical Centre',
                        date: req.appointmentDate,
                        slot: req.timeSlot,
                        type: req.appointmentType,
                        totalAmount: document.getElementById('live-fee-total')?.innerText || '₹1,010.10'
                    });
                    return;
                }
            } catch(err){}
        }
    }

    if (submitBtn) {
        submitBtn.innerHTML = `<i class="fa-solid fa-check"></i> Appointment Confirmed ✓`;
    }
    showToast('Appointment booked successfully! (Demo Mode)', 'success');

    if (typeof loadAppointmentsTable === 'function') loadAppointmentsTable();
    if (typeof loadDashboardStats === 'function') loadDashboardStats();

    openAppointmentInvoiceModal({
        code: 'APT' + Math.floor(10000 + Math.random() * 90000),
        patientName: `${fname} ${lname}`,
        doctorName: docSelect.options[docSelect.selectedIndex]?.text.split('-')[0] || 'Dr. Dev Rao',
        hospitalName: hospSelect.options[hospSelect.selectedIndex]?.text || 'AURA Medical Centre',
        date: req.appointmentDate,
        slot: req.timeSlot,
        type: req.appointmentType,
        totalAmount: document.getElementById('live-fee-total')?.innerText || '₹1,010.10'
    });
}

function openAppointmentInvoiceModal(invoice) {
    const modal = document.getElementById('modal-invoice');
    if (!modal) return;

    if (document.getElementById('inv-code')) document.getElementById('inv-code').innerText = invoice.code;
    if (document.getElementById('inv-patient')) document.getElementById('inv-patient').innerText = invoice.patientName;
    if (document.getElementById('inv-doctor')) document.getElementById('inv-doctor').innerText = invoice.doctorName;
    if (document.getElementById('inv-hospital')) document.getElementById('inv-hospital').innerText = invoice.hospitalName;
    if (document.getElementById('inv-date')) document.getElementById('inv-date').innerText = `${invoice.date} (${invoice.slot})`;
    if (document.getElementById('inv-type')) document.getElementById('inv-type').innerText = invoice.type;
    if (document.getElementById('inv-amount')) document.getElementById('inv-amount').innerText = invoice.totalAmount;

    modal.classList.remove('hidden');
}

function closeInvoiceModal() {
    const modal = document.getElementById('modal-invoice');
    if (modal) modal.classList.add('hidden');
    switchView('appointments');
}
