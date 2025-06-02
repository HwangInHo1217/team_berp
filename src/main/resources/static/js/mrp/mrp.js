// File: src/main/resources/static/js/mrp/mrp.js

const pageSize = 5;  // 페이지당 보여줄 개수
let currentSortKey = "mrpId";
let currentSortDir = "desc";

let currentMrpList = [];
let currentPage = 1;
let totalPages = 1;
let currentModalIdx = -1;

// 모달 인스턴스 전역 저장 (중복 백드롭 제거용)
let mrpModalInstance = null;
let lastModalMrpData = null; // 모달 생성 시 전달받은 mrp 데이터 보관

// 최초 로딩
window.onload = function() {
    // → 오늘 날짜(YYYY-MM-DD)를 “기간(From)” input에 자동으로 채워 놓기
    const today = new Date().toISOString().substr(0, 10);
    const startDateInput = document.getElementById("startDate");
    if (startDateInput) {
        startDateInput.value = today;
    }

    // ■ selectAll 체크박스 리스너 (한 번만 aggregate 호출)
    document.getElementById('selectAll')?.addEventListener('change', function() {
        const checked = this.checked;
        document.querySelectorAll('.mrp-checkbox').forEach(cb => {
            cb.checked = checked;
        });
        onMrpCheckboxChange();
    });

    // ■ bomSelectAll 체크박스 리스너
    document.getElementById('bomSelectAll')?.addEventListener('change', function() {
        const checked = this.checked;
        document.querySelectorAll('.bom-checkbox').forEach(cb => cb.checked = checked);
    });

    // “생산계획으로 이동” 버튼 클릭 (투입 자재 소요 리스트 바로 아래)
    document.getElementById('btn-bulk-create-plan')?.addEventListener('click', () => {
        const checked = Array.from(document.querySelectorAll('.bom-checkbox:checked'));
        if (!checked.length) {
            return alert("생산계획을 만들 자재를 최소 한 개 이상 선택하세요.");
        }
        const codes = checked.map(cb => cb.dataset.code).join(",");
        window.location.href = `/prod-plan?items=${encodeURIComponent(codes)}`;
    });

    // “발주 등록” 버튼 클릭 (투입 자재 소요 리스트 바로 아래)
    document.getElementById('btn-bulk-create-order')?.addEventListener('click', () => {
        const checked = Array.from(document.querySelectorAll('.bom-checkbox:checked'));
        if (!checked.length) {
            return alert("발주할 자재를 최소 한 개 이상 선택하세요.");
        }
        const codes = checked.map(cb => cb.dataset.code).join(",");
        window.location.href = `/purchase-order?items=${encodeURIComponent(codes)}`;
    });

    // 엔터키 → calculateMrp() 호출
    document.getElementById("itemSearch")?.addEventListener("keypress", e => {
        if (e.key === "Enter") calculateMrp();
    });

    // (A) 화면 하단 툴바: "btn-create-plan" / "btn-create-order"
    document.getElementById('btn-create-plan')?.addEventListener('click', () => {
        // (1) 화면에 렌더된 모든 BOM 행(<tr>)을 가져옴
        const allBomRows = Array.from(document.querySelectorAll('#bomListTableBody tr'));
        // (2) 현재 사용자가 체크한 행들을 필터링
        const checkedRows = allBomRows.filter(tr => {
            const cb = tr.querySelector('.bom-checkbox');
            return cb && cb.checked;
        });

        let toPlanCodes;

        if (checkedRows.length === 1) {
            // 딱 하나만 체크된 경우 → 그 행의 shortageQty 판단
            const shortage = parseInt(checkedRows[0].dataset.shortageQty || "0", 10);
            if (shortage > 0) {
                return alert("체크된 자재는 발주가 필요합니다.");
            }
            toPlanCodes = [ checkedRows[0].dataset.code ];
        }
        else if (checkedRows.length > 1) {
            // 여러 개 체크된 경우 → 그 중 shortageQty === 0인 것만
            toPlanCodes = checkedRows
              .filter(tr => parseInt(tr.dataset.shortageQty || "0", 10) === 0)
              .map(tr => tr.dataset.code);

            if (!toPlanCodes.length) {
                return alert("체크된 자재 중 생산계획을 세울 자재가 없습니다.");
            }
        }
        else {
            // 하나도 체크 안 한 경우 → 화면 전체에서 shortageQty === 0인 것만
            toPlanCodes = allBomRows
              .filter(tr => parseInt(tr.dataset.shortageQty || "0", 10) === 0)
              .map(tr => tr.dataset.code);

            if (!toPlanCodes.length) {
                return alert("생산계획을 세울 자재가 없습니다.");
            }
        }

        // (3) 확인 메시지
        const confirmMsg = "생산계획으로 넘어갈 자재들이 선택되었습니다.\n생산계획 페이지로 이동하시겠습니까?";
        if (!window.confirm(confirmMsg)) {
            return;
        }

        // (4) 실제 이동
        const query = encodeURIComponent(toPlanCodes.join(','));
        window.location.href = `/prod-plan?items=${query}`;
    });

    document.getElementById('btn-create-order')?.addEventListener('click', () => {
        // (1) 화면에 렌더된 모든 BOM 행(<tr>)을 가져옴
        const allBomRows = Array.from(document.querySelectorAll('#bomListTableBody tr'));
        // (2) 현재 사용자가 체크한 행들을 필터링
        const checkedRows = allBomRows.filter(tr => {
            const cb = tr.querySelector('.bom-checkbox');
            return cb && cb.checked;
        });

        let toOrderCodes;

        if (checkedRows.length === 1) {
            // 딱 하나만 체크된 경우 → 그 행의 shortageQty 판단
            const shortage = parseInt(checkedRows[0].dataset.shortageQty || "0", 10);
            if (shortage <= 0) {
                return alert("체크된 자재는 발주가 필요하지 않습니다.");
            }
            toOrderCodes = [ checkedRows[0].dataset.code ];
        }
        else if (checkedRows.length > 1) {
            // 여러 개 체크된 경우 → 그 중 shortageQty > 0인 것만
            toOrderCodes = checkedRows
              .filter(tr => parseInt(tr.dataset.shortageQty || "0", 10) > 0)
              .map(tr => tr.dataset.code);

            if (!toOrderCodes.length) {
                return alert("체크된 자재 중 발주가 필요한 자재가 없습니다.");
            }
        }
        else {
            // 하나도 체크 안 한 경우 → 화면 전체에서 shortageQty > 0인 것만
            toOrderCodes = allBomRows
              .filter(tr => parseInt(tr.dataset.shortageQty || "0", 10) > 0)
              .map(tr => tr.dataset.code);

            if (!toOrderCodes.length) {
                return alert("발주할 자재가 없습니다.");
            }
        }

        // (3) 확인 메시지
        const confirmMsg = "발주가 필요한 자재들이 선택되었습니다.\n발주 등록하시겠습니까?";
        if (!window.confirm(confirmMsg)) {
            return;
        }

        // (4) 실제 이동
        const query = encodeURIComponent(toOrderCodes.join(','));
        window.location.href = `/purchase-order?items=${query}`;
    });

    // ■ 투입 자재 소요 리스트 체크박스 변화에 따라 버튼 활성/비활성 갱신
    function updateToolbarButtons() {
        const checkedBoxes = Array.from(document.querySelectorAll('.bom-checkbox:checked'));
        const btnPlan  = document.getElementById('btn-create-plan');
        const btnOrder = document.getElementById('btn-create-order');

        if (checkedBoxes.length === 1) {
            // 딱 하나만 체크된 경우
            const tr = checkedBoxes[0].closest('tr');
            const shortageQty = parseInt(tr.dataset.shortageQty || "0", 10);

            if (shortageQty > 0) {
                // 발주 대상
                btnOrder.disabled = false;
                btnPlan.disabled  = true;
            } else {
                // 생산계획 대상
                btnOrder.disabled = true;
                btnPlan.disabled  = false;
            }
        }
        else if (checkedBoxes.length > 1) {
            // 여러 개 체크된 경우 → 두 버튼 모두 활성화
            btnOrder.disabled = false;
            btnPlan.disabled  = false;
        }
        else {
            // 하나도 체크 안 한 경우 → 두 버튼 모두 활성화 (자동 필터링 로직 사용)
            btnOrder.disabled = false;
            btnPlan.disabled  = false;
        }
    }

    // 페이지 로드 시와 체크박스가 변화할 때마다 updateToolbarButtons() 호출
    document.querySelectorAll('#bomListTableBody').forEach(tbody => {
        tbody.addEventListener('change', e => {
            if (e.target.classList.contains('bom-checkbox')) {
                updateToolbarButtons();
            }
        });
    });
    // 초기 버튼 상태 셋팅
    updateToolbarButtons();

    // 초기 MRP 리스트 로드
    loadMrpList(1);
};

