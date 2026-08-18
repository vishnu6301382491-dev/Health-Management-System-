/* ==========================================
   AURA HEALTH 3D - MAIN PLATFORM ENGINE
   ========================================== */

/* Configurable Production API URL (e.g. window.AURA_CONFIG = { API_BASE_URL: 'https://your-java-backend.up.railway.app' }) */
const API_BASE_URL = (window.AURA_CONFIG && window.AURA_CONFIG.API_BASE_URL)
    ? window.AURA_CONFIG.API_BASE_URL
    : (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1' ? 'http://localhost:8080' : '');

let isDemoMode = false;
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
   3D THREE.JS CANVAS BACKGROUND ENGINE
---------------------------------------------------- */
function init3DEngine() {
    const container = document.getElementById('canvas-container');
    if (!container || performanceMode === 'PERFORMANCE') return;

    scene = new THREE.Scene();
    camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 1000);
    camera.position.z = 30;

    renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    container.appendChild(renderer.domElement);

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.8);
    scene.add(ambientLight);

    const dirLight = new THREE.DirectionalLight(0x0D9488, 1.5);
    dirLight.position.set(20, 20, 20);
    scene.add(dirLight);

    buildSection3DScene('dashboard');
    animate3D();

    window.addEventListener('resize', onWindowResize);
}

function buildSection3DScene(viewName) {
    if (!scene || performanceMode === 'PERFORMANCE') return;

    themeObjects.forEach(obj => scene.remove(obj));
    themeObjects = [];

    const geom = new THREE.IcosahedronGeometry(8, 2);
    const mat = new THREE.MeshPhongMaterial({
        color: 0x0D9488,
        wireframe: true,
        transparent: true,
        opacity: 0.35
    });
    const mesh = new THREE.Mesh(geom, mat);
    scene.add(mesh);
    themeObjects.push(mesh);

    // Particle field
    const particlesCount = 200;
    const posArray = new Float32Array(particlesCount * 3);
    for (let i = 0; i < particlesCount * 3; i++) {
        posArray[i] = (Math.random() - 0.5) * 80;
    }
    const partGeom = new THREE.BufferGeometry();
    partGeom.setAttribute('position', new THREE.BufferAttribute(posArray, 3));
    const partMat = new THREE.PointsMaterial({
        size: 0.4,
        color: 0x0284C7,
        transparent: true,
        opacity: 0.6
    });
    const particles = new THREE.Points(partGeom, partMat);
    scene.add(particles);
    themeObjects.push(particles);
}

function animate3D() {
    animationFrameId = requestAnimationFrame(animate3D);

    themeObjects.forEach((obj, idx) => {
        if (obj.isMesh) {
            obj.rotation.x += 0.003;
            obj.rotation.y += 0.005;
        } else if (obj.isPoints) {
            obj.rotation.y -= 0.001;
        }
    });

    if (renderer && scene && camera) {
        renderer.render(scene, camera);
    }
}

function onWindowResize() {
    if (!camera || !renderer) return;
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
}

function setup3DTiltEffect() {
    document.addEventListener('mousemove', (e) => {
        if (!camera || performanceMode === 'PERFORMANCE') return;
        const mouseX = (e.clientX / window.innerWidth - 0.5) * 2;
        const mouseY = (e.clientY / window.innerHeight - 0.5) * 2;
        camera.position.x += (mouseX * 2 - camera.position.x) * 0.05;
        camera.position.y += (-mouseY * 2 - camera.position.y) * 0.05;
        camera.lookAt(scene.position);
    });
}

