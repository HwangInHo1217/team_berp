function openplaceDetailModal(data) {
  document.getElementById('placeDetailDate').textContent = data.date;
  document.getElementById('placeDetailCustomer').textContent = data.customer;
  document.getElementById('placeDetailItemCode').textContent = data.itemCode;
  document.getElementById('placeDetailItemName').textContent = data.itemName;
  document.getElementById('placeDetailQty').textContent = data.quantity;
  document.getElementById('placeDetailUnit').textContent = data.unit;
  document.getElementById('placeDetailWarehouse').textContent = data.warehouse;
  document.getElementById('placeDetailManager').textContent = data.manager;
  document.getElementById('placeDetailNote').textContent = data.note || ''; // note 없을 때 대비

  const modal = new bootstrap.Modal(document.getElementById('placeDetailModal'));
  modal.show();
}

//<!-- ✅ 등록/수정 모달 fetch 및 동작 함수 -->

      // 모달이 닫힐 때 기본 상태로 초기화
      const modalEl = document.getElementById('placeRegisterModal');
      if (modalEl) {
        modalEl.addEventListener('hidden.bs.modal', () => {
          document.getElementById('placeForm').reset();
          document.getElementById('placeForm').action = '/place/add';
          document.getElementById('submitBtn').textContent = '등록';
        });
      }

  // 등록 모드
  function openplaceAddModal() {
    document.getElementById('placeForm').reset();
    document.getElementById('placeForm').action = '/place/add';
    document.getElementById('submitBtn').textContent = '등록';
  }

  // 수정 모드
  function openplaceEditModal(data) {
    document.getElementById('placeDate').value = data.date;
    document.getElementById('placeNum').value = data.placeNum;
    document.getElementById('customerId').value = data.customer_id;
    document.getElementById('itemCode').value = data.item_code;
    document.getElementById('itemId').value = data.item_id;
    document.getElementById('quantity').value = data.quantity;
    document.getElementById('unit').value = data.unit;
    document.getElementById('warehouseId').value = data.warehouse_id;
    document.getElementById('manager').value = data.manager;
    document.getElementById('managerEmail').value = data.manager_email;
    document.getElementById('managerNum').value = data.manager_num;
    document.getElementById('note').value = data.note;

    document.getElementById('placeForm').action = `/place/update/${data.id}`;
    document.getElementById('submitBtn').textContent = '수정';

    const modal = new bootstrap.Modal(document.getElementById('placeRegisterModal'));
    modal.show();
  }

  //발주 초기화
  function placereset(){
	if (confirm("입력한 정보를 초기화하시겠습니까?")) {
	       document.querySelector('form[action="/place"]').reset();
	       alert("입력한 정보가 초기화되었습니다.");
	   } else {
	       alert("초기화가 취소되었습니다.");
	   }
  }
  
  //발주 등록
    function placesubmit() {
      if (confirm("입력한 정보를 저장하시겠습니까?")) {
        const form = document.querySelector('form[action="/place/add"]');
        const inputs = form.querySelectorAll('input[required], select[required]');

        for (let input of inputs) {
          if (!input.value.trim()) {
            alert("모든 필수 항목을 입력해주세요.");
            return;
          }
        }

        alert("저장이 완료되었습니다.");
        form.submit();
      } else {
        alert("저장이 취소되었습니다.");
      }
    }
	
	function addItemRow() {
	  const container = document.getElementById("itemListContainer");
	  const newRow = document.createElement("div");
	  newRow.className = "row align-items-end g-2 mb-2 item-row";
	  newRow.innerHTML = `
	    <div class="col-md-3">
	      <select class="form-select" name="itemId" required>
	        <option value="">-- 품목 선택 --</option>
	        <option value="P001">완제품 A</option>
	        <option value="P002">완제품 B</option>
	      </select>
	    </div>
	    <div class="col-md-3">
	      <input type="text" class="form-control" name="itemCode" placeholder="품목코드" required />
	    </div>
	    <div class="col-md-2">
	      <input type="number" class="form-control" name="quantity" placeholder="수량" min="1" required />
	    </div>
	    <div class="col-md-2">
	      <input type="text" class="form-control" name="unit" value="EA" required />
	    </div>
	    <div class="col-md-2">
	      <button type="button" class="btn btn-danger btn-sm" onclick="removeItemRow(this)">삭제</button>
	    </div>
	  `;
	  container.appendChild(newRow);
	}

	function removeItemRow(button) {
	  const row = button.closest(".item-row");
	  row.remove();
	}
