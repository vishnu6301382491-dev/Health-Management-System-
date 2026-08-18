/* ==========================================
   PHARMACY CATALOG MODULE
   ========================================== */

async function loadPharmacyCatalog() {
    const search = document.getElementById('pharmacy-search')?.value || '';
    try {
        const res = await fetch(`/api/pharmacy?search=${encodeURIComponent(search)}`);
        const list = await res.json();
        const tbl = document.getElementById('tbl-pharmacy-list');
        if (!tbl) return;

        if (list.length === 0) {
            tbl.innerHTML = `<tr><td colspan="7" class="text-center p-md text-muted">No medicines found in pharmacy catalog.</td></tr>`;
            return;
        }

        tbl.innerHTML = list.map(m => `
            <tr>
                <td><strong>${m.medicineCode}</strong></td>
                <td><strong>${m.name}</strong></td>
                <td><span class="badge badge-info">${m.category}</span></td>
                <td>${m.manufacturer || 'General'}</td>
                <td><strong>₹${m.unitPrice.toFixed(2)}</strong></td>
                <td><span class="badge ${m.stockQuantity > 50 ? 'badge-success' : 'badge-warning'}">${m.stockQuantity} Units</span></td>
                <td>${m.requiresPrescription ? '<span class="badge badge-danger">Rx Required</span>' : '<span class="badge badge-success">OTC</span>'}</td>
            </tr>
        `).join('');
    } catch (e) { console.error(e); }
}
