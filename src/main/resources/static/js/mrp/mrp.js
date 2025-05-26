const pageSize = 5;
let currentSortKey = "mrpId";
let currentSortDir = "desc";

let currentMrpList = [];
let currentPage = 1;
let totalPages = 1;

let currentModalIdx = -1;

// 최초 로딩
window.onload = function() {
    loadMrpList(1);
};

function sortMrp(key) {
    if (currentSortKey === key) {
        currentSortDir = currentSortDir === "asc" ? "desc" : "asc";
    } else {
        currentSortKey = key;
        currentSortDir = "asc";
    }
    loadMrpList(1);
}

function loadMrpList(page, callback) {
    currentPage = page;
    fetch(`/api/mrp/list?page=${page}&size=${pageSize}&sortKey=${currentSortKey}&sortDir=${currentSortDir}`)
        .then(res => res.json())
        .then(data => {
            const tbody = document.getElementById("mrpTableBody");
            tbody.innerHTML = "";

            currentMrpList = data.content;
            totalPages = data.totalPages;

            data.content.forEach((mrp, idx) => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td>${mrp.itemCode ?? '-'}</td>
                    <td>${mrp.itemName ?? '-'}</td>
                    <td>${mrp.itemType ?? '-'}</td>
                    <td>${mrp.unit ?? '-'}</td>
                    <td>${mrp.baseDate ?? '-'}</td>
                    <td>${mrp.requiredQty ?? 0}</td>
                    <td>${mrp.stockQty ?? 0}</td>
                    <td>${mrp.confirmedQty ?? 0}</td>
                    <td style="color: red;">${mrp.shortageQty ?? 0}</td>
                    <td>${mrp.source ?? '-'}</td>
                    <td>${mrp.leadTime ?? 0}</td>
                    <td>${mrp.comment ?? '-'}</td>
                    <td>
                        <button class="btn btn-info btn-sm" onclick="showMrpDetailModalByIndex(${idx})">상세</button>
                    </td>
                `;
                tbody.appendChild(tr);
            });

            renderPagination(data.currentPage, data.totalPages);

            if (callback) callback();
        });
}

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

function showMrpDetailModalByIndex(idx) {
    currentModalIdx = idx;
    showMrpDetailModal(currentMrpList[idx], true);
}

function showMrpDetailModal(mrp, openModal) {
    document.getElementById("modalItemCode").textContent = mrp.itemCode || '-';
    document.getElementById("modalItemName").textContent = mrp.itemName || '-';
    document.getElementById("modalItemType").textContent = mrp.itemType || '-';
    document.getElementById("modalUnit").textContent = mrp.unit || '-';
    document.getElementById("modalBaseDate").textContent = mrp.baseDate || '-';
    document.getElementById("modalRequiredQty").textContent = mrp.requiredQty ?? '-';
    document.getElementById("modalStockQty").textContent = mrp.stockQty ?? '-';
    document.getElementById("modalConfirmedQty").textContent = mrp.confirmedQty ?? '-';
    document.getElementById("modalShortageQty").textContent = mrp.shortageQty ?? '-';
    document.getElementById("modalSource").textContent = mrp.source || '-';
    document.getElementById("modalLeadTime").textContent = mrp.leadTime ?? '-';
    document.getElementById("modalComment").textContent = mrp.comment || '-';

    document.getElementById("modalPrevBtn").disabled = (currentPage === 1 && currentModalIdx === 0);
    document.getElementById("modalNextBtn").disabled = (currentPage === totalPages && currentModalIdx === currentMrpList.length - 1);

    if (openModal) {
        const modal = new bootstrap.Modal(document.getElementById('mrpDetailModal'));
        modal.show();
    }
}

function moveModal(offset) {
    let newIdx = currentModalIdx + offset;

    // 다음 페이지로 이동
    if (newIdx >= currentMrpList.length) {
        if (currentPage < totalPages) {
            loadMrpList(currentPage + 1, function() {
                currentModalIdx = 0;
                showMrpDetailModal(currentMrpList[0], false);
            });
        }
        return;
    }
    // 이전 페이지로 이동
    if (newIdx < 0) {
        if (currentPage > 1) {
            loadMrpList(currentPage - 1, function() {
                currentModalIdx = currentMrpList.length - 1;
                showMrpDetailModal(currentMrpList[currentModalIdx], false);
            });
        }
        return;
    }
    // 같은 페이지 내 이동
    currentModalIdx = newIdx;
    showMrpDetailModal(currentMrpList[currentModalIdx], false);
}