// 정렬
function sortMrp(key) {
    if (currentSortKey === key) {
        currentSortDir = (currentSortDir === "asc" ? "desc" : "asc");
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
        tbody.innerHTML = `<tr><td colspan="15" class="text-center">데이터가 없습니다.</td></tr>`;
        renderBomList([]);
        return;
    }

    data.content.forEach((mrp, idx) => {
        const rowNumber = data.totalElements - ((page - 1) * pageSize + idx);
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><input type="checkbox" class="mrp-checkbox" data-code="${mrp.itemCode}" onchange="onMrpCheckboxChange()"></td>
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
        // tr 전체 클릭 시, 체크박스 토글
        tr.addEventListener('click', e => {
            if (e.target.type === 'checkbox' || e.target.tagName === 'BUTTON') return;
            const cb = tr.querySelector('.mrp-checkbox');
            cb.checked = !cb.checked;
            onMrpCheckboxChange();
        });
        tbody.appendChild(tr);
    });

    renderBomList([]);  // 처음 렌더 시에는 하단 자재 리스트를 비워 둡니다.
}

// ■ onMrpCheckboxChange(상단 MRP 체크박스 변화 시)
function onMrpCheckboxChange() {
    const checkedBoxes = Array.from(document.querySelectorAll('.mrp-checkbox:checked'));
    if (!checkedBoxes.length) {
        renderBomList([]);
        updateToolbarButtons();
        return;
    }
    const codes = checkedBoxes.map(cb => cb.dataset.code);
    loadAllBomLists(codes);
    updateToolbarButtons();
}

