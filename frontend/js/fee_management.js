/* ==========================================
   ADMIN BULK FEE MANAGEMENT MODULE
   ========================================== */

async function handleBulkFeeUpdateSubmit(e) {
    e.preventDefault();

    const spec = document.getElementById('fee-bulk-spec')?.value || '';
    const city = document.getElementById('fee-bulk-city')?.value || '';
    const newFee = document.getElementById('fee-bulk-amount')?.value;
    const mult = document.getElementById('fee-bulk-multiplier')?.value;

    if (!newFee && !mult) {
        showToast('Please specify either a New Fee Amount or a Percentage Multiplier.', 'danger');
        return;
    }

    try {
        const res = await fetch('/api/admin/fees/bulk-update', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                specialization: spec,
                city: city,
                fee: newFee ? parseFloat(newFee) : null,
                multiplier: mult ? parseFloat(mult) : null
            })
        });
        const data = await res.json();
        if (data.success) {
            showToast(`Bulk fee update applied to ${data.updated.toLocaleString()} doctor records!`, 'success');
            loadDoctorsDirectory(1);
        } else {
            showToast('Failed to apply bulk fee update.', 'danger');
        }
    } catch (err) {
        showToast('Server communication error', 'danger');
    }
}
