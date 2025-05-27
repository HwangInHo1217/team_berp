// ✅ 자재 행 추가 함수
function addChildRow() {
  const container = document.getElementById('child-items-area');
  const template = document.getElementById('child-item-select-template');
  const row = template.content.firstElementChild.cloneNode(true);
  container.appendChild(row);
}

// ✅ BOM 등록 요청
const bomForm = document.getElementById('bomForm');
if (bomForm) {
  bomForm.addEventListener('submit', function(e) {
    e.preventDefault();
    const form = e.target;

    const parentItemId = form.querySelector('select[name="parent_item_id"]').value;
    const childItemIds = [...form.querySelectorAll('select[name="child_item_id[]"]')].map(el => el.value);
    const quantities   = [...form.querySelectorAll('input[name="qty[]"]')].map(el => el.value);
    const seqNos       = [...form.querySelectorAll('input[name="seq_no[]"]')].map(el => el.value);
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
        qty: parseInt(quantities[i]),
        seqNo: seqNos[i] ? parseInt(seqNos[i]) : null,
        lossRt: lossRates[i] ? parseFloat(lossRates[i]) : null,
        itemPrice: itemPrices[i] ? parseInt(itemPrices[i]) : null,
        remark: remarks[i] || ""
      }))
    };

    fetch('/bom/bom', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).then(res => {
      if (res.ok) {
        alert('BOM 등록 성공!');
        location.reload();
      } else {
        alert('등록 실패');
      }
    }).catch(err => {
      console.error(err);
      alert('에러가 발생했습니다.');
    });
  });
}

// ✅ BOM 보기 (모달)
function openBomViewModal(parentId) {
  const modalEl = document.getElementById("bomViewModal");
  if (!modalEl) return alert("모달 요소가 없습니다.");

  fetch(`/bom/bom/${parentId}`)
    .then(res => res.json())
    .then(data => {
      const parentNameEl = document.getElementById("bomParentName");
      const productTbody = document.getElementById("bomProductList");
      const materialTbody = document.getElementById("bomMaterialList");
      const treeList = document.getElementById("bomTreeList");
      const tableBody = document.getElementById("bomComponentTableBody");

      if (!parentNameEl || !productTbody || !materialTbody || !treeList || !tableBody) return;

      parentNameEl.innerText = data.parentName;

      productTbody.innerHTML = `
        <tr>
          <td>1</td>
          <td>${data.parentCode}</td>
          <td>${data.parentName}</td>
          <td>${data.components[0]?.spec || '-'}</td>
          <td>${data.components[0]?.unit || '-'}</td>
        </tr>`;

      materialTbody.innerHTML = "";
      data.components.forEach((c, i) => {
        materialTbody.insertAdjacentHTML("beforeend", `
          <tr>
            <td>${i + 1}</td>
            <td>${c.childCode}</td>
            <td>${c.childName}</td>
            <td>${c.spec || '-'}</td>
            <td>${c.unit || '-'}</td>
          </tr>`);
      });

      treeList.innerHTML = "";
      data.components.forEach(c => {
        treeList.insertAdjacentHTML("beforeend", `
          <li>└ <span>${c.childName}</span>
            <span class="text-muted small">수량: ${c.qty}, 로스율: ${c.lossRate}, 단가: ${c.unitPrice}, 비고: ${c.remark}</span>
          </li>`);
      });

      tableBody.innerHTML = "";
      data.components.forEach((c, i) => {
        tableBody.insertAdjacentHTML("beforeend", `
          <tr>
            <td>${i + 1}</td>
            <td>${data.parentName}</td>
            <td>${c.childName}</td>
            <td>${c.seqNo ?? '-'}</td>
            <td>${c.qty}</td>
            <td>${c.lossRate}</td>
            <td>${c.unitPrice}</td>
            <td>${c.remark}</td>
          </tr>`);
      });

      bootstrap.Modal.getOrCreateInstance(modalEl).show();
    })
    .catch(err => {
      alert("BOM 정보를 불러오지 못했습니다.");
      console.error(err);
    });
}

