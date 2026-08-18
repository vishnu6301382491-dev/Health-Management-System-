/* ==========================================
   SUPER ADMIN CSV BATCH DATA IMPORTER MODULE
   ========================================== */

async function handleCsvImportSubmit() {
    const content = document.getElementById('csv-import-textarea')?.value.trim();
    if (!content) {
        showToast('Please paste CSV dataset content to import.', 'danger');
        return;
    }

    try {
        const res = await fetch('/api/import/hospitals', {
            method: 'POST',
            headers: { 'Content-Type': 'text/plain' },
            body: content
        });
        const data = await res.json();

        if (data.success) {
            showToast(`Successfully imported ${data.importedCount} hospital records into MySQL!`, 'success');
            document.getElementById('csv-import-textarea').value = '';
            loadHospitalsDirectory();
        } else {
            showToast('CSV import failed. Check dataset format.', 'danger');
        }
    } catch (err) { showToast('Server communication error during CSV import.', 'danger'); }
}
