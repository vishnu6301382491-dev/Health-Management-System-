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
    document.getElementById('auth-screen').classList.remove('hidden');
    document.getElementById('app-shell').classList.add('hidden');
}

function showAppShell() {
    document.getElementById('auth-screen').classList.add('hidden');
    document.getElementById('app-shell').classList.remove('hidden');

    // Update user profile badges
    document.getElementById('user-display-name').textContent = currentUser.username || 'User';
    document.getElementById('user-display-role').textContent = currentUser.role;
    document.getElementById('sidebar-role-badge').textContent = currentUser.role;
    document.getElementById('user-avatar-initials').textContent = (currentUser.username || 'US').substring(0, 2).toUpperCase();

    // Render Role-Based Sidebar Navigation
    buildSidebarNav(currentUser.role);

    // Initial view based on role
    if (currentUser.role === 'ADMIN') switchView('admin-dashboard');
    else if (currentUser.role === 'DOCTOR') switchView('doctor-dashboard');
    else if (currentUser.role === 'PATIENT') switchView('patient-dashboard');
    else if (currentUser.role === 'RECEPTIONIST') switchView('receptionist-dashboard');
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

    document.getElementById('login-role').value = role;
    document.getElementById('login-title').textContent = `${role.charAt(0) + role.slice(1).toLowerCase()} Sign In`;

    fillDemo(role);
}

function fillDemo(role) {
    const userMap = {
        'ADMIN': { u: 'admin', p: 'admin123' },
        'DOCTOR': { u: 'dr_sharma', p: 'doc123' },
        'PATIENT': { u: 'patient1', p: 'pat123' },
        'RECEPTIONIST': { u: 'receptionist1', p: 'rec123' }
    };
    const cred = userMap[role] || userMap['ADMIN'];
    document.getElementById('login-username').value = cred.u;
    document.getElementById('login-password').value = cred.p;
}

function toggleAuthMode(mode) {
    if (mode === 'register') {
        document.getElementById('login-form-box').classList.add('hidden');
        document.getElementById('register-form-box').classList.remove('hidden');
    } else {
        document.getElementById('register-form-box').classList.add('hidden');
        document.getElementById('login-form-box').classList.remove('hidden');
    }
}

async function handleLogin(e) {
    e.preventDefault();
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;

    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();

        if (data.success) {
            currentUser = data;
            sessionStorage.setItem('aura_hms_session', JSON.stringify(currentUser));
            showToast(`Welcome back, ${currentUser.username}!`, 'success');
            showAppShell();
        } else {
            showToast(data.message || 'Invalid login credentials', 'danger');
        }
    } catch (err) {
        showToast('Server connection failed. Ensure Java backend is running.', 'danger');
    }
}

async function handlePatientRegister(e) {
    e.preventDefault();
    const req = {
        name: document.getElementById('reg-name').value.trim(),
        gender: document.getElementById('reg-gender').value,
        dob: document.getElementById('reg-dob').value,
        age: parseInt(document.getElementById('reg-age').value) || 25,
        bloodGroup: document.getElementById('reg-blood').value,
        phone: document.getElementById('reg-phone').value.trim(),
        email: document.getElementById('reg-email').value.trim(),
        username: document.getElementById('reg-username').value.trim(),
        password: document.getElementById('reg-password').value,
        address: document.getElementById('reg-address').value.trim()
    };

    try {
        const res = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });
        const data = await res.json();

        if (data.success) {
            showToast('Account registered successfully! Please sign in.', 'success');
            toggleAuthMode('login');
        } else {
            showToast(data.message || 'Registration failed', 'danger');
        }
    } catch (err) {
        showToast('Registration failed due to connection error.', 'danger');
    }
}

function handleLogout() {
    sessionStorage.removeItem('aura_hms_session');
    currentUser = null;
    showToast('Logged out successfully', 'info');
    showAuthScreen();
}
