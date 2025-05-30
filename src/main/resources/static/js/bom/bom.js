// 🔍 사용여부 필터 선택 시 목록 새로 로드
let bomEditMode = false;
let selectedParentId = null;

document.getElementById("searchUseYn").addEventListener("change", function () {
  loadBomList(0);
});

// ✅ 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', () => {
  loadBomList();
});

// ✅ 검색
document.getElementById('searchForm').addEventListener('submit', function (e) {
  e.preventDefault();
  loadBomList(0);
});

// ✅ BOM 목록 불러오기
function loadBomList(page = 0) {
  const searchField = document.getElementById('searchType').value;
  const keyword = document.getElementById('searchKeyword').value;
  const useYn = document.getElementById('searchUseYn').value;

  const params = new URLSearchParams({ searchField, keyword, useYn, page });
  fetch(`/api/bom/list?${params}`)
    .then(res => res.json())
    .then(data => {
      renderBomTable(data.content);
      renderPagination(data.totalPages, data.number, searchField, keyword, useYn);

      window.allRawItemOptions = data.selectMaterialList.map(item => ({
        value: item.id,
        code: item.code,
        label: `${item.code} - ${item.name}`
      }));

      const parentSelect = document.getElementById("parentItemSelect");
      if (parentSelect) {
        parentSelect.innerHTML = `<option value="">-- 선택하세요 --</option>` +
          data.selectProductList.map(item =>
            `<option value="${item.id}">${item.code} - ${item.name}</option>`
          ).join('');
      }
    })
    .catch(err => {
      console.error("❌ BOM 목록 조회 실패:", err);
      alert("BOM 목록 조회 실패");
    });
}

