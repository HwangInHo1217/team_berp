// File: src/main/resources/static/js/order/order.js
/**
 * order.js
 * - Bootstrap Modal 제어 (th:replace 로 로드된 모달 열기/닫기)
 * - CRUD API 호출 (fetch)
 * - 필터/페이징/테이블 렌더링
 * - 동적 Row 추가·삭제 및 합계 계산
 */

document.addEventListener('DOMContentLoaded', () => {
  loadOrders(0);            // 1) 첫 페이지 불러오기
  initRegisterForm();       // 2) 등록 폼 초기 행 셋팅
});

/** ── 1) 주문 목록 + 테이블·페이징 렌더링 ── */
function loadOrders(page) {
  const params = new URLSearchParams({
    page,
    companyName: document.getElementById('filter-company').value || '',
    dateFrom:    document.getElementById('filter-date-from').value || '',
    dateTo:      document.getElementById('filter-date-to').value || ''
  });
  fetch(`/api/orders?${params}`)
    .then(r => r.json())
    .then(dto => renderTable(dto, page))
    .catch(console.error);
}

function renderTable(pageDto, currentPage) {
  const tbody = document.getElementById('orderTableBody');
  tbody.innerHTML = '';
  pageDto.content.forEach((o, i) => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><input type="checkbox" value="${o.orderNum}" /></td>
      <td>${currentPage * pageDto.size + i + 1}</td>
      <td>${o.orderDate}</td>
      <td>${o.orderNum}</td>
      <td>${o.companyName}</td>
      <td>${o.items.length}</td>
      <td>${o.totalPrice.toLocaleString()}</td>
      <td>${o.manager}</td>
      <td>${o.note||''}</td>
      <td><button class="btn btn-info btn-sm"    onclick="openDetail(${o.orderNum})">상세</button></td>
      <td><button class="btn btn-warning btn-sm" onclick="openEdit(${o.orderNum})">수정</button></td>
      <td><button class="btn btn-success btn-sm" onclick="openShipment(${o.orderNum})">출고등록</button></td>
    `;
    tbody.appendChild(tr);
  });

  // 페이징
  const nav = document.getElementById('pagination');
  nav.innerHTML = '';
  for (let i = 0; i < pageDto.totalPages; i++) {
    const btn = document.createElement('button');
    btn.className = `btn btn-sm btn-outline-primary me-1 ${i===currentPage?'active':''}`;
    btn.textContent = i+1;
    btn.onclick = () => loadOrders(i);
    nav.appendChild(btn);
  }
}

/** ── 2) 등록 폼 초기화 ── */
function initRegisterForm() {
  clearItems('reg-items-body');
  addOrderItemRow();          // 기본 1행 추가
}

/** ── 3) 모달 열기/닫기 공통 ── */
function openRegisterModal() {
  initRegisterForm();
  new bootstrap.Modal(document.getElementById('orderRegisterModal')).show();
}
function openDetail(orderNum) {
  fetch(`/api/orders/${orderNum}`)
    .then(r => r.json())
    .then(populateDetailModal)
    .then(() => new bootstrap.Modal(document.getElementById('orderDetailModal')).show());
}
function openEdit(orderNum) {
  fetch(`/api/orders/${orderNum}`)
    .then(r => r.json())
    .then(populateUpdateModal)
    .then(() => new bootstrap.Modal(document.getElementById('orderEditModal')).show());
}
function closeModal() {
  document.querySelectorAll('.modal').forEach(m => bootstrap.Modal.getOrCreateInstance(m).hide());
}

/** ── 4) 고객사 변경 시 담당자 자동 채움 ── */
function onCompanyChange(sel) {
  const prefix = sel.id.startsWith('reg') ? 'reg' : 'upd';
  fetch(`/api/orders/company/${sel.value}`)
    .then(r => r.json())
    .then(data => {
      document.getElementById(`${prefix}-manager`).value      = data.empName;
      document.getElementById(`${prefix}-comp-emp`)?.remove(); // 거래처 담당자는 필요 시 확장
    })
    .catch(console.error);
}

/** ── 5) 상세 모달 데이터 채우기 ── */
function populateDetailModal(dto) {
  document.getElementById('det-num').textContent     = dto.orderNum;
  document.getElementById('det-company').textContent = dto.companyName;
  document.getElementById('det-date').textContent    = dto.orderDate;
  document.getElementById('det-manager').textContent = dto.manager;
  document.getElementById('det-note').textContent    = dto.note||'';

  const tb = document.getElementById('orderDetailItems');
  tb.innerHTML = '';
  dto.items.forEach(item => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${item.itemCode}</td>
      <td>${item.itemName}</td>
      <td>${item.quantity}</td>
      <td>${item.unit}</td>
      <td>${item.unitPrice.toLocaleString()}</td>
      <td>${(item.quantity * item.unitPrice).toLocaleString()}</td>
    `;
    tb.appendChild(tr);
  });
}

