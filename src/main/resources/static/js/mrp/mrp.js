const pageSize = 5;  // 페이지당 보여줄 개수
let currentSortKey = "mrpId";
let currentSortDir = "desc";

let currentMrpList = [];
let currentPage = 1;
let totalPages = 1;

let currentModalIdx = -1;

// 최초 로딩: 필요시 주석 처리 (자동 로딩 대신 버튼으로)
window.onload = function() {
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

// 상단(품목) 리스트 조회 및 렌더링
function loadMrpList(page, callback) {
    currentPage = page;
    // 검색 input에서 값 읽기
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
        });
}

// 품목 리스트 렌더링
function renderMrpList(data, page) {
    const tbody = document.getElementById("mrpTableBody");
    tbody.innerHTML = "";
    currentMrpList = data.content;
    totalPages = data.totalPages;

    if (!data.content || data.content.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="empty-row">등록된 품목 데이터가 없습니다.</td></tr>`;
        // 하위도 같이 초기화
        renderBomList([]);
        return;
    }

    data.content.forEach((mrp, idx) => {
		const rowNumber = data.totalElements - ((page - 1) * pageSize + idx);
		
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td class="col-no">${rowNumber}</td>
            <td>${mrp.itemCode ?? '-'}</td>
            <td>${mrp.itemName ?? '-'}</td>
            <td>${mrp.custName ?? '-'}</td>
            <td>${mrp.spec ?? '-'}</td>
            <td>${mrp.unit ?? '-'}</td>
            <td>${mrp.prodQty ?? 0}</td>
            <td>${mrp.orderQty ?? 0}</td>
            <td>
                <button class="btn btn-info btn-sm" onclick="showMrpDetailModalByIndex(${idx})">상세</button>
            </td>
        `;
        tbody.appendChild(tr);
    });

    // 리스트 갱신 시 하위 테이블(투입자재)도 비움
    renderBomList([]);
}

// 페이징
function renderPagination(currentPage, totalPages) {
    const pagination = document.getElementById("pagination");
    pagination.innerHTML = "";

    const prevLi = document.createElement("li");
    prevLi.className = "page-item" + (currentPage === 1 ? " disabled" : "");
    prevLi.innerHTML = `<a class="page-link" href="#">이전</a>`;
    prevLi.onclick = function() {
        if (currentPage > 1) loadMrpList(currentPage - 1);
    };
    pagination.appendChild(prevLi);

    for (let i = 1; i <= totalPages; i++) {
        const li = document.createElement("li");
        li.className = "page-item" + (i === currentPage ? " active" : "");
        li.innerHTML = `<a class="page-link" href="#">${i}</a>`;
        li.onclick = function() {
            loadMrpList(i);
        };
        pagination.appendChild(li);
    }

    const nextLi = document.createElement("li");
    nextLi.className = "page-item" + (currentPage === totalPages ? " disabled" : "");
    nextLi.innerHTML = `<a class="page-link" href="#">다음</a>`;
    nextLi.onclick = function() {
        if (currentPage < totalPages) loadMrpList(currentPage + 1);
    };
    pagination.appendChild(nextLi);
}

// 상세(모달) - 품목 선택
function showMrpDetailModalByIndex(idx) {
    currentModalIdx = idx;
    showMrpDetailModal(currentMrpList[idx], true);
    // 품목 상세를 선택하면 투입자재(BOM) 조회도 같이 요청
    loadBomList(currentMrpList[idx].itemCode);
}

// 하단(BOM/투입자재) 리스트 API
function loadBomList(itemCode) {
    // BOM이 없는 경우를 위해서 itemCode 없으면 비움
    if (!itemCode) {
        renderBomList([]);
        return;
    }
    fetch(`/api/mrp/bom/${itemCode}`)
        .then(res => res.json())
        .then(bomList => {
            renderBomList(bomList);
        });
}

// 하단(BOM/투입자재) 렌더링
function renderBomList(bomList) {
    const tbody = document.getElementById("bomListTableBody");
    tbody.innerHTML = "";
    if (!bomList || bomList.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty-row">소요 자재 데이터가 없습니다.</td></tr>`;
        return;
    }
    bomList.forEach((bom, idx) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td class="col-no">${idx + 1}</td>
            <td>${bom.subItemCode ?? '-'}</td>
            <td>${bom.subItemName ?? '-'}</td>
            <td>${bom.spec ?? '-'}</td>
            <td>${bom.qty ?? '-'}</td>
            <td>${bom.unit ?? '-'}</td>
            <td>${bom.estInput ?? '-'}</td>
        `;
        tbody.appendChild(tr);
    });
}

// 상세 모달 내용 표시
function showMrpDetailModal(mrp, openModal) {
	document.getElementById("mrpDetailItemCode").textContent = mrp.itemCode || '-';
	document.getElementById("mrpDetailItemName").textContent = mrp.itemName || '-';
	document.getElementById("mrpDetailItemType").textContent = mrp.itemType || '-';
	document.getElementById("mrpDetailUnit").textContent = mrp.unit || '-';
	document.getElementById("mrpDetailBaseDate").textContent = mrp.baseDate || '-';
    document.getElementById("mrpDetailRequiredQty").textContent = mrp.requiredQty ?? '-';
    document.getElementById("mrpDetailStock").textContent = mrp.stockQty ?? '-';
    document.getElementById("mrpDetailConfirmedQty").textContent = mrp.confirmedQty ?? '-';
    document.getElementById("mrpDetailShortQty").textContent = mrp.shortageQty ?? '-';
    document.getElementById("mrpDetailDemandSource").textContent = mrp.source || '-';
    document.getElementById("mrpDetailLeadTime").textContent = mrp.leadTime ?? '-';
    document.getElementById("mrpDetailNote").textContent = mrp.comment || '-';

    document.getElementById("modalPrevBtn").disabled = (currentPage === 1 && currentModalIdx === 0);
    document.getElementById("modalNextBtn").disabled = (currentPage === totalPages && currentModalIdx === currentMrpList.length - 1);

    if (openModal) {
        const modal = new bootstrap.Modal(document.getElementById('mrpDetailModal'));
        modal.show();
    }
}

// 모달 내 리스트 이전/다음 이동
function moveModal(offset) {
    let newIdx = currentModalIdx + offset;
    if (newIdx >= currentMrpList.length) {
        if (currentPage < totalPages) {
            loadMrpList(currentPage + 1, function() {
                currentModalIdx = 0;
                showMrpDetailModal(currentMrpList[0], false);
                loadBomList(currentMrpList[0].itemCode);
            });
        }
        return;
    }
    if (newIdx < 0) {
        if (currentPage > 1) {
            loadMrpList(currentPage - 1, function() {
                currentModalIdx = currentMrpList.length - 1;
                showMrpDetailModal(currentMrpList[currentModalIdx], false);
                loadBomList(currentMrpList[currentModalIdx].itemCode);
            });
        }
        return;
    }
    currentModalIdx = newIdx;
    showMrpDetailModal(currentMrpList[currentModalIdx], false);
    loadBomList(currentMrpList[currentModalIdx].itemCode);
}

// "MRP 계산" 버튼에 연결
function calculateMrp() {
    loadMrpList(1);
}

// 취소/리셋
function resetMrpForm() {
    document.getElementById("startDate").value = "";
    document.getElementById("endDate").value = "";
    document.getElementById("itemSearch").value = "";
    document.getElementById("custSearch").value = "";
    loadMrpList(1);
}

// 엑셀 다운로드
function downloadExcel() {
    alert("엑셀 다운로드 준비중!");
}
