// /js/shipment/shipment-tabs.js

// ──────────────────────────────────────────────────────────────────────────
// 1) 전역 변수 설정
// ──────────────────────────────────────────────────────────────────────────

let allShipments = [];            // 서버에서 내려받은 전체 출고/이동 로그 목록
const PAGE_SIZE = 10;             // 페이지 당 표시할 건수

// 현재 탭별 “검색 기준(필드)”과 “검색어”, “날짜 범위”, “창고 필터”를 저장할 변수
let filterField_Customer = 'itemName';
let filterKeyword_Customer = '';
let filterFromDate_Customer = '';
let filterToDate_Customer = '';
let filterWarehouse_Customer = '';

let filterField_Transfer = 'itemName';
let filterKeyword_Transfer = '';
let filterFromDate_Transfer = '';
let filterToDate_Transfer = '';
let filterWarehouse_Transfer = '';

let currentPage_Customer = 1;    // 고객사 탭 현재 페이지
let currentPage_Transfer = 1;    // 창고 이동 탭 현재 페이지


// ──────────────────────────────────────────────────────────────────────────
// 2) DOMContentLoaded 이벤트 바인딩
// ──────────────────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
  // 2-1) 서버에서 전체 출고/이동 데이터 불러오기
  fetchAllShipments();

  // 2-2) 탭 전환 시 렌더링 로직
  document
    .getElementById('tab-customer-shipment')
    .addEventListener('shown.bs.tab', () => {
      renderCustomerTab(1);
    });

  document
    .getElementById('tab-transfer-shipment')
    .addEventListener('shown.bs.tab', () => {
      renderTransferTab(1);
    });

  // 2-3) “고객사 출고” 검색 버튼 클릭
  document
    .getElementById('btnSearch_Customer')
    .addEventListener('click', () => {
      // 검색어, 필드, 날짜, 창고값을 읽어서 전역 변수에 할당
      filterField_Customer =
        document.getElementById('searchField_Customer').value;
      filterKeyword_Customer =
        document.getElementById('searchKeyword_Customer').value.trim().toLowerCase();
      filterFromDate_Customer =
        document.getElementById('fromDate_Customer').value;
      filterToDate_Customer =
        document.getElementById('toDate_Customer').value;
     

      currentPage_Customer = 1;
      renderCustomerTab(1);
    });

  // 2-4) “특별 출고” (창고 이동) 검색 버튼 클릭
  document
    .getElementById('btnSearch_Transfer')
    .addEventListener('click', () => {
      filterField_Transfer =
        document.getElementById('searchField_Transfer').value;
      filterKeyword_Transfer =
        document.getElementById('searchKeyword_Transfer').value.trim().toLowerCase();
      filterFromDate_Transfer =
        document.getElementById('fromDate_Transfer').value;
      filterToDate_Transfer =
        document.getElementById('toDate_Transfer').value;
     

      currentPage_Transfer = 1;
      renderTransferTab(1);
    });
});


// ──────────────────────────────────────────────────────────────────────────
// 3) 서버에서 전체 출고/이동 리스트를 한 번에 받아오는 함수
// ──────────────────────────────────────────────────────────────────────────

async function fetchAllShipments() {
  try {
    const res = await fetch('/api/shipments');
    if (!res.ok) throw new Error(`HTTP 상태 ${res.status}`);
    const data = await res.json();
    // data 예시: [
    //   {
    //     "logId": 1,
    //     "logDatetime": "2025-06-03T10:00:00",
    //     "companyName": "가나공급사2",
    //     "orderNum": "ORD-20250603-babc",
    //     "itemCode": "PRD002",
    //     "itemName": "무선 청소기",
    //     "quantity": 1,
    //     "unit": "EA",
    //     "warehouseName": "물류센터 1호",
    //     "empName": "윤보미",
    //     "comment": "",
    //     // … (필요에 따라 추가 필드)
    //   },
    //   // Transfer (특별 출고) 로그의 경우,
    //   // companyName/orderNum 이 없거나 공백이므로 구별 가능
    // ]
    allShipments = data;
    // 초기 화면은 “고객사 출고” 탭이 기본이므로, 그 탭 렌더
    renderCustomerTab(1);
  } catch (err) {
    console.error('출고 목록을 불러오는 중 오류:', err);
    alert('출고 데이터를 가져오는 중 오류가 발생했습니다.');
  }
}


