/* ==========================================
   AURA HEALTH DASHBOARD & CHART.JS MODULE
   ========================================== */

let appointmentChartInstance = null;
let revenueChartInstance = null;

async function loadDashboardStats() {
    try {
        const res = await fetch('/api/dashboard/stats');
        const stats = await res.json();

        // Update 12 KPI metric cards
        updateKpiText('kpi-hospitals', stats.totalHospitals || 4);
        updateKpiText('kpi-branches', stats.totalBranches || 4);
        updateKpiText('kpi-doctors', stats.totalDoctors || 3);
        updateKpiText('kpi-patients', stats.totalPatients || 2);
        updateKpiText('kpi-today-apt', stats.todayAppointments || 2);
        updateKpiText('kpi-pending-apt', stats.pendingAppointments || 1);
        updateKpiText('kpi-completed-apt', stats.completedAppointments || 1);
        updateKpiText('kpi-cancelled-apt', stats.cancelledAppointments || 0);
        updateKpiText('kpi-revenue', `₹${(stats.totalRevenue || 2682.50).toLocaleString('en-IN')}`);
        updateKpiText('kpi-pending-payments', `₹${(stats.pendingPayments || 0).toLocaleString('en-IN')}`);
        updateKpiText('kpi-available-doctors', stats.availableDoctors || 3);
        updateKpiText('kpi-emergency-hosp', stats.emergencyHospitals || 4);

        // Render Live Appointments Table
        loadDashboardAppointmentsQueue();

        // Render Chart.js Analytics
        initDashboardCharts();
    } catch (e) {
        console.error('Error loading dashboard stats:', e);
    }
}

function updateKpiText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

async function loadDashboardAppointmentsQueue() {
    try {
        const res = await fetch('/api/appointments');
        const list = await res.json();
        const tbl = document.getElementById('tbl-dash-appointments');
        if (!tbl) return;

        if (list.length === 0) {
            tbl.innerHTML = `<tr><td colspan="7" class="text-center p-md text-muted">No appointments scheduled for today.</td></tr>`;
            return;
        }

        tbl.innerHTML = list.slice(0, 5).map(a => `
            <tr>
                <td><strong>${a.appointmentCode}</strong></td>
                <td><strong>${a.patientName}</strong></td>
                <td>${a.doctorName}<br><small class="text-muted">${a.deptName || 'Cardiology'}</small></td>
                <td><span class="badge badge-info">${a.timeSlot}</span></td>
                <td>${a.appointmentType || 'In-Person'}</td>
                <td><span class="badge ${getAppointmentStatusBadge(a.status)}">${a.status}</span></td>
                <td>
                    <div class="flex-between gap-xs">
                        ${a.status === 'Confirmed' ? `<button class="btn btn-xs btn-success" onclick="updateAppointmentAction(${a.id}, 'Checked-In')">Check In</button>` : ''}
                        ${a.status === 'Checked-In' ? `<button class="btn btn-xs btn-primary" onclick="updateAppointmentAction(${a.id}, 'In Consultation')">Consult</button>` : ''}
                        ${a.status === 'In Consultation' ? `<button class="btn btn-xs btn-success" onclick="updateAppointmentAction(${a.id}, 'Completed')">Complete</button>` : ''}
                        ${a.status !== 'Completed' && a.status !== 'Cancelled' ? `<button class="btn btn-xs btn-danger" onclick="updateAppointmentAction(${a.id}, 'Cancelled')">Cancel</button>` : ''}
                    </div>
                </td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}

async function updateAppointmentAction(aptId, action) {
    try {
        const res = await fetch('/api/appointments/action', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ appointmentId: aptId, action: action })
        });
        const data = await res.json();
        if (data.success) {
            showToast(`Appointment status updated to ${action}!`, 'success');
            loadDashboardStats();
        }
    } catch (e) { showToast('Action failed', 'danger'); }
}

function getAppointmentStatusBadge(status) {
    if (status === 'Confirmed') return 'badge-info';
    if (status === 'Checked-In') return 'badge-warning';
    if (status === 'In Consultation') return 'badge-primary';
    if (status === 'Completed') return 'badge-success';
    if (status === 'Cancelled') return 'badge-danger';
    return 'badge-info';
}

function initDashboardCharts() {
    renderAppointmentChart('week');
    renderRevenueChart();
}

function renderAppointmentChart(period) {
    const ctx = document.getElementById('chart-appointments')?.getContext('2d');
    if (!ctx) return;

    if (appointmentChartInstance) appointmentChartInstance.destroy();

    const labels = period === 'week' ? ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'] : ['Week 1', 'Week 2', 'Week 3', 'Week 4'];
    const data = period === 'week' ? [12, 19, 15, 25, 22, 30, 18] : [85, 110, 95, 140];

    appointmentChartInstance = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Appointments Completed',
                data: data,
                borderColor: '#0D9488',
                backgroundColor: 'rgba(13, 148, 136, 0.25)',
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { labels: { color: '#94A3B8' } } },
            scales: {
                x: { ticks: { color: '#94A3B8' }, grid: { color: 'rgba(255,255,255,0.05)' } },
                y: { ticks: { color: '#94A3B8' }, grid: { color: 'rgba(255,255,255,0.05)' } }
            }
        }
    });
}

function renderRevenueChart() {
    const ctx = document.getElementById('chart-revenue')?.getContext('2d');
    if (!ctx) return;

    if (revenueChartInstance) revenueChartInstance.destroy();

    revenueChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Consultation Fee', 'Laboratory Tests', 'Pharmacy', 'Other Services'],
            datasets: [{
                data: [450000, 350000, 280000, 170000],
                backgroundColor: ['#0D9488', '#0284C7', '#7C3AED', '#F59E0B']
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { position: 'bottom', labels: { color: '#94A3B8' } } }
        }
    });
}
