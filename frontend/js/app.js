/* ==========================================
   AURA HEALTH 3D - MAIN PLATFORM ENGINE
   ========================================== */

let currentUser = null;
let currentView = 'dashboard';
let currentTheme = 'theme-modern';

// Three.js Engine Variables
let scene, camera, renderer, animationFrameId;
let themeObjects = [];

let performanceMode = localStorage.getItem('aura_perf_mode') || 'BALANCED';

document.addEventListener('DOMContentLoaded', () => {
    init3DEngine();
    checkAuthSession();
    checkBackendHealth();
    setup3DTiltEffect();

    document.addEventListener('visibilitychange', () => {
        if (document.hidden) {
            if (animationFrameId) cancelAnimationFrame(animationFrameId);
        } else {
            animate3D();
        }
    });
});

function setPerformanceMode(mode) {
    performanceMode = mode;
    localStorage.setItem('aura_perf_mode', mode);
    const container = document.getElementById('canvas-container');
    if (container) {
        container.style.display = (mode === 'PERFORMANCE') ? 'none' : 'block';
    }
    showToast(`Performance Mode updated to ${mode}`, 'info');
}

/* ----------------------------------------------------
   1. SECTION-SPECIFIC 3D THREE.JS ANIMATION ENGINE
   ---------------------------------------------------- */
function init3DEngine() {
    const container = document.getElementById('canvas-container');
    if (!container) return;

    scene = new THREE.Scene();
    camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 1000);
    camera.position.z = 30;

    renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    container.appendChild(renderer.domElement);

    buildSection3DScene(currentView);
    animate3D();

    window.addEventListener('resize', onWindowResize);
}

function buildSection3DScene(view) {
    themeObjects.forEach(obj => scene.remove(obj));
    themeObjects = [];

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.8);
    scene.add(ambientLight);

    if (view === 'hospitals' || view === 'hospital-profile') {
        const strandGroup = new THREE.Group();
        const count = 30;
        for (let i = 0; i < count; i++) {
            const y = (i - count / 2) * 1.2;
            const angle = i * 0.3;

            const geoA = new THREE.SphereGeometry(0.6, 16, 16);
            const matA = new THREE.MeshBasicMaterial({ color: 0x0D9488 });
            const sphereA = new THREE.Mesh(geoA, matA);
            sphereA.position.set(Math.sin(angle) * 8, y, Math.cos(angle) * 8);
            strandGroup.add(sphereA);

            const matB = new THREE.MeshBasicMaterial({ color: 0x0284C7 });
            const sphereB = new THREE.Mesh(geoA, matB);
            sphereB.position.set(Math.sin(angle + Math.PI) * 8, y, Math.cos(angle + Math.PI) * 8);
            strandGroup.add(sphereB);
        }
        scene.add(strandGroup);
        themeObjects.push(strandGroup);

    } else if (view === 'doctors') {
        for (let i = 1; i <= 3; i++) {
            const ringGeo = new THREE.TorusGeometry(i * 6, 0.2, 16, 100);
            const ringMat = new THREE.MeshBasicMaterial({
                color: i % 2 === 0 ? 0x10B981 : 0x06B6D4,
                wireframe: true
            });
            const ring = new THREE.Mesh(ringGeo, ringMat);
            ring.rotation.x = Math.PI / 3 * i;
            scene.add(ring);
            themeObjects.push(ring);
        }

    } else if (view === 'emergency') {
        const light = new THREE.PointLight(0xEF4444, 2, 100);
        light.position.set(10, 10, 10);
        scene.add(light);

        for (let i = 0; i < 15; i++) {
            const geo = new THREE.SphereGeometry(Math.random() * 2 + 1.5, 32, 32);
            const mat = new THREE.MeshStandardMaterial({
                color: 0xDC2626,
                roughness: 0.2,
                metalness: 0.3
            });
            const sphere = new THREE.Mesh(geo, mat);
            sphere.position.set((Math.random() - 0.5) * 50, (Math.random() - 0.5) * 30, (Math.random() - 0.5) * 20);
            scene.add(sphere);
            themeObjects.push(sphere);
        }

    } else if (view === 'pharmacy' || view === 'laboratory') {
        const molGroup = new THREE.Group();
        const atomGeo = new THREE.SphereGeometry(1.5, 16, 16);
        const atomMat = new THREE.MeshStandardMaterial({ color: 0x3B82F6, metalness: 0.5 });

        for (let x = -10; x <= 10; x += 10) {
            for (let y = -10; y <= 10; y += 10) {
                const atom = new THREE.Mesh(atomGeo, atomMat);
                atom.position.set(x, y, 0);
                molGroup.add(atom);
            }
        }
        scene.add(molGroup);
        themeObjects.push(molGroup);

    } else {
        for (let i = 0; i < 12; i++) {
            let geo;
            if (i % 3 === 0) geo = new THREE.BoxGeometry(3, 3, 3);
            else if (i % 3 === 1) geo = new THREE.OctahedronGeometry(3);
            else geo = new THREE.TorusGeometry(2, 0.8, 12, 24);

            const mat = new THREE.MeshStandardMaterial({ color: 0x2563EB, wireframe: true });
            const mesh = new THREE.Mesh(geo, mat);
            mesh.position.set((Math.random() - 0.5) * 60, (Math.random() - 0.5) * 40, (Math.random() - 0.5) * 30);
            scene.add(mesh);
            themeObjects.push(mesh);
        }
    }
}