// ──────────────────────────────────────────────────────────────────────────
// 4) “고객사 출고” 탭 렌더링 함수
//    - page: 보여줄 페이지 번호 (1부터 시작)
// ──────────────────────────────────────────────────────────────────────────

function renderCustomerTab(page) {
  const tbody = document.getElementById('tbodyCustomerShipment');
  tbody.innerHTML = '';

  // 4-1) “고객사 출고” 조건: companyName, orderNum 이 모두 비어 있지 않은 항목만
  let filtered = allShipments.filter((row) => {
    return row.companyName && row.companyName !== '' && row.orderNum && row.orderNum !== '';
  });

  // 4-2) 필터링: 검색어, 검색필드
  if (filterKeyword_Customer) {
    filtered = filtered.filter((row) => {
      const fieldVal = (row[filterField_Customer] || '').toString().toLowerCase();
      return fieldVal.includes(filterKeyword_Customer);
    });
  }

  // 4-3) 필터링: 날짜 범위 (logDatetime) — “YYYY-MM-DD” 형태로 비교
  if (filterFromDate_Customer) {
    filtered = filtered.filter((row) => {
      const datePart = row.logDatetime.split('T')[0];
      return datePart >= filterFromDate_Customer;
    });
  }
  if (filterToDate_Customer) {
    filtered = filtered.filter((row) => {
      const datePart = row.logDatetime.split('T')[0];
      return datePart <= filterToDate_Customer;
    });
  }

  // 4-4) 필터링: 창고
  if (filterWarehouse_Customer) {
    filtered = filtered.filter((row) => {
      return row.warehouseName === filterWarehouse_Customer;
    });
  }

  // 4-5) 전체 건수 → 페이징 처리
  const totalCount = filtered.length;
  const totalPages = Math.ceil(totalCount / PAGE_SIZE);
  if (page < 1) page = 1;
  if (page > totalPages) page = totalPages;
  currentPage_Customer = page;

  const startIdx = (page - 1) * PAGE_SIZE;
  const endIdx = startIdx + PAGE_SIZE;
  const pageItems = filtered.slice(startIdx, endIdx);

  // 4-6) 테이블 바디에 렌더링
  pageItems.forEach((row, idx) => {
    const no = startIdx + idx + 1;
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><input type="checkbox" class="selectBoxCustomer" value="${row.logId}" /></td>
      <td>${no}</td>
      <td>${(row.logDatetime || '').split('T')[0] || ''}</td>
      <td>${row.companyName || ''}</td>
      <td>${row.orderNum || ''}</td>
      <td>${row.itemCode || ''}</td>
      <td>${row.itemName || ''}</td>
      <td>${row.quantity || ''}</td>
      <td>${row.unit || ''}</td>
      <td>${row.warehouseName || ''}</td>
      <td>${row.empName || ''}</td>
      <td>${row.comment || ''}</td>

    `;
    tbody.appendChild(tr);
  });

  // 4-7) “전체 선택” 체크박스 이벤트 재바인딩
  document
    .getElementById('selectAllCustomer')
    .checked = false;
  document
    .getElementById('selectAllCustomer')
    .onclick = function () {
      document
        .querySelectorAll('.selectBoxCustomer')
        .forEach(cb => (cb.checked = this.checked));
    };

  // 4-8) 페이징 UI 렌더링
  renderPagination('paginationCustomer', totalPages, currentPage_Customer, renderCustomerTab);
}


// ──────────────────────────────────────────────────────────────────────────
// 5) “특별 출고” (창고 이동) 탭 렌더링 함수
// ──────────────────────────────────────────────────────────────────────────

function renderTransferTab(page) {
  const tbody = document.getElementById('tbodyTransferShipment');
  tbody.innerHTML = '';

  // 5-1) “창고 이동” 조건: companyName 또는 orderNum이 비어 있거나 null
  let filtered = allShipments.filter((row) => {
    return !(row.companyName && row.companyName !== '' && row.orderNum && row.orderNum !== '');
  });

  // 5-2) 필터링: 검색어, 검색필드
  if (filterKeyword_Transfer) {
    filtered = filtered.filter((row) => {
      const fieldVal = (row[filterField_Transfer] || '').toString().toLowerCase();
      return fieldVal.includes(filterKeyword_Transfer);
    });
  }

  // 5-3) 필터링: 날짜 범위
  if (filterFromDate_Transfer) {
    filtered = filtered.filter((row) => {
      const datePart = row.logDatetime.split('T')[0];
      return datePart >= filterFromDate_Transfer;
    });
  }
  if (filterToDate_Transfer) {
    filtered = filtered.filter((row) => {
      const datePart = row.logDatetime.split('T')[0];
      return datePart <= filterToDate_Transfer;
    });
  }

  // 5-4) 필터링: 창고
  if (filterWarehouse_Transfer) {
    filtered = filtered.filter((row) => {
      return row.warehouseName === filterWarehouse_Transfer;
    });
  }

  // 5-5) 전체 건수 → 페이징
  const totalCount = filtered.length;
  const totalPages = Math.ceil(totalCount / PAGE_SIZE);
  if (page < 1) page = 1;
  if (page > totalPages) page = totalPages;
  currentPage_Transfer = page;

  const startIdx = (page - 1) * PAGE_SIZE;
  const endIdx = startIdx + PAGE_SIZE;
  const pageItems = filtered.slice(startIdx, endIdx);

  // 5-6) 테이블 바디에 렌더링
  pageItems.forEach((row, idx) => {
    const no = startIdx + idx + 1;
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><input type="checkbox" class="selectBoxTransfer" value="${row.logId}" /></td>
      <td>${no}</td>
      <td>${(row.logDatetime || '').split('T')[0] || ''}</td>
      <td>${row.itemCode || ''}</td>
      <td>${row.itemName || ''}</td>
      <td>${row.quantity || ''}</td>
      <td>${row.unit || ''}</td>
      <td>${row.warehouseName || ''}</td>
      <td>${row.comment || ''}</td>
     
    `;
    tbody.appendChild(tr);
  });

  // 5-7) 전체 선택 체크박스 이벤트 재바인딩
  document.getElementById('selectAllTransfer').checked = false;
  document.getElementById('selectAllTransfer').onclick = function () {
    document
      .querySelectorAll('.selectBoxTransfer')
      .forEach(cb => (cb.checked = this.checked));
  };

  // 5-8) 페이징 UI 렌더링
  renderPagination('paginationTransfer', totalPages, currentPage_Transfer, renderTransferTab);
}


