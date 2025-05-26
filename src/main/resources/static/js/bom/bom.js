function addChildRow() {
  const container = document.getElementById('child-items-area');
  const template = document.getElementById('child-item-select-template');
  const row = template.content.firstElementChild.cloneNode(true);
  container.appendChild(row);
}


document.getElementById('bomForm').addEventListener('submit', function(e) { //bom 등록에 관한 메ㅔ소드
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


let bomViewModalInstance;

function openBomViewModal(parentId) {
  fetch(`/bom/bom/${parentId}`)
    .then(response => response.json())
    .then(data => {
      document.getElementById("bomParentCode").innerText = data.parentCode;
      document.getElementById("bomParentName").innerText = data.parentName;

      const tbody = document.getElementById("bomComponentTableBody");
      tbody.innerHTML = "";

      data.components.forEach(c => {
        const row = `<tr><td>${c.childCode}</td><td>${c.childName}</td><td>${c.qty}</td></tr>`;
        tbody.insertAdjacentHTML("beforeend", row);
      });

      // 기존 모달 인스턴스가 있으면 사용, 없으면 새로 생성
      const modalEl = document.getElementById("bomViewModal");
      if (!bomViewModalInstance) {
        bomViewModalInstance = new bootstrap.Modal(modalEl);
      }
      bomViewModalInstance.show();
    })
    .catch(err => {
      alert("BOM 정보를 불러오지 못했습니다.");
      console.error(err);
    });
}


// 모달 닫힐 때 cleanup
document.getElementById('bomViewModal').addEventListener('hidden.bs.modal', function () {
  document.getElementById('bomComponentTableBody').innerHTML = '';
  document.getElementById('bomParentCode').innerText = '';
  document.getElementById('bomParentName').innerText = '';

  // 인스턴스 삭제
  if (bomViewModalInstance) {
    bomViewModalInstance.dispose();
    bomViewModalInstance = null;
  }
});


function openBomEditModal(button) {
  const parentId = button.getAttribute('data-id');

  fetch(`/bom/bom/${parentId}`)
    .then(response => response.json())
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
          </div>
        `;
        container.appendChild(row);
      });

      new bootstrap.Modal(document.getElementById("bomEditModal")).show();
    })
    .catch(err => {
      console.error(err);
      alert("BOM 정보를 불러오지 못했습니다.");
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
    </div>
  `;
  container.appendChild(row);
}

function childItemOptionsHtml(selectedCode) {
  const options = window.allRawItemOptions || [];
  return options.map(opt =>
    `<option value="${opt.value}" ${opt.code === selectedCode ? 'selected' : ''}>${opt.label}</option>`
  ).join('');
}


document.getElementById('bomEditForm').addEventListener('submit', function(e) {
  e.preventDefault();

  const parentItemId = document.getElementById('editParentItemId').value;
  const childItemIds = [...document.querySelectorAll('select[name="child_item_id[]"]')].map(el => el.value);
  const quantities = [...document.querySelectorAll('input[name="qty[]"]')].map(el => el.value);

  const cleanedComponents = childItemIds
    .map((id, i) => ({ id, qty: quantities[i] }))
    .filter(c => c.id !== "" && c.qty !== "");

  const payload = {
    parentItemId: parseInt(parentItemId),
    components: cleanedComponents.map(c => ({
      childItemId: parseInt(c.id),
      qty: parseInt(c.qty)
    }))
  };

  fetch('/bom/bom', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
    .then(res => {
      if (res.ok) {
        alert('수정 완료');
        location.reload();
      } else {
        alert('수정 실패');
      }
    });
});


// 삭제 함수
function deleteSelectedBoms() {
  const checkedIds = [...document.querySelectorAll('input[type="checkbox"]:checked')]
    .map(cb => cb.value);

  if (checkedIds.length === 0) {
    alert("삭제할 BOM을 선택하세요.");
    return;
  }

  if (!confirm("정말 삭제하시겠습니까?")) return;

  fetch('/bom/bom', {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(checkedIds)
  })
    .then(res => {
      if (res.ok) {
        alert("삭제 완료");
        location.reload();
      } else {
        alert("삭제 실패");
      }
    })
    .catch(err => {
      console.error(err);
      alert("에러가 발생했습니다.");
    });
}
