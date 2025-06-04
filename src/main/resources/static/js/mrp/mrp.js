// File: src/main/resources/static/js/mrp/mrp.js

// ────────────────────────────────────────────────────────
// 전역 상수 및 전역 변수 선언
// ────────────────────────────────────────────────────────
const pageSize = 5;                // 페이지당 보여줄 개수
let currentSortKey = "mrpId";      // 정렬 키 (초기값)
let currentSortDir = "desc";       // 정렬 방향 (초기값)

let currentMrpList = [];           // 현재 화면에 보이는 MRP 리스트 데이터
let currentPage = 1;               // 현재 페이지 번호
let totalPages = 1;                // 전체 페이지 수

let currentModalIdx = -1;          // 모달 내비게이션(이전/다음)용 인덱스

let mrpModalInstance = null;       // 모달 인스턴스 (중복 백드롭 제거용)
let lastModalMrpData = null;       // 모달에 마지막으로 로드된 MRP 데이터 저장

// ────────────────────────────────────────────────────────
// 전역 함수: 버튼 활성/비활성 토글 함수
// ────────────────────────────────────────────────────────
function updateToolbarButtons() {
    const checkedBoxes = Array.from(document.querySelectorAll('.bom-checkbox:checked'));
    const btnPlan  = document.getElementById('btn-create-plan');
    const btnOrder = document.getElementById('btn-create-order');

    if (checkedBoxes.length === 1) {
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

// ────────────────────────────────────────────────────────
// 전역 함수: 상단 MRP 체크박스 변화 시 실행
// ────────────────────────────────────────────────────────
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

// ────────────────────────────────────────────────────────
// 전역 함수: 여러 BOM 리스트를 비동기로 로드하여 결합 후 렌더
// ────────────────────────────────────────────────────────
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

// ────────────────────────────────────────────────────────
// 전역 함수: MRP 리스트를 API 호출하여 테이블에 렌더
// ────────────────────────────────────────────────────────
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

// ────────────────────────────────────────────────────────
// 전역 함수: MRP 리스트 테이블 렌더링
// ────────────────────────────────────────────────────────
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
        // 화면 상에서 역순 번호 계산
        const rowNumber = data.totalElements - ((page - 1) * pageSize + idx);
        const tr = document.createElement("tr");

        tr.innerHTML = `
            <td>
              <input type="checkbox"
                     class="mrp-checkbox"
                     data-code="${mrp.itemCode}"
                     onchange="onMrpCheckboxChange()">
            </td>
            <td class="col-no">${rowNumber}</td>
            <td>${mrp.itemCode   ?? '-'}</td>
            <td>${mrp.itemName   ?? '-'}</td>
            <td>${mrp.custName   ?? '-'}</td>
            <td>${mrp.spec       ?? '-'}</td>
            <td>${mrp.unit       ?? '-'}</td>
            <td>${mrp.orderQty   ?? 0 }</td>
            <td>${mrp.stockQty   ?? 0 }</td>
            <td>${mrp.shortageQty ?? 0 }</td>
            <td>${mrp.dueDate    ?? '-'}</td>
            <td>${mrp.leadTime   ?? 0 }</td>
            <td>${mrp.mrpStatus  ?? '-'}</td>
            <td>
              <button class="btn btn-info btn-sm"
                      onclick="showMrpDetailModalByIndex(${idx})">
                상세
              </button>
            </td>
        `;

        tbody.appendChild(tr);
    });

    // 렌더할 때마다 하단 BOM 리스트 초기화
    renderBomList([]);
}

// ────────────────────────────────────────────────────────
// 전역 함수: 페이지네이션 영역 렌더링
// ────────────────────────────────────────────────────────
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

// ────────────────────────────────────────────────────────
// 전역 함수: BOM(투입 자재) 리스트 렌더링
// ────────────────────────────────────────────────────────
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
        tr.setAttribute("data-totalqty", (bom.totalQty ?? 0).toString());
        tr.setAttribute("data-shortage-qty", (bom.shortageQty ?? 0).toString());

        tr.innerHTML = `
            <td>
              <input type="checkbox"
                     class="bom-checkbox"
                     data-code="${bom.childCode}">
            </td>
            <td class="col-no">${idx + 1}</td>
            <td>${bom.childCode   ?? '-'}</td>
            <td>${bom.childName   ?? '-'}</td>
            <td>${bom.spec        ?? '-'}</td>
            <td>${bom.unit        ?? '-'}</td>
            <td>${bom.totalQty    ?? 0 }</td>
            <td>${bom.stockQty    ?? 0 }</td>
            <td>${bom.shortageQty ?? 0 }</td>
            <td>${bom.safetyStock ?? 0 }</td>
            <td>${bom.purchaseQty ?? 0 }</td>
            <td>${bom.purchaseLeadTime ?? 0 }</td>
            <td>${bom.expectedDate ?? '-'}</td>
        `;

        tbody.appendChild(tr);
    });
}

