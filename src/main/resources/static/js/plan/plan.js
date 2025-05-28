
function openPlanEditModal(data) {
  document.getElementById("planDate").value = data.plan_date;
  document.getElementById("itemCode").value = data.item_code;
  document.getElementById("itemName").value = data.item_name;
  document.getElementById("quantity").value = data.quantity;
  document.getElementById("unit").value = data.unit;
  document.getElementById("dueDate").value = data.due_date;
  document.getElementById("status").value = data.status;
  document.getElementById("manager").value = data.manager;
  document.getElementById("note").value = data.note || '';

  // 폼 액션 변경 및 버튼 텍스트 변경
  document.getElementById("planForm").action = `/plan/update/${data.id}`;
  document.getElementById("submitBtn").textContent = "수정";

  // 모달 열기
  const modal = new bootstrap.Modal(document.getElementById("planRegisterModal"));
  modal.show();
}

function openPlanDetailModal(data) {
  // 계획번호 표시
  document.getElementById("planDetailPlanNo").textContent = data.plan_no;
  document.getElementById("planDetailPlanDate").textContent = data.plan_date;
  document.getElementById("planDetailItemCode").textContent = data.item_code;
  document.getElementById("planDetailItemName").textContent = data.item_name;
  document.getElementById("planDetailQuantity").textContent = data.quantity;
  document.getElementById("planDetailUnit").textContent = data.unit;
  document.getElementById("planDetailDueDate").textContent = data.due_date;
  document.getElementById("planDetailStatus").textContent = data.status;
  document.getElementById("planDetailManager").textContent = data.manager;
  document.getElementById("planDetailNote").textContent = data.note;

  // 모달 열기
  const modal = new bootstrap.Modal(document.getElementById("planDetailModal"));
  modal.show();
}


// ✅ 생산계획 리스트 조회 함수
function fetchProdPlanList() {
  // API 호출
  fetch("/api/prod-plan/list")
    .then(response => response.json()) // JSON 변환
    .then(data => renderProdPlanTable(data)) // 테이블에 렌더링
    .catch(error => {
      console.error("생산계획 목록 조회 실패:", error);
      alert("생산계획 데이터를 불러오는 데 실패했습니다.");
    });
}

// ✅ 테이블 본문에 데이터 렌더링
function renderProdPlanTable(planList) {
  // 테이블 tbody 요소 가져오기
  const tbody = document.querySelector("table tbody");
  tbody.innerHTML = ""; // 기존 내용 초기화

  // 리스트가 비어있으면 "데이터 없음" 출력
  if (!planList || planList.length === 0) {
    const row = `<tr><td colspan="14">등록된 생산계획이 없습니다.</td></tr>`;
    tbody.insertAdjacentHTML("beforeend", row);
    return;
  }

  // 각 계획 데이터를 테이블 행으로 추가
  planList.forEach(plan => {
    // HTML 행 생성
    const row = `
      <tr>
        <td><input type="checkbox" /></td>
        <td>${generatePlanNo(plan.planId)}</td>
        <td>${plan.planDate || ''}</td>
        <td>${plan.itemCode}</td>
        <td>${plan.itemName}</td> <!-- 품목명은 현재 itemId만 있으므로 고정 -->
        <td>${plan.planQty}</td>
        <td>${plan.unit}</td>
        <td>${plan.dueDate || ''}</td>
        <td>${mapStatus(plan.status)}</td>
        <td>${plan.manager || ''}</td>
        <td>${plan.remark || '-'}</td>
        <td>
          <button class="btn btn-info btn-sm" onclick="openPlanDetailModal({
            plan_no: '${generatePlanNo(plan.planId)}',
            plan_date: '${plan.planDate}',
            item_code: '${plan.itemId ? 'P' + String(plan.itemId).padStart(3, '0') : ''}',
            item_name: '완제품',
            quantity: ${plan.planQty},
            unit: '${plan.unit}',
            due_date: '${plan.dueDate}',
            status: '${mapStatus(plan.status)}',
            manager: '${plan.manager}',
            note: '${plan.remark || '-'}'
          })">상세</button>
        </td>
        <td>
          <button class="btn btn-warning btn-sm" onclick="openPlanEditModal({
            id: ${plan.planId},
            plan_date: '${plan.planDate}',
            item_code: '${plan.itemCode}',
            item_name: '${plan.itemName}',
            quantity: ${plan.planQty},
            unit: '${plan.unit}',
            due_date: '${plan.dueDate}',
            status: '${mapStatus(plan.status)}',
            manager: '${plan.manager}',
            note: '${plan.remark || ''}'
          })">수정</button>
        </td>
        <td>
          <button class="btn btn-danger btn-sm" onclick="alert('삭제 기능은 추후 구현 예정')">삭제</button>
        </td>
      </tr>
    `;

    // 테이블에 행 추가
    tbody.insertAdjacentHTML("beforeend", row);
  });
}