/** ── 6) 수정 모달 데이터 채우기 ── */
function populateUpdateModal(dto) {
  document.getElementById('edit-order-num').value    = dto.orderNum;
  document.getElementById('upd-customer').value      = dto.customerId;
  document.getElementById('upd-order-date').value    = dto.orderDate;
  document.getElementById('upd-manager').value       = dto.manager;
  document.getElementById('upd-note').value          = dto.note||'';

  clearItems('edit-items-body');
  dto.items.forEach(item => addOrderItemRow(item));
}

/** ── 7) 동적 행 추가·삭제 및 합계 계산 ── */
function clearItems(tbodyId) {
  document.getElementById(tbodyId).innerHTML = '';
}
function addOrderItemRow(data={}) {
  const tbody = data.quantity!==undefined
    ? document.getElementById('edit-items-body')
    : document.getElementById('reg-items-body');

  const tr = document.createElement('tr');
  tr.innerHTML = `
    <td>
      <select name="itemId" class="form-select" required oninput="onItemChange(this)">
        <option value="">-- 선택 --</option>
        <option value="P001">P001 - 완제품 A</option>
        <option value="P002">P002 - 완제품 B</option>
      </select>
    </td>
    <td><input name="quantity"    class="form-control" type="number" oninput="calcRowTotal(this)" required /></td>
    <td><input name="unit"        class="form-control" readonly value="EA" /></td>
    <td><input name="unitPrice"   class="form-control" type="number" oninput="calcRowTotal(this)" required /></td>
    <td><input name="totalPrice"  class="form-control" readonly value="0" /></td>
    <td><button class="btn btn-danger btn-sm" onclick="this.closest('tr').remove()">삭제</button></td>
  `;
  if (data.quantity !== undefined) {
    tr.querySelector('[name=itemId]').value      = data.itemId;
    tr.querySelector('[name=quantity]').value    = data.quantity;
    tr.querySelector('[name=unit]').value        = data.unit;
    tr.querySelector('[name=unitPrice]').value   = data.unitPrice;
    tr.querySelector('[name=totalPrice]').value  = (data.quantity*data.unitPrice).toLocaleString();
  }
  tbody.appendChild(tr);
}

function onItemChange(sel) {
  fetch(`/api/orders/item/${sel.value}`)
    .then(r=>r.json())
    .then(d=> sel.closest('tr').querySelector('[name=unit]').value=d.unit)
    .catch(console.error);
}
function calcRowTotal(el) {
  const tr = el.closest('tr');
  const qty = +tr.querySelector('[name=quantity]').value || 0;
  const pr  = +tr.querySelector('[name=unitPrice]').value|| 0;
  tr.querySelector('[name=totalPrice]').value = (qty*pr).toLocaleString();
}

/** ── 8) CRUD API 호출 ── */
function registerOrder() {
  const payload = collectForm('formRegister');
  fetch('/api/orders', {
    method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(payload)
  })
    .then(()=>{ closeModal(); loadOrders(0); })
    .catch(console.error);
}

function updateOrder() {
  const num     = document.getElementById('edit-order-num').value;
  const payload = collectForm('formEdit');
  fetch(`/api/orders/${num}`, {
    method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify(payload)
  })
    .then(()=>{ closeModal(); loadOrders(0); })
    .catch(console.error);
}

function deleteSelected() {
  const sel = Array.from(document.querySelectorAll('#orderTableBody input[type=checkbox]:checked'))
                   .map(cb=>+cb.value);
  if(!sel.length) return alert('선택된 주문이 없습니다.');
  if(!confirm('삭제하시겠습니까?')) return;
  fetch('/api/orders',{method:'DELETE',headers:{'Content-Type':'application/json'},body:JSON.stringify(sel)})
    .then(()=>loadOrders(0)).catch(console.error);
}

/** ── 9) form 데이터를 payload 객체로 변환 ── */
function collectForm(formId) {
  const fm  = document.getElementById(formId);
  const obj = {};
  const items = [];
  const rows = fm.querySelectorAll('tbody tr');
  rows.forEach(tr => {
      const qty = +tr.querySelector('[name=quantity]').value || 0;
      const price = +tr.querySelector('[name=unitPrice]').value || 0;
      items.push({
        itemId:       tr.querySelector('[name=itemId]').value,
        unitQty:      qty,
        unitPrice:    price,
        unitPriceAll: qty * price
      });
    });
  // 공통 필드
  obj.customerId = +fm.querySelector('[name=customerId]').value;
  obj.orderDate  = fm.querySelector('[name=orderDate]').value;
  obj.manager    = fm.querySelector('[name=manager]').value;
  obj.note       = fm.querySelector('[name=note]').value;
  obj.items      = items;
  return obj;
}
