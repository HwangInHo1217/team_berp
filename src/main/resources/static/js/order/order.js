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

/**
 * 주문 목록을 화면에 그려주는 함수
 * - orders: 백엔드에서 내려준 페이지(content) 배열
 * - pageNumber, pageSize: 페이징용 계산값
 */
function renderOrderTable(orders, pageNumber, pageSize) {
  const $tbody = $('#orderTableBody').empty();

  // ───────────────────────────────────────────────────────
  // ★ 이미 출고가 완료된 행(allShipped===true)은 건너뛰고(!) 렌더링
  // ───────────────────────────────────────────────────────
  const visibleOrders = orders.filter(o => !o.allShipped);

  visibleOrders.forEach((o, i) => {
    const idx = i + 1 + pageNumber * pageSize;

    // (예시: allShipped가 false이므로, 항상 '출고등록' 버튼을 렌더링)
    const shipmentBtnHtml = `
      <button type="button"
              class="btn btn-success btn-sm shipmentBtn"
              data-id="${o.orderId}">
        출고 등록
      </button>`;

    const row = `
      <tr>
        <td><input type="checkbox" class="selectBox" value="${o.orderId}" /></td>
        <td>${idx}</td>
        <td>${o.orderDate}</td>
        <td>${o.orderNum || '-'}</td>
        <td>${o.companyName}</td>
        <td>${o.orderQty}</td>
        <td>${o.amount}원</td>
        <td>${o.companyEmpName || '-'}</td>
        <td>${o.empName || '-'}</td>
        <td>
          <button class="btn btn-info btn-sm detailBtn" data-id="${o.orderId}">상세</button>
        </td>
        <td>
          <button class="btn btn-warning btn-sm editBtn" data-id="${o.orderId}">수정</button>
        </td>
        <td>
          ${shipmentBtnHtml}
        </td>
      </tr>`;

    $tbody.append(row);
  });

  // 기존에 Detail/수정 버튼 바인딩 로직을 그대로 유지
  $('.detailBtn').off('click').on('click', function() {
    const orderId = $(this).data('id');
    $.get(`/api/orders/${orderId}`, populateDetailModal);
  });
  $('.editBtn').off('click').on('click', function() {
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
  }	// order.js (혹은 shipment-tabs.js 등 실제 사용하는 파일)
	document
	  .getElementById('orderTableBody')
	  .addEventListener('click', async (event) => {
	    // 1) 클릭된 요소가 .shipmentBtn 인지 확인
	    if (!event.target.classList.contains('shipmentBtn')) return;

	    // 2) 주문 ID
	    const orderId = Number(event.target.dataset.id);

	    try {
	      // 3) 서버에서 “해당 주문 상품별 창고/재고 정보” 가져오기
	      const response = await fetch(`/api/orders/${orderId}/shipment-info`);
	      if (!response.ok) {
	        throw new Error(`서버 오류: HTTP ${response.status}`);
	      }

	      // 4) JSON 파싱
	      const rawData = await response.json();
	      console.log("🚀 서버 응답 rawData:", rawData);

	      // ────────────────────────────────────────────────────────────
	      // 여기까지 왔다는 것은 서버가 “orderLineItem 별 재고” 정보를 내려준 상태입니다.
	      // rawData는 flat list 형태이며, 예시 한 행(row)의 형태는 아래와 같습니다:
	      // {
	      //   orderLineItemId: 10,
	      //   itemId: 5,
	      //   itemCode: "PRD001",
	      //   itemName: "휴대용 선풍기",
	      //   orderQty: 7,           // '이 주문상품의 주문 수량'
	      //   warehouseId: 2,
	      //   warehouseName: "완제품 창고",
	      //   stockQty: 3            // 이 창고에 남아 있는 재고 수량
	      // }
	      //
	      //    → rawData 배열에는 여러 창고(row)가 섞여 있을 수 있습니다.
	      //    → orderLineItemId가 동일한 row들을 모아서 “하나의 주문상품”에
	      //      여러 창고의 stockQty 정보를 합산해야 합니다.
	      // ────────────────────────────────────────────────────────────

	      // 5)  rawData가 비어 있으면 “아무 재고도 없이 빈 배열([])이 온 경우”이므로
	      //     바로 MRP 페이지로 이동하도록 합니다.
	      if (Array.isArray(rawData) && rawData.length === 0) {
	        alert("– 현재 재고가 전혀 없습니다.\nMRP 페이지로 이동합니다.");
	        window.location.href = '/mrp/mrp';
	        return;
	      }
	      if (!Array.isArray(rawData) && typeof rawData === 'object' && Object.keys(rawData).length === 0) {
	        alert("– 현재 재고가 전혀 없습니다.\nMRP 페이지로 이동합니다.");
	        window.location.href = '/mrp/mrp';
	        return;
	      }

	      // 6) rawData를 orderLineItemId 별로 그룹핑하여 dataMap 생성
	      //    → key: orderLineItemId (예: "10"), value: [ { … }, { … }, … ]
	      const dataMap = {};
	      rawData.forEach(wsi => {
	        const oliId = wsi.orderLineItemId;
	        if (!dataMap[oliId]) dataMap[oliId] = [];
	        dataMap[oliId].push(wsi);
	      });
	      console.log("🚀 그룹핑된 dataMap:", dataMap);

	      // 7) “MRP 이동 판정”: 
	      //    dataMap의 각 그룹(주문상품별)마다 재고합계 < 주문수량인지 검사
	      for (const [oliIdStr, warehouseList] of Object.entries(dataMap)) {
	        // 7-1) 주문수량 (모든 warehouseList 요소에서 동일하다고 가정)
	        const requiredQty = warehouseList[0].orderQty;

	        // 7-2) 해당 주문상품(주문라인)에 남아있는 총 재고합계를 계산
	        const totalStock = warehouseList.reduce((sum, wsi) => {
	          return sum + (wsi.stockQty || 0);
	        }, 0);

	        // 7-3) 재고<주문수량 이면 MRP 페이지로 이동
	        if (totalStock < requiredQty) {
	          alert(
	            `주문 상품(${warehouseList[0].itemCode} : ${warehouseList[0].itemName})의\n` +
	            `총 재고(${totalStock}개)가 주문 수량(${requiredQty}개)보다 적습니다.\n` +
	            `MRP 페이지로 이동합니다.`
	          );
	          window.location.href = '/mrp/mrp';
	          return; // 더 이상 모달을 띄우지 않고 함수 종료
	        }
	      }

	      // ────────────────────────────────────────────────────────────
	      // 8) 모든 주문상품에 대해 “재고 합계 >= 주문수량”이라면
	      //    아래 코드를 실행하여 출고 모달을 띄워줍니다.
	      // ────────────────────────────────────────────────────────────

	      // 8-1) <tbody> 비우기
	      const tbody = document.getElementById('shipmentItemTableBody');
	      tbody.innerHTML = '';

	      // 8-2) dataMap을 itemId나 orderLineItemId 별로 다시 한 번 순회하며 테이블 행 생성
	      //      예시에서는 orderLineItemId 기반으로 했으니, 그대로 진행합니다.
	      Object.entries(dataMap).forEach(([oliIdStr, warehouseList]) => {
	        // 하나의 주문상품(orderLineItem) 대표 정보 가져오기
	        const first = warehouseList[0];
	        const itemCode = first.itemCode;
	        const itemName = first.itemName;
	        const orderQty = first.orderQty; // 주문수량

	        // <tr> 생성
	        const tr = document.createElement('tr');

	        // ─── 품목 코드 셀
	        const tdCode = document.createElement('td');
	        tdCode.innerText = itemCode;
	        tr.appendChild(tdCode);

	        // ─── 품목 이름 셀
	        const tdName = document.createElement('td');
	        tdName.innerText = itemName;
	        tr.appendChild(tdName);

	        // ─── 주문 수량 셀
	        const tdOrderQty = document.createElement('td');
	        tdOrderQty.innerText = orderQty;
	        tr.appendChild(tdOrderQty);

	        // ─── 창고별 출고 수량 입력 셀
	        const tdWarehouseArea = document.createElement('td');
	        const container = document.createElement('div');
	        container.classList.add('warehouse-rows-container');

	        // 최초 1개의 “창고 입력 줄(wrapper)” 생성
	        const firstRow = createWarehouseRow(Number(oliIdStr), warehouseList);
	        container.appendChild(firstRow);

	        // “➕ 행 추가” 버튼
	        const addBtn = document.createElement('button');
	        addBtn.type = 'button';
	        addBtn.classList.add('btn', 'btn-sm', 'btn-outline-secondary', 'ms-2');
	        addBtn.innerText = '➕';
	        addBtn.title = '다른 창고도 추가 등록';
	        addBtn.addEventListener('click', () => {
	          const newRow = createWarehouseRow(Number(oliIdStr), warehouseList);
	          container.appendChild(newRow);
	        });

	        tdWarehouseArea.appendChild(container);
	        tdWarehouseArea.appendChild(addBtn);
	        tr.appendChild(tdWarehouseArea);

	        // 완성된 <tr>을 <tbody>에 붙이기
	        tbody.appendChild(tr);
	      });

	      // 9) 모달 띄우기 전, data-order-id 속성 설정
	      const modalEl = document.getElementById('shipmentModal');
	      modalEl.setAttribute('data-order-id', orderId);

	      // 10) 모달 띄우기
	      const modalInstance = new bootstrap.Modal(modalEl);
	      modalInstance.show();

	    } catch (err) {
	      console.error('❌ 출고 정보를 불러오는 데 실패했습니다.', err);
	      alert('❌ 출고 정보를 불러오는 데 실패했습니다.');
	    }
	  });

	/**
	 * createWarehouseRow 함수는 그대로 기존 로직을 사용하시면 됩니다.
	 * (orderLineItemId, warehouseList를 인자로 받아서 “숨김(hidden) input + 창고 select + 수량 input + ❌ 제거 버튼”을
	 *  가진 <div>를 반환하도록 이미 구현되어 있을 겁니다.)
	 */
