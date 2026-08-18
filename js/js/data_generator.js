/* ==========================================
   ADMIN DATASET GENERATOR MODULE
   ========================================== */

let generatorPollTimer = null;

async function startDatasetGeneration() {
    const hospCount = parseInt(document.getElementById('gen-hosp-count')?.value || '1000');
    const docCount = parseInt(document.getElementById('gen-doc-count')?.value || '100000');

    try {
        const res = await fetch('/api/admin/generate-data', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ hospitals: hospCount, doctors: docCount })
        });
        const data = await res.json();
        if (data.success) {
            showToast('Dataset generation task launched in background!', 'info');
            startPollingProgress();
        } else {
            showToast('Failed to start generation task', 'danger');
        }
    } catch (e) {
        showToast('Server communication error', 'danger');
    }
}

function startPollingProgress() {
    clearInterval(generatorPollTimer);
    generatorPollTimer = setInterval(pollGenerationStatus, 1500);
}

async function pollGenerationStatus() {
    try {
        const res = await fetch('/api/admin/generate-data/status');
        const status = await res.json();

        const msgElem = document.getElementById('gen-status-msg');
        const progressHosp = document.getElementById('gen-hosp-progress');
        const progressDoc = document.getElementById('gen-doc-progress');
        const progressBar = document.getElementById('gen-progress-bar');
        const btn = document.getElementById('btn-start-generator');

        if (msgElem) msgElem.innerText = status.statusMessage || 'Idle';
        if (progressHosp) progressHosp.innerText = `${(status.hospitalsGenerated || 0).toLocaleString()} / ${(status.targetHospitals || 1000).toLocaleString()}`;
        if (progressDoc) progressDoc.innerText = `${(status.doctorsGenerated || 0).toLocaleString()} / ${(status.targetDoctors || 100000).toLocaleString()}`;

        if (progressBar && status.targetDoctors > 0) {
            let pct = Math.min(100, Math.round(((status.doctorsGenerated || 0) / status.targetDoctors) * 100));
            progressBar.style.width = pct + '%';
            progressBar.innerText = pct + '%';
        }

        if (btn) {
            btn.disabled = status.isRunning === true;
            btn.innerHTML = status.isRunning ? `<i class="fa-solid fa-spinner fa-spin"></i> Generating Dataset...` : `<i class="fa-solid fa-bolt"></i> Generate Synthetic Dataset`;
        }

        if (!status.isRunning && status.doctorsGenerated > 0) {
            clearInterval(generatorPollTimer);
            showToast('Dataset generation complete!', 'success');
        }
    } catch (e) {
        console.error(e);
    }
}
