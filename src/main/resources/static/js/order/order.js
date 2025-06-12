// ==========================
// 주문+출고 통합 JS파일
// ==========================

// ===== 전역 변수 =====
let ALL_ITEMS = [];        // 품목 목록
let ALL_WAREHOUSES = [];   // 창고 목록

// ===== 숫자 포맷 함수 =====
function formatNumber(num) {
  if (num == null || num === "") return "";
  return Math.floor(Number(num)).toLocaleString('ko-KR');
}

// ===== 초기 로딩 =====
$(document).ready(() => {
  loadCompanies();
  loadProductItems(() => {
    loadWarehouses(() => {
      fetchOrders(0);
    });
  });
  bindGlobalEventHandlers();
});

// ===== 고객사/품목/창고 목록 불러오기 =====
function loadCompanies() {
  $.get('/api/companies', data => {
    const $select = $('#companySelect, #searchCompanySelect, #fixedCompanySelect');
    $select.empty().append('<option value="">전체</option>');
    data.forEach(c => $select.append(`<option value="${c.companyId}">${c.companyName}</option>`));
  });
}
function loadProductItems(callback) {
  $.get('/api/items?type=product', data => {
    ALL_ITEMS = data;
    if (callback) callback();
  });
}
function loadWarehouses(callback) {
  $.get('/api/warehouses/all-simple', data => {
    if (!Array.isArray(data)) return alert('창고 목록 불러오기 실패');
    ALL_WAREHOUSES = data.filter(w => w.warehouseType === 'PRODUCT');
    if (callback) callback();
  });
}

// ===== 버튼/이벤트 핸들러 등록 =====
function bindGlobalEventHandlers() {
  $('#openRegisterModal').click(handleOpenRegisterModal);
  $('#companySelect').change(handleCompanyChange);
  $('#addItem').click(addItemRow);
  $('#selectAll').change(toggleSelectAll);
  $('#deleteSelected').click(deleteSelectedOrders);
  $('#filterForm').submit(e => { e.preventDefault(); fetchOrders(0); });
  $('#fixedaddItem').off('click').on('click', () => {
    addFixedItemRow();
    calcFixedTotalAmount();
  });
  $('#excelDownloadBtn').on('click', downloadExcelOrderList);

  // 출고등록(동적 버튼)
  $(document).on('click', '.shipmentBtn', openShipmentModal);

  // 출고등록 저장버튼(모달)
  $(document).on('click', '#shipmentSaveBtn', saveShipment);
}

// ===== 주문등록 모달 오픈 =====
function handleOpenRegisterModal() {
  $('#registerForm')[0].reset();
  $('#itemsTable tbody').empty();
  $('#totalAmount').text('0');
  addItemRow();
  $('#registerModal').modal('show');
  $('#saveOrder').off('click').on('click', saveOrder);
}

// ===== 고객사 변경시 담당자/거래처담당자 자동 =====
function handleCompanyChange() {
  const companyId = $(this).val();
  if (!companyId) {
    $('#empName').val('');
    $('#companyEmpName').val('');
    return;
  }
  $.get(`/api/companies/${companyId}/info`, data => {
    $('#empName').val(data.empName || '');
    $('#companyEmpName').val(data.companyEmpName || '');
  });
}

// ===== 주문 저장 =====
function saveOrder() {
  const companyId = $('#companySelect').val();
  const orderDate = $('input[name="orderDate"]').val();
  const empName = $('#empName').val();
  const companyEmpName = $('#companyEmpName').val();
  if (!companyId) return alert('고객사를 선택하세요.');
  if (!orderDate) return alert('주문일자를 입력하세요.');
  if (!empName) return alert('담당자가 없습니다. 고객사를 다시 선택하세요.');
  if (!companyEmpName) return alert('거래처 담당자가 없습니다. 고객사를 다시 선택하세요.');

  const itemRows = $('#itemsTable tbody tr');
  if (itemRows.length === 0) return alert('품목을 1개 이상 추가하세요.');

  let valid = true;
  itemRows.each(function() {
    const itemId = $(this).find('.itemSelect').val();
    const unit = $(this).find('.unit').val();
    const unitPrice = $(this).find('.unitPrice').val();
    const unitQty = $(this).find('.unitQty').val();
    if (!itemId || !unit || !unitPrice || !unitQty) valid = false;
  });
  if (!valid) return alert('품목 정보(품목/단가/수량)를 모두 입력하세요.');

  const orderItems = [];
  itemRows.each(function() {
    const itemId = $(this).find('.itemSelect').val();
    const unit = $(this).find('.unit').val();
    const unitPrice = $(this).find('.unitPrice').val();
    const unitQty = $(this).find('.unitQty').val();
    orderItems.push({
      itemId: Number(itemId), unit, unitPrice: Number(unitPrice), unitQty: Number(unitQty)
    });
  });

  const orderData = {
    companyId: Number(companyId),
    orderDate, empName, companyEmpName,
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
    error: function() { alert('저장 중 오류 발생'); }
  });
}

