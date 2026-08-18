/* ==========================================
   AURA HEALTH DASHBOARD & CHART.JS MODULE
   ========================================== */

let appointmentChartInstance = null;
let revenueChartInstance = null;

async function loadDashboardStats() {
    let stats = {
        totalHospitals: 1000,
        totalBranches: 2500,
        totalDoctors: 1000000,
        totalPatients: 45000,
        todayAppointments: 342,
        pendingAppointments: 28,
        completedAppointments: 294,
        cancelledAppointments: 20,
        totalRevenue: 345000.00,
        pendingPayments: 12500.00,
        availableDoctors: 842000,
        emergencyHospitals: 850
    };

    try {
        const res = await fetch('/api/dashboard/stats');
        if (res && res.status === 200) {
            const data = await res.json();
            stats = Object.assign(stats, data);
        }
    } catch (e) {
        console.log('Running in static mode - rendering platform stats.');
    }

    // Update 12 KPI metric cards
    updateKpiText('kpi-hospitals', (stats.totalHospitals || 1000).toLocaleString());
    updateKpiText('kpi-branches', (stats.totalBranches || 2500).toLocaleString());
    updateKpiText('kpi-doctors', (stats.totalDoctors || 1000000).toLocaleString());
    updateKpiText('kpi-patients', (stats.totalPatients || 45000).toLocaleString());
    updateKpiText('kpi-today-apt', stats.todayAppointments || 342);
    updateKpiText('kpi-pending-apt', stats.pendingAppointments || 28);
    updateKpiText('kpi-completed-apt', stats.completedAppointments || 294);
    updateKpiText('kpi-cancelled-apt', stats.cancelledAppointments || 20);
    updateKpiText('kpi-revenue', `₹${(stats.totalRevenue || 345000.00).toLocaleString('en-IN')}`);
    updateKpiText('kpi-pending-payments', `₹${(stats.pendingPayments || 12500.00).toLocaleString('en-IN')}`);
    updateKpiText('kpi-available-doctors', (stats.availableDoctors || 842000).toLocaleString());
    updateKpiText('kpi-emergency-hosp', (stats.emergencyHospitals || 850).toLocaleString());

    // Render Live Appointments Table
    loadDashboardAppointmentsQueue();

    // Render Chart.js Analytics
    initDashboardCharts();
}

function updateKpiText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

async function loadDashboardAppointmentsQueue() {
    const tbl = document.getElementById('tbl-dashboard-recent-apt');
    if (!tbl) return;

    let list = [];
    try {
        const res = await fetch('/api/appointments');
        if (res && res.status === 200) {
            list = await res.json();
        }
    } catch (e) {}

    if (!Array.isArray(list) || list.length === 0) {
        list = [
            { appointmentCode: "APT00130", patientName: "Anjali Sharma", doctorName: "Dr. Dev Rao", deptName: "Cardiology", appointmentDate: "2026-08-21", timeSlot: "10:00 AM - 10:30 AM", status: "Confirmed" },
            { appointmentCode: "APT00129", patientName: "Vishnu Reddy", doctorName: "Dr. Dev Rao", deptName: "Cardiology", appointmentDate: "2026-08-20", timeSlot: "09:00 AM - 09:30 AM", status: "Confirmed" },
            { appointmentCode: "APT00128", patientName: "Priya Singh", doctorName: "Dr. Ananya Sharma", deptName: "Neurology", appointmentDate: "2026-08-19", timeSlot: "11:00 AM - 11:30 AM", status: "Checked-In" }
        ];
    }

    tbl.innerHTML = list.slice(0, 5).map(a => `
        <tr>
            <td><strong>${a.appointmentCode}</strong></td>
            <td><strong>${a.patientName}</strong></td>
            <td>${a.doctorName}<br><small class="text-muted">${a.deptName || 'General Medicine'}</small></td>
            <td>${a.appointmentDate}<br><small class="text-primary">${a.timeSlot}</small></td>
            <td><span class="badge ${getAppointmentStatusBadge(a.status)}">${a.status}</span></td>
        </tr>
    `).join('');
}

function getAppointmentStatusBadge(status) {
    switch (status) {
        case 'Confirmed': return 'badge-success';
        case 'Checked-In': return 'badge-info';
        case 'In Consultation': return 'badge-warning';
        case 'Completed': return 'badge-primary';
        case 'Cancelled': return 'badge-danger';
        default: return 'badge-secondary';
    }
}

function initDashboardCharts() {
    const ctxApt = document.getElementById('chart-appointments-overview')?.getContext('2d');
    const ctxRev = document.getElementById('chart-revenue-trends')?.getContext('2d');

    if (ctxApt) {
        if (appointmentChartInstance) appointmentChartInstance.destroy();
        appointmentChartInstance = new Chart(ctxApt, {
            type: 'bar',
            data: {
                labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
                datasets: [
                    { label: 'Completed', data: [120, 150, 180, 140, 200, 90, 60], backgroundColor: '#0D9488' },
                    { label: 'Pending', data: [20, 30, 25, 40, 35, 15, 10], backgroundColor: '#0284C7' },
                    { label: 'Cancelled', data: [5, 8, 4, 10, 6, 3, 2], backgroundColor: '#EF4444' }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { labels: { color: '#94A3B8' } } },
                scales: {
                    x: { ticks: { color: '#94A3B8' }, grid: { color: 'rgba(255,255,255,0.05)' } },
                    y: { ticks: { color: '#94A3B8' }, grid: { color: 'rgba(255,255,255,0.05)' } }
                }
            }
        });
    }

    if (ctxRev) {
        if (revenueChartInstance) revenueChartInstance.destroy();
        revenueChartInstance = new Chart(ctxRev, {
            type: 'line',
            data: {
                labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug'],
                datasets: [{
                    label: 'Consultation Revenue (₹ in Lakhs)',
                    data: [42, 48, 55, 62, 70, 85, 98, 112],
                    borderColor: '#10B981',
                    backgroundColor: 'rgba(16, 185, 129, 0.15)',
                    fill: true,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { labels: { color: '#94A3B8' } } },
                scales: {
                    x: { ticks: { color: '#94A3B8' }, grid: { color: 'rgba(255,255,255,0.05)' } },
                    y: { ticks: { color: '#94A3B8' }, grid: { color: 'rgba(255,255,255,0.05)' } }
                }
            }
        });
    }
}
