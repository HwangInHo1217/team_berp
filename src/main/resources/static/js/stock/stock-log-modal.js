const StockLogModal = {
  open() {
    const stockId = StockModal.currentStockId;
    if (!stockId) return;

    fetch(`/api/stocks/${stockId}/logs`)
      .then(res => res.json())
      .then(logs => {
        const tbody = document.getElementById('logTableBody');
        tbody.innerHTML = '';

        logs.forEach(log => {
          const tr = document.createElement('tr');
          tr.innerHTML = `
            <td>${StockUtils.formatDateTime(log.logDatetime)}</td>
            <td>${log.logType}</td>
            <td>${log.quantity}</td>
            <td>${log.comment || '-'}</td>
          `;
          tbody.appendChild(tr);
        });

        new bootstrap.Modal(document.getElementById('stockLogModal')).show();
      })
      .catch(e => alert('이력 조회 실패'));
  }
};