// ===== 품목 행 추가 =====
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

// ===== 품목행 입력시 자동처리 =====
$('#itemsTable').on('change', '.itemSelect', function() {
  const $tr = $(this).closest('tr');
  const opt = $(this).find('option:selected');
  $tr.find('.unit').val(opt.data('unit'));
  $tr.find('.unitPrice').val(opt.data('price'));
  $tr.find('.unitQty').val(1);
  calculateRowTotal($tr); calculateTotalAmount();
});
$('#itemsTable').on('input', '.unitQty', function() {
  const $tr = $(this).closest('tr');
  calculateRowTotal($tr); calculateTotalAmount();
});
$('#itemsTable').on('click', '.removeItem', function() {
  $(this).closest('tr').remove();
  calculateTotalAmount();
});
function calculateRowTotal($tr) {
  const qty = parseFloat($tr.find('.unitQty').val()) || 0;
  const price = parseFloat($tr.find('.unitPrice').val()) || 0;
  $tr.find('.total').val(formatNumber(qty * price));
}
function calculateTotalAmount() {
  let total = 0;
  $('#itemsTable tbody tr').each(function() {
    total += Number(String($(this).find('.total').val()).replace(/,/g,"")) || 0;
  });
  $('#totalAmount').text(formatNumber(total));
}