// ✅ 테이블 렌더링
function renderBomTable(items) {
  const tbody = document.getElementById("bomTableBody");
  tbody.innerHTML = '';

  if (!items || items.length === 0) {
    tbody.innerHTML = '<tr><td colspan="10">데이터가 없습니다.</td></tr>';
    return;
  }

  items.forEach(item => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td><input type="checkbox" value="${item.id}" /></td>
      <td>${item.code}</td>
      <td>${item.name}</td>
      <td>${item.type}</td>
      <td>${item.spec}</td>
      <td>${item.unit}</td>
      <td>${item.use}</td>
      <td><button class="btn btn-sm btn-info" onclick="openBomVersionSelectModal(${item.id})">보기</button></td>
      <td><button class="btn btn-sm btn-outline-info" onclick="openItemDetailModal('${item.code}')">보기</button></td>
      <td><button class="btn btn-sm btn-outline-secondary" onclick="openBomEditVersionSelectModal(${item.id})">수정</button></td>`;
    tbody.appendChild(row);
  });
}

// ✅ 페이징 렌더링
function renderPagination(totalPages, currentPage, searchField, keyword, useYn) {
  const pagination = document.getElementById("bomPagination");
  pagination.innerHTML = '';

  if (totalPages <= 1) return;

  const createItem = (page, label, active = false, disabled = false) => `
    <li class="page-item ${active ? 'active' : ''} ${disabled ? 'disabled' : ''}">
      <a class="page-link" href="#" onclick="loadBomList(${page}); event.preventDefault();">${label}</a>
    </li>`;

  pagination.innerHTML += createItem(currentPage - 1, '이전', false, currentPage === 0);

  for (let i = 0; i < totalPages; i++) {
    pagination.innerHTML += createItem(i, i + 1, currentPage === i);
  }

  pagination.innerHTML += createItem(currentPage + 1, '다음', false, currentPage === totalPages - 1);
}

// ✅ 등록 모달 자재 행 추가
function addChildRow() {
  const container = document.getElementById("child-items-area");
  const template = document.getElementById("child-item-template");
  const row = template.content.firstElementChild.cloneNode(true);

  const select = row.querySelector("select[name='child_item_id[]']");
  select.innerHTML = window.allRawItemOptions.map(opt =>
    `<option value="${opt.value}">${opt.label}</option>`
  ).join('');

  container.appendChild(row);
}

// ✅ BOM 보기 모달 열기
function openBomViewModal(parentId) {
  fetch(`/api/bom/${parentId}`)
    .then(res => {
      if (!res.ok) throw new Error(`조회 실패: ${res.status}`);
      return res.json();
    })
    .then(data => {
      document.getElementById("bomParentName").innerText = data.parentName;
      const tbody = document.getElementById("bomComponentTableBody");
      tbody.innerHTML = "";

      data.components.forEach((c, i) => {
        const row = `
          <tr>
            <td>${i + 1}</td>
            <td>${data.parentName}</td>
            <td>${c.childName}</td>
            <td>${c.seqNo ?? '-'}</td>
            <td>${c.qty}</td>
            <td>${c.lossRate}</td>
            <td>${c.unitPrice}</td>
            <td>${c.remark}</td>
          </tr>`;
        tbody.insertAdjacentHTML("beforeend", row);
      });

      bootstrap.Modal.getOrCreateInstance(document.getElementById("bomViewModal")).show();
    })
    .catch(err => {
      alert("구성 보기 실패");
      console.error(err);
    });
}

// ✅ 수정 버튼 클릭 시: 버전 선택 모달 오픈 (수정용)
function openBomEditVersionSelectModal(parentId) {
  selectedParentId = parentId;
  bomEditMode = true; // 수정모드 플래그 ON
  openBomVersionSelectModal(parentId); // 기존 함수 재사용
}

// ✅ 버전 선택 모달 오픈
function openBomVersionSelectModal(parentId) {
  fetch(`/api/bom/versions/${parentId}`)
    .then(res => {
      if (!res.ok) throw new Error("버전 목록 조회 실패");
      return res.json();
    })
    .then(versions => {
      const select = document.getElementById("bomVersionSelect");
      select.innerHTML = versions.map(v =>
        `<option value="${v.id}">${v.versionCode} (${v.useYn})</option>`
      ).join('');

      bootstrap.Modal.getOrCreateInstance(document.getElementById("bomVersionSelectModal")).show();
    })
    .catch(err => {
      console.error("❌ BOM 버전 목록 조회 실패:", err);
      alert("버전 목록 조회 실패");
    });
}

// ✅ 선택한 버전 ID 기반 구성 조회
function loadBomByVersion() {
  const versionId = document.getElementById("bomVersionSelect").value;
  if (!versionId) {
    alert("버전을 선택해주세요.");
    return;
  }

  if (bomEditMode) {
    loadBomVersionForEdit(versionId);
  } else {
    loadBomVersionForView(versionId);
  }
}

// ✅ 보기용 구성 조회
function loadBomVersionForView(versionId) {
  fetch(`/api/bom/version/${versionId}`)
    .then(res => res.json())
    .then(data => {
      document.getElementById("bomParentName").innerText = data.parentName;
      const tbody = document.getElementById("bomComponentTableBody");
      tbody.innerHTML = "";

      data.components.forEach((c, i) => {
        const row = `
          <tr>
            <td>${i + 1}</td>
            <td>${data.parentName}</td>
            <td>${c.childName}</td>
            <td>${c.seqNo ?? '-'}</td>
            <td>${c.qty}</td>
            <td>${c.lossRate}</td>
            <td>${c.unitPrice}</td>
            <td>${c.remark}</td>
          </tr>`;
        tbody.insertAdjacentHTML("beforeend", row);
      });

      bootstrap.Modal.getInstance(document.getElementById("bomVersionSelectModal")).hide();
      bootstrap.Modal.getOrCreateInstance(document.getElementById("bomViewModal")).show();
    })
    .catch(err => {
      alert("BOM 구성 조회 실패");
      console.error(err);
    });
}

// ✅ 수정 구성 정보 로드 함수
function loadBomVersionForEdit(versionId) {
	// 숫자만 추출하는 유틸 함수
	function extractNumber(str) {
	  if (!str) return "";
	  return parseFloat(str.toString().replace(/[^\d.-]/g, "")) || "";
	}

  fetch(`/api/bom/version/${versionId}`)
    .then(res => {
      if (!res.ok) throw new Error("서버 오류");
      return res.json();
    })
    .then(data => {
      // 버전 정보 채우기
      document.getElementById("editVersionId").value = data.versionId;
      document.getElementById("editVersionCode").value = data.versionCode;
      document.getElementById("editDescription").value = data.description;
      document.getElementById("editUseYn").value = data.useYn;

      // 완제품 목록 드롭다운 다시 채우기 (parentItemSelect)
      const parentSelect = document.getElementById("editParentItemSelect");
      parentSelect.innerHTML = `<option value="">-- 선택하세요 --</option>` +
        window.allRawItemOptions
          .filter(item => item.type === "product")
          .map(item =>
            `<option value="${item.value}" ${item.value === data.parentItemId ? 'selected' : ''}>
              ${item.code} - ${item.label}
            </option>`
          ).join('');

      parentSelect.value = data.parentItemId;

      // 구성 자재 행 렌더링
      const area = document.getElementById("bomEditTableArea");
      area.innerHTML = ""; // 초기화

      data.components.forEach((c, i) => {
        const row = document.createElement("div");
        row.className = "row g-2 align-items-end mb-2";
        row.innerHTML = `
          <div class="col-md-2">
            <label class="form-label">자재</label>
            <select class="form-select" name="child_item_id[]">
              ${window.allRawItemOptions.map(opt =>
                `<option value="${opt.value}" ${opt.value === c.childItemId ? 'selected' : ''}>
                  ${opt.label}
                </option>`).join('')}
            </select>
          </div>
          <div class="col-md-1">
            <label class="form-label">순번</label>
            <input type="number" class="form-control" name="seq_no[]" value="${c.seqNo ?? ''}" />
          </div>
          <div class="col-md-1">
            <label class="form-label">소요량</label>
            <input type="number" class="form-control" name="qty[]" value="${c.qty}" required />
          </div>
          <div class="col-md-1">
            <label class="form-label">로스율</label>
            <input type="number" step="0.01" class="form-control" name="loss_rt[]" value="${extractNumber(c.lossRate)}" />
          </div>
          <div class="col-md-2">
            <label class="form-label">재료비</label>
            <input type="number" class="form-control" name="item_price[]" value="${extractNumber(c.unitPrice)}" />
          </div>
          <div class="col-md-3">
            <label class="form-label">비고</label>
            <input type="text" class="form-control" name="remark[]" value="${c.remark ?? ''}" />
          </div>
          <div class="col-md-2">
            <label class="form-label d-block">&nbsp;</label>
            <button type="button" class="btn btn-outline-danger w-100" onclick="this.closest('.row').remove()">삭제</button>
          </div>
        `;
        area.appendChild(row);
      });

      // 모달 표시
      bootstrap.Modal.getInstance(document.getElementById("bomVersionSelectModal")).hide();
      bootstrap.Modal.getOrCreateInstance(document.getElementById("bomEditModal")).show();
    })
    .catch(err => {
      console.error("❌ 수정 정보 불러오기 실패:", err);
      alert("수정 정보 불러오기 실패");
    });
}


// ✅ BOM 삭제 요청
function deleteSelectedBoms() {
  const checkboxes = document.querySelectorAll('#bomTableBody input[type="checkbox"]:checked');
  if (checkboxes.length === 0) {
    alert("삭제할 BOM을 선택하세요.");
    return;
  }

  if (!confirm("정말 삭제하시겠습니까?")) return;

  const parentIds = Array.from(checkboxes).map(cb => parseInt(cb.value));

  fetch('/api/bom', {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(parentIds)
  }).then(res => {
    if (res.ok) {
      alert("✅ 삭제 성공");
      location.reload();
    } else {
      alert("❌ 삭제 실패");
    }
  }).catch(err => {
    console.error('❌ 삭제 중 오류 발생:', err);
    alert("에러가 발생했습니다.");
  });
}

// ✅ BOM 버전 삭제 요청
function deleteBomVersion() {
  const versionId = document.getElementById("bomVersionSelect").value;
  if (!versionId) {
    alert("삭제할 버전을 선택해주세요.");
    return;
  }

  if (!confirm("정말 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")) return;

  fetch(`/api/bom/version/${versionId}`, {
    method: 'DELETE'
  })
    .then(res => {
      if (res.ok) {
        alert("✅ BOM 버전 삭제 완료");
        bootstrap.Modal.getInstance(document.getElementById("bomVersionSelectModal")).hide();
        loadBomList();
      } else {
        throw new Error("삭제 실패");
      }
    })
    .catch(err => {
      console.error("❌ 삭제 실패:", err);
      alert("BOM 버전 삭제에 실패했습니다.");
    });
}
