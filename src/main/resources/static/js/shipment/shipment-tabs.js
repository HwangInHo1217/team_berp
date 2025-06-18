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
      <td>${row.comment && row.comment.trim() !== "" ? row.comment : "고객사 출고"}</td>

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
        fetchAllShipments();
      })
      .catch(err => {
        console.error('❌ 출고 삭제 중 오류 발생:', err);
        alert('❌ 삭제 중 오류가 발생했습니다:\n' + err.message);
      });
  });


// ─────────────────────────────────────────────────────────────────
// ② “창고 이동(특별 출고)” 탭 내 삭제 버튼 핸들러 예시
// ─────────────────────────────────────────────────────────────────
document.getElementById('btnDelete_Transfer')
  .addEventListener('click', () => {
    const checkedBoxes = document.querySelectorAll('#tblTransferShipment tbody input[type="checkbox"]:checked');
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

        // --- 탭 전환 ---
        let tabBtn = document.getElementById('tab-transfer-shipment');
        if (tabBtn) {
          let tab = new bootstrap.Tab(tabBtn);
          tab.show();
        }

        // --- 데이터 다시 받아서 1페이지 보여주기 ---
        // fetchAllShipments()는 데이터를 받아오고 기본적으로 renderCustomerTab(1)을 호출
        // 특별 출고 탭이 활성화된 후, renderTransferTab(1)도 호출해야 안전합니다.
        fetchAllShipments().then(() => {
          renderTransferTab(1);
        });

      })
      .catch(err => {
        console.error('❌ 이동 삭제 중 오류 발생:', err);
        alert('❌ 삭제 중 오류가 발생했습니다:\n' + err.message);
      });
  });


  
  
  // ─────────────────────────────────────────────────────────────────
  // ③ “창고 이동(특별 출고)” 등록 버튼 핸들러 및 모달 폼 제출 처리
  // ─────────────────────────────────────────────────────────────────

  // 1) 등록 버튼 클릭 시 모달 열기
  document.getElementById('btnRegister_Transfer').addEventListener('click', () => {
    const modal = new bootstrap.Modal(document.getElementById('registerTransferModal'));
    modal.show();
  });

  // 2) 모달 폼 제출 처리
  document.getElementById('formTransferRegister').addEventListener('submit', async function(e) {
    e.preventDefault();

    // 1. 값 읽기
    // - select, input, textarea 값 전부 수집
    const itemName = document.getElementById('selectItemName').value;
    const itemCode = document.getElementById('inputItemCode').value;
    const unit = document.getElementById('inputUnit').value;
    const warehouse = document.getElementById('selectWarehouse').value;
    const shipmentDate = document.getElementById('inputShipmentDate').value;
    const quantity = document.getElementById('inputQuantity').value;
    const remark = document.getElementById('inputRemark') ? document.getElementById('inputRemark').value : '';

    // 2. 유효성 체크
    if (!itemName || !itemCode || !unit || !warehouse || !shipmentDate || !quantity) {
      alert('필수 입력값을 모두 입력하세요.');
      return;
    }

    // 3. 등록 데이터 구성
    const data = {
      shipmentDate,
      itemCode,
      itemName,
      quantity: Number(quantity),
      unit,
      warehouseName: warehouse,
      comment: remark,
	  companyName: "",
	  orderNum: ""
    };

    // 4. API 호출
    try {
      const res = await fetch('/api/shipment/transfer', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });

      if (!res.ok) {
        alert('등록 실패: ' + await res.text());
        return;
      }

      alert('✅ 특별 출고 등록 완료!');
      this.reset();

      // 모달 닫기
	  const modalEl = document.getElementById('registerTransferModal');
	    const modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
	    modalInstance.hide();

      // 목록 새로고침
      await fetchAllShipments();
	  
	  // 창고 이동 탭 자동 전환
	  renderTransferTab(1);
	  
    } catch (err) {
      alert('❌ 등록 중 오류: ' + err.message);
    }
  });


  
  // 품목, 창고 마스터데이터 (최초 1회만 불러옴)
  let itemsMaster = [];
  let warehousesMaster = [];

  document.addEventListener('DOMContentLoaded', async () => {
    await loadMasterData(); // 품목, 창고 데이터 채우기

    // 품목명 선택시 코드/단위 자동 입력
    document.getElementById('selectItemName').addEventListener('change', function() {
      const selectedName = this.value;
      const item = itemsMaster.find(it => it.itemName === selectedName);
      document.getElementById('inputItemCode').value = item ? item.itemCode : '';
      document.getElementById('inputUnit').value = item ? item.unit : '';
    });
  });

  // 품목/창고 마스터 불러와서 select option 생성
  async function loadMasterData() {
    // 품목
    const itemsRes = await fetch('/api/item/items');
    itemsMaster = await itemsRes.json();
    const itemSel = document.getElementById('selectItemName');
    itemSel.innerHTML = '<option value="">-- 품목명 선택 --</option>';
    itemsMaster.forEach(item => {
      const opt = document.createElement('option');
      opt.value = item.itemName;
      opt.textContent = item.itemName;
      itemSel.appendChild(opt);
    });

    // 창고
    const whsRes = await fetch('/api/warehouses/list');
    warehousesMaster = await whsRes.json();
    const whSel = document.getElementById('selectWarehouse');
    whSel.innerHTML = '<option value="">-- 창고 선택 --</option>';
    warehousesMaster.forEach(wh => {
      const opt = document.createElement('option');
      opt.value = wh.warehouseName;
      opt.textContent = wh.warehouseName;
      whSel.appendChild(opt);
    });
  }

  document.getElementById("btnExcelDownload_Customer").addEventListener("click", function () {
      var table = document.getElementById("tblCustomerShipment");
      var wb = XLSX.utils.table_to_book(table, { sheet: "고객사 출고" });
      XLSX.writeFile(wb, "고객사출고내역.xlsx");
  });

  document.getElementById("btnExcelDownload_Transfer").addEventListener("click", function () {
      var table = document.getElementById("tblTransferShipment");
      var wb = XLSX.utils.table_to_book(table, { sheet: "특별 출고" });
      XLSX.writeFile(wb, "특별출고내역.xlsx");
  });
  
  
  
  // 모달이 열릴 때마다 오늘 날짜를 세팅
  document.getElementById('btnRegister_Transfer').addEventListener('click', function () {
    const dateInput = document.getElementById('inputShipmentDate');
    if (dateInput) {
      dateInput.value = getToday();
      dateInput.readOnly = true;
    }
    // 모달이 이미 열려있는지 확인하고, 한 번만 show
    const modalEl = document.getElementById('registerTransferModal');
    const modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl); // 이미 열려있으면 재활용
    modalInstance.show();
  });
  
  // 오늘 날짜 yyyy-MM-dd 포맷 구하기
  function getToday() {
    const d = new Date();
    const month = (d.getMonth() + 1).toString().padStart(2, '0');
    const day = d.getDate().toString().padStart(2, '0');
    return `${d.getFullYear()}-${month}-${day}`;
  }