// ────────────────────────────────────────────────────────
// 전역 함수: MRP 상세 모달 내용을 채우고 표시
// ────────────────────────────────────────────────────────
function showMrpDetailModal(mrp, openModal) {
    // “openModal = true”인 경우, 기존 인스턴스가 있으면 dispose
    if (openModal && mrpModalInstance) {
        mrpModalInstance.dispose();
        mrpModalInstance = null;
    }

    // 마지막으로 로드된 MRP 데이터를 전역에 저장
    lastModalMrpData = mrp;

    // A. 기본 정보
    document.getElementById("mrpDetailId").textContent         = mrp.mrpId        ?? '-';
    document.getElementById("mrpDetailCreateDate").textContent = mrp.createDate   || '-';
    document.getElementById("mrpDetailDueDate").textContent    = mrp.dueDate      || '-';
    document.getElementById("mrpDetailPlanType").textContent   = mrp.planType     || '-';
    document.getElementById("mrpDetailStatus").textContent     = mrp.status       || '-';

    // B. 품목 정보 (완제품 기준)
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

    // “재고 위치” 데이터를 API 호출하여 드롭다운 채우기 (404 대비)
    fetch(`/api/mrp/warehouses/${encodeURIComponent(mrp.itemCode)}`)
        .then(res => {
            if (!res.ok) {
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
            // 가장 재고가 많은 순으로 정렬(내림차순)
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
    //   → 이제 DTO에서 직접 “requestQty(요청 수량)”과 “shortageQty(부족 수량)”을 내려줍니다.
    document.getElementById("mrpDetailRequiredQty").textContent       = mrp.requiredQty        ?? '0';
    document.getElementById("mrpDetailShortQty").textContent          = mrp.shortageQty        ?? '0';
    document.getElementById("mrpDetailPurchaseLeadTime").textContent  = mrp.purchaseLeadTime   ?? '0';
    document.getElementById("mrpDetailProductionLeadTime").textContent = mrp.productionLeadTime ?? '0';
    document.getElementById("mrpDetailOrderableDate").textContent      = mrp.orderableDate      || '-';

    // D. BOM 구성 (완제품 기준)
    const bomBody = document.getElementById("mrpDetailBomBody");
    bomBody.innerHTML = "";
    (mrp.bomComponents || []).forEach(c => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${c.childCode   ?? '-'}</td>
            <td>${c.childName   ?? '-'}</td>
            <td>${c.perParentQty ?? 0 }</td>
            <td>${c.totalQty    ?? 0 }</td>
            <td>${c.stockQty    ?? 0 }</td>
            <td>${c.shortageQty ?? 0 }</td>
            <td>${c.leadTime    ?? 0 }</td>
        `;
        bomBody.appendChild(tr);
    });

    // E. 연계 오더 현황: 생산 오더(WO) 리스트
    const woBody = document.getElementById("mrpDetailWoList");
    woBody.innerHTML = "";
    (mrp.workOrders || []).forEach(wo => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${wo.woNo      ?? '-'}</td>
            <td>${wo.itemCode  ?? '-'}</td>
            <td>${wo.qty       ?? 0 }</td>
            <td>${wo.startDate ?? '-'}</td>
            <td>${wo.endDate   ?? '-'}</td>
            <td>${wo.status    ?? '-'}</td>
        `;
        woBody.appendChild(tr);
    });

    // F. 스케줄·이력(History)
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

    // H. 모달 내 “생산계획으로 이동” 버튼 이벤트
    document.getElementById("modal-create-plan").onclick = () => {
        if (!lastModalMrpData || !Array.isArray(lastModalMrpData.bomComponents)) {
            return alert("생산계획으로 넘어갈 자재 정보가 없습니다.");
        }
        // “코드:수량” 형태로 URL 파라미터에 붙이기 위해, dataset-totalqty 를 같이 읽어옵니다.
        const checkedPairs = lastModalMrpData.bomComponents
            .filter(c => (c.shortageQty ?? 0) === 0)
            .map(c => `${c.childCode}:${c.totalQty}`);
        const allPairs = (checkedPairs.length)
            ? checkedPairs
            : lastModalMrpData.bomComponents
                .filter(c => (c.shortageQty ?? 0) === 0)
                .map(c => `${c.childCode}:${c.totalQty}`);

        const uniquePairs = Array.from(new Set(allPairs));
        if (!uniquePairs.length) {
            return alert("생산계획을 세울 자재가 없습니다.");
        }

        const confirmMsg = "생산계획으로 넘어갈 자재들이 자동으로 선택되었습니다.\n생산계획 페이지로 이동하시겠습니까?";
        if (!window.confirm(confirmMsg)) {
            return;
        }
        window.location.href = `/prod-plan?items=${encodeURIComponent(uniquePairs.join(","))}`;
    };

    // H. 모달 내 “발주 등록” 버튼 이벤트
    document.getElementById("modal-create-order").onclick = () => {
        if (!lastModalMrpData || !Array.isArray(lastModalMrpData.bomComponents)) {
            return alert("발주할 자재 정보가 없습니다.");
        }
        // “코드:수량” 형태로 URL 파라미터에 붙이기 위해, dataset-totalqty 를 같이 읽어옵니다.
        const checkedPairs = lastModalMrpData.bomComponents
            .filter(c => (c.shortageQty ?? 0) > 0)
            .map(c => `${c.childCode}:${c.totalQty}`);
        const allPairs = (checkedPairs.length)
            ? checkedPairs
            : lastModalMrpData.bomComponents
                .filter(c => (c.shortageQty ?? 0) > 0)
                .map(c => `${c.childCode}:${c.totalQty}`);

        const uniquePairs = Array.from(new Set(allPairs));
        if (!uniquePairs.length) {
            return alert("발주할 자재가 없습니다.");
        }

        const confirmMsg = "발주가 필요한 자재들이 자동으로 선택되었습니다.\n발주 등록하시겠습니까?";
        if (!window.confirm(confirmMsg)) {
            return;
        }
        window.location.href = `/place?items=${encodeURIComponent(uniquePairs.join(","))}`;
    };

    // 모달 열기 (openModal=true인 경우)
    if (openModal) {
        mrpModalInstance = new bootstrap.Modal(document.getElementById('mrpDetailModal'));
        mrpModalInstance.show();
    }
}

// ────────────────────────────────────────────────────────
// 전역 함수: 모달 내비게이션 “이전/다음” 버튼 클릭
// ────────────────────────────────────────────────────────
function moveModal(offset) {
    let newIdx = currentModalIdx + offset;
    if (newIdx < 0 || newIdx >= currentMrpList.length) return;
    currentModalIdx = newIdx;
    // “mrpId” 자리에 실제로는 planId 가 담겨 있습니다.
    const mrpId = currentMrpList[newIdx].mrpId;
    fetch(`/api/mrp/detail/${mrpId}`)
        .then(res => res.json())
        .then(data => {
            showMrpDetailModal(data, false);
        })
        .catch(err => console.error("상세 모달 데이터 로드 실패:", err));
}

// ────────────────────────────────────────────────────────
// 전역 함수: “상세” 버튼 클릭 시 모달 열기
// ────────────────────────────────────────────────────────
function showMrpDetailModalByIndex(idx) {
    currentModalIdx = idx;
    // data.content[idx].mrpId → 실제로는 planId 가 담겨 있습니다.
    const mrpId = currentMrpList[idx].mrpId;
    fetch(`/api/mrp/detail/${mrpId}`)
      .then(res => res.json())
      .then(data => {
          showMrpDetailModal(data, true);
      })
      .catch(err => console.error("상세 모달 데이터 로드 실패:", err));
}

// ────────────────────────────────────────────────────────
// 전역 함수: 정렬 키 클릭(정렬 방향 토글 후 새로 로드)
// ────────────────────────────────────────────────────────
function sortMrp(key) {
    if (currentSortKey === key) {
        currentSortDir = (currentSortDir === "asc" ? "desc" : "asc");
    } else {
        currentSortKey = key;
        currentSortDir = "asc";
    }
    loadMrpList(1);
}

// ────────────────────────────────────────────────────────
// 전역 함수: “MRP 계산” 버튼/엔터키 처리
// ────────────────────────────────────────────────────────
function calculateMrp() {
    const startDate = document.getElementById("startDate").value.trim();
    const endDate   = document.getElementById("endDate").value.trim();
    const itemSearch = document.getElementById("itemSearch").value.trim();

    // 필수 체크
    if (!startDate || !endDate || !itemSearch) {
        alert("기간(From), 기간(To), 그리고 품목명/코드를 모두 입력해주세요.");
        return;
    }
    // startDate <= endDate 간단 체크
    if (startDate > endDate) {
        alert("기간(From)이 기간(To)보다 클 수 없습니다.");
        return;
    }

    loadMrpList(1);
}

// ────────────────────────────────────────────────────────
// 전역 함수: 검색 필드 초기화 및 재조회
// ────────────────────────────────────────────────────────
function resetMrpForm() {
    ['startDate','endDate','itemSearch'].forEach(id => {
        document.getElementById(id).value = "";
    });
    loadMrpList(1);
}

// ────────────────────────────────────────────────────────
// 최초 페이지 로드 시 실행 (window.onload)
// ────────────────────────────────────────────────────────
window.onload = function() {
    // → 오늘 날짜(YYYY-MM-DD)를 “기간(From)” input에 자동 채워 넣기
    const today = new Date().toISOString().substr(0, 10);
    const startDateInput = document.getElementById("startDate");
    if (startDateInput) {
        startDateInput.value = today;
    }

    // ■ selectAll 체크박스 리스너 (MRP 리스트 전체 선택/해제)
    document.getElementById('selectAll')?.addEventListener('change', function() {
        const checked = this.checked;
        document.querySelectorAll('.mrp-checkbox').forEach(cb => {
            cb.checked = checked;
        });
        onMrpCheckboxChange();
    });

    // ■ bomSelectAll 체크박스 리스너 (BOM 리스트 전체 선택/해제)
    document.getElementById('bomSelectAll')?.addEventListener('change', function() {
        const checked = this.checked;
        document.querySelectorAll('.bom-checkbox').forEach(cb => cb.checked = checked);
    });

    // ■ “생산계획으로 이동” 버튼 클릭 (하단 BOM 리스트 바로 아래)
    document.getElementById('btn-create-plan')?.addEventListener('click', () => {
        const allBomRows = Array.from(document.querySelectorAll('#bomListTableBody tr'));
        const checkedRows = allBomRows.filter(tr => {
            const cb = tr.querySelector('.bom-checkbox');
            return cb && cb.checked;
        });

        let pairs;
        if (checkedRows.length > 0) {
            pairs = checkedRows
                .filter(tr => parseInt(tr.dataset.shortageQty || "0", 10) === 0)
                .map(tr => `${tr.dataset.code}:${tr.dataset.totalqty}`);
        } else {
            pairs = allBomRows
                .filter(tr => parseInt(tr.dataset.shortageQty || "0", 10) === 0)
                .map(tr => `${tr.dataset.code}:${tr.dataset.totalqty}`);
        }

        const uniquePairs = Array.from(new Set(pairs));
        if (!uniquePairs.length) {
            return alert("생산계획을 세울 자재가 없습니다.");
        }

        const confirmMsg = "생산계획으로 넘어갈 자재들이 선택되었습니다.\n생산계획 페이지로 이동하시겠습니까?";
        if (!window.confirm(confirmMsg)) {
            return;
        }
        window.location.href = `/plan?items=${encodeURIComponent(uniquePairs.join(","))}`;
    });

    // ■ “발주 등록” 버튼 클릭 (하단 BOM 리스트 바로 아래)
    document.getElementById('btn-create-order')?.addEventListener('click', () => {
        const allBomRows = Array.from(document.querySelectorAll('#bomListTableBody tr'));
        const checkedRows = allBomRows.filter(tr => {
            const cb = tr.querySelector('.bom-checkbox');
            return cb && cb.checked;
        });

        let pairs;
        if (checkedRows.length > 0) {
            pairs = checkedRows
                .filter(tr => parseInt(tr.dataset.shortageQty || "0", 10) > 0)
                .map(tr => `${tr.dataset.code}:${tr.dataset.totalqty}`);
        } else {
            pairs = allBomRows
                .filter(tr => parseInt(tr.dataset.shortageQty || "0", 10) > 0)
                .map(tr => `${tr.dataset.code}:${tr.dataset.totalqty}`);
        }

        const uniquePairs = Array.from(new Set(pairs));
        if (!uniquePairs.length) {
            return alert("발주할 자재가 없습니다.");
        }

        const confirmMsg = "발주가 필요한 자재들이 선택되었습니다.\n발주 등록하시겠습니까?";        if (!window.confirm(confirmMsg)) {
            return;
        }
        window.location.href = `/purchase-order?items=${encodeURIComponent(uniquePairs.join(","))}`;
    });

    // ■ 엔터키 → calculateMrp() 호출
    document.getElementById("itemSearch")?.addEventListener("keypress", e => {
        if (e.key === "Enter") calculateMrp();
    });

    // ■ 페이지 로드 시와 체크박스 변화 때마다 updateToolbarButtons() 호출
    document.querySelectorAll('#bomListTableBody').forEach(tbody => {
        tbody.addEventListener('change', e => {
            if (e.target.classList.contains('bom-checkbox')) {
                updateToolbarButtons();
            }
        });
    });

    // 초기 버튼 상태 셋팅
    updateToolbarButtons();

    // 최초 MRP 리스트 로드
    loadMrpList(1);
};