function animate3D() {
    animationFrameId = requestAnimationFrame(animate3D);
    themeObjects.forEach((obj, idx) => {
        obj.rotation.x += 0.003 * (idx + 1);
        obj.rotation.y += 0.004 * (idx + 1);
    });
    renderer.render(scene, camera);
}

function onWindowResize() {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
}

function switchTheme(themeName) {
    currentTheme = themeName;
    document.body.className = themeName;
    document.documentElement.setAttribute('data-theme', themeName);
    buildSection3DScene(currentView);
    showToast(`Visual Theme set to ${themeName.replace('theme-', '').toUpperCase()}`, 'info');
}

function setup3DTiltEffect() {
    document.addEventListener('mousemove', (e) => {
        const cards = document.querySelectorAll('.card-3d');
        const mouseX = e.clientX / window.innerWidth - 0.5;
        const mouseY = e.clientY / window.innerHeight - 0.5;

        cards.forEach(card => {
            const rotX = mouseY * -12;
            const rotY = mouseX * 12;
            card.style.transform = `perspective(1000px) rotateX(${rotX}deg) rotateY(${rotY}deg) translateY(-4px)`;
        });
    });
}

function toggleNavSubmenu(el) {
    const parent = el.closest('.nav-group');
    if (parent) {
        parent.classList.toggle('open');
        const sub = parent.querySelector('.nav-submenu');
        if (sub) sub.classList.toggle('hidden');
    }
}

function switchView(viewName) {
    currentView = viewName;

    document.querySelectorAll('.app-view').forEach(el => el.classList.add('hidden'));
    const target = document.getElementById(`view-${viewName}`);
    if (target) target.classList.remove('hidden');

    document.querySelectorAll('.sidebar-nav li').forEach(li => li.classList.remove('active'));
    const activeNav = document.querySelector(`.sidebar-nav li[data-view="${viewName}"]`);
    if (activeNav) activeNav.classList.add('active');

    const titleEl = document.getElementById('current-page-title');
    const breadEl = document.getElementById('current-page-breadcrumb');
    const formattedTitle = viewName.replace('-', ' ').replace(/\b\w/g, c => c.toUpperCase());
    if (titleEl) titleEl.textContent = formattedTitle;
    if (breadEl) breadEl.textContent = `Home / ${formattedTitle}`;

    buildSection3DScene(viewName);
    loadViewData(viewName);
}

function loadViewData(viewName) {
    if (viewName === 'dashboard') loadDashboardStats();
    else if (viewName === 'hospitals') loadHospitalsDirectory();
    else if (viewName === 'doctors') loadDoctorsDirectory();
    else if (viewName === 'appointments') loadAppointmentsTable();
    else if (viewName === 'book-appointment') initBookingFormDropdowns();
    else if (viewName === 'emergency') loadEmergencyHub();
    else if (viewName === 'pharmacy') loadPharmacyCatalog();
    else if (viewName === 'patients') loadPatientsDirectory();
    else if (viewName === 'departments') loadDepartmentsGrid();
    else if (viewName === 'laboratory') loadLaboratoryTable();
    else if (viewName === 'billing') loadBillingTable();
    else if (viewName === 'insurance') loadInsuranceTable();
    else if (viewName === 'staff') loadStaffTable();
    else if (viewName === 'audit-logs') loadAuditLogsTable();
    else if (viewName === 'superadmin-dashboard') loadDashboardStats();
}

