document.addEventListener("DOMContentLoaded", function() {
  fetch("/api/mrp")
    .then(response => response.json())
    .then(data => {
      const tbody = document.getElementById("mrpTableBody");
      tbody.innerHTML = "";
      data.forEach((mrp, idx) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
          <td>${mrp.itemCode || '-'}</td>
          <td>${mrp.itemName || '-'}</td>
          <td>${mrp.itemType || '-'}</td>
          <td>${mrp.unit || '-'}</td>
          <td>${mrp.baseDate || '-'}</td>
          <td>${mrp.requiredQty ?? '-'}</td>
          <td>${mrp.stockQty ?? '-'}</td>
          <td>${mrp.confirmedQty ?? '-'}</td>
          <td>${mrp.shortageQty ?? '-'}</td>
          <td>${mrp.source || '-'}</td>
          <td>${mrp.leadTime ?? '-'}</td>
          <td>${mrp.comment || '-'}</td>
          <td>
            <button type="button" class="btn btn-info btn-sm"
              onclick='showMrpDetailModal(${JSON.stringify(mrp)})'>상세</button>
          </td>
        `;
        tbody.appendChild(tr);
      });
    })
    .catch(error => {
      alert("MRP 데이터 조회 실패: " + error);
    });
});

// 상세 모달 열기 함수
function showMrpDetailModal(mrp) {
  document.getElementById("modalItemCode").textContent = mrp.itemCode || '-';
  document.getElementById("modalItemName").textContent = mrp.itemName || '-';
  document.getElementById("modalItemType").textContent = mrp.itemType || '-';
  document.getElementById("modalUnit").textContent = mrp.unit || '-';
  document.getElementById("modalBaseDate").textContent = mrp.baseDate || '-';
  document.getElementById("modalRequiredQty").textContent = mrp.requiredQty ?? '-';
  document.getElementById("modalStockQty").textContent = mrp.stockQty ?? '-';
  document.getElementById("modalConfirmedQty").textContent = mrp.confirmedQty ?? '-';
  document.getElementById("modalShortageQty").textContent = mrp.shortageQty ?? '-';
  document.getElementById("modalSource").textContent = mrp.source || '-';
  document.getElementById("modalLeadTime").textContent = mrp.leadTime ?? '-';
  document.getElementById("modalComment").textContent = mrp.comment || '-';

  // Bootstrap 5 모달 표시
  const modal = new bootstrap.Modal(document.getElementById('mrpDetailModal'));
  modal.show();
}
