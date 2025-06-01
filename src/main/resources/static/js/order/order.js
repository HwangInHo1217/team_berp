// ✅ 전역 변수 초기화
let ALL_ITEMS = [];
let ALL_WAREHOUSES = [];
function loadCompanies() {
  $.get('/api/companies', data => {
    const $select = $('#companySelect, #searchCompanySelect');
    $select.empty().append('<option value=\"\">전체</option>');
    data.forEach(c => {
      $select.append(`<option value=\"${c.companyId}\">${c.companyName}</option>`);
    });
  });
}

function loadProductItems(callback) {
  $.get('/api/items?type=product', data => {
    ALL_ITEMS = data;
    if (callback) callback();
  });
}

function loadWarehouses(callback) {
  $.get('/api/warehouses/all', data => {
    if (!Array.isArray(data)) return alert('창고 목록 불러오기 실패');
    ALL_WAREHOUSES = data.filter(w => w.warehouseType === 'PRODUCT');
    if (callback) callback();
  });
}

$(document).ready(() => {
  loadCompanies();
  loadProductItems(() => {
    loadWarehouses(() => {
      fetchOrders(0);
    });
  });

  bindGlobalEventHandlers();
});

// ✅ 전역 이벤트 바인딩
function bindGlobalEventHandlers() {
	
  $('#openRegisterModal').click(handleOpenRegisterModal);
  $('#companySelect').change(handleCompanyChange);
  $('#addItem').click(addItemRow);
  $('#selectAll').change(toggleSelectAll);
  $('#deleteSelected').click(deleteSelectedOrders);
  $('#filterForm').submit(e => { e.preventDefault(); fetchOrders(0); });
}

// ✅ 모달 열기 및 초기화
function handleOpenRegisterModal() {
  $('#registerForm')[0].reset();
  $('#itemsTable tbody').empty();
  $('#totalAmount').text('0');
  addItemRow();
  $('#registerModal').modal('show');

  // 중복 방지
  $('#saveOrder').off('click').on('click', saveOrder);
}

// ✅ 고객사 변경 시 담당자 자동 세팅
function handleCompanyChange() {
  const companyId = $(this).val();
  if (!companyId) return $('#empName').val('');

  $.get(`/api/companies/${companyId}/info`, data => {
    $('#empName').val(data.empName);
  });
}

// ✅ 주문 저장
function saveOrder() {
  const orderItems = [];

  $('#itemsTable tbody tr').each(function () {
    const itemId = $(this).find('.itemSelect').val();
    const unit = $(this).find('.unit').val();
    const unitPrice = $(this).find('.unitPrice').val();
    const unitQty = $(this).find('.unitQty').val();

    if (itemId && unit && unitPrice && unitQty) {
      orderItems.push({
        itemId: Number(itemId),
        unit,
        unitPrice: Number(unitPrice),
        unitQty: Number(unitQty)
      });
    }
  });

  const orderData = {
    companyId: Number($('#companySelect').val()),
    orderDate: $('input[name="orderDate"]').val(),
    empName: $('#empName').val(),
    companyEmpName: $('#companyEmpName').val(),
    remark: $('textarea[name="remark"]').val(),
    orderType: $('input[name="orderType"]').val(),
    itemType: $('input[name="itemType"]').val(),
    items: orderItems
  };

  $.ajax({
    url: '/api/orders',
    method: 'POST',
    contentType: 'application/json',
    data: JSON.stringify(orderData),
    success: function () {
      alert('저장 완료');
      $('#registerModal').modal('hide');
      $('#registerForm')[0].reset();
      $('#itemsTable tbody').empty();
      fetchOrders(0);
    },
    error: function () {
      alert('저장 중 오류 발생');
    }
  });
}

// ✅ 품목 행 추가
function addItemRow() {
  const idx = $('#itemsTable tbody tr').length;
  const $tr = $(`
    <tr>
      <td>
        <select name="items[${idx}].itemId" class="form-select itemSelect" required>
          <option value="">선택</option>
        </select>
      </td>
      <td><input type="text" class="form-control unit" readonly></td>
      <td><input type="number" class="form-control unitPrice" readonly></td>
      <td><input type="number" class="form-control unitQty" required></td>
      <td><input type="text" class="form-control total" readonly></td>
      <td><button type="button" class="btn btn-danger removeItem">삭제</button></td>
    </tr>
  `);

  const $itemSel = $tr.find('.itemSelect');
  ALL_ITEMS.forEach(i => {
    $itemSel.append(`<option value="${i.id}" data-unit="${i.unit}" data-price="${i.itemPrice}">${i.itemName}</option>`);
  });

  $('#itemsTable tbody').append($tr);
}