// ===== 체크박스/삭제/목록조회/상세/수정 =====
function toggleSelectAll() { $('.selectBox').prop('checked', this.checked); }
function deleteSelectedOrders() {
  const ids = $('.selectBox:checked').map((i, el) => el.value).get();
  if (ids.length === 0) return alert('삭제할 주문을 선택하세요.');
  if (!confirm('정말 삭제하시겠습니까?')) return;
  fetch('/api/orders', {
    method: 'DELETE', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(ids)
  })
  .then(response => {
    if (!response.ok) return response.text().then(msg => { throw new Error(msg); });
    return response.text();
  })
  .then(msg => { alert('✅ ' + msg); fetchOrders(0); })
  .catch(err => { alert('❌ ' + err.message); });
}
function fetchOrders(page = 0) {
  const params = new URLSearchParams({
    companyId: $('#searchCompanySelect').val(),
    itemId: $('#searchItemSelect').val(),
    fromDate: $('input[name="fromDate"]').val(),
    toDate: $('input[name="toDate"]').val(),
    page, size: 10, sort: 'orderDate,asc'
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
    const shipmentBtnHtml = o.allShipped
      ? '<span style="font-weight:bold; color:#007bff;">출고완료</span>'
      : `<button type="button" class="btn btn-success btn-sm shipmentBtn" data-id="${o.orderId}">출고 등록</button>`;
    const row = `
      <tr>
        <td><input type="checkbox" class="selectBox" value="${o.orderId}" /></td>
        <td>${idx}</td>
        <td>${o.orderDate}</td>
        <td>${o.orderNum || '-'}</td>
        <td>${o.companyName}</td>
        <td>${formatNumber(o.orderQty)}</td>
        <td>${formatNumber(o.amount)}원</td>
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
  // 상세/수정 버튼 이벤트
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
  const $tbody = $('#detailItemsTable tbody').empty();
  data.items.forEach(item => {
    const total = item.unitQty * item.unitPrice;
    const row = `
      <tr>
        <td>${item.itemName}</td>
        <td>${item.unit}</td>
        <td>${formatNumber(item.unitPrice)}</td>
        <td>${formatNumber(item.unitQty)}</td>
        <td>${formatNumber(total)}</td>
      </tr>
    `;
    $tbody.append(row);
  });
  const modalEl = document.getElementById('detailModal');
  const modal = new bootstrap.Modal(modalEl);
  modal.show();
}

// ===== 주문수정 모달 및 기능 =====
function populateFixedForm(orderData) {
  $('#fixedOrderId').val(orderData.orderId);
  $('#fixedCompanySelect').val(orderData.companyId);
  $('#fixedOrderDate').val(orderData.orderDate);
  $('#fixedEmpName').val(orderData.empName || '');
  $('#fixedCompanyEmpName').val(orderData.companyEmpName || '');
  $('#fixedRemark').val(orderData.remark || '');
  const $tbody = $('#fixedItemsTable tbody').empty();
  (orderData.items || []).forEach(item => addFixedItemRow(item));
  calcFixedTotalAmount();
  $tbody.off('click', '.fixedRemoveItem').on('click', '.fixedRemoveItem', function() {
    $(this).closest('tr').remove(); calcFixedTotalAmount();
  });
  $tbody.off('change', '.fixedItemSelect').on('change', '.fixedItemSelect', function() {
    const $tr = $(this).closest('tr');
    const opt = $(this).find('option:selected');
    $tr.find('.fixedUnit').val(opt.data('unit'));
    $tr.find('.fixedUnitPrice').val(opt.data('price'));
    $tr.find('.fixedUnitQty').val(1);
    $tr.find('.fixedTotal').val(formatNumber(opt.data('price')));
    calcFixedTotalAmount();
  });
  $tbody.off('input', '.fixedUnitQty').on('input', '.fixedUnitQty', function() {
    const $tr = $(this).closest('tr');
    const qty = parseFloat($tr.find('.fixedUnitQty').val()) || 0;
    const price = parseFloat($tr.find('.fixedUnitPrice').val()) || 0;
    $tr.find('.fixedTotal').val(formatNumber(qty * price));
    calcFixedTotalAmount();
  });
  $('#fixedModal').modal('show');
}
function addFixedItemRow(item) {
  const idx = $('#fixedItemsTable tbody tr').length;
  const tr = $(`
    <tr data-index="${idx}">
      <td>
        <select class="form-select fixedItemSelect" required>
          <option value="">선택</option>
        </select>
      </td>
      <td><input type="text" class="form-control fixedUnit" readonly></td>
      <td><input type="number" class="form-control fixedUnitPrice" readonly></td>
      <td><input type="number" class="form-control fixedUnitQty" value="1" required></td>
      <td><input type="text" class="form-control fixedTotal" readonly></td>
      <td><button type="button" class="btn btn-danger fixedRemoveItem">삭제</button></td>
    </tr>
  `);
  const $sel = tr.find('.fixedItemSelect');
  ALL_ITEMS.forEach(i => {
    $sel.append(`<option value="${i.id}" data-unit="${i.unit}" data-price="${i.itemPrice}">${i.itemName}</option>`);
  });
  if (item) {
    $sel.val(item.itemId);
    tr.find('.fixedUnit').val(item.unit);
    tr.find('.fixedUnitPrice').val(item.unitPrice);
    tr.find('.fixedUnitQty').val(item.unitQty);
    tr.find('.fixedTotal').val(formatNumber(item.unitQty * item.unitPrice));
  }
  $('#fixedItemsTable tbody').append(tr);
}
function calcFixedTotalAmount() {
  let total = 0;
  $('#fixedItemsTable tbody tr').each(function() {
    total += Number(String($(this).find('.fixedTotal').val()).replace(/,/g,"")) || 0;
  });
  $('#totalAmount').text(formatNumber(total));
}
$('#updateOrder').off('click').on('click', function() {
  const orderId = Number($('#fixedOrderId').val());
  const companyId = Number($('#fixedCompanySelect').val());
  const orderDate = $('#fixedOrderDate').val();
  const empName = $('#fixedEmpName').val();
  const companyEmpName = $('#fixedCompanyEmpName').val();
  const remark = $('#fixedRemark').val();
  const orderType = $('input[name="orderType"]').val();
  const itemType = $('input[name="itemType"]').val();
  const items = [];
  $('#fixedItemsTable tbody tr').each(function() {
    const itemId = Number($(this).find('.fixedItemSelect').val());
    const unit = $(this).find('.fixedUnit').val();
    const unitPrice = Number($(this).find('.fixedUnitPrice').val());
    const unitQty = Number($(this).find('.fixedUnitQty').val());
    items.push({ itemId, unit, unitPrice, unitQty });
  });
  const updatedOrder = { companyId, orderDate, empName, companyEmpName, remark, orderType, itemType, items };
  $.ajax({
    url: '/api/orders/' + orderId,
    method: 'PUT',
    contentType: 'application/json',
    data: JSON.stringify(updatedOrder),
    success: function() {
      alert('주문이 수정되었습니다.');
      $('#fixedModal').modal('hide');
      fetchOrders(0);
    },
    error: function(xhr) { alert('수정 중 오류: ' + (xhr.responseText || '')); }
  });
});

// ==============================
// ===== 출고등록 모달 관련 =====
// ==============================

/**
 * 출고등록 버튼 클릭시: 출고모달 오픈 + 데이터 바인딩
 */
async function openShipmentModal(e) {
  e.preventDefault();
  const orderId = Number($(this).data('id'));
  try {
    const response = await fetch(`/api/orders/${orderId}/shipment-info`);
    if (!response.ok) throw new Error(`서버 오류: HTTP ${response.status}`);
    const rawData = await response.json();
    if ((Array.isArray(rawData) && rawData.length === 0) ||
        (!Array.isArray(rawData) && typeof rawData === 'object' && Object.keys(rawData).length === 0)) {
      alert("– 현재 재고가 전혀 없습니다.\nMRP 페이지로 이동합니다.");
      window.location.href = '/mrp/mrp';
      return;
    }
    // 그룹핑
    const dataMap = {};
    rawData.forEach(wsi => {
      const oliId = wsi.orderLineItemId;
      if (!dataMap[oliId]) dataMap[oliId] = [];
      dataMap[oliId].push(wsi);
    });
    // 재고부족 체크
    for (const [oliIdStr, warehouseList] of Object.entries(dataMap)) {
      const requiredQty = warehouseList[0].orderQty;
      const totalStock = warehouseList.reduce((sum, wsi) => sum + (wsi.stockQty || 0), 0);
      if (totalStock < requiredQty) {
        alert(
          `주문 상품(${warehouseList[0].itemCode} : ${warehouseList[0].itemName})의\n` +
          `총 재고(${formatNumber(totalStock)}개)가 주문 수량(${formatNumber(requiredQty)}개)보다 적습니다.\n` +
          `MRP 페이지로 이동합니다.`
        );
        window.location.href = '/mrp/mrp';
        return;
      }
    }
    // 테이블 비우고 행 생성
    const tbody = document.getElementById('shipmentItemTableBody');
    tbody.innerHTML = '';
    Object.entries(dataMap).forEach(([oliIdStr, warehouseList]) => {
      const first = warehouseList[0];
      const itemCode = first.itemCode;
      const itemName = first.itemName;
      const orderQty = first.orderQty;
      const tr = document.createElement('tr');
      // 품목코드
      const tdCode = document.createElement('td');
      tdCode.innerText = itemCode;
      tr.appendChild(tdCode);
      // 품목명
      const tdName = document.createElement('td');
      tdName.innerText = itemName;
      tr.appendChild(tdName);
      // 주문수량
      const tdOrderQty = document.createElement('td');
      tdOrderQty.innerText = formatNumber(orderQty);
      tr.appendChild(tdOrderQty);
      // 창고/수량
      const tdWarehouseArea = document.createElement('td');
      const container = document.createElement('div');
      container.classList.add('warehouse-rows-container');
      const firstRow = createWarehouseRow(Number(oliIdStr), warehouseList);
      container.appendChild(firstRow);
      // ➕ 행 추가
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
      tbody.appendChild(tr);
    });
    // 모달 data-order-id 설정 + 띄우기
    const modalEl = document.getElementById('shipmentModal');
    modalEl.setAttribute('data-order-id', orderId);
    const modalInstance = new bootstrap.Modal(modalEl);
    modalInstance.show();
  } catch (err) {
    alert('❌ 출고 정보를 불러오는 데 실패했습니다.');
  }
}

/**
 * 창고-수량 입력 한 줄 생성
 */
function createWarehouseRow(itemId, warehouseList) {
  const wrapper = document.createElement('div');
  wrapper.classList.add('mb-2', 'd-flex', 'align-items-center', 'warehouse-row');
  const hiddenOrderLineItem = document.createElement('input');
  hiddenOrderLineItem.type = 'hidden';
  hiddenOrderLineItem.value = warehouseList[0].orderLineItemId;
  hiddenOrderLineItem.setAttribute('data-field', 'orderLineItemId');
  wrapper.appendChild(hiddenOrderLineItem);
  // 창고 select
  const warehouseSelect = document.createElement('select');
  warehouseSelect.classList.add('form-select', 'form-select-sm', 'me-2');
  warehouseSelect.style.minWidth = '200px';
  warehouseSelect.setAttribute('data-field', 'warehouseId');
  warehouseList.forEach(wsi => {
    const opt = document.createElement('option');
    opt.value = wsi.warehouseId;
    opt.textContent = `${wsi.warehouseName} (재고: ${formatNumber(wsi.stockQty)})`;
    warehouseSelect.appendChild(opt);
  });
  wrapper.appendChild(warehouseSelect);
  // 수량
  const qtyInput = document.createElement('input');
  qtyInput.type = 'number';
  qtyInput.min = '0';
  qtyInput.value = '0';
  qtyInput.classList.add('form-control', 'form-control-sm', 'me-2');
  qtyInput.setAttribute('data-field', 'quantity');
  wrapper.appendChild(qtyInput);
  // 삭제
  const removeBtn = document.createElement('button');
  removeBtn.type = 'button';
  removeBtn.classList.add('btn', 'btn-sm', 'btn-outline-danger');
  removeBtn.innerText = '❌';
  removeBtn.title = '이 줄 제거';
  removeBtn.addEventListener('click', () => { wrapper.remove(); });
  wrapper.appendChild(removeBtn);
  return wrapper;
}

/**
 * 출고등록 저장(POST)
 */
function saveShipment() {
  // 모달에서 orderId 읽기
  const modal = document.getElementById('shipmentModal');
  const orderId = Number(modal.getAttribute('data-order-id'));
  if (!orderId) return alert('주문ID가 없습니다. 다시 시도하세요.');

  // 각 행의 출고 정보 수집
  const data = [];
  $('#shipmentItemTableBody tr').each(function() {
    $(this).find('.warehouse-rows-container > .warehouse-row').each(function() {
      const $wr = $(this);
      const quantity = Number($wr.find('[data-field="quantity"]').val());
      if (quantity > 0) {
        data.push({
          orderLineItemId: $wr.find('[data-field="orderLineItemId"]').val(),
          warehouseId: $wr.find('[data-field="warehouseId"]').val(),
          quantity
        });
      }
    });
  });

  if (data.length === 0) return alert('출고수량을 1개 이상 입력하세요.');

  // === 출고등록 API 호출 === (백엔드가 요구하는 구조로)
  const payload = {
    orderId: orderId,
    shipmentItems: data
  };

  $.ajax({
    url: '/api/shipments',
    method: 'POST',
    contentType: 'application/json',
    data: JSON.stringify(payload),
    success: function() {
      alert('출고 등록 완료!');
      $('#shipmentModal').modal('hide');
      fetchOrders(0); // 목록 새로고침
    },
    error: function(xhr) {
      alert('출고 등록 오류: ' + (xhr.responseText || ''));
    }
  });
}

// ================================
// 주문목록 엑셀 다운로드(SheetJS 필요)
// ================================
function downloadExcelOrderList() {
  const table = document.getElementById('orderTableBody');
  const rows = Array.from(table.querySelectorAll('tr'));
  const data = [
    ['No', '주문일자', '주문번호', '고객사', '주문수량', '금액', '거래처담당자', '담당자']
  ];
  rows.forEach(tr => {
    const tds = tr.querySelectorAll('td');
    data.push([
      tds[1]?.innerText,
      tds[2]?.innerText,
      tds[3]?.innerText,
      tds[4]?.innerText,
      tds[5]?.innerText,
      tds[6]?.innerText,
      tds[7]?.innerText,
      tds[8]?.innerText
    ]);
  });
  const ws = XLSX.utils.aoa_to_sheet(data);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, '주문목록');
  XLSX.writeFile(wb, `주문목록_${new Date().toISOString().slice(0,10)}.xlsx`);
}

