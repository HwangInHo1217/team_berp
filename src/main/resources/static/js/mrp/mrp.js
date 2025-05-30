// File: src/main/resources/static/js/mrp/mrp.js

const pageSize = 5;  // 페이지당 보여줄 개수
let currentSortKey = "mrpId";
let currentSortDir = "desc";

let currentMrpList = [];
let currentPage = 1;
let totalPages = 1;
let currentModalIdx = -1;

// 모달 인스턴스 전역 저장 (중복 backdrop 제거용)
let mrpModalInstance = null;

// 최초 로딩
window.onload = function() {
    document.getElementById('selectAll')?.addEventListener('change', function() {
        const checked = this.checked;
        document.querySelectorAll('.mrp-checkbox').forEach(cb => {
            cb.checked = checked;
            onMrpCheckboxChange(cb);
        });
    });
    document.getElementById('bomSelectAll')?.addEventListener('change', function() {
        const checked = this.checked;
        document.querySelectorAll('.bom-checkbox').forEach(cb => cb.checked = checked);
    });
    document.getElementById('btn-create-plan')?.addEventListener('click', () => {
        const codes = Array.from(document.querySelectorAll('.bom-checkbox:checked'))
                           .map(cb => cb.dataset.code);
        if (!codes.length) return alert('생산계획을 생성할 자재를 선택하세요.');
        window.location.href = `/prod-plan?items=${codes.join(',')}`;
    });
    document.getElementById('btn-create-order')?.addEventListener('click', () => {
        const codes = Array.from(document.querySelectorAll('.bom-checkbox:checked'))
                           .map(cb => cb.dataset.code);
        if (!codes.length) return alert('발주할 자재를 선택하세요.');
        window.location.href = `/purchase-order?items=${codes.join(',')}`;
    });

    loadMrpList(1);
};

// 정렬
function sortMrp(key) {
    if (currentSortKey === key) {
        currentSortDir = currentSortDir === "asc" ? "desc" : "asc";
    } else {
        currentSortKey = key;
        currentSortDir = "asc";
    }
    loadMrpList(1);
}

