// ✅ 사용여부 셀렉트 박스 변경 시 자동 조회
document.getElementById('searchUseYn').addEventListener('change', () => {
  loadItemList(0); // 첫 페이지부터 다시 조회
});


// ✅ 공통 상태 관리
let currentTab = sessionStorage.getItem("tab") || "all";

// ✅ DOM 로딩 후 기본 실행
document.addEventListener("DOMContentLoaded", () => {
  bindTabClickEvents();
  bindSearchEvent();
  loadItemList(); // 기본 로딩
});

// ✅ 탭 클릭 이벤트 바인딩
function bindTabClickEvents() {
  document.querySelectorAll('[data-bs-toggle="tab"]').forEach(tab => {
    tab.addEventListener('click', () => {
      currentTab = tab.id.split("-")[0];
      sessionStorage.setItem("tab", currentTab);
      loadItemList(0);
    });
  });
}

// ✅ 검색 버튼 바인딩
function bindSearchEvent() {
  document.getElementById('searchForm').addEventListener('submit', function (e) {
    e.preventDefault();
    loadItemList(0);
  });
}

// ✅ 목록 조회
function loadItemList(page = 0) {
  const type = document.getElementById("type")?.value || "";
  const keyword = document.getElementById("keyword")?.value || "";
  const useYn = document.getElementById("searchUseYn")?.value || "";

  const params = new URLSearchParams({
    tab: currentTab,
    type: type,
    keyword: keyword,
    useYn: useYn,
    page: page
  });

  fetch(`/api/item/list?${params.toString()}`)
    .then(res => res.json())
    .then(data => {
      renderItemTable(data.content || []);
      renderPagination(data.totalPages, data.number);
    })
    .catch(err => {
      console.error("❌ 목록 조회 실패", err);
      alert("품목 목록을 불러오는 중 오류 발생");
    });
}

// ✅ 테이블 렌더링
function renderItemTable(items) {
  const container = document.querySelector(`#${currentTab} tbody`);
  if (!container) return;

  container.innerHTML = '';

  if (items.length === 0) {
    container.innerHTML = `<tr><td colspan="8">등록된 품목이 없습니다.</td></tr>`;
    return;
  }

  items.forEach(item => {
    container.insertAdjacentHTML('beforeend', `
      <tr>
        <td><input type="checkbox" name="ids" value="${item.id}" /></td>
        <td>${item.code}</td>
        <td>${item.name}</td>
        ${currentTab === 'all' ? `<td>${item.type}</td>` : ''}
        <td>${item.spec || '-'}</td>
        <td>${item.unit || '-'}</td>
        <td>${item.use}</td>
        <td>
          <button type="button" class="btn btn-sm btn-outline-primary"
            data-id="${item.id}" data-name="${item.name}" data-type="${item.type}"
            data-spec="${item.spec}" data-unit="${item.unit}" data-use="${item.use}"
            onclick="openEditModal(this)">수정</button>
        </td>
      </tr>
    `);
  });
}

// ✅ 페이징 렌더링
function renderPagination(totalPages, currentPage) {
  const pagination = document.getElementById("bomPagination") || document.querySelector(".pagination");
  if (!pagination) return;
  pagination.innerHTML = '';

  const createItem = (page, label, active = false, disabled = false) => `
    <li class="page-item ${active ? "active" : ""} ${disabled ? "disabled" : ""}">
      <a class="page-link" href="#" onclick="loadItemList(${page}); event.preventDefault();">${label}</a>
    </li>
  `;

  pagination.innerHTML += createItem(currentPage - 1, "이전", false, currentPage === 0);

  for (let i = 0; i < totalPages; i++) {
    pagination.innerHTML += createItem(i, i + 1, i === currentPage);
  }

  pagination.innerHTML += createItem(currentPage + 1, "다음", false, currentPage >= totalPages - 1);
}

// ✅ 등록 요청
function submitItem() {
  const form = document.getElementById('addItemForm');
  const formData = new FormData(form);
  const jsonData = {};
  formData.forEach((value, key) => jsonData[key] = value);

  fetch('/item/item', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(jsonData)
  })
    .then(res => {
      if (!res.ok) throw new Error("등록 실패");
      return res.json();
    })
    .then(() => {
      alert('저장 완료!');
      bootstrap.Modal.getOrCreateInstance(document.getElementById("itemRegisterModal")).hide();
      loadItemList();
    })
    .catch(err => {
      alert('저장 실패!');
      console.error(err);
    });
}

// ✅ 수정 요청
function submitItemUpdate() {
  const id = document.getElementById("editItemId").value;
  const jsonData = {
    name: document.getElementById("editItemName").value,
    type: document.getElementById("editItemType").value,
    spec: document.getElementById("editItemSpec").value,
    unit: document.getElementById("editItemUnit").value,
    use: document.getElementById("editItemUse").value
  };

  fetch(`/item/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(jsonData)
  })
    .then(res => {
      if (!res.ok) throw new Error("수정 실패");
      alert("수정 완료!");
      bootstrap.Modal.getOrCreateInstance(document.getElementById("itemEditModal")).hide();
      loadItemList();
    })
    .catch(err => {
      alert("수정 실패!");
      console.error(err);
    });
}

// ✅ 삭제 요청
function submitDelete() {
  const checked = document.querySelectorAll("input[name='ids']:checked");
  if (checked.length === 0) return alert("삭제할 품목을 선택하세요.");

  const ids = Array.from(checked).map(cb => cb.value);
  if (!confirm("정말 삭제하시겠습니까?")) return;

  fetch('/item/delete', {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(ids)
  })
    .then(res => {
      if (!res.ok) throw new Error("삭제 실패");
      alert("삭제 완료");
      loadItemList();
    })
    .catch(err => {
      alert("삭제 중 오류");
      console.error(err);
    });
}

// ✅ 수정 모달 오픈
function openEditModal(btn) {
  document.getElementById("editItemId").value = btn.dataset.id;
  document.getElementById("editItemName").value = btn.dataset.name;
  document.getElementById("editItemType").value = btn.dataset.type;
  document.getElementById("editItemSpec").value = btn.dataset.spec;
  document.getElementById("editItemUnit").value = btn.dataset.unit;
  document.getElementById("editItemUse").value = btn.dataset.use;

  bootstrap.Modal.getOrCreateInstance(document.getElementById("itemEditModal")).show();
}
