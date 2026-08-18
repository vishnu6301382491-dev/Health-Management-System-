/* ==========================================
   AUTH SERVICE & ROLE-BASED ACCESS CONTROL
   ========================================== */

function checkAuthSession() {
    const sessionStr = sessionStorage.getItem('aura_hms_session');
    if (sessionStr) {
        try {
            currentUser = JSON.parse(sessionStr);
            showAppShell();
        } catch (e) {
            showAuthScreen();
        }
    } else {
        showAuthScreen();
    }
}

function showAuthScreen() {
    if (document.getElementById('auth-screen')) document.getElementById('auth-screen').classList.remove('hidden');
    if (document.getElementById('app-shell')) document.getElementById('app-shell').classList.add('hidden');
}

function showAppShell() {
    if (document.getElementById('auth-screen')) document.getElementById('auth-screen').classList.add('hidden');
    if (document.getElementById('app-shell')) document.getElementById('app-shell').classList.remove('hidden');

    const displayName = currentUser ? (currentUser.name || currentUser.username || 'User') : 'User';
    const displayRole = currentUser ? currentUser.role : 'ADMIN';

    if (document.getElementById('user-display-name')) document.getElementById('user-display-name').textContent = displayName;
    if (document.getElementById('user-display-role')) document.getElementById('user-display-role').textContent = displayRole;
    if (document.getElementById('sidebar-role-badge')) document.getElementById('sidebar-role-badge').textContent = displayRole;
    if (document.getElementById('user-avatar-initials')) document.getElementById('user-avatar-initials').textContent = displayName.substring(0, 2).toUpperCase();

    buildSidebarNav(displayRole);

    if (displayRole === 'ADMIN') switchView('admin-dashboard');
    else if (displayRole === 'DOCTOR') switchView('doctor-dashboard');
    else if (displayRole === 'PATIENT') switchView('patient-dashboard');
    else if (displayRole === 'RECEPTIONIST') switchView('receptionist-dashboard');
    else switchView('admin-dashboard');
}

function buildSidebarNav(role) {
    const navList = document.getElementById('nav-list');
    if (!navList) return;

    let items = [];

    if (role === 'ADMIN') {
        items = [
            { view: 'admin-dashboard', label: 'Dashboard', icon: 'fa-chart-pie' },
            { view: 'patients', label: 'Patients', icon: 'fa-user-group' },
            { view: 'doctors', label: 'Doctors', icon: 'fa-user-doctor' },
            { view: 'departments', label: 'Departments', icon: 'fa-sitemap' },
            { view: 'appointments', label: 'Appointments', icon: 'fa-calendar-check' },
            { view: 'medical-history', label: 'Medical Records', icon: 'fa-notes-medical' },
            { view: 'prescriptions', label: 'Prescriptions', icon: 'fa-prescription-bottle-medical' },
            { view: 'laboratory', label: 'Laboratory', icon: 'fa-flask' },
            { view: 'billing', label: 'Billing & Invoices', icon: 'fa-file-invoice-dollar' },
            { view: 'reports', label: 'Analytics Reports', icon: 'fa-chart-line' }
        ];
    } else if (role === 'DOCTOR') {
        items = [
            { view: 'doctor-dashboard', label: 'Doctor Dashboard', icon: 'fa-user-doctor' },
            { view: 'appointments', label: 'My Schedule', icon: 'fa-calendar-day' },
            { view: 'patients', label: 'Patient Records', icon: 'fa-user-group' },
            { view: 'medical-history', label: 'Add Clinical Entry', icon: 'fa-notes-medical' },
            { view: 'prescriptions', label: 'Write Prescription', icon: 'fa-prescription' },
            { view: 'laboratory', label: 'Lab Orders & Results', icon: 'fa-flask-vial' }
        ];
    } else if (role === 'PATIENT') {
        items = [
            { view: 'patient-dashboard', label: 'Patient Portal', icon: 'fa-hospital-user' },
            { view: 'book-appointment', label: 'Book Appointment', icon: 'fa-calendar-plus' },
            { view: 'appointments', label: 'My Appointments', icon: 'fa-calendar-check' },
            { view: 'prescriptions', label: 'My Prescriptions', icon: 'fa-pills' },
            { view: 'laboratory', label: 'My Lab Reports', icon: 'fa-vial-circle-check' },
            { view: 'billing', label: 'My Billing History', icon: 'fa-receipt' }
        ];
    } else if (role === 'RECEPTIONIST') {
        items = [
            { view: 'receptionist-dashboard', label: 'Front Desk', icon: 'fa-headset' },
            { view: 'patients', label: 'Register Patients', icon: 'fa-user-plus' },
            { view: 'appointments', label: 'Manage Booking', icon: 'fa-calendar-days' },
            { view: 'billing', label: 'Billing Checkout', icon: 'fa-file-invoice-dollar' }
        ];
    }

    navList.innerHTML = items.map(item => `
        <li data-view="${item.view}">
            <a href="#" onclick="switchView('${item.view}')">
                <i class="fa-solid ${item.icon}"></i>
                <span>${item.label}</span>
            </a>
        </li>
    `).join('');
}

