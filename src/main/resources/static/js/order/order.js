// /js/order/order.js

// 1) 주문 등록 모달 열기
function openOrderAddModal() {
  const modalEl = document.getElementById('orderRegisterModal');
  if (!modalEl) return;

  // 폼 리셋
  const form = modalEl.querySelector('form');
  if (form) form.reset();

  // 품목 테이블 초기화
  const tbody = modalEl.querySelector('#orderItemTable tbody');
  if (tbody) {
    // 첫 번째 행 템플릿으로 모든 행 삭제 후 재생성
    const prototypeRow = tbody.querySelector('tr');
    tbody.innerHTML = '';
    if (prototypeRow) tbody.appendChild(prototypeRow.cloneNode(true));
  }

  // 모달 표시
  new bootstrap.Modal(modalEl).show();
}

// 2) 주문 상세 모달 열기
// data: { customer, order_date, due_date, manager, note, items: [ { item_code, item_name, quantity, unit, unit_price }, ... ] }
function openOrderDetailModal(data) {
  const modalEl = document.getElementById('orderDetailModal');
  if (!modalEl) return;

  // 기본 정보 채우기
  modalEl.querySelector('#detailCustomer').textContent  = data.customer;
  modalEl.querySelector('#detailOrderDate').textContent = data.order_date;
  modalEl.querySelector('#detailDueDate').textContent   = data.due_date;
  modalEl.querySelector('#detailManager').textContent   = data.manager;
  modalEl.querySelector('#detailNote').textContent      = data.note;

  // 품목 리스트 채우기
  const tbody = modalEl.querySelector('#orderDetailItems');
  if (tbody) {
    tbody.innerHTML = '';
    data.items.forEach(item => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${item.item_code}</td>
        <td>${item.item_name}</td>
        <td>${item.quantity}</td>
        <td>${item.unit}</td>
        <td>${item.unit_price.toLocaleString()}</td>
        <td>${(item.quantity * item.unit_price).toLocaleString()}</td>
      `;
      tbody.appendChild(tr);
    });
  }

  // 모달 표시
  new bootstrap.Modal(modalEl).show();
}

// 3) 주문 수정 모달 열기
function openOrderEditModal(data) {
  const modalEl = document.getElementById('orderRegisterModal');
  if (!modalEl) return;

  // 폼 리셋 및 action 변경
  const form = modalEl.querySelector('#orderForm');
  if (form) {
    form.reset();
    form.action = `/orders/${data.order_id}`;
  }

  // 기본 정보 채우기
  modalEl.querySelector('[name="customerId"]').value = data.company_id;
  modalEl.querySelector('[name="orderDate"]').value   = data.order_date;
  modalEl.querySelector('[name="dueDate"]').value     = data.due_date;
  modalEl.querySelector('[name="manager"]').value     = data.emp_name;
  modalEl.querySelector('[name="note"]').value        = data.note || '';

  // 품목 테이블 채우기
  const tbody = modalEl.querySelector('#orderItemTable tbody');
  if (tbody) {
    tbody.innerHTML = '';
    data.items.forEach((item, idx) => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>
          <select class="form-select" name="items[${idx}].itemId" required>
            <option value="${item.item_id}" selected>${item.item_code} - ${item.item_name}</option>
            <!-- 추가 옵션은 서버에서 로드 -->
          </select>
        </td>
        <td><input type="number" name="items[${idx}].quantity" value="${item.quantity}" class="form-control" required oninput="calculateRowTotal(this)" /></td>
        <td><input type="text" name="items[${idx}].unit" value="${item.unit}" class="form-control" required /></td>
        <td><input type="number" name="items[${idx}].unitPrice" value="${item.unit_price}" class="form-control" required oninput="calculateRowTotal(this)" /></td>
        <td><input type="text" readonly class="form-control total" value="${(item.quantity * item.unit_price).toLocaleString()}" /></td>
        <td><button type="button" class="btn btn-sm btn-outline-danger" onclick="removeOrderItemRow(this)">삭제</button></td>
      `;
      tbody.appendChild(tr);
    });
  }

  // 모달 제목 및 버튼 텍스트 변경
  modalEl.querySelector('.modal-title').textContent = '주문 수정';
  modalEl.querySelector('.modal-footer button[type="submit"]').textContent = '수정';

  // 모달 표시
  new bootstrap.Modal(modalEl).show();
}

// 4) 품목 행 추가
function addOrderItemRow() {
  const tbody = document.querySelector('#orderItemTable tbody');
  if (!tbody) return;
  const prototype = tbody.querySelector('tr');
  if (!prototype) return;
  const clone = prototype.cloneNode(true);
  clone.querySelectorAll('input, select').forEach(el => {
    if (el.tagName === 'INPUT') el.value = '';
    if (el.tagName === 'SELECT') el.selectedIndex = 0;
  });
  clone.querySelector('.total').value = '0';
  tbody.appendChild(clone);
}

declareAddRowButton();

function declareAddRowButton() {
  const btn = document.getElementById('addItemBtn');
  if (btn) btn.addEventListener('click', addOrderItemRow);
}

// 5) 품목 행 삭제
function removeOrderItemRow(btn) {
  const tbody = document.querySelector('#orderItemTable tbody');
  if (tbody.children.length > 1) {
    btn.closest('tr').remove();
  } else {
    alert('최소 1개의 품목을 입력해야 합니다.');
  }
}

// 6) 행별 합계 계산
function calculateRowTotal(input) {
  const row = input.closest('tr');
  const qty   = Number(row.querySelector('input[name$=".quantity"]').value) || 0;
  const price = Number(row.querySelector('input[name$=".unitPrice"]').value) || 0;
  row.querySelector('.total').value = (qty * price).toLocaleString();
}
