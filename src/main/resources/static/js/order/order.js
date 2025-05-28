// 서버에서 Thymeleaf로 주입된 customers/items 리스트
// (order.html 혹은 프래그먼트 내에 아래 스크립트를 추가하세요)


document.addEventListener('DOMContentLoaded', () => {
  // ... 기존 코드 ...

  /**
   * 고객사 선택 시 emp_name 자동 채우기
   * @param sel  선택된 <select name="company_id">
   */
  window.onCompanyChange = function(sel) {
    // 1) data-attribute 로 바로 읽기
    const empName = sel.selectedOptions[0].dataset.empName;
    document.querySelector('input[name="empName"]').value = empName;

    // --- 또는, customersList 로도 찾을 수 있고요: ---
    // const companyId = +sel.value;
    // const cust = window.customersList.find(c => c.companyId === companyId);
    // document.querySelector('input[name="emp_name"]').value =
    //   cust ? cust.employee.empName : '';
  };

  /**
   * 품목 선택 시 해당 row의 unit, unit_price 자동 채우기
   * @param sel  선택된 <select name="item_id[]">
   */
  window.onItemChange = function(sel) {
    const option = sel.selectedOptions[0];
    const unit    = option.dataset.unit    || '';
    const uprice  = option.dataset.price   || '';
    const row     = sel.closest('tr');
    row.querySelector('input[name="unit[]"]').value         = unit;
    row.querySelector('input[name="unitPrice[]"]').value = parseInt(uprice);
    // 금액도 즉시 재계산
    const qtyInput = row.querySelector('input[name="orderQty[]"]');
    if (qtyInput.value) calculateRowTotal(qtyInput);
  };

  // addOrderItemRow 에도 onchange 바인딩 추가
  const originalAdd = window.addOrderItemRow;
  window.addOrderItemRow = function() {
    originalAdd();
    // 맨 마지막 행의 select 에 이벤트 연결
    const tb = document.querySelector('#orderItemTable tbody');
    const sel = tb.querySelector('tr:last-child select[name="itemId[]"]');
    sel.addEventListener('change', () => onItemChange(sel));
  };

  // 기존 remove, calculateRowTotal 등 나머지 함수는 그대로 유지...
});




document.addEventListener('DOMContentLoaded', () => {
  const orderForm = document.getElementById('orderForm');
  const unitQtyInput = orderForm.querySelector('input[name="unitQty"]');

  // 테이블 행 개수를 unit_qty 에 설정
  function updateUnitQty() {
    unitQtyInput.value = document.querySelectorAll('#orderItemTable tbody tr').length;
  }
  updateUnitQty();

  // 주문 등록 모달 열기
  window.openOrderAddModal = () => {
    orderForm.reset();
    orderForm.action = '/order/add';
    document.getElementById('submitBtn').textContent = '등록';
    const tb = document.querySelector('#orderItemTable tbody');
    tb.innerHTML = tb.querySelector('tr').outerHTML;
    updateUnitQty();
  };

  // 상세 모달 열기
  window.openOrderDetailModal = (orderId) => {
	const dto = window.ordersList.find(o => o.orderId === orderId);
	  if (!dto) {
	    console.error('Order not found:', orderId);
	    return;
	  }
	
    document.getElementById('detailCustomer').textContent = dto.companyName;
    document.getElementById('detailOrderDate').textContent = dto.orderDate;
    document.getElementById('detailDueDate').textContent = dto.dueDate;
    document.getElementById('detailManager').textContent = dto.empName;
    document.getElementById('detailNote').textContent = dto.note;

    const tb = document.getElementById('orderDetailItems');
    tb.innerHTML = '';
    dto.items.forEach(i => {
      const tr = document.createElement('tr');
      tr.innerHTML =
        `<td>${i.itemCode}</td><td>${i.itemName}</td>` +
        `<td>${i.orderQty}</td><td>${i.unit}</td>` +
        `<td>${i.unitPrice.toLocaleString()}</td>` +
        `<td>${(i.orderQty * i.unitPrice).toLocaleString()}</td>`;
      tb.appendChild(tr);
    });
    new bootstrap.Modal(document.getElementById('orderDetailModal')).show();
  };

  // 행 추가
  window.addOrderItemRow = () => {
    const tr = document.querySelector('#orderItemTable tbody tr').cloneNode(true);
    tr.querySelectorAll('input').forEach(i => {
      if (!i.classList.contains('total')) i.value = '';
    });
    tr.querySelectorAll('select').forEach(s => s.selectedIndex = 0);
    document.querySelector('#orderItemTable tbody').appendChild(tr);
    updateUnitQty();
  };

  // 행 삭제
  window.removeOrderItemRow = (btn) => {
    const tb = document.querySelector('#orderItemTable tbody');
    if (tb.rows.length > 1) {
      btn.closest('tr').remove();
      updateUnitQty();
    } else {
      alert('최소 1개 품목이 필요합니다.');
    }
  };

  // 금액 계산 (order_qty[] × unit_price[])
  window.calculateRowTotal = (inp) => {
    const row = inp.closest('tr');
    const qty = parseFloat(row.querySelector("input[name='orderQty[]']").value) || 0;
    const price = parseFloat(row.querySelector("input[name='unitPrice[]']").value) || 0;
    row.querySelector('.total').value = (qty * price).toLocaleString();
  };

  // reset 시 1행만 남기고 초기화
  orderForm.addEventListener('reset', () => {
    const tb = document.querySelector('#orderItemTable tbody');
    const base = tb.querySelector('tr').cloneNode(true);
    base.querySelectorAll('input').forEach(i => {
      if (i.classList.contains('total')) i.value = '0';
      else i.value = '';
    });
    base.querySelectorAll('select').forEach(s => s.selectedIndex = 0);
    tb.innerHTML = '';
    tb.appendChild(base);
    updateUnitQty();
  });
});


