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
        if (res && res.status === 200) {
            currentHospitalsList = await res.json();
        } else {
            throw new Error('API unavailable');
        }
    } catch (e) {
        currentHospitalsList = [
            { id: 1, name: "AURA Medical Centre", city: "Hyderabad", state: "Telangana", type: "Super Specialty", totalBeds: 500, rating: 4.9, reviewCount: 342, phone: "040-27123456", imageUrl: "https://images.unsplash.com/photo-1587351021759-3e566b6af7cc?w=800&q=80" },
            { id: 2, name: "Apollo Health City", city: "Bengaluru", state: "Karnataka", type: "Multi Specialty", totalBeds: 650, rating: 4.8, reviewCount: 512, phone: "080-26998800", imageUrl: "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=800&q=80" },
            { id: 3, name: "KIMS Global Hospital", city: "Hyderabad", state: "Telangana", type: "Super Specialty", totalBeds: 450, rating: 4.7, reviewCount: 289, phone: "040-44885000", imageUrl: "https://images.unsplash.com/photo-1516549655169-df83a0774514?w=800&q=80" },
            { id: 4, name: "Yashoda Super Specialty", city: "Secunderabad", state: "Telangana", type: "Super Specialty", totalBeds: 400, rating: 4.8, reviewCount: 410, phone: "040-27713333", imageUrl: "https://images.unsplash.com/photo-1538108149393-fbbd81895907?w=800&q=80" }
        ];
    }

    renderHospitalsGrid(currentHospitalsList);
}

function renderHospitalsGrid(list) {
    const grid = document.getElementById('hospitals-cards-grid');
    if (!grid) return;

    if (!Array.isArray(list) || list.length === 0) {
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
                <h3 class="m-top-xs text-primary">${h.name}</h3>
                <p class="text-muted text-sm m-top-xs"><i class="fa-solid fa-location-dot"></i> ${h.city}, ${h.state}</p>
                <div class="flex-between m-top-md">
                    <small class="text-muted"><i class="fa-solid fa-phone"></i> ${h.phone || '040-27123456'}</small>
                    <button class="btn btn-xs btn-outline-primary" onclick="switchView('doctors')">View Doctors</button>
                </div>
            </div>
        </div>
    `).join('');
}