// ✅ 모달 닫힐 때 클린업
const modalEl = document.getElementById('bomViewModal');
if (modalEl) {
  modalEl.addEventListener('hidden.bs.modal', () => {
    document.getElementById('bomComponentTableBody').innerHTML = '';
    document.getElementById('bomParentName').innerText = '선택된 제품 없음';
    document.getElementById('bomProductList').innerHTML = '';
    document.getElementById('bomMaterialList').innerHTML = '';
    document.getElementById('bomTreeList').innerHTML = '';
  });
}

// ✅ BOM 수정 모달 열기
function openBomEditModal(button) {
  const parentId = button.getAttribute('data-id');

  fetch(`/bom/bom/${parentId}`)
    .then(res => res.json())
    .then(data => {
      document.getElementById("editParentItemId").value = parentId;
      document.getElementById("editParentItemText").value = `${data.parentCode} - ${data.parentName}`;

      const container = document.getElementById("bomEditTableArea");
      container.innerHTML = "";

      data.components.forEach(c => {
        const row = document.createElement('div');
        row.className = 'row mb-2';
        row.innerHTML = `
          <div class="col-md-6">
            <select name="child_item_id[]" class="form-select" required>
              ${childItemOptionsHtml(c.childCode)}
            </select>
          </div>
          <div class="col-md-3">
            <input type="number" name="qty[]" class="form-control" value="${c.qty}" required />
          </div>
          <div class="col-md-3 d-flex align-items-end">
            <button type="button" class="btn btn-outline-danger w-100" onclick="this.closest('.row').remove()">삭제</button>
          </div>`;
        container.appendChild(row);
      });

      new bootstrap.Modal(document.getElementById("bomEditModal")).show();
    });
}

function addEditRow() {
  const container = document.getElementById("bomEditTableArea");
  const row = document.createElement("div");
  row.className = "row mb-2";
  row.innerHTML = `
    <div class="col-md-6">
      <select name="child_item_id[]" class="form-select" required>
        ${childItemOptionsHtml()}
      </select>
    </div>
    <div class="col-md-3">
      <input type="number" name="qty[]" class="form-control" required placeholder="예: 10" />
    </div>
    <div class="col-md-3 d-flex align-items-end">
      <button type="button" class="btn btn-outline-danger w-100" onclick="this.closest('.row').remove()">삭제</button>
    </div>`;
  container.appendChild(row);
}

function childItemOptionsHtml(selectedCode) {
  const options = window.allRawItemOptions || [];
  return options.map(opt =>
    `<option value="${opt.value}" ${opt.code === selectedCode ? 'selected' : ''}>${opt.label}</option>`
  ).join('');
}

// ✅ BOM 수정 요청
const bomEditForm = document.getElementById('bomEditForm');
if (bomEditForm) {
  bomEditForm.addEventListener('submit', function(e) {
    e.preventDefault();

    const parentItemId = document.getElementById('editParentItemId').value;
    const childItemIds = [...document.querySelectorAll('select[name="child_item_id[]"]')].map(el => el.value);
    const quantities = [...document.querySelectorAll('input[name="qty[]"]')].map(el => el.value);

    const payload = {
      parentItemId: parseInt(parentItemId),
      components: childItemIds.map((id, i) => ({
        childItemId: parseInt(id),
        qty: parseInt(quantities[i])
      }))
    };

    fetch('/bom/bom', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).then(res => {
      if (res.ok) {
        alert('수정 완료');
        location.reload();
      } else {
        alert('수정 실패');
      }
    });
  });
}

// ✅ BOM 삭제 요청
function deleteSelectedBoms() {
  const checkedIds = [...document.querySelectorAll('input[type="checkbox"]:checked')].map(cb => cb.value);

  if (checkedIds.length === 0) return alert("삭제할 BOM을 선택하세요.");
  if (!confirm("정말 삭제하시겠습니까?")) return;

  fetch('/bom/bom', {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(checkedIds)
  }).then(res => {
    if (res.ok) {
      alert("삭제 완료");
      location.reload();
    } else {
      alert("삭제 실패");
    }
  }).catch(err => {
    console.error(err);
    alert("에러가 발생했습니다.");
  });
}
