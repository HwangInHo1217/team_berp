// File: team_berp/src/main/resources/static/js/order/order.js

// 주문 관리 스크립트

document.addEventListener('DOMContentLoaded', () => {

  loadFilterData();

  loadOrders(0);

});



// 모달 열기

function openRegister() {

  fetch('/order/fragments/registerModal')

    .then(res => res.text())

    .then(html => {

      document.getElementById('modal-container').innerHTML = html;

    });

}

function openUpdate(num) {

  fetchOrderDetail(num, html => {

    fetch('/order/fragments/updateModal')

      .then(res => res.text())

      .then(fragment => {

        document.getElementById('modal-container').innerHTML = fragment;

        populateUpdateModal(html);

      });

  });

}

function showDetail(num) {

  fetchOrderDetail(num, data => {

    fetch('/order/fragments/detailModal')

      .then(res => res.text())

      .then(fragment => {

        document.getElementById('modal-container').innerHTML = fragment;

        populateDetailModal(data);

      });

  });

}

function closeModal() {

  document.getElementById('modal-container').innerHTML = '';

}



// 고객사 변경 시 emp/companyEmp 자동 채움

function onCompanyChange(el) {

  fetch(`/api/orders/company/${encodeURIComponent(el.value)}`)

    .then(res => res.json())

    .then(data => {

      const prefix = el.id.startsWith('reg') ? 'reg' : 'upd';

      document.getElementById(`${prefix}-emp`).value = data.empName;

      document.getElementById(`${prefix}-comp-emp`).value = data.companyEmpName;

    });

}



// 행 추가

function addItemRow(bodyId) {

  const tbody = document.getElementById(bodyId);

  const idx = tbody.children.length;

  const row = document.createElement('tr');

  row.innerHTML = `

    <td>

      <select class="form-select form-select-sm item-select" onchange="onItemChange(this)">

        <option value="">선택</option>

      </select>

    </td>

    <td><input type="text" class="form-control form-control-sm unit-input" readonly></td>

    <td><input type="number" class="form-control form-control-sm price-input" oninput="calcRow(this)"></td>

    <td><input type="number" class="form-control form-control-sm qty-input" oninput="calcRow(this)"></td>

    <td><input type="number" class="form-control form-control-sm total-input" readonly></td>

    <td><button class="btn btn-sm btn-danger" onclick="this.closest('tr').remove()">삭제</button></td>

  `;

  tbody.appendChild(row);

  // 옵션 채우기

  document.querySelectorAll('.item-select').forEach(sel => {

    if (sel.children.length === 1) {

      fetch('/api/items')

        .then(res => res.json())

        .then(items => {

          items.forEach(i => {

            const opt = document.createElement('option');

            opt.value = i.name;

            opt.text = i.name;

            sel.append(opt);

          });

        });

    }

  });

}



// 품목 변경 시 단위 자동 채움

function onItemChange(el) {

  const unitInput = el.closest('tr').querySelector('.unit-input');

  fetch(`/api/items/${encodeURIComponent(el.value)}`)

    .then(res => res.json())

    .then(item => {

      unitInput.value = item.unit;

    });

}



// 단가·수량 변경 시 합계 계산

function calcRow(el) {

  const tr = el.closest('tr');

  const price = parseFloat(tr.querySelector('.price-input').value) || 0;

  const qty   = parseFloat(tr.querySelector('.qty-input').value)   || 0;

  tr.querySelector('.total-input').value = price * qty;

}



// 주문 등록

function registerOrder(e) {

  const form = e.target;

  const payload = collectFormData('reg');

  fetch('/api/orders', {

    method: 'POST',

    headers: {'Content-Type':'application/json'},

    body: JSON.stringify(payload)

  }).then(() => { closeModal(); loadOrders(0); });

}



// 주문 수정

function updateOrder(e) {

  const num = document.getElementById('det-num').textContent;

  const payload = collectFormData('upd');

  fetch(`/api/orders/${num}`, {

    method: 'PUT',

    headers: {'Content-Type':'application/json'},

    body: JSON.stringify(payload)

  }).then(() => { closeModal(); loadOrders(0); });

}



// 선택 삭제

function deleteSelected() {

  const nums = Array.from(document.querySelectorAll('input[name="chk"]:checked'))

    .map(chk => chk.value);

  fetch('/api/orders', {

    method: 'DELETE',

    headers: {'Content-Type':'application/json'},

    body: JSON.stringify(nums)

  }).then(() => loadOrders(0));

}