// ──────────────────────────────────────────────────────────────────────────
// 6) 공통 페이징 UI 렌더 함수
//
//    containerId: 'paginationCustomer' or 'paginationTransfer'
//    totalPages: 총 페이지 수
//    currentPage: 현재 활성화된 페이지 (1-based)
//    onPageClick: 페이지 버튼 클릭 시 호출할 함수 (renderCustomerTab or renderTransferTab)
// ──────────────────────────────────────────────────────────────────────────

function renderPagination(containerId, totalPages, currentPage, onPageClick) {
  const ul = document.getElementById(containerId);
  ul.innerHTML = '';

  if (totalPages <= 1) return;

  // “이전” 버튼
  const prevLi = document.createElement('li');
  prevLi.className = `page-item ${currentPage === 1 ? 'disabled' : ''}`;
  prevLi.innerHTML = `
    <button class="page-link" ${currentPage === 1 ? 'disabled' : ''}>
      이전
    </button>
  `;
  prevLi.onclick = () => {
    if (currentPage > 1) {
      onPageClick(currentPage - 1);
    }
  };
  ul.appendChild(prevLi);

  // 페이지 번호 버튼
  for (let i = 1; i <= totalPages; i++) {
    const li = document.createElement('li');
    li.className = `page-item ${i === currentPage ? 'active' : ''}`;
    li.innerHTML = `<button class="page-link">${i}</button>`;
    li.onclick = () => {
      onPageClick(i);
    };
    ul.appendChild(li);
  }

  // “다음” 버튼
  const nextLi = document.createElement('li');
  nextLi.className = `page-item ${currentPage === totalPages ? 'disabled' : ''}`;
  nextLi.innerHTML = `
    <button class="page-link" ${currentPage === totalPages ? 'disabled' : ''}>
      다음
    </button>
  `;
  nextLi.onclick = () => {
    if (currentPage < totalPages) {
      onPageClick(currentPage + 1);
    }
  };
  ul.appendChild(nextLi);
}
// shipment-tabs.js (또는 기존 shipment.js)
// 탭 별로 로직을 분리해서 관리한다고 가정합니다.
// ─────────────────────────────────────────────────────────────────
// ① “고객사 출고” 탭 내 삭제 버튼 핸들러 예시
// ─────────────────────────────────────────────────────────────────
document
  .getElementById('btnDelete_Customer').addEventListener('click', () => {
    // 1) “고객사 출고” 테이블의 모든 체크된 체크박스(value=logId)를 수집
    const checkedBoxes = document
      .querySelectorAll('#tblCustomerShipment tbody input[type="checkbox"]:checked');
    const logIds = Array.from(checkedBoxes).map(cb => Number(cb.value));

    if (logIds.length === 0) {
      alert("✅ 삭제할 출고 항목을 하나 이상 선택해주세요.");
      return;
    }
    if (!confirm(`총 ${logIds.length}개 항목을 정말 삭제하시겠습니까?`)) {
      return;
    }

    // 2) DELETE /api/shipments 요청
    fetch('/api/shipments-del', {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(logIds)
    })
      .then(async res => {
        if (!res.ok) {
          const text = await res.text();
          throw new Error(text || `HTTP ${res.status}`);
        }
        return res.text();
      })
      .then(msg => {
        alert('✅ 삭제되었습니다.');
        // 3) 테이블 리프레시: 고객사 출고 목록을 다시 불러옵니다.
        loadCustomerShipments(/* 페이지 번호나 필터조건 인자 등 필요 시 */);
      })
      .catch(err => {
        console.error('❌ 출고 삭제 중 오류 발생:', err);
        alert('❌ 삭제 중 오류가 발생했습니다:\n' + err.message);
      });
  });