// ■ loadAllBomLists: 선택된 MRP 항목들의 BOM 정보를 API로 불러온 뒤 합쳐서 렌더
function loadAllBomLists(codes) {
    const fetchPromises = codes.map(code =>
        fetch(`/api/mrp/bom/${encodeURIComponent(code)}`)
            .then(res => {
                if (!res.ok) throw new Error(`BOM 로드 실패(${code}): ${res.status}`);
                return res.json();
            })
            .catch(err => {
                console.error(err);
                return [];
            })
    );

    Promise.all(fetchPromises)
        .then(results => {
            const combined = results.flat();
            renderBomList(combined);
            updateToolbarButtons();
        })
        .catch(err => {
            console.error("여러 BOM 합치기 실패:", err);
            renderBomList([]);
            updateToolbarButtons();
        });
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

function renderBomList(bomList) {
    const tbody = document.getElementById("bomListTableBody");
    tbody.innerHTML = "";
    if (!bomList.length) {
        tbody.innerHTML = `<tr><td colspan="13" class="text-center">소요 자재 데이터가 없습니다.</td></tr>`;
        return;
    }
    bomList.forEach((bom, idx) => {
        const tr = document.createElement("tr");
        tr.setAttribute("data-code", bom.childCode ?? "");
        tr.setAttribute("data-purchase-qty", (bom.purchaseQty ?? 0).toString());
        tr.setAttribute("data-shortage-qty", (bom.shortageQty ?? 0).toString());
        tr.innerHTML = `
            <td><input type="checkbox" class="bom-checkbox" data-code="${bom.childCode}"></td>
            <td class="col-no">${idx + 1}</td>
            <td>${bom.childCode ?? '-'}</td>
            <td>${bom.childName ?? '-'}</td>
            <td>${bom.spec ?? '-'}</td>
            <td>${bom.unit ?? '-'}</td>
            <td>${bom.qty ?? 0}</td>
            <td>${bom.stockQty ?? 0}</td>
            <td>${bom.shortageQty ?? 0}</td>
            <td>${bom.safetyStock ?? 0}</td>
            <td>${bom.purchaseQty ?? 0}</td>
            <td>${bom.purchaseLeadTime ?? 0}</td>
            <td>${bom.expectedDate ?? '-'}</td>
        `;
        tbody.appendChild(tr);
    });
}

// ■ updateToolbarButtons 함수 정의 (rmqList 하단 버튼 활성/비활성 제어)
function updateToolbarButtons() {
    const checkedBoxes = Array.from(document.querySelectorAll('.bom-checkbox:checked'));
    const btnPlan  = document.getElementById('btn-create-plan');
    const btnOrder = document.getElementById('btn-create-order');

    if (checkedBoxes.length === 1) {
        // 딱 하나만 체크된 경우
        const tr = checkedBoxes[0].closest('tr');
        const shortageQty = parseInt(tr.dataset.shortageQty || "0", 10);

        if (shortageQty > 0) {
            // 발주 대상
            btnOrder.disabled = false;
            btnPlan.disabled  = true;
        } else {
            // 생산계획 대상
            btnOrder.disabled = true;
            btnPlan.disabled  = false;
        }
    }
    else if (checkedBoxes.length > 1) {
        // 여러 개가 체크된 경우 → 두 버튼 모두 활성화
        btnOrder.disabled = false;
        btnPlan.disabled  = false;
    }
    else {
        // 하나도 체크 안 한 경우 → 두 버튼 모두 활성화 (자동 필터링 로직 사용)
        btnOrder.disabled = false;
        btnPlan.disabled  = false;
    }
}

/**
 * ■ showMrpDetailModal
 * - mrp: 서버에서 받아온 JSON 데이터(MrpDetailDto 형태)
 * - openModal: true → “모달을 열 때” (백드롭과 함께 show)
 *              false → “모달이 이미 떠 있는 상태에서 내용만 바꿀 때”
 */
function showMrpDetailModal(mrp, openModal) {
    // openModal=true인 경우, 기존 인스턴스를 먼저 dispose하고 새로 생성
    if (openModal && mrpModalInstance) {
        mrpModalInstance.dispose();
        mrpModalInstance = null;
    }

    // “마지막으로 로드된 mrp 데이터를 전역에 저장”
    lastModalMrpData = mrp;

    // A. 기본 정보
    document.getElementById("mrpDetailId").textContent         = mrp.mrpId        ?? '-';
    document.getElementById("mrpDetailCreateDate").textContent = mrp.createDate   || '-';
    document.getElementById("mrpDetailDueDate").textContent    = mrp.dueDate      || '-';
    document.getElementById("mrpDetailPlanType").textContent   = mrp.planType     || '-';
    document.getElementById("mrpDetailStatus").textContent     = mrp.status       || '-';

    // B. 품목 정보
    document.getElementById("mrpDetailItemCode").textContent    = mrp.itemCode    || '-';
    document.getElementById("mrpDetailItemName").textContent    = mrp.itemName    || '-';
    document.getElementById("mrpDetailItemType").textContent    = mrp.itemType    || '-';
    document.getElementById("mrpDetailUnit").textContent        = mrp.unit        || '-';
    document.getElementById("mrpDetailSpec").textContent        = mrp.spec        || '-';
    document.getElementById("mrpDetailStock").textContent       = mrp.stockQty    ?? '0';
    document.getElementById("mrpDetailSafetyStock").textContent = mrp.safetyStock ?? '0';

    // 재고 위치 드롭다운 초기화
    const locationDropdown = document.getElementById("mrpDetailLocationDropdown");
    locationDropdown.innerHTML = `<option>로딩 중...</option>`;

    // “재고 위치” 데이터를 가져올 API 호출 (404 대비)
    fetch(`/api/mrp/warehouses/${encodeURIComponent(mrp.itemCode)}`)
        .then(res => {
            if (!res.ok) {
                // API가 없거나 404 등 오류인 경우, 빈 드롭다운으로 변경
                throw new Error("API 없음 또는 404");
            }
            return res.json();
        })
        .then(data => {
            locationDropdown.innerHTML = "";
            if (!Array.isArray(data) || data.length === 0) {
                locationDropdown.innerHTML = `<option>데이터 없음</option>`;
                return;
            }
            // 가장 재고가 많은 순으로 정렬 (내림차순)
            data.sort((a, b) => (b.quantity || 0) - (a.quantity || 0));
            data.forEach(item => {
                const opt = document.createElement("option");
                opt.value = item.warehouseName;
                opt.textContent = `${item.warehouseName} (${item.quantity || 0})`;
                locationDropdown.appendChild(opt);
            });
        })
        .catch(err => {
            console.warn("재고 위치 로딩 실패:", err);
            locationDropdown.innerHTML = `<option>데이터 없음</option>`;
        });

    // C. 수량·리드타임
    document.getElementById("mrpDetailRequiredQty").textContent       = mrp.requiredQty        ?? '0';
    document.getElementById("mrpDetailShortQty").textContent          = mrp.shortageQty        ?? '0';
    document.getElementById("mrpDetailPurchaseLeadTime").textContent  = mrp.purchaseLeadTime   ?? '0';
    document.getElementById("mrpDetailProductionLeadTime").textContent = mrp.productionLeadTime ?? '0';
    document.getElementById("mrpDetailOrderableDate").textContent      = mrp.orderableDate      || '-';

    // D. BOM 구성
    const bomBody = document.getElementById("mrpDetailBomBody");
    bomBody.innerHTML = "";  // 매번 비우고 새로 렌더
    (mrp.bomComponents || []).forEach(c => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${c.childCode ?? '-'}</td>
            <td>${c.childName ?? '-'}</td>
            <td>${c.perParentQty ?? 0}</td>
            <td>${c.totalQty ?? 0}</td>
            <td>${c.stockQty ?? 0}</td>
            <td>${c.shortageQty ?? 0}</td>
            <td>${c.leadTime ?? 0}</td>
        `;
        bomBody.appendChild(tr);
    });

    // E. 연계 오더 현황: 생산 오더 리스트만 렌더링
    const woBody = document.getElementById("mrpDetailWoList");
    woBody.innerHTML = "";
    (mrp.workOrders || []).forEach(wo => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${wo.woNo ?? '-'}</td>
            <td>${wo.itemCode ?? '-'}</td>
            <td>${wo.qty ?? 0}</td>
            <td>${wo.startDate ?? '-'}</td>
            <td>${wo.endDate ?? '-'}</td>
            <td>${wo.status ?? '-'}</td>
        `;
        woBody.appendChild(tr);
    });

    // F. 스케줄·이력
    const logDiv = document.getElementById("mrpDetailLog");
    logDiv.innerHTML = "";
    (mrp.history || []).forEach(h => {
        const div = document.createElement("div");
        div.textContent = `[${h.timestamp ?? '-'}] ${h.message ?? ''}`;
        logDiv.appendChild(div);
    });

    // G. 네비게이션 버튼 활성/비활성
    document.getElementById("modalPrevBtn").disabled = (currentModalIdx <= 0);
    document.getElementById("modalNextBtn").disabled = (currentModalIdx >= currentMrpList.length - 1);

    // H. 상세 모달 내부 버튼 이벤트 바인딩
    document.getElementById("modal-create-plan").onclick = () => {
        if (!lastModalMrpData || !Array.isArray(lastModalMrpData.bomComponents)) {
            return alert("생산계획으로 넘어갈 자재 정보가 없습니다.");
        }
        // 부족 수량(shortageQty)이 0인 자재만 필터링합니다.
        const toPlanCodes = lastModalMrpData.bomComponents
            .filter(c => (c.shortageQty ?? 0) === 0)
            .map(c => c.childCode);

        if (!toPlanCodes.length) {
            return alert("생산계획으로 넘어갈 자재가 없습니다.");
        }

        const confirmMsg = "생산계획으로 넘어갈 자재들이 자동으로 선택되었습니다.\n생산계획 페이지로 이동하시겠습니까?";
        if (!window.confirm(confirmMsg)) {
            return;
        }

        const bomCodes = toPlanCodes.join(",");
        window.location.href = `/prod-plan?items=${encodeURIComponent(bomCodes)}`;
    };

    document.getElementById("modal-create-order").onclick = () => {
        if (!lastModalMrpData || !Array.isArray(lastModalMrpData.bomComponents)) {
            return alert("발주할 자재 정보가 없습니다.");
        }
        // 부족 수량(shortageQty)이 0보다 큰, 즉 발주가 필요한 자재만 필터링합니다.
        const toOrderCodes = lastModalMrpData.bomComponents
            .filter(c => (c.shortageQty ?? 0) > 0)
            .map(c => c.childCode);

        if (!toOrderCodes.length) {
            return alert("발주할 자재가 없습니다.");
        }

        const confirmMsg = "발주가 필요한 자재들이 자동으로 선택되었습니다.\n발주 등록하시겠습니까?";
        if (!window.confirm(confirmMsg)) {
            return;
        }

        const bomCodes = toOrderCodes.join(",");
        window.location.href = `/purchase-order?items=${encodeURIComponent(bomCodes)}`;
    };

    // 모달 열기 (openModal = true인 경우)
    if (openModal) {
        mrpModalInstance = new bootstrap.Modal(document.getElementById('mrpDetailModal'));
        mrpModalInstance.show();
    }
}