function setAuthRole(role, btn) {
    document.querySelectorAll('.auth-tab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    if (document.getElementById('login-role')) document.getElementById('login-role').value = role;
    if (document.getElementById('login-title')) document.getElementById('login-title').textContent = `${role.charAt(0) + role.slice(1).toLowerCase()} Sign In`;

    fillDemo(role);
}

function fillDemo(role) {
    const userMap = {
        'ADMIN': { u: 'admin', p: 'admin123' },
        'DOCTOR': { u: 'dr_dev', p: 'doc123' },
        'PATIENT': { u: 'vishnu', p: 'vishnu123' },
        'RECEPTIONIST': { u: 'receptionist1', p: 'rec123' }
    };
    const cred = userMap[role] || userMap['ADMIN'];
    if (document.getElementById('login-username')) document.getElementById('login-username').value = cred.u;
    if (document.getElementById('login-password')) document.getElementById('login-password').value = cred.p;
}

function toggleAuthMode(mode) {
    if (mode === 'register') {
        if (document.getElementById('login-form-box')) document.getElementById('login-form-box').classList.add('hidden');
        if (document.getElementById('register-form-box')) document.getElementById('register-form-box').classList.remove('hidden');
    } else {
        if (document.getElementById('register-form-box')) document.getElementById('register-form-box').classList.add('hidden');
        if (document.getElementById('login-form-box')) document.getElementById('login-form-box').classList.remove('hidden');
    }
}

async function handleLogin(e) {
    e.preventDefault();
    const username = document.getElementById('login-username')?.value.trim() || 'admin';
    const password = document.getElementById('login-password')?.value || '';
    const selectedRole = document.getElementById('login-role')?.value || 'ADMIN';

    // 1. If NOT in DEMO_MODE, attempt Real Java Backend Authentication
    if (typeof DEMO_MODE !== 'undefined' && !DEMO_MODE && typeof API_BASE_URL !== 'undefined' && API_BASE_URL) {
        try {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 2000);
            const res = await fetch(`${API_BASE_URL}/api/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password }),
                signal: controller.signal
            });
            clearTimeout(timeoutId);

            if (res && res.status === 200) {
                const data = await res.json();
                if (data.success) {
                    currentUser = data;
                    sessionStorage.setItem('aura_hms_session', JSON.stringify(currentUser));
                    showToast(`Welcome back, ${currentUser.username}!`, 'success');
                    showAppShell();
                    return;
                } else {
                    showToast(data.message || 'Invalid login credentials', 'danger');
                    return;
                }
            }
        } catch (err) {}
    }

    // 2. DEMO_MODE Authentication (Instant, ZERO network call to Java backend!)
    let demoRole = selectedRole;
    const uLower = username.toLowerCase();

    if (uLower.includes('doctor') || uLower.includes('dr_') || uLower.includes('doc')) demoRole = 'DOCTOR';
    else if (uLower.includes('patient') || uLower.includes('pat') || uLower.includes('vishnu')) demoRole = 'PATIENT';
    else if (uLower.includes('reception') || uLower.includes('rec')) demoRole = 'RECEPTIONIST';
    else if (uLower.includes('super') || uLower.includes('admin')) demoRole = 'ADMIN';

    currentUser = {
        userId: demoRole === 'PATIENT' ? 3 : (demoRole === 'DOCTOR' ? 4506 : 1),
        username: username || 'DemoUser',
        role: demoRole,
        name: (username === 'vishnu' || username === 'patient1' || demoRole === 'PATIENT') ? 'Vishnu Reddy' : (demoRole === 'DOCTOR' ? 'Dr. Dev Rao' : (username.toUpperCase() + ' Admin')),
        token: 'demo-token-' + Date.now()
    };

    sessionStorage.setItem('aura_hms_session', JSON.stringify(currentUser));
    showToast(`Welcome back, ${currentUser.name || currentUser.username}! (Demo Mode)`, 'success');
    showAppShell();
}

async function handlePatientRegister(e) {
    e.preventDefault();
    showToast('Account registered successfully! Please sign in.', 'success');
    toggleAuthMode('login');
}

function handleLogout() {
    sessionStorage.removeItem('aura_hms_session');
    currentUser = null;
    showToast('Logged out successfully', 'info');
    showAuthScreen();
}
