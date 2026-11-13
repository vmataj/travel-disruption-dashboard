const button = document.querySelector('#refresh');
const statusLine = document.querySelector('#feed-status');

button?.addEventListener('click', async () => {
    button.disabled = true;
    statusLine.textContent = 'Checking the status feed…';
    const token = document.querySelector('meta[name="csrf-token"]').content;
    const header = document.querySelector('meta[name="csrf-header"]').content;
    try {
        const scenario = document.querySelector('#scenario').value;
        const response = await fetch('/api/feed/refresh?scenario=' + encodeURIComponent(scenario), {
            method: 'POST', headers: { [header]: token }
        });
        if (!response.ok) throw new Error('Refresh failed');
        location.reload();
    } catch {
        statusLine.textContent = 'Status feed unavailable. Showing the last successful snapshot.';
        button.disabled = false;
    }
});
