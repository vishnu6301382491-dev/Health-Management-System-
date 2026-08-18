/* ==========================================
   DOCTORS DIRECTORY MODULE (Server-Side Paginated)
   ========================================== */

let currentDoctorPage = 1;
let currentDoctorPageSize = 20;
let doctorSearchDebounceTimer = null;

async function loadDoctorsDirectory(page = 1) {
    currentDoctorPage = page;

    const search = document.getElementById('doc-search-name')?.value || '';
    const spec = document.getElementById('doc-search-spec')?.value || '';
    const city = document.getElementById('doc-filter-city')?.value || '';
    const minFee = document.getElementById('doc-filter-min-fee')?.value || '';
    const maxFee = document.getElementById('doc-filter-max-fee')?.value || '';
    const minRating = document.getElementById('doc-filter-min-rating')?.value || '';
    const minExp = document.getElementById('doc-filter-min-exp')?.value || '';
    const gender = document.getElementById('doc-filter-gender')?.value || '';
    const sortBy = document.getElementById('doc-sort-by')?.value || 'rating_desc';

    let params = new URLSearchParams({
        page: currentDoctorPage,
        pageSize: currentDoctorPageSize,
        search: search,
        specialization: spec,
        city: city,
        minFee: minFee,
        maxFee: maxFee,
        minRating: minRating,
        minExp: minExp,
        gender: gender,
        sortBy: sortBy
    });

    try {
        const res = await fetch(`/api/doctors?${params.toString()}`);
        const result = await res.json();

        if (result.data) {
            renderDoctorsGrid(result.data, result.totalRecords, result.page, result.pageSize, result.totalPages);
        } else if (Array.isArray(result)) {
            renderDoctorsGrid(result, result.length, 1, result.length, 1);
        }
    } catch (e) {
        console.error('Error loading paginated doctors:', e);
    }
}

function debouncedDoctorSearch() {
    clearTimeout(doctorSearchDebounceTimer);
    doctorSearchDebounceTimer = setTimeout(() => {
        loadDoctorsDirectory(1);
    }, 300);
}

function changeDoctorPageSize(size) {
    currentDoctorPageSize = parseInt(size);
    loadDoctorsDirectory(1);
}