async function loadEmergencyHub() {
    try {
        const res = await fetch('/api/emergency');
        if (!res) return;
        const list = await res.json();
        const grid = document.getElementById('emergency-ambulances-grid');
        if (!grid) return;
        grid.innerHTML = list.map(a => `
            <div class="card card-3d p-md">
                <div class="flex-between">
                    <span class="badge badge-danger"><i class="fa-solid fa-truck-medical"></i> ${a.status || 'AVAILABLE'}</span>
                    <strong class="text-accent">${a.vehicleNo || 'AMB-108'}</strong>
                </div>
                <h4 class="m-top-sm">${a.driverName || 'Emergency Responder'}</h4>
                <p class="text-muted" style="font-size:0.85rem;"><i class="fa-solid fa-phone text-primary"></i> ${a.driverPhone || '1066'}</p>
                <p class="text-muted" style="font-size:0.8rem;"><i class="fa-solid fa-location-dot text-danger"></i> Base: ${a.currentLocation || 'City Center'}</p>
            </div>
        `).join('');
    } catch (e) { console.error(e); }
}

async function loadPharmacyCatalog() {
    try {
        const res = await fetch('/api/pharmacy');
        if (!res) return;
        const list = await res.json();
        const tbl = document.getElementById('tbl-medicines-list');
        if (!tbl) return;
        tbl.innerHTML = list.map(m => `
            <tr>
                <td><strong>${m.code || 'MED-001'}</strong></td>
                <td><strong>${m.name}</strong></td>
                <td><span class="badge badge-info">${m.category || 'General'}</span></td>
                <td>${m.manufacturer || 'Aura Pharma'}</td>
                <td>₹${(m.price || 120).toFixed(2)}</td>
                <td><span class="badge badge-success">In Stock</span></td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}

async function loadPatientsDirectory(search = '') {
    try {
        const res = await fetch(`/api/patients?search=${encodeURIComponent(search)}`);
        const list = await res.json();
        const tbl = document.getElementById('tbl-patients-list');
        if (!tbl) return;
        tbl.innerHTML = list.map(p => `
            <tr>
                <td><strong>${p.patientCode}</strong></td>
                <td><strong>${p.name}</strong></td>
                <td>${p.gender}</td>
                <td>${p.age || 30}</td>
                <td><span class="badge badge-danger">${p.bloodGroup || 'O+'}</span></td>
                <td>${p.phone}</td>
                <td>${p.email}</td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}

async function loadDepartmentsGrid() {
    try {
        const res = await fetch('/api/departments');
        const list = await res.json();
        const grid = document.getElementById('departments-grid');
        if (!grid) return;
        grid.innerHTML = list.map(d => `
            <div class="card card-3d p-md">
                <h3><i class="fa-solid fa-sitemap text-primary"></i> ${d.name}</h3>
                <p class="text-muted m-top-sm" style="font-size:0.85rem;">${d.description}</p>
                <span class="badge badge-info m-top-md">Dept Code: ${d.deptCode}</span>
            </div>
        `).join('');
    } catch (e) { console.error(e); }
}

async function loadLaboratoryTable() {
    try {
        const res = await fetch('/api/lab');
        const list = await res.json();
        const tbl = document.getElementById('tbl-lab-tests-list');
        if (!tbl) return;
        tbl.innerHTML = list.map(l => `
            <tr>
                <td><strong>${l.testCode}</strong></td>
                <td><strong>${l.testName}</strong></td>
                <td><span class="badge badge-info">${l.category}</span></td>
                <td>${l.sampleType}</td>
                <td>₹${(l.price || 450).toFixed(2)}</td>
                <td><span class="badge ${l.status === 'Completed' ? 'badge-success' : 'badge-warning'}">${l.status}</span></td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}

async function loadBillingTable() {
    try {
        const res = await fetch('/api/billing');
        const list = await res.json();
        const tbl = document.getElementById('tbl-bills-list');
        if (!tbl) return;
        tbl.innerHTML = list.map(b => `
            <tr>
                <td><strong>${b.invoiceCode}</strong></td>
                <td>₹${b.totalAmount.toFixed(2)}</td>
                <td>₹${b.paidAmount.toFixed(2)}</td>
                <td><span class="badge ${b.paymentStatus === 'Paid' ? 'badge-success' : 'badge-warning'}">${b.paymentStatus}</span></td>
                <td>${b.invoiceDate}</td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}

async function loadInsuranceTable() {
    try {
        const res = await fetch('/api/insurance');
        const list = await res.json();
        const tbl = document.getElementById('tbl-insurance-list');
        if (!tbl) return;
        tbl.innerHTML = list.map(c => `
            <tr>
                <td><strong>${c.claimCode}</strong></td>
                <td>${c.patientName}</td>
                <td>${c.hospitalName}</td>
                <td>${c.providerName}</td>
                <td><strong>₹${c.claimAmount.toFixed(2)}</strong></td>
                <td><span class="badge ${c.status === 'Approved' ? 'badge-success' : 'badge-warning'}">${c.status}</span></td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}

async function loadStaffTable() {
    try {
        const res = await fetch('/api/staff');
        const list = await res.json();
        const tbl = document.getElementById('tbl-staff-list');
        if (!tbl) return;
        tbl.innerHTML = list.map(s => `
            <tr>
                <td><strong>${s.staffCode}</strong></td>
                <td><strong>${s.name}</strong></td>
                <td><span class="badge badge-info">${s.staffRole}</span></td>
                <td>${s.designation}</td>
                <td>${s.hospitalName}</td>
                <td>₹${s.salary.toFixed(2)}</td>
                <td><span class="badge badge-success">${s.status}</span></td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}

async function loadAuditLogsTable() {
    try {
        const res = await fetch('/api/audit');
        const list = await res.json();
        const tbl = document.getElementById('tbl-audit-logs');
        if (!tbl) return;
        tbl.innerHTML = list.map(l => `
            <tr>
                <td><strong>#${l.id}</strong></td>
                <td><strong>${l.username || 'SYSTEM'}</strong></td>
                <td><span class="badge badge-info">${l.module}</span></td>
                <td><strong>${l.action}</strong></td>
                <td>${l.details}</td>
                <td><small class="text-muted">${l.timestamp}</small></td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}

function openModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.remove('hidden');
}

function closeModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.add('hidden');
}

const activeToastMessages = new Set();
let healthCheckInterval = null;

async function checkBackendHealth() {
    try {
        const res = await fetch('/api/health');
        if (res.status === 200) {
            const data = await res.json();
            updateBackendStatusIndicator(true, data.database === 'CONNECTED');
            return true;
        } else {
            updateBackendStatusIndicator(false, false);
            return false;
        }
    } catch (e) {
        updateBackendStatusIndicator(false, false);
        return false;
    }
}

function updateBackendStatusIndicator(backendOk, dbOk) {
    const statusElem = document.getElementById('backend-status-indicator');
    if (!statusElem) return;

    if (backendOk && dbOk) {
        statusElem.className = 'badge badge-success flex-align-center gap-xs cursor-pointer';
        statusElem.innerHTML = `<i class="fa-solid fa-circle-check"></i> System Online`;
        statusElem.title = 'Backend and MySQL database connected';
    } else if (backendOk) {
        statusElem.className = 'badge badge-warning flex-align-center gap-xs cursor-pointer';
        statusElem.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> DB Degraded`;
        statusElem.title = 'Java backend running, but MySQL connection failed';
    } else {
        statusElem.className = 'badge badge-danger flex-align-center gap-xs cursor-pointer';
        statusElem.innerHTML = `<i class="fa-solid fa-plug-circle-xmark"></i> Server Offline (Click to Retry)`;
        statusElem.title = 'Java backend server is unreachable. Click to retry connection.';
    }
}

async function apiFetch(url, options = {}) {
    try {
        const res = await fetch(url, options);
        if (res.status === 401) {
            showToast('Session expired. Please sign in again.', 'warning');
            if (typeof showAuthScreen === 'function') showAuthScreen();
            return null;
        }
        if (res.status === 403) {
            showToast('Access denied for this request.', 'danger');
            return null;
        }
        if (res.status >= 500) {
            showToast('Internal server error (HTTP ' + res.status + '). Check server log.', 'danger');
            return null;
        }
        updateBackendStatusIndicator(true, true);
        return res;
    } catch (err) {
        showToast('Backend server connection failure. Verify Java backend is running.', 'danger');
        updateBackendStatusIndicator(false, false);
        return null;
    }
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    // Toast Deduplication: Prevent duplicate toasts with the exact same message
    const toastKey = `${type}:${message}`;
    if (activeToastMessages.has(toastKey)) return;

    activeToastMessages.add(toastKey);

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    let icon = type === 'success' ? 'fa-circle-check' : (type === 'danger' ? 'fa-circle-exclamation' : 'fa-info-circle');
    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.remove();
        activeToastMessages.delete(toastKey);
    }, 4000);
}

function handleGlobalSearch(e) {
    if (e.key === 'Enter') {
        const query = e.target.value.trim();
        if (query) {
            showToast(`Searching for "${query}" across platform...`, 'info');
            switchView('hospitals');
            const hInput = document.getElementById('hosp-search-name');
            if (hInput) {
                hInput.value = query;
                loadHospitalsDirectory();
            }
        }
    }
}

function togglePasswordVisibility(inputId, btn) {
    const input = document.getElementById(inputId);
    if (input) {
        input.type = input.type === 'password' ? 'text' : 'password';
        btn.innerHTML = `<i class="fa-solid fa-${input.type === 'password' ? 'eye' : 'eye-slash'}"></i>`;
    }
}

function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    if (sidebar) sidebar.classList.toggle('collapsed');
}