// ✅ 계획번호 형식 생성 함수 (예: PP202405001)
function generatePlanNo(id) {
  // 오늘 날짜 기준 연월 + planId 기준 번호 생성
  const today = new Date();
  const yyyyMM = today.getFullYear().toString() + String(today.getMonth() + 1).padStart(2, '0');
  return `PP${yyyyMM}${String(id).padStart(3, '0')}`;
}

// ✅ 상태 코드 매핑 함수 (Enum → 한글)
function mapStatus(status) {
  switch (status) {
    case 'PLANNED': return '계획';
    case 'ORDERED': return '지시됨';
    case 'COMPLETED': return '완료';
    default: return '-';
  }
}



// ✅ 일반 셀렉트로 품목 목록 불러오기
function loadItemOptions() {
  fetch("/api/item/list?tab=product")
    .then(response => response.json())
    .then(data => {
      const select = document.getElementById("itemSelect");

      // ✅ 기존 옵션 초기화
      select.innerHTML = "";

      // ✅ 기본 옵션 추가
      const defaultOption = document.createElement("option");
      defaultOption.value = "";
      defaultOption.disabled = true;
      defaultOption.selected = true;
      defaultOption.textContent = "품목을 선택하세요";
      select.appendChild(defaultOption);

      // ✅ product 타입만 필터링해서 옵션 추가
      const items = data.content || [];
      const products = items.filter(item => item.type === "product");

      products.forEach(item => {
        const option = document.createElement("option");
        option.value = item.id; // 제출될 item_id
        option.textContent = `[${item.code}] ${item.name}`;
        option.dataset.code = item.code;
        option.dataset.name = item.name;
        option.dataset.unit = item.unit;
        select.appendChild(option);
      });
    })
    .catch(error => {
      console.error("품목 목록 로딩 실패", error);
    });
}
// ✅ select 요소에 change 이벤트 리스너 추가
document.addEventListener("DOMContentLoaded", function () {
  
  document.getElementById("itemSelect").addEventListener("change", function (e) {
    const selectedOption = e.target.selectedOptions[0];
    if (!selectedOption) return;

    // ✅ dataset에서 정보 추출하여 입력란 자동 채움
    document.getElementById("itemCode").value = selectedOption.dataset.code || '';
    document.getElementById("itemName").value = selectedOption.dataset.name || '';
    document.getElementById("unit").value = selectedOption.dataset.unit || '';

    document.getElementById("itemCode").readOnly = true;
    document.getElementById("itemName").readOnly = true;
    document.getElementById("unit").readOnly = true;
  });
});

function openPlanAddModal() {
  const modalEl = document.getElementById("planRegisterModal");
  if (!modalEl) return;

  document.getElementById("planForm").reset();

  // ✅ 필드 초기화
  document.getElementById("itemCode").value = "";
  document.getElementById("itemName").value = "";
  document.getElementById("unit").value = "";
  document.getElementById("itemCode").readOnly = true;
  document.getElementById("itemName").readOnly = true;
  document.getElementById("unit").readOnly = true;

  // ✅ 일반 select 선택 초기화
  document.getElementById("itemSelect").selectedIndex = 0;

  // ✅ 목록 다시 불러오기 (모달이 열릴 때마다)
  loadItemOptions();

  // ✅ 폼 설정
  
  document.getElementById("submitBtn").textContent = "등록";

  const modal = new bootstrap.Modal(modalEl);
  modal.show();
}