document.addEventListener('DOMContentLoaded', () => {
  // 1) Thymeleaf에서 내려준 전체 주문 데이터
  let data = window.ordersList || [];

  // 1.1) 주문일자 오름차순으로 정렬
  data = data.slice().sort((a, b) => {
    const da = new Date(a.orderDate).getTime();
    const db = new Date(b.orderDate).getTime();
    return da - db;
  });

  // 2) 페이지당 항목 수
  const pageSize = 10;

  // 3) 현재 페이지
  let currentPage = 1;

  // 4) 전체 페이지 수
  const totalPages = Math.ceil(data.length / pageSize);

  // 5) table body 선택
  const tableBody = document.querySelector('table tbody');

  // 6) 페이지네이션 컨테이너를 동적으로 추가
  const paginationNav = document.createElement('nav');
  paginationNav.setAttribute('aria-label', 'Page navigation');
  paginationNav.innerHTML =
    `<ul class="pagination justify-content-center mt-3" id="pagination"></ul>`;
  document.querySelector('.table-responsive').after(paginationNav);

  // 7) 테이블 다시 그리기
  function renderTable() {
    tableBody.innerHTML = '';
    const start = (currentPage - 1) * pageSize;
    const pageItems = data.slice(start, start + pageSize);

    pageItems.forEach((order, idx) => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><input type="checkbox" /></td>
        <td>${start + idx + 1}</td>
        <td>${order.orderDate}</td>
        <td>${order.orderLineItemId}</td>
        <td>${order.companyName}</td>
        <td>${order.unitQty}</td>
        <td>${order.amount}</td>
        <td>${order.empName}</td>
        <td>
          <button class="btn btn-info btn-sm"
            onclick="openOrderDetailModal(${order.orderId})">
            상세
          </button>
        </td>
        <td>
          <button class="btn btn-warning btn-sm"
            onclick="openOrderEditModal(${order.orderId})">
            수정
          </button>
        </td>
        <td>
          <button class="btn btn-success btn-sm"
            onclick="openShipmentFromOrder('${order.company_id}')">
            출고등록
          </button>
        </td>
      `;
      tableBody.appendChild(tr);
    });
  }

  // 8) 페이지네이션 다시 그리기
  function renderPagination() {
    const ul = document.getElementById('pagination');
    ul.innerHTML = '';

    // 이전 버튼
    const prevLi = document.createElement('li');
    prevLi.className = `page-item ${currentPage === 1 ? 'disabled' : ''}`;
    prevLi.innerHTML = `
      <a class="page-link" href="#" aria-label="Previous">
        <span aria-hidden="true">&laquo;</span>
      </a>`;
    prevLi.addEventListener('click', e => {
      e.preventDefault();
      if (currentPage > 1) {
        currentPage--;
        update();
      }
    });
    ul.appendChild(prevLi);

    // 번호 버튼
    for (let i = 1; i <= totalPages; i++) {
      const li = document.createElement('li');
      li.className = `page-item ${i === currentPage ? 'active' : ''}`;
      li.innerHTML = `<a class="page-link" href="#">${i}</a>`;
      li.addEventListener('click', e => {
        e.preventDefault();
        currentPage = i;
        update();
      });
      ul.appendChild(li);
    }

    // 다음 버튼
    const nextLi = document.createElement('li');
    nextLi.className = `page-item ${currentPage === totalPages ? 'disabled' : ''}`;
    nextLi.innerHTML = `
      <a class="page-link" href="#" aria-label="Next">
        <span aria-hidden="true">&raquo;</span>
      </a>`;
    nextLi.addEventListener('click', e => {
      e.preventDefault();
      if (currentPage < totalPages) {
        currentPage++;
        update();
      }
    });
    ul.appendChild(nextLi);
  }

  // 9) 전체 업데이트
  function update() {
    renderTable();
    renderPagination();
  }

  // 10) 최초 렌더링
  update();
  
  // (orderList.js 내부, update() 호출 직후 등 DOMContentLoaded 끝나기 전에 추가)
  const selectAllCb = document.getElementById('selectAll');
  selectAllCb.addEventListener('change', () => {
    // 현재 페이지에 보이는 tbody 체크박스만 토글
    document
      .querySelectorAll('table tbody input[type="checkbox"]')
      .forEach(cb => cb.checked = selectAllCb.checked);
  });
  
  
});



window.openOrderDetailModal = (orderId) => {
  // 1) 전달받은 orderId 로 전체 데이터에서 dto 찾기
  const dto = window.ordersList.find(o => o.orderId === orderId);
  if (!dto) return console.error('Order not found:', orderId);

  // 2) 모달 안의 각 요소에 데이터 채우기
  document.getElementById('detailCustomer').textContent = dto.companyName;
  document.getElementById('detailOrderDate').textContent = dto.orderDate;
  // … 나머지 필드도

  // 3) 테이블 아이템 채우기
  const tb = document.getElementById('orderDetailItems');
  tb.innerHTML = '';
  dto.items.forEach(i => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${i.itemCode}</td>
      <td>${i.itemName}</td>
      <td>${i.orderQty}</td>
      <td>${i.unit}</td>
      <td>${i.unitPrice.toLocaleString()}</td>
      <td>${(i.orderQty * i.unitPrice).toLocaleString()}</td>
    `;
    tb.append(tr);
  });

  // 4) Bootstrap modal API 로 열기
  const modalEl = document.getElementById('orderDetailModal');
  new bootstrap.Modal(modalEl).show();
};


document.addEventListener('DOMContentLoaded', () => {
  // 전체 주문 데이터를 Thymeleaf가 깔끔한 JSON으로 내려줌
  const orders = window.ordersList || [];

  // 상세 모달 띄우기 함수
  window.openOrderDetailModal = (orderId) => {
    // 1) 해당 orderId의 DTO 찾기
    const dto = orders.find(o => o.orderId === orderId);
    if (!dto) {
      console.error('Order not found:', orderId);
      return;
    }

    // 2) 모달 필드 채우기
    document.getElementById('detailCustomer').textContent    = dto.companyName;
    document.getElementById('detailOrderDate').textContent   = dto.orderDate;
    document.getElementById('detailDueDate').textContent     = dto.dueDate;
    document.getElementById('detailManager').textContent     = dto.empName;
    document.getElementById('detailNote').textContent        = dto.note;

    // 3) 라인 아이템 테이블 채우기
    const tbody = document.getElementById('orderDetailItems');
    tbody.innerHTML = '';  // 초기화
    (dto.items || []).forEach(i => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${i.itemCode}</td>
        <td>${i.itemName}</td>
        <td>${i.orderQty}</td>
        <td>${i.unit}</td>
        <td>${i.unitPrice.toLocaleString()}</td>
        <td>${(i.orderQty * i.unitPrice).toLocaleString()}</td>`;
      tbody.appendChild(tr);
    });

    // 4) 부트스트랩 모달 띄우기
    new bootstrap.Modal(document.getElementById('orderDetailModal')).show();
  };
});