// ✅ BOM 목록 불러오기
function loadBomList(page = 0) {
  const searchField = document.getElementById('searchType').value;
  const keyword = document.getElementById('searchKeyword').value;
  const useYn = document.getElementById('searchUseYn').value;

  const params = new URLSearchParams({ searchField, keyword, useYn, page });

  fetch(`/api/bom/list?${params.toString()}`)
    .then(res => res.json())
    .then(data => {
      if (!data || !data.content) throw new Error("Invalid response structure");

      renderBomTable(data.content);
      renderPagination(data.totalPages, data.currentPage, searchField, keyword, useYn);

      // ✅ 전역 raw 자재 옵션 설정
      window.allRawItemOptions = data.selectMaterialList.map(item => ({
        value: item.id,
        code: item.code,
        label: `${item.code} - ${item.name}`
      }));

      // ✅ 등록용 완제품 옵션
      const parentSelect = document.getElementById("parentItemSelect");
      if (parentSelect) {
        parentSelect.innerHTML = `<option value="">-- 선택하세요 --</option>` +
          data.selectProductList.map(item =>
            `<option value="${item.id}">${item.code} - ${item.name}</option>`
          ).join('');
      }
    })
    .catch(err => {
      console.error("❌ BOM 리스트 불러오기 실패", err);
      alert("BOM 목록 조회에 실패했습니다.");
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
      <td><button class="btn btn-sm btn-info" onclick="openBomViewModal(${item.id})">보기</button></td>
      <td><button class="btn btn-sm btn-outline-info" onclick="openItemDetailModal('${item.code}')">보기</button></td>
      <td><button class="btn btn-sm btn-outline-secondary" onclick="openBomEditModal(this)" data-id="${item.id}">수정</button></td>
    `;
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

      if (data.components) {
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
      }

      bootstrap.Modal.getOrCreateInstance(document.getElementById("bomViewModal")).show();
    })
    .catch(err => {
      alert("구성 보기 실패");
      console.error(err);
    });
}

// ✅ 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', () => {
  loadBomList();
});

// ✅ 검색
document.getElementById('searchForm').addEventListener('submit', function (e) {
  e.preventDefault();
  loadBomList(0);
});

// ✅ BOM 등록 요청
const bomForm = document.getElementById('bomForm');
if (bomForm) {
  bomForm.addEventListener('submit', function (e) {
    e.preventDefault(); // 기본 submit 막기

    const form = e.target;

    const parentItemId = form.querySelector('select[name="parent_item_id"]').value;
    const childItemIds = [...form.querySelectorAll('select[name="child_item_id[]"]')].map(el => el.value);
    const seqNos       = [...form.querySelectorAll('input[name="seq_no[]"]')].map(el => el.value);
    const quantities   = [...form.querySelectorAll('input[name="qty[]"]')].map(el => el.value);
    const lossRates    = [...form.querySelectorAll('input[name="loss_rt[]"]')].map(el => el.value);
    const itemPrices   = [...form.querySelectorAll('input[name="item_price[]"]')].map(el => el.value);
    const remarks      = [...form.querySelectorAll('input[name="remark[]"]')].map(el => el.value);

    if (!parentItemId || childItemIds.includes("") || quantities.includes("")) {
      alert("모든 필수 항목을 입력해주세요.");
      return;
    }

    const payload = {
      parentItemId: parseInt(parentItemId),
      components: childItemIds.map((id, i) => ({
        childItemId: parseInt(id),
        seqNo: seqNos[i] ? parseInt(seqNos[i]) : null,
        qty: parseInt(quantities[i]),
        lossRt: lossRates[i] ? parseFloat(lossRates[i]) : null,
        itemPrice: itemPrices[i] ? parseInt(itemPrices[i]) : null,
        remark: remarks[i] || ""
      }))
    };

    fetch('/api/bom', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).then(res => {
      if (res.ok) {
        alert('✅ 등록 성공!');
        location.reload();
      } else {
        alert('❌ 등록 실패');
      }
    }).catch(err => {
      console.error('❌ 등록 중 에러 발생', err);
      alert('에러가 발생했습니다.');
    });
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