// ✅ 품목 선택 시 자동입력
$('#itemsTable').on('change', '.itemSelect', function () {
  const $tr = $(this).closest('tr');
  const opt = $(this).find('option:selected');
  $tr.find('.unit').val(opt.data('unit'));
  $tr.find('.unitPrice').val(opt.data('price'));
  $tr.find('.unitQty').val(1);
  calculateRowTotal($tr);
  calculateTotalAmount();
});

// ✅ 수량 변경 시 합계 재계산
$('#itemsTable').on('input', '.unitQty', function () {
  const $tr = $(this).closest('tr');
  calculateRowTotal($tr);
  calculateTotalAmount();
});

// ✅ 행 삭제
$('#itemsTable').on('click', '.removeItem', function () {
  $(this).closest('tr').remove();
  calculateTotalAmount();
});

function calculateRowTotal($tr) {
  const qty = parseFloat($tr.find('.unitQty').val()) || 0;
  const price = parseFloat($tr.find('.unitPrice').val()) || 0;
  $tr.find('.total').val((qty * price).toFixed(2));
}

function calculateTotalAmount() {
  let total = 0;
  $('#itemsTable tbody tr').each(function () {
    total += parseFloat($(this).find('.total').val()) || 0;
  });
  $('#totalAmount').text(total.toFixed(2));
}

function toggleSelectAll() {
  $('.selectBox').prop('checked', this.checked);
}

function deleteSelectedOrders() {
  const ids = $('.selectBox:checked').map((i, el) => el.value).get();

  if (ids.length === 0) return alert('삭제할 주문을 선택하세요.');
  if (!confirm('정말 삭제하시겠습니까?')) return;

  fetch('/api/orders', {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(ids)
  })
    .then(response => {
      if (!response.ok) {
        return response.text().then(msg => { throw new Error(msg); });
      }
      return response.text();
    })
    .then(msg => {
      alert('✅ ' + msg);
      fetchOrders(); // 목록 다시 불러오기
    })
    .catch(err => {
      alert('❌ ' + err.message); // API에서 전달한 에러 메시지 출력
    });
}


function fetchOrders(page = 0) {
  const params = new URLSearchParams({
    companyId: $('#searchCompanySelect').val(),
    itemId: $('#searchItemSelect').val(),
    fromDate: $('input[name="fromDate"]').val(),
    toDate: $('input[name="toDate"]').val(),
    page,
    size: 10
  });

  fetch(`/api/orders/list?${params.toString()}`)
    .then(res => res.json())
    .then(data => {
      renderOrderTable(data.content, data.number, data.size);
      renderPagination(data.totalPages, data.number);
    })
    .catch(() => alert("주문 목록 조회 실패"));
}

function renderOrderTable(orders, pageNumber, pageSize) {
  const $tbody = $('#orderTableBody').empty();
  orders.forEach((o, i) => {
    const idx = i + 1 + pageNumber * pageSize;
	const row = `
	  <tr>
	    <td><input type="checkbox" class="selectBox" value="${o.orderId}" /></td>
	    <td>${idx}</td>
	    <td>${o.orderDate}</td>
	    <td>${o.orderNum || '-'}</td>
	    <td>${o.companyName}</td>
	    <td>${o.orderQty}</td>
	    <td>${o.amount}</td>
	    <td>${o.companyEmpName || '-'}</td>
	    <td>${o.empName || '-'}</td>
	    <td><button class="btn btn-info btn-sm detailBtn" data-id="${o.orderId}">상세</button></td>
	    <td><button class="btn btn-warning btn-sm editBtn" data-id="${o.orderId}">수정</button></td>
	    <td><button class="btn btn-success btn-sm shipmentBtn" data-id="${o.orderId}">출고 등록</button></td>
	  </tr>`;

    $tbody.append(row);
  });

  $('.detailBtn').click(function () {
    const orderId = $(this).data('id');
    $.get(`/api/orders/${orderId}`, populateDetailModal);
  });

  $('.editBtn').click(function () {
    const orderId = $(this).data('id');
    $.get(`/api/orders/${orderId}`, populateFixedForm);
  });
}