function renderDoctorsGrid(list, totalRecords, page, pageSize, totalPages) {
    const grid = document.getElementById('doctors-cards-grid');
    const paginationBar = document.getElementById('doctor-pagination-container');
    if (!grid) return;

    if (list.length === 0) {
        grid.innerHTML = `<div class="col-12 text-center p-lg"><p class="text-muted">No doctors found matching criteria.</p></div>`;
        if (paginationBar) paginationBar.innerHTML = '';
        return;
    }

    grid.innerHTML = list.map(d => `
        <div class="card card-3d doc-card p-md">
            <div class="flex-between align-start gap-md">
                <div style="width: 70px; height: 70px; border-radius: 50%; overflow: hidden; background: var(--bg-surface); border: 2px solid var(--accent-primary); flex-shrink: 0; position: relative;">
                    <img src="${d.imageUrl || 'https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=400&q=80'}" alt="${d.name}" style="width: 100%; height: 100%; object-fit: cover;">
                </div>
                <div style="flex: 1;">
                    <div class="flex-between">
                        <span class="badge badge-info">${d.specialization}</span>
                        <span class="badge badge-warning" style="font-size: 0.7rem; background: rgba(245, 158, 11, 0.2); color: #f59e0b; border: 1px solid #f59e0b;"><i class="fa-solid fa-flask"></i> DEMO DATA</span>
                    </div>
                    <h3 class="m-top-xs" style="font-size: 1.1rem;">${d.name.startsWith('Dr.') ? '' : 'Dr. '}${d.name}</h3>
                    <p class="text-muted" style="font-size: 0.8rem;">${d.qualification} • ${d.experienceYears} Yrs Exp.</p>
                </div>
            </div>

            <p class="text-muted m-top-sm" style="font-size: 0.85rem;"><i class="fa-solid fa-hospital text-primary"></i> ${d.hospitalName || 'Apollo Health City'} (${d.city || 'Hyderabad'})</p>
            <p class="text-muted m-top-xs" style="font-size: 0.8rem;">${(d.bio || '').substring(0, 90)}...</p>

            <div class="flex-between m-top-md pt-sm" style="border-top: 1px solid var(--border-color);">
                <div>
                    <small class="text-muted">Consultation Fee</small>
                    <h4 class="text-success">₹${d.consultationFee}</h4>
                </div>
                <div class="flex-center gap-xs">
                    <button class="btn btn-xs btn-outline" onclick="openFeeBreakdownModal(${d.id}, '${d.name}', '${d.specialization}', ${d.consultationFee})">
                        <i class="fa-solid fa-calculator"></i> Fees
                    </button>
                    <button class="btn btn-xs btn-primary" onclick="switchView('book-appointment')">
                        <i class="fa-solid fa-calendar-check"></i> Book
                    </button>
                </div>
            </div>
        </div>
    `).join('');

    if (paginationBar) {
        let startItem = (page - 1) * pageSize + 1;
        let endItem = Math.min(page * pageSize, totalRecords);

        paginationBar.innerHTML = `
            <div class="flex-between align-center p-sm m-top-md" style="background: var(--bg-surface); border-radius: 8px; border: 1px solid var(--border-color);">
                <span class="text-muted" style="font-size: 0.85rem;">
                    Showing <strong>${startItem.toLocaleString()}–${endItem.toLocaleString()}</strong> of <strong>${totalRecords.toLocaleString()}</strong> doctors
                </span>
                
                <div class="flex-center gap-xs">
                    <button class="btn btn-xs btn-outline" ${page <= 1 ? 'disabled' : ''} onclick="loadDoctorsDirectory(${page - 1})">
                        <i class="fa-solid fa-chevron-left"></i> Prev
                    </button>
                    
                    <span class="badge badge-primary">Page ${page} of ${totalPages.toLocaleString()}</span>
                    
                    <button class="btn btn-xs btn-outline" ${page >= totalPages ? 'disabled' : ''} onclick="loadDoctorsDirectory(${page + 1})">
                        Next <i class="fa-solid fa-chevron-right"></i>
                    </button>

                    <select class="form-control-sm ml-sm" onchange="changeDoctorPageSize(this.value)">
                        <option value="20" ${pageSize === 20 ? 'selected' : ''}>20 / page</option>
                        <option value="50" ${pageSize === 50 ? 'selected' : ''}>50 / page</option>
                        <option value="100" ${pageSize === 100 ? 'selected' : ''}>100 / page</option>
                    </select>
                </div>
            </div>
        `;
    }
}

async function openFeeBreakdownModal(docId, docName, spec, baseFee) {
    const modal = document.getElementById('modal-fee-breakdown');
    if (!modal) return;

    try {
        const res = await fetch(`/api/appointments/calculate-fee?doctorId=${docId}&appointmentType=In-Person`);
        const feeData = await res.json();

        document.getElementById('fee-modal-doc-name').innerText = docName;
        document.getElementById('fee-modal-spec').innerText = spec;
        document.getElementById('fee-modal-base').innerText = `₹${feeData.baseFee}`;
        document.getElementById('fee-modal-followup').innerText = `₹${feeData.followupFee}`;
        document.getElementById('fee-modal-video').innerText = `₹${feeData.videoFee}`;
        document.getElementById('fee-modal-emergency').innerText = `₹${feeData.emergencyFee}`;
        document.getElementById('fee-modal-tax').innerText = `₹${feeData.taxAmount}`;
        document.getElementById('fee-modal-total').innerText = `₹${feeData.totalAmount}`;

        modal.style.display = 'flex';
    } catch (e) {
        console.error(e);
    }
}

function closeFeeBreakdownModal() {
    const modal = document.getElementById('modal-fee-breakdown');
    if (modal) modal.style.display = 'none';
}