// 필터 데이터 로드

function loadFilterData() {

  // companies, items 는 서버에서 OrderController 에서 모델로 전달

}



// 주문 목록 로드

function loadOrders(page) {

  const params = new URLSearchParams();

  ['filter-company','filter-item','filter-date-from','filter-date-to']

    .forEach(id => {

      const v = document.getElementById(id).value;

      if (v) params.append(id.replace('filter-',''), v);

    });

  params.append('page', page);

  fetch(`/api/orders?${params.toString()}`)

    .then(res => res.json())

    .then(data => renderTable(data));

}



// 테이블 렌더링

function renderTable(dto) {

  const tbody = document.getElementById('order-table-body');

  tbody.innerHTML = '';

  dto.content.forEach((o,i) => {

    const tr = document.createElement('tr');

    tr.innerHTML = `

      <td><input type="checkbox" name="chk" value="${o.orderNum}"></td>

      <td>${i+1}</td>

      <td>${o.orderDate}</td>

      <td>${o.orderNum}</td>

      <td>${o.companyName}</td>

      <td>${o.orderQty}</td>

      <td>${o.amount}</td>

      <td>${o.companyEmpName}</td>

      <td>${o.empName}</td>

      <td><button class="btn btn-info btn-sm" onclick="showDetail(${o.orderNum})">상세</button></td>

      <td><button class="btn btn-warning btn-sm" onclick="openUpdate(${o.orderNum})">수정</button></td>

    `;

    tbody.append(tr);

  });

  // 페이징 렌더링...

}



// 상세 모달 채우기

function populateDetailModal(data) {

  document.getElementById('det-num').textContent = data.orderNum;

  document.getElementById('det-date').textContent = data.orderDate;

  document.getElementById('det-company').textContent = data.companyName;

  document.getElementById('det-comp-emp').textContent = data.companyEmpName;

  document.getElementById('det-emp').textContent = data.empName;

  document.getElementById('det-remark').textContent = data.note || '';



  const tb = document.getElementById('det-items');

  tb.innerHTML = '';

  data.items.forEach(li => {

    const row = document.createElement('tr');

    row.innerHTML = `<td>${li.itemName}</td>

                     <td>${li.unit}</td>

                     <td>${li.unitPrice}</td>

                     <td>${li.unitQty}</td>

                     <td>${li.unitPriceall}</td>`;

    tb.append(row);

  });

}



// 수정 모달 채우기

function populateUpdateModal(data) {

  document.getElementById('upd-company').value      = data.companyName;

  document.getElementById('upd-emp').value          = data.empName;

  document.getElementById('upd-comp-emp').value     = data.companyEmpName;

  document.getElementById('upd-date').value         = data.orderDate;

  document.getElementById('upd-remark').value       = data.note || '';



  const body = document.getElementById('upd-items-body');

  body.innerHTML = '';

  data.items.forEach(li => {

    addItemRow('upd-items-body');

    const last = body.lastElementChild;

    last.querySelector('.item-select').value  = li.itemName;

    last.querySelector('.unit-input').value   = li.unit;

    last.querySelector('.price-input').value  = li.unitPrice;

    last.querySelector('.qty-input').value    = li.unitQty;

    last.querySelector('.total-input').value  = li.unitPriceall;

  });

}



// 공통: form 에서 payload 수집

function collectFormData(prefix) {

  const companyName    = document.getElementById(`${prefix}-company`).value;

  const empName        = document.getElementById(`${prefix}-emp`).value;

  const companyEmpName = document.getElementById(`${prefix}-comp-emp`).value;

  const orderDate      = document.getElementById(`${prefix}-date`).value;

  const note           = document.getElementById(`${prefix}-remark`).value;

  const items = Array.from(document.querySelectorAll(`#${prefix}-items-body tr`)).map(tr => ({

    itemName: tr.querySelector('.item-select').value,

    unit:     tr.querySelector('.unit-input').value,

    unitPrice: parseFloat(tr.querySelector('.price-input').value)||0,

    unitQty:   parseFloat(tr.querySelector('.qty-input').value)||0

  }));

  return { companyName, empName, companyEmpName, orderDate, note, items };

}



// 전체 선택 토글

function toggleAll(cb) {

  document.querySelectorAll('input[name="chk"]').forEach(c => c.checked = cb.checked);

}



// 주문 상세 API 호출 헬퍼

function fetchOrderDetail(num, cb) {

  fetch(`/api/orders/${num}`)

    .then(res => res.json())

    .then(cb);

}