function renderPagination(totalPages, currentPage) {
  const $ul = $('.pagination').empty();
  if (totalPages === 0) return;

  $ul.append(`<li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
    <button class="page-link" onclick="fetchOrders(${currentPage - 1})">이전</button></li>`);

  for (let i = 0; i < totalPages; i++) {
    $ul.append(`<li class="page-item ${i === currentPage ? 'active' : ''}">
      <button class="page-link" onclick="fetchOrders(${i})">${i + 1}</button></li>`);
  }

  $ul.append(`<li class="page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}">
    <button class="page-link" onclick="fetchOrders(${currentPage + 1})">다음</button></li>`);
}

function populateDetailModal(data) {
  $('#detailOrderNum').text(data.orderNum || '-');
  $('#detailOrderDate').text(data.orderDate || '-');
  $('#detailCompanyName').text(data.companyName || '-');
  $('#detailEmpName').text(data.empName || '-');
  $('#detailCompanyEmpName').text(data.companyEmpName || '-');
  $('#detailRemark').text(data.remark || '-');

  const $tbody = $('#detailItemsTable tbody');
  $tbody.empty();

  data.items.forEach(item => {
    const total = item.unitQty * item.unitPrice;
    const row = `
      <tr>
        <td>${item.itemName}</td>
        <td>${item.unit}</td>
        <td>${item.unitPrice.toFixed(2)}</td>
        <td>${item.unitQty}</td>
        <td>${total.toFixed(2)}</td>
      </tr>
    `;
    $tbody.append(row);
  });

  const modalEl = document.getElementById('detailModal');
  const modal = new bootstrap.Modal(modalEl);
  modal.show();
}

// ──────────────────────────────────────────────
// 1) "출고 등록" 버튼 클릭 시 처리
// ──────────────────────────────────────────────