// ─────────────────────────────────────────────────────────────────
// ② “창고 이동(특별 출고)” 탭 내 삭제 버튼 핸들러 예시
// ─────────────────────────────────────────────────────────────────
document
  .getElementById('btnDelete_Transfer')
  .addEventListener('click', () => {
    // “창고 이동” 전용 테이블에서 체크된 박스(value=logId) 모으기
    const checkedBoxes = document
      .querySelectorAll('#tblTransferShipment tbody input[type="checkbox"]:checked');
    const logIds = Array.from(checkedBoxes).map(cb => Number(cb.value));

    if (logIds.length === 0) {
      alert("✅ 삭제할 이동 항목을 하나 이상 선택해주세요.");
      return;
    }
    if (!confirm(`총 ${logIds.length}개 항목을 정말 삭제하시겠습니까?`)) {
      return;
    }

    fetch('/api/shipments-del', {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(logIds)
    })
      .then(async res => {
        if (!res.ok) {
          const text = await res.text();
          throw new Error(text || `HTTP ${res.status}`);
        }
        return res.text();
      })
      .then(msg => {
        alert('✅ 삭제되었습니다.');
        // “창고 이동” 목록 리프레시
        loadTransferShipments(/*필요한 인자*/);
      })
      .catch(err => {
        console.error('❌ 이동 삭제 중 오류 발생:', err);
        alert('❌ 삭제 중 오류가 발생했습니다:\n' + err.message);
      });
  });