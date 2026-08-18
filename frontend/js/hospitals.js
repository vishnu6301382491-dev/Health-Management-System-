/* ==========================================
   HOSPITALS DIRECTORY MODULE
   ========================================== */

let currentHospitalsList = [];

async function loadHospitalsDirectory() {
    const search = document.getElementById('hosp-search-name')?.value || '';
    const city = document.getElementById('hosp-search-city')?.value || '';
    const type = document.getElementById('hosp-search-type')?.value || '';

    let url = `/api/hospitals?search=${encodeURIComponent(search)}&city=${encodeURIComponent(city)}&type=${encodeURIComponent(type)}`;

    try {
        const res = await fetch(url);
        currentHospitalsList = await res.json();
        renderHospitalsGrid(currentHospitalsList);
    } catch (e) {
        console.error('Error loading hospitals:', e);
    }
}

function renderHospitalsGrid(list) {
    const grid = document.getElementById('hospitals-cards-grid');
    if (!grid) return;

    if (list.length === 0) {
        grid.innerHTML = `<div class="col-12 text-center p-lg"><p class="text-muted">No hospitals found matching criteria.</p></div>`;
        return;
    }

    grid.innerHTML = list.map(h => `
        <div class="card card-3d hosp-card">
            <div class="hosp-banner-img" style="background-image: url('${h.imageUrl || 'https://images.unsplash.com/photo-1587351021759-3e566b6af7cc?w=800&q=80'}'); height: 160px; background-size: cover; background-position: center; border-radius: 14px 14px 0 0; position: relative;">
                <span class="badge badge-primary" style="position: absolute; top: 12px; right: 12px;"><i class="fa-solid fa-star"></i> ${h.rating || 4.8} (${h.reviewCount || 100})</span>
            </div>
            <div class="p-md">
                <div class="flex-between">
                    <span class="badge badge-info">${h.type}</span>
                    <small class="text-muted"><i class="fa-solid fa-bed text-primary"></i> ${h.totalBeds} Beds</small>
                </div>
                <h3 class="m-top-sm" style="font-size: 1.25rem;">${h.name}</h3>
                <p class="text-muted m-top-xs" style="font-size: 0.85rem;"><i class="fa-solid fa-location-dot text-danger"></i> ${h.address}, ${h.city}</p>
                <p class="text-muted m-top-xs" style="font-size: 0.8rem;">${(h.description || '').substring(0, 85)}...</p>

                <div class="flex-between m-top-md pt-sm" style="border-top: 1px solid var(--border-color);">
                    <a href="${h.googleMapsUrl || ('https://maps.google.com/?q=' + encodeURIComponent(h.name + ' ' + h.city))}" target="_blank" class="btn btn-xs btn-outline-primary">
                        <i class="fa-solid fa-map-location-dot"></i> Get Directions
                    </a>
                    <button class="btn btn-xs btn-primary" onclick="viewHospitalProfile(${h.id})">
                        View Hospital <i class="fa-solid fa-arrow-right"></i>
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

async function viewHospitalProfile(hospitalId) {
    try {
        const res = await fetch(`/api/hospitals`);
        const list = await res.json();
        const hosp = list.find(h => h.id === hospitalId);

        if (!hosp) return;

        switchView('hospital-profile');
        const container = document.getElementById('hospital-profile-container');
        if (!container) return;

        container.innerHTML = `
            <div class="card card-3d p-lg">
                <div class="flex-between flex-wrap gap-md">
                    <div>
                        <h2>${hosp.name}</h2>
                        <p class="text-muted m-top-xs"><i class="fa-solid fa-location-dot text-danger"></i> ${hosp.address}, ${hosp.city}, ${hosp.state} - ${hosp.pincode}</p>
                        <span class="badge badge-primary m-top-sm"><i class="fa-solid fa-star"></i> ${hosp.rating} Rating (${hosp.reviewCount} Reviews)</span>
                        <span class="badge badge-info m-top-sm">${hosp.type}</span>
                    </div>
                    <div class="flex-between gap-sm">
                        <a href="${hosp.googleMapsUrl}" target="_blank" class="btn btn-secondary"><i class="fa-solid fa-map-location-dot text-danger"></i> Google Maps Navigation</a>
                        <button class="btn btn-primary" onclick="switchView('book-appointment')"><i class="fa-solid fa-calendar-check"></i> Book Doctor</button>
                    </div>
                </div>

                <div class="m-top-lg" style="border-radius: 16px; overflow: hidden; height: 260px; background-image: url('${hosp.imageUrl}'); background-size: cover; background-position: center;"></div>

                <div class="m-top-lg grid-4 gap-md text-center">
                    <div class="card p-md">
                        <h4>Total Beds</h4>
                        <h2 class="text-primary m-top-xs">${hosp.totalBeds}</h2>
                    </div>
                    <div class="card p-md">
                        <h4>ICU Beds</h4>
                        <h2 class="text-danger m-top-xs">${hosp.icuBeds}</h2>
                    </div>
                    <div class="card p-md">
                        <h4>Emergency Phone</h4>
                        <h3 class="text-amber m-top-xs">${hosp.emergencyPhone}</h3>
                    </div>
                    <div class="card p-md">
                        <h4>Established</h4>
                        <h2 class="text-info m-top-xs">${hosp.establishedYear}</h2>
                    </div>
                </div>

                <div class="m-top-lg">
                    <h3>About Hospital</h3>
                    <p class="text-muted m-top-sm">${hosp.description}</p>
                </div>

                <div class="m-top-lg">
                    <h3>Location & Directions</h3>
                    <p class="text-muted m-top-xs">Address: ${hosp.address}, ${hosp.city}, ${hosp.state} - ${hosp.pincode}</p>
                    <div class="m-top-md" style="width: 100%; height: 240px; border-radius: 14px; background: rgba(0,0,0,0.2); border: 1px solid var(--border-color); display: flex; align-items: center; justify-content: center;">
                        <a href="${hosp.googleMapsUrl}" target="_blank" class="btn btn-lg btn-outline-primary">
                            <i class="fa-solid fa-map-marked-alt"></i> Open Google Maps Satellite Navigation
                        </a>
                    </div>
                </div>
            </div>
        `;
    } catch (e) { console.error(e); }
}

function openAddHospitalModal() {
    openModal('modal-add-hospital');
}

function closeAddHospitalModal() {
    closeModal('modal-add-hospital');
}

async function handleAddHospitalSubmit(e) {
    e.preventDefault();
    const name = document.getElementById('add-hosp-name').value.trim();
    const city = document.getElementById('add-hosp-city').value.trim();
    const state = document.getElementById('add-hosp-state').value.trim();
    const type = document.getElementById('add-hosp-type').value;
    const totalBeds = parseInt(document.getElementById('add-hosp-beds').value) || 100;
    const phone = document.getElementById('add-hosp-phone').value.trim();

    try {
        const res = await fetch('/api/hospitals', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, city, state, type, totalBeds, phone, status: 'ACTIVE' })
        });
        const data = await res.json();
        if (data.success) {
            showToast('Hospital registered successfully!', 'success');
            closeAddHospitalModal();
            loadHospitalsDirectory();
        } else {
            showToast('Failed to register hospital.', 'danger');
        }
    } catch (err) {
        showToast('Connection error while adding hospital.', 'danger');
    }
}

function trigger247Emergency() {
    showToast('🚨 24/7 Emergency Dispatch active! Call 1066 for instant ambulance.', 'danger');
    switchView('emergency');
}