/* ----------------------------------------------------
   NAVIGATION & VIEW ROUTING ENGINE
---------------------------------------------------- */
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
    if (viewName === 'dashboard' || viewName === 'admin-dashboard' || viewName === 'superadmin-dashboard') {
        if (typeof loadDashboardStats === 'function') loadDashboardStats();
    }
    else if (viewName === 'hospitals') { if (typeof loadHospitalsDirectory === 'function') loadHospitalsDirectory(); }
    else if (viewName === 'doctors') { if (typeof loadDoctorsDirectory === 'function') loadDoctorsDirectory(); }
    else if (viewName === 'appointments') { if (typeof loadAppointmentsTable === 'function') loadAppointmentsTable(); }
    else if (viewName === 'book-appointment') { if (typeof initBookingFormDropdowns === 'function') initBookingFormDropdowns(); }
    else if (viewName === 'emergency') { if (typeof loadEmergencyHub === 'function') loadEmergencyHub(); }
    else if (viewName === 'pharmacy') { if (typeof loadPharmacyCatalog === 'function') loadPharmacyCatalog(); }
    else if (viewName === 'patients') { if (typeof loadPatientsDirectory === 'function') loadPatientsDirectory(); }
    else if (viewName === 'departments') { if (typeof loadDepartmentsGrid === 'function') loadDepartmentsGrid(); }
    else if (viewName === 'laboratory') { if (typeof loadLaboratoryTable === 'function') loadLaboratoryTable(); }
    else if (viewName === 'billing') { if (typeof loadBillingTable === 'function') loadBillingTable(); }
    else if (viewName === 'insurance') { if (typeof loadInsuranceTable === 'function') loadInsuranceTable(); }
    else if (viewName === 'staff') { if (typeof loadStaffTable === 'function') loadStaffTable(); }
    else if (viewName === 'audit-logs') { if (typeof loadAuditLogsTable === 'function') loadAuditLogsTable(); }
    else if (viewName === 'doctor-dashboard') { if (typeof loadAppointmentsTable === 'function') loadAppointmentsTable(); }
    else if (viewName === 'patient-dashboard') { if (typeof loadAppointmentsTable === 'function') loadAppointmentsTable(); }
}

const activeToastMessages = new Set();

async function checkBackendHealth() {
    try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 2000);
        const res = await fetch(`${API_BASE_URL}/api/health`, { signal: controller.signal });
        clearTimeout(timeoutId);
        if (res && res.status === 200) {
            const data = await res.json();
            isDemoMode = false;
            updateBackendStatusIndicator(true, data.database === 'CONNECTED');
            return true;
        }
    } catch (e) {}

    // On static GitHub Pages deployment, automatically enable DEMO MODE without blocking errors
    isDemoMode = true;
    updateBackendStatusIndicator(true, true);
    return false;
}

function updateBackendStatusIndicator(backendOk, dbOk) {
    const statusElem = document.getElementById('backend-status-indicator');
    if (!statusElem) return;

    if (isDemoMode || window.location.hostname.includes('github.io')) {
        statusElem.className = 'badge badge-warning flex-align-center gap-xs cursor-pointer';
        statusElem.innerHTML = `<i class="fa-solid fa-flask"></i> Standalone Demo Mode`;
        statusElem.title = 'Running in GitHub Pages Demo Mode with simulated live data. No Java backend required!';
    } else if (backendOk && dbOk) {
        statusElem.className = 'badge badge-success flex-align-center gap-xs cursor-pointer';
        statusElem.innerHTML = `<i class="fa-solid fa-circle-check"></i> Java & MySQL Connected`;
        statusElem.title = `Connected to Java backend at ${API_BASE_URL || 'http://localhost:8080'}`;
    } else {
        statusElem.className = 'badge badge-danger flex-align-center gap-xs cursor-pointer';
        statusElem.innerHTML = `<i class="fa-solid fa-plug-circle-xmark"></i> Server Offline`;
    }
}

async function apiFetch(url, options = {}) {
    const targetUrl = url.startsWith('http') ? url : `${API_BASE_URL}${url}`;
    try {
        const res = await fetch(targetUrl, options);
        if (res.status === 401) {
            showToast('Session expired. Please sign in again.', 'warning');
            if (typeof showAuthScreen === 'function') showAuthScreen();
            return null;
        }
        if (res.status === 403) {
            showToast('Access denied for this request.', 'danger');
            return null;
        }
        return res;
    } catch (err) {
        if (!isDemoMode) {
            isDemoMode = true;
            updateBackendStatusIndicator(true, true);
        }
        return null;
    }
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

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
        }
    }
}
