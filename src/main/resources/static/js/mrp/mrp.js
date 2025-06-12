// File: src/main/resources/static/js/mrp/mrp.js (진짜 최종 완성본)

// ────────────────────────────────────────────────────────
// 전역 상수 및 전역 변수 선언
// ────────────────────────────────────────────────────────
const pageSize = 5;
let currentSortKey = "mrpId";
let currentSortDir = "desc";
let currentMrpList = [];
let currentPage = 1;
let totalPages = 1;
let currentModalIdx = -1;
let mrpModalInstance = null;
let lastModalMrpData = null;

// ────────────────────────────────────────────────────────
// UI 업데이트 함수
// ────────────────────────────────────────────────────────
/**
 * 하단 툴바 버튼(생산계획, 발주)의 활성/비활성 및 텍스트를 업데이트합니다.
 */
function updateToolbarButtons() {
    const checkedRows = Array.from(document.querySelectorAll('.bom-checkbox:checked')).map(cb => cb.closest('tr'));
    const btnPlan = document.getElementById('btn-create-plan');
    const btnOrder = document.getElementById('btn-create-order');

    const originalPlanText = "생산계획으로 이동";
    const originalOrderText = "발주 등록";

    if (checkedRows.length === 0) {
        btnPlan.disabled = true;
        btnOrder.disabled = true;
        btnPlan.textContent = originalPlanText;
        btnOrder.textContent = originalOrderText;
        return;
    }

    const productionItems = checkedRows.filter(tr => parseInt(tr.dataset.shortageQty || "0", 10) === 0 && parseInt(tr.dataset.totalqty || "0", 10) > 0);
    const orderItems = checkedRows.filter(tr => parseInt(tr.dataset.purchaseQty || "0", 10) > 0);

    btnPlan.disabled = productionItems.length === 0;
    btnPlan.textContent = productionItems.length > 0 ? `${originalPlanText} (${productionItems.length}개)` : originalPlanText;
    btnOrder.disabled = orderItems.length === 0;
    btnOrder.textContent = orderItems.length > 0 ? `${originalOrderText} (${orderItems.length}개)` : originalOrderText;
}

// ────────────────────────────────────────────────────────
// 데이터 로드 및 렌더링 함수
// ────────────────────────────────────────────────────────

function onMrpCheckboxChange() {
    const checkedCodes = Array.from(document.querySelectorAll('.mrp-checkbox:checked')).map(cb => cb.dataset.code);
    if (checkedCodes.length === 0) {
        renderBomList([]);
    } else {
        loadAllBomLists(checkedCodes);
    }
}

function loadAllBomLists(codes) {
    const fetchPromises = codes.map(code =>
        fetch(`/api/mrp/bom/${encodeURIComponent(code)}`).then(res => res.ok ? res.json() : Promise.resolve([]))
    );

    Promise.all(fetchPromises)
        .then(results => {
            const combinedComponents = results.flat();
            renderBomList(combinedComponents);
        });
}

function loadMrpList(page, callback) {
    currentPage = page;
    const params = new URLSearchParams({
        page,
        size: pageSize,
        sortKey: currentSortKey,
        sortDir: currentSortDir,
        startDate: document.getElementById("startDate").value,
        endDate: document.getElementById("endDate").value,
        itemSearch: document.getElementById("itemSearch").value,
    }).toString();

    fetch(`/api/mrp/list?${params}`)
        .then(res => res.json())
        .then(data => {
            renderMrpList(data, page);
            renderPagination(data.currentPage, data.totalPages);
            if (callback) callback();
        })
        .catch(err => console.error("MRP 리스트 로드 실패:", err));
}

