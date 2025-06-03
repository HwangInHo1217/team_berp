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
document.addEventListener('DOMContentLoaded', () => {
  const saveBtn = document.getElementById('shipmentSaveBtn');

  saveBtn.addEventListener('click', () => {
    // 1) 모달 내 모든 <tr>을 순회하며,
    //    각 tr 안에 있는 “hiddenOrderLineItem + select(warehouseId) + input(quantity)” 조합을 찾아 출고 데이터 수집
    const rows = document.querySelectorAll('#shipmentItemTableBody tr');
    const shipmentItems = [];

    rows.forEach(tr => {
      // tr 내의 각 “wrapper” 선택
      const wrappers = tr.querySelectorAll('.warehouse-row');
      wrappers.forEach(wrapper => {
        // 각각 wrapper 안의 hidden, select, input을 찾아낸다.
        const hiddenEl = wrapper.querySelector('input[type="hidden"][data-field="orderLineItemId"]');
        const selectEl = wrapper.querySelector('select[data-field="warehouseId"]');
        const inputEl = wrapper.querySelector('input[type="number"][data-field="quantity"]');

        if (!hiddenEl || !selectEl || !inputEl) return; // 안전장치

        const orderLineItemId = Number(hiddenEl.value);               // 주문상품 ID
        const warehouseId = Number(selectEl.value);                   // 사용자가 선택한 창고 ID
        const quantity = Number(inputEl.value) || 0;                   // 출고수량

        if (quantity > 0) {
          shipmentItems.push({
            orderLineItemId: orderLineItemId,
            warehouseId: warehouseId,
            quantity: quantity
          });
        }
      });
    });

    // 2) 출고 데이터가 없으면 경고
    if (shipmentItems.length === 0) {
      alert('🚨 출고할 수량을 하나 이상 입력해주세요.');
      return;
    }

    // 3) 모달 내에서 “comment”(비고)와 “companyEmpName”(출고담당자) 값을 가져오기
    //    (모달 내에 이 두 필드가 있다고 가정: #shipmentComment, #shipmentCompanyEmpName)
    const comment = document.querySelector('#shipmentComment')?.value || '';
    const companyEmpName = document.querySelector('#shipmentCompanyEmpName')?.value || '';

    // 4) 현재 주문 ID도 같이 포함
    const orderId = Number(document.querySelector('#shipmentModal').dataset.orderId);

    // 5) 최종 payload 객체
    const payload = {
      orderId: orderId,
      comment: comment,
      companyEmpName: companyEmpName,
      shipmentItems: shipmentItems
    };

    // 6) 서버에 출고 정보 전달
    fetch('/api/shipments', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })
      .then(res => {
        if (!res.ok) {
          return res.text().then(msg => { throw new Error(msg); });
        }
        return res.text();
      })
      .then(_ => {
        alert('✅ 출고가 정상적으로 등록되었습니다.');

        // 모달 닫기
        const modalEl = document.getElementById('shipmentModal');
        const modalInstance = bootstrap.Modal.getInstance(modalEl);
        if (modalInstance) modalInstance.hide();

        // 필요 시, 주문목록/재고 현황 갱신
        // fetchOrders(0);
        // fetchInventoryStatus();
      })
      .catch(err => {
        console.error('❌ 출고 등록 중 오류 발생:', err);
        alert('❌ 출고 등록 중 오류가 발생했습니다.');
      });
  });
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

	$('#itemsTable tbody tr').each(function() {
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
		success: function() {
			alert('저장 완료');
			$('#registerModal').modal('hide');
			$('#registerForm')[0].reset();
			$('#itemsTable tbody').empty();
			fetchOrders(0);
		},
		error: function() {
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
$('#itemsTable').on('change', '.itemSelect', function() {
	const $tr = $(this).closest('tr');
	const opt = $(this).find('option:selected');
	$tr.find('.unit').val(opt.data('unit'));
	$tr.find('.unitPrice').val(opt.data('price'));
	$tr.find('.unitQty').val(1);
	calculateRowTotal($tr);
	calculateTotalAmount();
});

// ✅ 수량 변경 시 합계 재계산
$('#itemsTable').on('input', '.unitQty', function() {
	const $tr = $(this).closest('tr');
	calculateRowTotal($tr);
	calculateTotalAmount();
});

// ✅ 행 삭제
$('#itemsTable').on('click', '.removeItem', function() {
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
	$('#itemsTable tbody tr').each(function() {
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
		  // ① allShipped 상태에 따라 버튼 HTML을 달리 만든다.
		  let shipmentBtnHtml;
		  if (o.allShipped) {
		      // 출고가 이미 완료된 상태라면 비활성화된 “출고완료” 버튼
		      shipmentBtnHtml = `
		          <button type="button" class="btn btn-secondary btn-sm" disabled>
		              출고완료
		          </button>`;
		  } else {
		      // 아직 출고되지 않은 상태라면 “출고등록” 버튼 활성화
		      shipmentBtnHtml = `
		          <button type="button" class="btn btn-success btn-sm shipmentBtn" data-id="${o.orderId}">
		              출고 등록
		          </button>`;
		  }

		  // ② tr 전체 문자열을 조립할 때 새로 만든 shipmentBtnHtml을 넣어준다.
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
		  <td>${shipmentBtnHtml}</td>
		</tr>`;
		  $tbody.append(row)
	});

	$('.detailBtn').click(function() {
		const orderId = $(this).data('id');
		$.get(`/api/orders/${orderId}`, populateDetailModal);
	});

	$('.editBtn').click(function() {
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
// order.js




  /**
   * 1) “창고 입력 줄(wrapper)”을 생성해 주는 함수
   *    - itemId: 해당 품목 ID
   *    - warehouseList: 해당 품목이 보유된 모든 창고 정보 배열
   *      (각 원소: { orderLineItemId, itemId, itemCode, itemName, orderQty, warehouseId, warehouseName, stockQty } )
   */
  function createWarehouseRow(itemId, warehouseList) {
    // 1) wrapper <div> 생성
    const wrapper = document.createElement('div');
    wrapper.classList.add('mb-2', 'd-flex', 'align-items-center', 'warehouse-row');

    // 2) “orderLineItemId”를 숨겨서 보관할 hidden <input> 생성
    //    (이 값을 서버로 보내서 어떤 주문상품에서 출고할 건지 구분)
    const hiddenOrderLineItem = document.createElement('input');
    hiddenOrderLineItem.type = 'hidden';
    // 첫 번째 warehouseList 요소에 있는 orderLineItemId를 사용
    hiddenOrderLineItem.value = warehouseList[0].orderLineItemId;
    hiddenOrderLineItem.setAttribute('data-field', 'orderLineItemId');
    wrapper.appendChild(hiddenOrderLineItem);

    // 3) 창고 선택 <select> 생성
    const warehouseSelect = document.createElement('select');
    warehouseSelect.classList.add('form-select', 'form-select-sm', 'me-2');
    warehouseSelect.style.minWidth = '200px'; // 너비 지정(필요 시)
    warehouseSelect.setAttribute('data-field', 'warehouseId');

    // 옵션 추가: “warehouseList” 배열을 순회하며
    warehouseList.forEach(wsi => {
      const opt = document.createElement('option');
      opt.value = wsi.warehouseId;           // ex: 11
      opt.textContent = `${wsi.warehouseName} (재고: ${wsi.stockQty})`;
      warehouseSelect.appendChild(opt);
    });
    wrapper.appendChild(warehouseSelect);

    // 4) 출고 수량 입력 <input> 생성
    const qtyInput = document.createElement('input');
    qtyInput.type = 'number';
    qtyInput.min = '0';
    qtyInput.value = '0';
    qtyInput.classList.add('form-control', 'form-control-sm', 'me-2');
    qtyInput.setAttribute('data-field', 'quantity');
    wrapper.appendChild(qtyInput);

    // 5) “❌ 삭제” 버튼 생성: 잘못 추가된 줄 제거 용
    const removeBtn = document.createElement('button');
    removeBtn.type = 'button';
    removeBtn.classList.add('btn', 'btn-sm', 'btn-outline-danger');
    removeBtn.innerText = '❌';
    removeBtn.title = '이 줄 제거';
    removeBtn.addEventListener('click', () => {
      wrapper.remove();
    });
    wrapper.appendChild(removeBtn);

    // 6) wrapper(DOM 요소) 반환
    return wrapper;
  }
  // “출고 등록” 버튼 클릭 시 모달 열기 로직 (OrderApiController.getShipmentInfo 호출 후)
  document.getElementById('orderTableBody').addEventListener('click', async (event) => {
    if (!event.target.classList.contains('shipmentBtn')) return;

    const orderId = Number(event.target.dataset.id);

    try {
      const response = await fetch(`/api/orders/${orderId}/shipment-info`);
      if (!response.ok) throw new Error(`서버 오류: ${response.status}`);
      const rawData = await response.json();
      // rawData: [ { orderLineItemId, itemId, itemCode, itemName, orderQty, warehouseId, warehouseName, stockQty }, … ]

      // (1) 테이블 <tbody> 초기화
      const tbody = document.getElementById('shipmentItemTableBody');
      tbody.innerHTML = '';

      // (2) rawData를 “itemId → [창고정보…]” 형태로 그룹핑
      const dataMap = {};
      rawData.forEach(wsi => {
        const key = wsi.orderLineItemId; // 주문상품 ID로 그룹핑
        if (!dataMap[key]) dataMap[key] = [];
        dataMap[key].push(wsi);
      });

      // (3) 그룹별로 <tr> 생성
      Object.entries(dataMap).forEach(([oliIdStr, warehouseList]) => {
        const tr = document.createElement('tr');

        // (가) <td> 품목코드, 품목명, 주문수량
        const first = warehouseList[0];
        const tdCode = document.createElement('td');
        tdCode.innerText = first.itemCode;  
        tr.appendChild(tdCode);

        const tdName = document.createElement('td');
        tdName.innerText = first.itemName;
        tr.appendChild(tdName);

        const tdOrderQty = document.createElement('td');
        tdOrderQty.innerText = first.orderQty;
        tr.appendChild(tdOrderQty);

        // (나) 창고+수량 입력 영역 <td>
        const tdWarehouseArea = document.createElement('td');
        const container = document.createElement('div');
        container.classList.add('warehouse-rows-container');

        // 최초 1개 라인
        const firstRow = createWarehouseRow(first.itemId, warehouseList);
        container.appendChild(firstRow);

        // “➕ 행 추가” 버튼
        const addBtn = document.createElement('button');
        addBtn.type = 'button';
        addBtn.classList.add('btn', 'btn-sm', 'btn-outline-secondary', 'ms-2');
        addBtn.innerText = '➕';
        addBtn.title = '다른 창고도 추가 등록';
        addBtn.addEventListener('click', () => {
          const newRow = createWarehouseRow(first.itemId, warehouseList);
          container.appendChild(newRow);
        });

        tdWarehouseArea.appendChild(container);
        tdWarehouseArea.appendChild(addBtn);
        tr.appendChild(tdWarehouseArea);

        tbody.appendChild(tr);
      });

      // (4) 모달을 띄우기 전에 data-order-id 속성 추가
      const modalEl = document.getElementById('shipmentModal');
      modalEl.setAttribute('data-order-id', orderId);

      // (5) 모달 띄우기
      const modalInstance = new bootstrap.Modal(modalEl);
      modalInstance.show();

    } catch (err) {
      console.error('❌ 출고 정보를 불러오는 데 실패했습니다.', err);
      alert('❌ 출고 정보를 불러오는 데 실패했습니다.');
    }
  });
