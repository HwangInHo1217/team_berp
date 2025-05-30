// ✅ 페이지 로드 시 목록 자동 조회
document.addEventListener("DOMContentLoaded", function () {
  fetchProdPlanList(0);

  document.getElementById("itemSelect").addEventListener("change", function (e) {
    const selectedOption = e.target.selectedOptions[0];
    if (!selectedOption) return;

    document.getElementById("itemCode").value = selectedOption.dataset.code || '';
    document.getElementById("itemName").value = selectedOption.dataset.name || '';
    document.getElementById("unit").value = selectedOption.dataset.unit || '';

    document.getElementById("itemCode").readOnly = true;
    document.getElementById("itemName").readOnly = true;
    document.getElementById("unit").readOnly = true;
  });
  document.getElementById("statusFilter").addEventListener("change", () => {
    fetchProdPlanList(0); // 상태 변경 시 첫 페이지로 조회
  });

});

// ✅ 생산계획 목록 조회 함수
function fetchProdPlanList(page = 0) {
  const keyword = document.getElementById("keywordInput")?.value || '';
  const startDate = document.getElementById("startDateInput")?.value || '';
  const endDate = document.getElementById("endDateInput")?.value || '';
  const status = document.getElementById("statusFilter")?.value || ''; // 🔥 추가
  const params = new URLSearchParams({ page, size: 10, keyword, startDate, endDate, status });

  fetch(`/api/prod-plan/list?${params}`)
    .then(res => res.json())
    .then(data => {
      renderProdPlanTable(data.content);
      renderPagination(data.totalPages, data.number);
    })
    .catch(err => {
      console.error("조회 실패:", err);
      alert("생산계획 조회 중 오류 발생");
    });
}

// ✅ 생산계획 테이블 렌더링
function renderProdPlanTable(planList) {
  const tbody = document.querySelector("table tbody");
  tbody.innerHTML = "";

  if (!planList || planList.length === 0) {
    tbody.insertAdjacentHTML("beforeend", `<tr><td colspan="14">등록된 생산계획이 없습니다.</td></tr>`);
    return;
  }

  planList.forEach(plan => {
    const row = `
      <tr>
        <td><input type="checkbox" /></td>
        <td>${generatePlanNo(plan.planId)}</td>
        <td>${plan.planDate || ''}</td>
        <td>${plan.itemCode}</td>
        <td>${plan.itemName}</td>
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
            item_code: '${plan.itemCode}',
            item_name: '${plan.itemName}',
            quantity: ${plan.planQty},
            unit: '${plan.unit}',
            due_date: '${plan.dueDate}',
            status: '${plan.status}',
            manager: '${plan.manager}',
            note: '${plan.remark || ''}'
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
            status: '${plan.status}',
            manager: '${plan.manager}',
            note: '${plan.remark || ''}'
          })">수정</button>
        </td>
        <td>
          <button class="btn btn-danger btn-sm" onclick="deleteProdPlan(${plan.planId})">삭제</button>
        </td>
      </tr>
    `;
    tbody.insertAdjacentHTML("beforeend", row);
  });
}

// ✅ 계획번호 생성
function generatePlanNo(id) {
  const today = new Date();
  const yyyyMM = today.getFullYear().toString() + String(today.getMonth() + 1).padStart(2, '0');
  return `PP${yyyyMM}${String(id).padStart(3, '0')}`;
}

// ✅ 상태 매핑
function mapStatus(status) {
  switch (status) {
    case 'PLANNED': return '계획';
    case 'ORDERED': return '지시됨';
    case 'COMPLETED': return '완료';
    default: return '-';
  }
}

// ✅ 페이지네이션
function renderPagination(totalPages, currentPage) {
  const pagination = document.getElementById("pagination");
  if (!pagination) return;
  pagination.innerHTML = "";
  if (totalPages <= 1) return;

  if (currentPage > 0) {
    const prevBtn = document.createElement("button");
    prevBtn.textContent = "이전";
    prevBtn.className = "btn btn-sm btn-outline-primary me-1";
    prevBtn.onclick = () => fetchProdPlanList(currentPage - 1);
    pagination.appendChild(prevBtn);
  }

  for (let i = 0; i < totalPages; i++) {
    const btn = document.createElement("button");
    btn.textContent = i + 1;
    btn.className = `btn btn-sm ${i === currentPage ? 'btn-primary' : 'btn-outline-primary'} me-1`;
    btn.onclick = () => fetchProdPlanList(i);
    pagination.appendChild(btn);
  }

  if (currentPage < totalPages - 1) {
    const nextBtn = document.createElement("button");
    nextBtn.textContent = "다음";
    nextBtn.className = "btn btn-sm btn-outline-primary me-1";
    nextBtn.onclick = () => fetchProdPlanList(currentPage + 1);
    pagination.appendChild(nextBtn);
  }
}

// ✅ 품목 로딩
function loadItemOptions() {
  return fetch("/api/item/all-products")
    .then(res => res.json())
    .then(data => {
      const products = data.products;
      const select = document.getElementById("itemSelect");
      select.innerHTML = "";
      const defaultOption = new Option("품목을 선택하세요", "", true, true);
      defaultOption.disabled = true;
      select.appendChild(defaultOption);

      products.forEach(item => {
        const option = new Option(`[${item.code}] ${item.name}`, item.id);
        option.dataset.code = item.code;
        option.dataset.name = item.name;
        option.dataset.unit = item.unit;
        select.appendChild(option);
      });
    })
    .catch(err => {
      console.error("전체 product 불러오기 실패", err);
    });
}

