// shipment.js
// 출고 관리 페이지 전용 JS
// - 전체 체크/해제
// - 버튼 활성화 로직
// - 모달에 ID 전달
// - AJAX fragment 로딩

document.addEventListener('DOMContentLoaded', function() {
  const chkAll = document.getElementById('chk-all-pending');
  const chkItems = document.querySelectorAll('.chk-pending');
  const btnPre  = document.getElementById('btn-preShip');
  const btnDo   = document.getElementById('btn-doShip');

  // 1) 전체 체크박스 클릭 → 개별 체크박스 모두 동기화
  chkAll.addEventListener('click', function() {
    chkItems.forEach(ch => ch.checked = chkAll.checked);
    toggleShipButtons();
  });

  // 2) 개별 체크박스 변화 시 → 가/출고 버튼 활성화 토글
  chkItems.forEach(ch => {
    ch.addEventListener('change', toggleShipButtons);
  });

  // 버튼 활성/비활성 처리 함수
  function toggleShipButtons() {
    const anyChecked = Array.from(chkItems).some(ch => ch.checked);
    btnPre.disabled = !anyChecked;
    btnDo.disabled  = !anyChecked;
  }

  // 3) 가출고 버튼 클릭 시 선택된 orderIds를 hidden input에 설정
  btnPre.addEventListener('click', function() {
    const ids = Array.from(chkItems)
              .filter(ch => ch.checked)
              .map(ch => ch.value)
              .join(',');
    document.getElementById('preShipIds').value = ids;
  });

  // 4) 출고 버튼 클릭 시 선택된 shipmentIds를 hidden input에 설정
  btnDo.addEventListener('click', function() {
    const ids = Array.from(chkItems)
              .filter(ch => ch.checked)
              .map(ch => ch.value)
              .join(',');
    document.getElementById('doShipIds').value = ids;
  });

  // 5) AJAX: 주문 상세 모달 로딩
  document.querySelectorAll('.btn-order-detail').forEach(btn => {
    btn.addEventListener('click', function() {
      const orderId = btn.getAttribute('data-id');
      fetch(`/shipments/order/${orderId}/detail`)
        .then(res => res.text())
        .then(html => {
          document.getElementById('registerDetailModalContent')
                  .innerHTML = html;
        });
    });
  });

  // 6) AJAX: 출고 상세 모달 로딩
  document.querySelectorAll('.btn-ship-detail').forEach(btn => {
    btn.addEventListener('click', function() {
      const shipId = btn.getAttribute('data-id');
      fetch(`/shipments/${shipId}/detail`)
        .then(res => res.text())
        .then(html => {
          document.getElementById('detailModalContent')
                  .innerHTML = html;
        });
    });
  });

  // 7) 수정 버튼 클릭 시 → registerModal에 기존 form 로딩
  document.querySelectorAll('.btn-ship-update').forEach(btn => {
    btn.addEventListener('click', function() {
      const shipId = btn.getAttribute('data-id');
      fetch(`/shipments/${shipId}/detail`)
        .then(res => res.text())
        .then(html => {
          document.getElementById('registerModalContent')
                  .innerHTML = html;
        });
    });
  });

});