// “이전/다음” 버튼 클릭 시, detail API를 호출하여 “내용만 교체” (openModal=false)
function moveModal(offset) {
    let newIdx = currentModalIdx + offset;
    if (newIdx < 0 || newIdx >= currentMrpList.length) return;
    currentModalIdx = newIdx;
    const mrpId = currentMrpList[newIdx].mrpId;
    fetch(`/api/mrp/detail/${mrpId}`)
        .then(res => res.json())
        .then(data => {
            showMrpDetailModal(data, false);
        })
        .catch(err => console.error("상세 모달 데이터 로드 실패:", err));
}

// “상세” 버튼 클릭 시, detail API 호출 후 모달 열기(openModal=true)
function showMrpDetailModalByIndex(idx) {
    currentModalIdx = idx;
    const mrpId = currentMrpList[idx].mrpId;
    fetch(`/api/mrp/detail/${mrpId}`)
      .then(res => res.json())
      .then(data => {
          showMrpDetailModal(data, true);
      })
      .catch(err => console.error("상세 모달 데이터 로드 실패:", err));
}

// “MRP 계산” 버튼 클릭
function calculateMrp() {
    const startDate = document.getElementById("startDate").value.trim();
    const endDate   = document.getElementById("endDate").value.trim();
    const itemSearch = document.getElementById("itemSearch").value.trim();

    // 필수 체크
    if (!startDate || !endDate || !itemSearch) {
        alert("기간(From), 기간(To), 그리고 품목명/코드를 모두 입력해주세요.");
        return;
    }
    // startDate <= endDate인지 간단히 체크
    if (startDate > endDate) {
        alert("기간(From)이 기간(To)보다 클 수 없습니다.");
        return;
    }

    loadMrpList(1);
}

function resetMrpForm() {
    ['startDate','endDate','itemSearch'].forEach(id => {
        document.getElementById(id).value = "";
    });
    loadMrpList(1);
}
