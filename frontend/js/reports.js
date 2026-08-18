/* ==========================================
   REPORTS & ANALYTICS MODULE
   ========================================== */

async function generateReports() {
    const start = document.getElementById('report-start-date')?.value || '2026-01-01';
    const end = document.getElementById('report-end-date')?.value || '2026-12-31';

    try {
        const res = await fetch(`/api/reports?startDate=${start}&endDate=${end}`);
        const data = await res.json();

        document.getElementById('rep-inv-count').textContent = data.invoiceCount || 0;
        document.getElementById('rep-total-billed').textContent = `₹${(data.totalBilled || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
        document.getElementById('rep-total-collected').textContent = `₹${(data.totalCollected || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
        document.getElementById('rep-total-pending').textContent = `₹${(data.totalPending || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;

    } catch (e) {
        console.error('Failed to run reports:', e);
    }
}
