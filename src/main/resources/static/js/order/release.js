/**
 * <1> 전역 변수 선언
 * - 현재 선택된 주문서 ID를 저장할 변수입니다.
 */
let currentOrderId = null;

/**
 * <2> 모달 열기 함수
 * @param {Number|String} orderId - 출고 등록을 진행하려는 주문서의 ID
 */
function openReleaseRegisterModal(orderId) {
  // <2-1> 전역 변수에 주문서 ID 저장
  currentOrderId = orderId;
  
  // <2-2> 테이블 <tbody> 영역 비우기 (이전 데이터 초기화)
  const tbody = document.querySelector('#releaseItemTable tbody');
  tbody.innerHTML = '';

  // <2-3> 주문서 아이템 목록을 조회하는 API 호출
  // (예: GET /api/orders/{orderId}/items => [{ itemId, itemCode, itemName, orderQty }, ...])
  fetch(`/api/orders/${orderId}/items`)
    .then(response => {
      if (!response.ok) {
        throw new Error('주문서 아이템 목록을 불러오는 데 실패했습니다.');
      }
      return response.json();
    })
    .then(items => {
      // <2-4> 응답으로 받은 아이템 배열(items)을 순회하며 각 아이템의 재고 조회 및 행 추가
      items.forEach(item => {
        // <2-4-1> 아이템별 창고 재고를 조회하는 API 호출
        // (예: GET /api/stocks?itemId={itemId} => [{ warehouseName, stockQty }, ...])
        fetch(`/api/stocks?itemId=${item.itemId}`)
          .then(resp => {
            if (!resp.ok) {
              throw new Error('창고 재고 정보를 불러오는 데 실패했습니다.');
            }
            return resp.json();
          })
          .then(stocks => {
            /**
             * <2-4-2> 재고 정보를 문자열로 가공
             * 예시: "창고A: 10개, 창고B: 5개"
             */
            const stockInfoText = stocks.map(s => `${s.warehouseName}: ${s.stockQty}개`).join(', ');
            
            // <2-4-3> 테이블에 새로운 행(<tr>) 추가
            const tr = document.createElement('tr');
            
            // 품목 코드 셀
            const tdCode = document.createElement('td');
            tdCode.innerText = item.itemCode;
            tr.appendChild(tdCode);
            
            // 품목 이름 셀
            const tdName = document.createElement('td');
            tdName.innerText = item.itemName;
            tr.appendChild(tdName);
            
            // 주문 수량 셀
            const tdOrderQty = document.createElement('td');
            tdOrderQty.innerText = item.orderQty;
            tr.appendChild(tdOrderQty);
            
            // 창고별 재고 셀
            const tdStockInfo = document.createElement('td');
            tdStockInfo.innerText = stockInfoText || '재고 없음';
            tr.appendChild(tdStockInfo);
            
            // 출고 수량 입력을 위한 입력창 셀
            const tdReleaseQty = document.createElement('td');
            const input = document.createElement('input');
            input.type = 'number';
            input.min = '0';
            input.max = item.orderQty; // 최대값을 주문 수량으로 제한
            input.value = '0'; // 기본값 0
            input.classList.add('form-control', 'form-control-sm');
            input.setAttribute('data-item-id', item.itemId); // 나중에 값을 수집할 때 사용
            tdReleaseQty.appendChild(input);
            tr.appendChild(tdReleaseQty);
            
            // 완성된 행을 tbody에 추가
            tbody.appendChild(tr);
          })
          .catch(err => {
            console.error(err);
            // 오류 발생 시에도 테이블에는 최소한 행을 추가
            const trError = document.createElement('tr');
            trError.innerHTML = `
              <td>${item.itemCode}</td>
              <td>${item.itemName}</td>
              <td>${item.orderQty}</td>
              <td>재고 정보 조회 실패</td>
              <td>-</td>
            `;
            tbody.appendChild(trError);
          });
      });
    })
    .catch(err => {
      console.error(err);
      alert('주문서 아이템을 로드하는 중 오류가 발생했습니다.');
    });

  // <2-5> Bootstrap Modal 인스턴스 생성 후 모달 표시
  const modalElement = document.getElementById('releaseRegisterModal');
  const modal = new bootstrap.Modal(modalElement);
  modal.show();
}

/**
 * <3> 출고 등록 처리 함수
 * - 사용자가 "출고 등록" 버튼을 클릭했을 때 호출됩니다.
 * - 테이블의 각 행에서 출고 수량(input 값)을 수집하여 API로 전송
 */
function submitReleaseRegistration() {
  // <3-1> 테이블의 모든 input (data-item-id 속성이 있는 요소) 선택
  const inputs = document.querySelectorAll('#releaseItemTable tbody input[data-item-id]');
  
  // <3-2> 출고 데이터를 담을 배열
  const releaseDataList = [];
  
  inputs.forEach(input => {
    const itemId = input.getAttribute('data-item-id');          // 아이템 ID
    const releaseQty = parseInt(input.value, 10) || 0;           // 입력된 출고 수량 (정수)
    
    if (releaseQty > 0) {                                        // 출고 수량이 1 이상일 때만 포함
      releaseDataList.push({
        orderId: currentOrderId,                                 // 현재 주문서 ID
        itemId: Number(itemId),                                  // 아이템 ID
        quantity: releaseQty                                      // 출고 수량
      });
    }
  });
  
  if (releaseDataList.length === 0) {
    alert('출고할 수량을 하나 이상 입력해주세요.');
    return;
  }
  
  // <3-3> 실제 출고 등록 API 호출 (예: POST /api/shipments)
  // 요청 바디: [{ orderId, itemId, quantity }, ...]
  fetch('/api/shipments', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(releaseDataList)
  })
    .then(response => {
      if (!response.ok) {
        throw new Error('출고 등록에 실패했습니다.');
      }
      return response.json();
    })
    .then(result => {
      // <3-4> 성공 시 처리
      alert('출고가 정상적으로 등록되었습니다.');
      // 모달 닫기
      const modalElement = document.getElementById('releaseRegisterModal');
      const modal = bootstrap.Modal.getInstance(modalElement);
      modal.hide();
      
      // 필요하다면 화면 갱신 (예: 주문 목록 다시 조회)
      // fetchOrders(0);
    })
    .catch(err => {
      console.error(err);
      alert('출고 등록 중 오류가 발생했습니다.');
    });
}

/**
 * <4> 이벤트 리스너 등록
 */
document.addEventListener('DOMContentLoaded', function() {
  // <4-1> "출고 등록" 버튼에 클릭 이벤트 바인딩
  const btnSubmit = document.getElementById('btnSubmitRelease');
  btnSubmit.addEventListener('click', submitReleaseRegistration);
});