document
  .getElementById('orderTableBody')
  .addEventListener('click', async (event) => {
    // 1-1) 클릭된 요소가 '출고 등록' 버튼(shipmentBtn)이 아니면 무시
    if (!event.target.classList.contains('shipmentBtn')) return;

    // 1-2) 버튼의 data-id 속성에서 orderId를 꺼낸다 (문자→숫자 변환도 가능)
    const orderId = event.target.dataset.id;

    try {
      // 2) 서버에서 해당 주문(orderId)에 속한 item별 창고 재고 정보를 받아온다.
      //    (백엔드 코드에서 Map<itemId, List<ItemWarehouseResponse>> 형태로 내려준다고 가정)
      const response = await fetch(`/api/orders/${orderId}/shipment-info`);
      if (!response.ok) {
        // 상태 코드가 200이 아니면 에러로 처리
        throw new Error(`서버 오류: HTTP ${response.status}`);
      }

      // 2-1) JSON을 자바스크립트 객체로 파싱
      //      구조 예시:
      //      {
      //         "3": [ { itemId:3, itemName:"모터", warehouseId:9, warehouseName:"완제품 3창고", quantity:10 }, … ],
      //         "7": [ { itemId:7, itemName:"휴대용 선풍기", warehouseId:11, … }, … ]
      //      }
      const data = await response.json();
      console.log("🚀 서버 응답(맵 전체):", data);

      // 3) HTML에 있는 <select id="shipmentWarehouseSelect"> 요소를 찾는다
      const selectEl = document.getElementById('shipmentWarehouseSelect');
      if (!selectEl) {
        // (만약 모달 삽입 타이밍이 늦어져서 아직 DOM에 없는 경우를 방지)
        console.error('❌ shipmentWarehouseSelect 셀렉트 박스를 찾을 수 없습니다.');
        alert('프론트엔드 오류: 셀렉트 박스를 찾을 수 없습니다.');
        return;
      }

      // 4) <select> 내부를 비운다 (이전 데이터가 남아 있으면 지워 줌)
      selectEl.innerHTML = '';

      // 5) data가 비어 있거나, 빈 객체({})이 넘어오면 “재고 없음” 처리
      //    Object.keys(data).length가 0이면, 아무 아이템도 재고가 없다는 뜻이다.
      if (
        !data ||                          // 데이터가 null/undefined거나
        typeof data !== 'object' ||       // 객체가 아니거나
        Object.keys(data).length === 0    // 키가 하나도 없으면
      ) {
        // 5-1) 모달 열기 전에 안내 메시지를 띄우거나, 모달 내 메시지 보여준 뒤 닫는다.
        alert('❌ 해당 주문의 모든 상품이 창고에 재고가 없습니다.');
        return;
      }

      // 6) “Map 형태”이므로, Object.entries(data)로 [ [itemId, [WSI,...]], [itemId2, […]] ] 꼴의 배열을 얻는다.
      //    예) [ ["3", [ {itemId:3, itemName:"모터",…}, … ] ], ["7", [ {itemId:7,…} ] ] ]
      const entries = Object.entries(data);

      // ★★★ 옵션에 넣을 텍스트와 value를 만들기 위해 모든 itemId별 창고 리스트를 전부 하나로 합친다. ★★★
      // 예) allWarehouseArray = [ {itemId:3,itemName:"모터",warehouseId:9,…}, …, {itemId:7,itemName:"휴대용 선풍기",warehouseId:11,…} ]
      const allWarehouseArray = entries
        .map(([itemId, warehouseList]) => warehouseList)  // 각 entry의 warehouseList만 뽑아서
        .flat();                                           // 1차원 배열로 합친다.

      // 7) 지금은 “하나의 <select>에 모든 item–warehouse 조합”을 넣는 예시
      //    (아이템별로 분리된 <select>가 필요하면 이 부분을 itemId별로 따로 append해 주면 됩니다.)
      allWarehouseArray.forEach((wsi) => {
        // wsi는 { itemId, itemName, warehouseId, warehouseName, quantity } 객체
        const opt = document.createElement('option');
        opt.value = wsi.warehouseId; 
        opt.textContent = `${wsi.itemName} – ${wsi.warehouseName} (보유: ${wsi.quantity})`;
        selectEl.appendChild(opt);
      });

      // 8) 모달 엘리먼트를 꺼내서 Bootstrap modal API로 띄운다
      const modalEl = document.getElementById('shipmentModal');
      // getOrCreateInstance → 이미 인스턴스가 있으면 재사용, 없으면 새로 생성
      const modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
      modalInstance.show();

      // 9) (선택사항) 모달의 “출고 등록” 버튼 클릭 이벤트를 나중에 처리하고 싶다면 여기서 한 번만 bind
      //    예) 모달이 켜질 때마다 #shipmentSaveBtn의 click 리스너를 내부 state를 바꿔서 처리할 수도 있고,
      //        모달 닫힐 때 “이전 리스너”를 제거하는 등 추가 작업을 해줄 수도 있습니다.
      //    아래 코드는 예시이므로, 실제 구현에 맞게 커스터마이징하세요.
      const saveBtn = document.getElementById('shipmentSaveBtn');
      saveBtn.onclick = () => {
        // 모달 내에서 “출고할 창고”와 “출고 수량”을 읽어 오는 예시
        const chosenWarehouseId = Number(selectEl.value);
        const chosenQuantity = Number(
          document.getElementById('shipmentQuantityInput').value
        );
        
        // 간단하게 alert로 찍어 보거나, 실제 출고 API를 호출할 수 있다.
        alert(
          `→ 주문ID=${orderId} / itemId=${/* wsi.itemId은 선택된 option의 index에 따라 어떻게 찾을지 결정 */ ''} ` +
          `/ 창고ID=${chosenWarehouseId} / 출고수량=${chosenQuantity}`
        );

        // ↓ 예시: 출고를 실제로 처리할 API 호출 (POST /api/orders/{orderId}/ship … 등)
        // fetch(`/api/orders/${orderId}/ship`, {
        //   method: 'POST',
        //   headers: { 'Content-Type': 'application/json' },
        //   body: JSON.stringify({
        //     warehouseId: chosenWarehouseId,
        //     itemId: /* 위에서 선택한 itemId */,
        //     quantity: chosenQuantity
        //   })
        // })
        // .then(res => { … })
        // .catch(err => { … });

        // 모달 닫기
        modalInstance.hide();
      };

    } catch (err) {
      console.error('❌ 출고 정보를 불러오는 데 실패했습니다.', err);
      alert('❌ 출고 정보를 불러오는 데 실패했습니다.');
    }
  });