// 상단 MRP 리스트 조회
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
        custSearch: document.getElementById("custSearch").value,
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

    if (!data.content || data.content.length === 0) {
        tbody.innerHTML = `<tr><td colspan="15" class="empty-row">등록된 품목 데이터가 없습니다.</td></tr>`;
        renderBomList([]);
        return;
    }

    data.content.forEach((mrp, idx) => {
        const rowNumber = data.totalElements - ((page - 1) * pageSize + idx);
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><input type="checkbox" class="mrp-checkbox" data-code="${mrp.itemCode}" onchange="onMrpCheckboxChange(this)"></td>
            <td class="col-no">${rowNumber}</td>
            <td>${mrp.itemCode ?? '-'}</td>
            <td>${mrp.itemName ?? '-'}</td>
            <td>${mrp.custName ?? '-'}</td>
            <td>${mrp.spec ?? '-'}</td>
            <td>${mrp.unit ?? '-'}</td>
            <td>${mrp.prodQty ?? 0}</td>
            <td>${mrp.orderQty ?? 0}</td>
            <td>${mrp.stockQty ?? 0}</td>
            <td>${mrp.shortageQty ?? 0}</td>
            <td>${mrp.dueDate ?? '-'}</td>
            <td>${mrp.leadTime ?? 0}</td>
            <td>${mrp.mrpStatus ?? '-'}</td>
            <td><button class="btn btn-info btn-sm" onclick="showMrpDetailModalByIndex(${idx})">상세</button></td>
        `;
        tr.addEventListener('click', e => {
            if (e.target.type === 'checkbox' || e.target.tagName === 'BUTTON') return;
            const cb = tr.querySelector('.mrp-checkbox');
            cb.checked = !cb.checked;
            onMrpCheckboxChange(cb);
        });
        tbody.appendChild(tr);
    });

    renderBomList([]);
}

function onMrpCheckboxChange(checkbox) {
    const code = checkbox.dataset.code;
    if (checkbox.checked) {
        loadBomList(code);
    } else {
        renderBomList([]);
    }
}

function renderPagination(currentPage, totalPages) {
    const pagination = document.getElementById("pagination");
    pagination.innerHTML = "";

    const prevLi = document.createElement("li");
    prevLi.className = "page-item" + (currentPage === 1 ? " disabled" : "");
    prevLi.innerHTML = `<a class="page-link" href="#">이전</a>`;
    prevLi.onclick = () => currentPage > 1 && loadMrpList(currentPage - 1);
    pagination.appendChild(prevLi);

    for (let i = 1; i <= totalPages; i++) {
        const li = document.createElement("li");
        li.className = "page-item" + (i === currentPage ? " active" : "");
        li.innerHTML = `<a class="page-link" href="#">${i}</a>`;
        li.onclick = () => loadMrpList(i);
        pagination.appendChild(li);
    }

    const nextLi = document.createElement("li");
    nextLi.className = "page-item" + (currentPage === totalPages ? " disabled" : "");
    nextLi.innerHTML = `<a class="page-link" href="#">다음</a>`;
    nextLi.onclick = () => currentPage < totalPages && loadMrpList(currentPage + 1);
    pagination.appendChild(nextLi);
}

function showMrpDetailModalByIndex(idx) {
    currentModalIdx = idx;
    showMrpDetailModal(currentMrpList[idx], true);
    loadBomList(currentMrpList[idx].itemCode);
}

function loadBomList(itemCode) {
    if (!itemCode) {
        renderBomList([]);
        return;
    }
    fetch(`/api/mrp/bom/${itemCode}`)
        .then(res => {
            if (!res.ok) throw new Error(`BOM 로드 실패: ${res.status}`);
            return res.json();
        })
        .then(bomList => {
            if (Array.isArray(bomList)) renderBomList(bomList);
            else renderBomList([]);
        })
        .catch(err => {
            console.error(err);
            renderBomList([]);
        });
}

function renderBomList(bomList) {
    const tbody = document.getElementById("bomListTableBody");
    tbody.innerHTML = "";
    if (!bomList.length) {
        tbody.innerHTML = `<tr><td colspan="14" class="empty-row">소요 자재 데이터가 없습니다.</td></tr>`;
        return;
    }
    bomList.forEach((bom, idx) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><input type="checkbox" class="bom-checkbox" data-code="${bom.subItemCode}"></td>
            <td class="col-no">${idx + 1}</td>
            <td>${bom.subItemCode ?? '-'}</td>
            <td>${bom.subItemName ?? '-'}</td>
            <td>${bom.spec ?? '-'}</td>
            <td>${bom.unit ?? '-'}</td>
            <td>${bom.qty ?? 0}</td>
            <td>${bom.stockQty ?? 0}</td>
            <td>${bom.shortageQty ?? 0}</td>
            <td>${bom.safetyStock ?? 0}</td>
            <td>${bom.purchaseQty ?? 0}</td>
            <td>${bom.purchaseLeadTime ?? 0}</td>
            <td>${bom.expectedDate ?? '-'}</td>
            <td>
              <button class="btn btn-outline-primary btn-sm"
                      onclick="window.location.href='/purchase-order?items=${bom.subItemCode}'">
                발주
              </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function showMrpDetailModal(mrp, openModal) {
    // 기존 모달 인스턴스 제거
    if (mrpModalInstance) {
        mrpModalInstance.dispose();
        mrpModalInstance = null;
    }

    // A. 기본 정보
    document.getElementById("mrpDetailId").textContent         = mrp.mrpId          ?? '-';
    document.getElementById("mrpDetailCreateDate").textContent = mrp.baseDate       || '-';
    document.getElementById("mrpDetailDueDate").textContent    = mrp.dueDate        || '-';
    document.getElementById("mrpDetailPlanType").textContent   = mrp.source         || '-';
    document.getElementById("mrpDetailStatus").textContent     = mrp.mrpStatus      || '-';
    document.getElementById("mrpDetailOwner").textContent      = mrp.ownerName      || '-';

    // B. 품목 정보
    document.getElementById("mrpDetailItemCode").textContent    = mrp.itemCode       || '-';
    document.getElementById("mrpDetailItemName").textContent    = mrp.itemName       || '-';
    document.getElementById("mrpDetailItemType").textContent    = mrp.itemType       || '-';
    document.getElementById("mrpDetailUnit").textContent        = mrp.unit           || '-';
    document.getElementById("mrpDetailSpec").textContent        = mrp.spec           || '-';
    document.getElementById("mrpDetailSafetyStock").textContent = mrp.safetyStock    ?? '0';
    document.getElementById("mrpDetailStock").textContent       = mrp.stockQty       ?? '0';
    document.getElementById("mrpDetailLocation").textContent    = mrp.location       || '-';

    // C. 수량·리드타임
    document.getElementById("mrpDetailRequiredQty").textContent     = mrp.requiredQty    ?? '0';
    document.getElementById("mrpDetailShortQty").textContent        = mrp.shortageQty    ?? '0';
    document.getElementById("mrpDetailPurchaseLeadTime").textContent= mrp.purchaseLeadTime ?? '0';
    document.getElementById("mrpDetailProductionLeadTime").textContent = mrp.productionLeadTime ?? '0';
    document.getElementById("mrpDetailStartDate").textContent       = mrp.startDate      || '-';

    // D. BOM 구성
    const bomBody = document.getElementById("mrpDetailBomBody");
    bomBody.innerHTML = "";
    (mrp.bomComponents || []).forEach(c => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${c.childCode}</td>
            <td>${c.childName}</td>
            <td>${c.perParentQty}</td>
            <td>${c.totalQty}</td>
            <td>${c.stockQty}</td>
            <td>${c.shortageQty}</td>
            <td>${c.leadTime}</td>
            <td>${c.supplier}</td>
        `;
        bomBody.appendChild(tr);
    });

    // E. 연계 오더
    const poBody = document.getElementById("mrpDetailPoList");
    poBody.innerHTML = "";
    (mrp.purchaseOrders || []).forEach(po => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${po.poNo}</td>
            <td>${po.itemCode}</td>
            <td>${po.qty}</td>
            <td>${po.dueDate}</td>
            <td>${po.status}</td>
        `;
        poBody.appendChild(tr);
    });
    const woBody = document.getElementById("mrpDetailWoList");
    woBody.innerHTML = "";
    (mrp.workOrders || []).forEach(wo => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${wo.woNo}</td>
            <td>${wo.itemCode}</td>
            <td>${wo.qty}</td>
            <td>${wo.startDate}</td>
            <td>${wo.endDate}</td>
            <td>${wo.status}</td>
        `;
        woBody.appendChild(tr);
    });

    // F. 스케줄·이력
    const logDiv = document.getElementById("mrpDetailLog");
    logDiv.innerHTML = (mrp.history || []).map(h =>
        `<div>[${h.timestamp}] ${h.message}</div>`
    ).join("");

    // 네비게이션 버튼 활성/비활성
    document.getElementById("modalPrevBtn").disabled = currentModalIdx <= 0;
    document.getElementById("modalNextBtn").disabled = currentModalIdx >= currentMrpList.length - 1;

    // G. 액션 버튼 이벤트
    document.getElementById("detailBtnCreatePlan").onclick = () => {
        window.location.href = `/prod-plan?items=${mrp.itemCode}`;
    };
    document.getElementById("detailBtnCreateOrder").onclick = () => {
        window.location.href = `/purchase-order?items=${mrp.itemCode}`;
    };

    // 모달 띄우기
    mrpModalInstance = new bootstrap.Modal(document.getElementById('mrpDetailModal'));
    if (openModal) mrpModalInstance.show();
}

// 모달 이전/다음
function moveModal(offset) {
    let newIdx = currentModalIdx + offset;
    if (newIdx < 0 || newIdx >= currentMrpList.length) return;
    currentModalIdx = newIdx;
    showMrpDetailModal(currentMrpList[newIdx], true);
    loadBomList(currentMrpList[newIdx].itemCode);
}

// 버튼 연결
function calculateMrp() {
    loadMrpList(1);
}

function resetMrpForm() {
    ['startDate','endDate','itemSearch','custSearch'].forEach(id => {
        document.getElementById(id).value = "";
    });
    loadMrpList(1);
}

function downloadExcel() {
    alert("엑셀 다운로드 준비중!");
}