// ✅ 등록
function addProdPlan() {
  const form = document.getElementById("planForm");
  const dto = {
    item_id: document.getElementById("itemSelect").value,
    plan_date: form.plan_date.value,
    quantity: form.quantity.value,
    due_date: form.due_date.value,
    unit: form.unit.value,
    status: form.status.value,
    manager: form.manager.value,
    note: form.note.value
  };

  fetch(`/plan/add`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(dto)
  })
    .then(res => {
      if (!res.ok) throw new Error("등록 실패");
      return res.text();
    })
    .then(() => {
      alert("등록 완료");
      const modalInstance = bootstrap.Modal.getOrCreateInstance(document.getElementById("planRegisterModal"));
      modalInstance.hide();
	  console.log("모달 닫힘 직후 실행됨"); // 디버깅용

      fetchProdPlanList();
    })
    .catch(err => {
      console.error(err);
      alert("등록 중 오류 발생");
    });
}

// ✅ 수정
function updateProdPlan(planId) {
  const form = document.getElementById("planForm");
  const dto = {
    item_id: document.getElementById("itemSelect").value,
    plan_date: form.plan_date.value,
    quantity: form.quantity.value,
    due_date: form.due_date.value,
    unit: form.unit.value,
    status: form.status.value,
    manager: form.manager.value,
    note: form.note.value
  };

  fetch(`/api/prod-plan/update/${planId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(dto)
  })
    .then(res => {
      if (!res.ok) throw new Error("수정 실패");
      return res.text();
    })
    .then(() => {
      alert("수정 완료");
      bootstrap.Modal.getInstance(document.getElementById("planRegisterModal")).hide();
      fetchProdPlanList();
    })
    .catch(err => {
      console.error(err);
      alert("수정 중 오류 발생");
    });
}

// ✅ 삭제
function deleteProdPlan(planId) {
  if (!confirm("정말로 삭제하시겠습니까?")) return;

  fetch(`/api/prod-plan/delete/${planId}`, { method: "DELETE" })
    .then(res => {
      if (!res.ok) throw new Error("삭제 실패");
      return res.text();
    })
    .then(() => {
      alert("삭제되었습니다.");
      fetchProdPlanList();
    })
    .catch(err => {
      alert("해당 생산계획은 MRP에서 사용 중이므로 삭제할 수 없습니다.");
      console.error(err);
    });
}

// ✅ 상세 모달 열기
function openPlanDetailModal(data) {
  document.getElementById("planDetailPlanNo").textContent = data.plan_no;
  document.getElementById("planDetailPlanDate").textContent = data.plan_date;
  document.getElementById("planDetailItemCode").textContent = data.item_code;
  document.getElementById("planDetailItemName").textContent = data.item_name;
  document.getElementById("planDetailQuantity").textContent = data.quantity;
  document.getElementById("planDetailUnit").textContent = data.unit;
  document.getElementById("planDetailDueDate").textContent = data.due_date;
  document.getElementById("planDetailStatus").textContent = mapStatus(data.status);
  document.getElementById("planDetailManager").textContent = data.manager;
  document.getElementById("planDetailNote").textContent = data.note;

  const modal = new bootstrap.Modal(document.getElementById("planDetailModal"));
  modal.show();
}

// ✅ 등록 모달 열기
function openPlanAddModal() {
  const form = document.getElementById("planForm");
  form.reset();
  document.getElementById("submitBtn").textContent = "등록";
  document.getElementById("planForm").action = ""; // ✅ 등록일 때는 action을 비워줘야 함

  loadItemOptions().then(() => {
    const modal = new bootstrap.Modal(document.getElementById("planRegisterModal"));
    modal.show();
  });
}


// ✅ 수정 모달 열기
function openPlanEditModal(data) {
  loadItemOptions().then(() => {
    document.getElementById("planDate").value = data.plan_date;
    document.getElementById("itemCode").value = data.item_code;
    document.getElementById("itemName").value = data.item_name;
    document.getElementById("quantity").value = data.quantity;
    document.getElementById("unit").value = data.unit;
    document.getElementById("dueDate").value = data.due_date;
    document.getElementById("status").value = data.status;
    document.getElementById("manager").value = data.manager;
    document.getElementById("note").value = data.note;

    const select = document.getElementById("itemSelect");
    for (let opt of select.options) {
      if (opt.dataset.code === data.item_code) {
        opt.selected = true;
        break;
      }
    }

    document.getElementById("planForm").action = `/plan/update/${data.id}`;
    document.getElementById("submitBtn").textContent = "수정";
    const modal = new bootstrap.Modal(document.getElementById("planRegisterModal"));
    modal.show();
  });
}

// ✅ 제출 버튼 핸들러
document.getElementById("submitBtn").onclick = function (e) {
  e.preventDefault();

  const form = document.getElementById("planForm");
  const action = form.action;
  const match = action.match(/\/update\/(\d+)/);
  if (match) {
    const planId = match[1];
    updateProdPlan(planId);
  } else {
    addProdPlan();
  }
};