function renderMrpList(data, page) {
    const tbody = document.getElementById("mrpTableBody");
    tbody.innerHTML = "";
    currentMrpList = data.content || [];
    totalPages = data.totalPages || 1;

    if (!currentMrpList.length) {
        tbody.innerHTML = `<tr><td colspan="13" class="text-center">데이터가 없습니다.</td></tr>`;
        renderBomList([]);
        return;
    }

    currentMrpList.forEach((mrp, idx) => {
        const rowNumber = data.totalElements - ((page - 1) * pageSize + idx);
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><input type="checkbox" class="mrp-checkbox" data-code="${mrp.itemCode}" onchange="onMrpCheckboxChange()"></td>
            <td class="col-no">${rowNumber}</td>
            <td>${mrp.itemCode ?? '-'}</td>
            <td>${mrp.itemName ?? '-'}</td>
            <td>${mrp.spec ?? '-'}</td>
            <td>${mrp.unit ?? '-'}</td>
            <td>${(mrp.orderQty ?? 0).toLocaleString()}</td>
            <td>${(mrp.stockQty ?? 0).toLocaleString()}</td>
            <td>${(mrp.shortageQty ?? 0).toLocaleString()}</td>
            <td>${mrp.dueDate ?? '-'}</td>
            <td>${mrp.leadTime ?? 0}</td>
            <td>${mrp.mrpStatus ?? '-'}</td>
            <td><button class="btn btn-info btn-sm" onclick="showMrpDetailModalByIndex(${idx})">상세</button></td>
        `;
        tbody.appendChild(tr);
    });
    renderBomList([]);
}

function renderPagination(currentPage, totalPages) {
    const pagination = document.getElementById("pagination");
    pagination.innerHTML = "";
    if (totalPages <= 0) return;
    const createPageItem = (text, pageNum, isDisabled = false, isActive = false) => {
        const li = document.createElement("li");
        li.className = `page-item ${isDisabled ? 'disabled' : ''} ${isActive ? 'active' : ''}`;
        li.innerHTML = `<a class="page-link" href="#">${text}</a>`;
        if (!isDisabled) {
            li.onclick = (e) => { e.preventDefault(); loadMrpList(pageNum); };
        }
        return li;
    };
    pagination.appendChild(createPageItem('이전', currentPage - 1, currentPage === 1));
    for (let i = 1; i <= totalPages; i++) {
        pagination.appendChild(createPageItem(i, i, false, i === currentPage));
    }
    pagination.appendChild(createPageItem('다음', currentPage + 1, currentPage === totalPages));
}

function renderBomList(bomList) {
    const tbody = document.getElementById("bomListTableBody");
    tbody.innerHTML = "";
    document.getElementById('bomSelectAll').checked = false;

    if (!bomList.length) {
        tbody.innerHTML = `<tr><td colspan="13" class="text-center">소요 자재 데이터가 없습니다.</td></tr>`;
        updateToolbarButtons();
        return;
    }

    bomList.forEach((bom, idx) => {
        const tr = document.createElement("tr");
        tr.dataset.code = bom.childCode ?? "";
        tr.dataset.shortageQty = bom.shortageQty ?? "0";
        tr.dataset.purchaseQty = bom.purchaseQty ?? "0";
        tr.dataset.totalqty = bom.totalQty ?? "0";
        tr.innerHTML = `
            <td><input type="checkbox" class="bom-checkbox"></td>
            <td class="col-no">${idx + 1}</td>
            <td>${bom.childCode ?? '-'}</td>
            <td>${bom.childName ?? '-'}</td>
            <td>${bom.spec ?? '-'}</td>
            <td>${bom.unit ?? '-'}</td>
            <td>${(bom.totalQty ?? 0).toLocaleString()}</td>
            <td>${(bom.stockQty ?? 0).toLocaleString()}</td>
            <td>${(bom.shortageQty ?? 0).toLocaleString()}</td>
            <td>${(bom.safetyStock ?? 0).toLocaleString()}</td>
            <td>${(bom.purchaseQty ?? 0).toLocaleString()}</td>
            <td>${bom.purchaseLeadTime ?? 0}</td>
            <td>${bom.expectedDate ?? '-'}</td>
        `;
        tbody.appendChild(tr);
    });
    updateToolbarButtons();
}

// ────────────────────────────────────────────────────────
// 모달 관련 함수
// ────────────────────────────────────────────────────────
function showMrpDetailModal(mrp, openNew) {
    if (openNew) {
        mrpModalInstance = bootstrap.Modal.getOrCreateInstance(document.getElementById('mrpDetailModal'));
    }
    lastModalMrpData = mrp;

    // [수정] 누락되었던 모든 모달 내용 채우기 코드 복원
    // A. 기본 정보
    document.getElementById("mrpDetailId").textContent = mrp.mrpId ?? '-';
    document.getElementById("mrpDetailCreateDate").textContent = mrp.createDate || '-';
    document.getElementById("mrpDetailDueDate").textContent = mrp.dueDate || '-';
    document.getElementById("mrpDetailPlanType").textContent = mrp.planType || '-';
    document.getElementById("mrpDetailStatus").textContent = mrp.status || '-';
    // B. 품목 정보
    document.getElementById("mrpDetailItemCode").textContent = mrp.itemCode || '-';
    document.getElementById("mrpDetailItemName").textContent = mrp.itemName || '-';
    document.getElementById("mrpDetailItemType").textContent = mrp.itemType || '-';
    document.getElementById("mrpDetailUnit").textContent = mrp.unit || '-';
    document.getElementById("mrpDetailSpec").textContent = mrp.spec || '-';
    document.getElementById("mrpDetailStock").textContent = (mrp.stockQty ?? 0).toLocaleString();
    document.getElementById("mrpDetailSafetyStock").textContent = (mrp.safetyStock ?? 0).toLocaleString();
    // C. 수량·리드타임
    document.getElementById("mrpDetailRequiredQty").textContent = (mrp.requiredQty ?? 0).toLocaleString();
    document.getElementById("mrpDetailShortQty").textContent = (mrp.shortageQty ?? 0).toLocaleString();
    document.getElementById("mrpDetailPurchaseLeadTime").textContent = mrp.purchaseLeadTime ?? '0';
    document.getElementById("mrpDetailProductionLeadTime").textContent = mrp.productionLeadTime ?? '0';
    document.getElementById("mrpDetailOrderableDate").textContent = mrp.orderableDate || '-';
    
    // [수정] 재고 위치(Warehouse) 로드 로직 복원
    const locationDropdown = document.getElementById("mrpDetailLocationDropdown");
    locationDropdown.innerHTML = `<option>로딩 중...</option>`;
    fetch(`/api/mrp/warehouses/${encodeURIComponent(mrp.itemCode)}`)
        .then(res => res.ok ? res.json() : [])
        .then(data => {
            locationDropdown.innerHTML = "";
            if (data.length === 0) {
                locationDropdown.innerHTML = `<option>데이터 없음</option>`;
                return;
            }
            data.forEach(item => {
                locationDropdown.innerHTML += `<option value="${item.warehouseName}">${item.warehouseName} (${item.quantity || 0})</option>`;
            });
        })
        .catch(err => {
            console.warn("재고 위치 로딩 실패:", err);
            locationDropdown.innerHTML = `<option>오류 발생</option>`;
        });

    // D. BOM 구성
    const bomBody = document.getElementById("mrpDetailBomBody");
    bomBody.innerHTML = "";
    (mrp.bomComponents || []).forEach(c => {
        bomBody.innerHTML += `
            <tr>
                <td>${c.childCode ?? '-'}</td>
                <td>${c.childName ?? '-'}</td>
                <td>${c.perParentQty ?? 0}</td>
                <td>${(c.totalQty ?? 0).toLocaleString()}</td>
                <td>${(c.stockQty ?? 0).toLocaleString()}</td>
                <td>${(c.shortageQty ?? 0).toLocaleString()}</td>
                <td>${c.leadTime ?? 0}</td>
            </tr>
        `;
    });

    // E. 버튼 로직 처리
    const modalBtnPlan = document.getElementById("modal-create-plan");
    const modalBtnOrder = document.getElementById("modal-create-order");
    const bomComponents = mrp.bomComponents || [];
    const productionItems = bomComponents.filter(c => c.shortageQty === 0 && c.totalQty > 0);
    const orderItems = bomComponents.filter(c => c.purchaseQty > 0);

    modalBtnPlan.disabled = productionItems.length === 0;
    modalBtnPlan.textContent = productionItems.length > 0 ? `생산계획으로 이동 (${productionItems.length}개)` : '생산계획으로 이동';
    modalBtnOrder.disabled = orderItems.length === 0;
    modalBtnOrder.textContent = orderItems.length > 0 ? `발주 등록 (${orderItems.length}개)` : '발주 등록';

    modalBtnPlan.onclick = () => {
        if (productionItems.length === 0) return alert("생산계획을 세울 자재가 없습니다.");
        const pairs = productionItems.map(c => `${c.childCode}:${c.totalQty}`);
        if (!confirm("생산계획 페이지로 이동하시겠습니까?")) return;
        window.location.href = `/plan?items=${encodeURIComponent(Array.from(new Set(pairs)).join(","))}`;
    };

    modalBtnOrder.onclick = () => {
        if (orderItems.length === 0) return alert("발주할 자재가 없습니다.");
        const pairs = orderItems.map(c => `${c.childCode}:${c.purchaseQty}`);
        if (!confirm("발주 등록하시겠습니까?")) return;
        window.location.href = `/place/purchase-order?items=${encodeURIComponent(Array.from(new Set(pairs)).join(","))}`;
    };
    
    document.getElementById("modalPrevBtn").disabled = (currentModalIdx <= 0);
    document.getElementById("modalNextBtn").disabled = (currentModalIdx >= currentMrpList.length - 1);

    if (openNew) {
        mrpModalInstance.show();
    }
}

function showMrpDetailModalByIndex(idx) {
    currentModalIdx = idx;
    const mrpId = currentMrpList[idx]?.mrpId;
    if (!mrpId) return;
    fetch(`/api/mrp/detail/${mrpId}`)
      .then(res => res.json())
      .then(data => showMrpDetailModal(data, true))
      .catch(err => console.error("상세 모달 데이터 로드 실패:", err));
}

function moveModal(offset) {
    const newIdx = currentModalIdx + offset;
    if (newIdx >= 0 && newIdx < currentMrpList.length) {
        showMrpDetailModalByIndex(newIdx);
    }
}

// ────────────────────────────────────────────────────────
// 기타 유틸리티 함수
// ────────────────────────────────────────────────────────

/**
 * [수정] HTML과의 호환성을 위해 calculateMrp 함수를 복원합니다.
 * 이 함수는 단순히 검색 버튼을 누르는 것과 동일하게 동작합니다.
 */
function calculateMrp() {
    const startDate = document.getElementById("startDate").value.trim();
    const endDate   = document.getElementById("endDate").value.trim();
    const itemSearch = document.getElementById("itemSearch").value.trim();

    if (!startDate || !endDate || !itemSearch) {
        alert("기간(From), 기간(To), 그리고 품목명/코드를 모두 입력해주세요.");
        return;
    }
    if (startDate > endDate) {
        alert("기간(From)이 기간(To)보다 클 수 없습니다.");
        return;
    }

    loadMrpList(1);
}

// ────────────────────────────────────────────────────────
// 페이지 초기화 및 이벤트 리스너
// ────────────────────────────────────────────────────────
window.onload = function() {
    document.getElementById("startDate").value = new Date().toISOString().substring(0, 10);

    // 공통 이벤트 핸들러
    document.getElementById('selectAll')?.addEventListener('change', function() {
        document.querySelectorAll('.mrp-checkbox').forEach(cb => cb.checked = this.checked);
        onMrpCheckboxChange();
    });

    document.getElementById('bomSelectAll')?.addEventListener('change', function() {
        document.querySelectorAll('.bom-checkbox').forEach(cb => cb.checked = this.checked);
        updateToolbarButtons();
    });
    
    document.getElementById('bomListTableBody').addEventListener('change', e => {
        if (e.target.classList.contains('bom-checkbox')) {
            updateToolbarButtons();
        }
    });

    document.getElementById('btn-create-plan')?.addEventListener('click', () => {
        const selectedRows = Array.from(document.querySelectorAll('#bomListTableBody .bom-checkbox:checked'));
        const productionItems = selectedRows.map(cb => cb.closest('tr'))
            .filter(tr => parseInt(tr.dataset.shortageQty || "0", 10) === 0 && parseInt(tr.dataset.totalqty || "0", 10) > 0);
        
        if (productionItems.length === 0) return alert("생산계획을 세울 자재를 선택해주세요.");
        
        const pairs = productionItems.map(tr => `${tr.dataset.code}:${tr.dataset.totalqty}`);
        if (!confirm("생산계획 페이지로 이동하시겠습니까?")) return;
        window.location.href = `/plan?items=${encodeURIComponent(Array.from(new Set(pairs)).join(","))}`;
    });

    document.getElementById('btn-create-order')?.addEventListener('click', () => {
        const selectedRows = Array.from(document.querySelectorAll('#bomListTableBody .bom-checkbox:checked'));
        const orderItems = selectedRows.map(cb => cb.closest('tr'))
             .filter(tr => parseInt(tr.dataset.purchaseQty || "0", 10) > 0);

        if (orderItems.length === 0) return alert("발주할 자재를 선택해주세요.");
        
        const pairs = orderItems.map(tr => `${tr.dataset.code}:${tr.dataset.purchaseQty}`);
        if (!confirm("발주 등록하시겠습니까?")) return;
        window.location.href = `/place/purchase-order?items=${encodeURIComponent(Array.from(new Set(pairs)).join(","))}`;
    });

    // 검색 버튼에 직접 이벤트 리스너 추가 (HTML의 onclick 제거 가능)
    document.getElementById("btn-search")?.addEventListener("click", calculateMrp);

    document.getElementById("itemSearch")?.addEventListener("keypress", e => {
        if (e.key === "Enter") calculateMrp();
    });

    document.getElementById("btn-reset")?.addEventListener("click", () => {
        document.getElementById("startDate").value = new Date().toISOString().substring(0, 10);
        document.getElementById("endDate").value = "";
        document.getElementById("itemSearch").value = "";
        loadMrpList(1);
    });

    // 초기 로드
    updateToolbarButtons();
    loadMrpList(1);
};