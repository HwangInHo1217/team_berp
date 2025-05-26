function openStockDetailModal(stock) {
      // 모달이 없으면 fetch후 재호출 (이중실행 방지)
      if (!document.getElementById('stockDetailModal')) {
        fetch('/pages/stock/stock-detail-modal.html')
          .then(res => res.text())
          .then(html => {
            document.getElementById('stockDetailModalWrapper').innerHTML = html;
            setTimeout(() => openStockDetailModal(stock), 50);
          });
        return;
      }

      // 상세 데이터 채우기
      document.getElementById('detailItemCode').textContent = stock.item_code;
      document.getElementById('detailItemName').textContent = stock.item_name;
      document.getElementById('detailWarehouse').textContent = stock.warehouse;
      document.getElementById('detailQty').textContent = stock.qty;
      document.getElementById('detailUnit').textContent = stock.unit;
      document.getElementById('detailLastIn').textContent = stock.last_in;
      document.getElementById('detailLastOut').textContent = stock.last_out;

      // 모달 열기
      const modal = new bootstrap.Modal(document.getElementById('stockDetailModal'));
      modal.show();
    }