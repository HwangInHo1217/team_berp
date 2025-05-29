//상세 모달 오픈
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

  //발주 등록 초기화
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
	
	//품목 선택 
	function addItemRow() {
	  const container = document.getElementById("itemListContainer");
	  const newRow = document.createElement("tr");
	  newRow.className = "item-row";
	  newRow.innerHTML = `
	    <td>
	      <select class="form-select" name="itemId" required>
	        <option value="">-- 품목 선택 --</option>
	        <option value="P001">완제품 A</option>
	        <option value="P002">완제품 B</option>
	      </select>
	    </td>
	    <td>
	      <input type="text" class="form-control" name="itemCode" placeholder="품목코드" required />
	    </td>
	    <td>
	      <input type="number" class="form-control" name="quantity" placeholder="수량" min="1" required />
	    </td>
	    <td>
	      <input type="text" class="form-control" name="unit" value="EA" required />
	    </td>
		<td>
	      <input type="text" class="form-control" name="unit" placeholder="단가" required />
		</td>
		<td>
			<input type="text" class="form-control" name="unit" placeholder="품목총계" required />
		</td>			
	    <td class="text-center">
	      <button type="button" class="btn btn-danger btn-sm" onclick="removeItemRow(this)">삭제</button>
	    </td>
	  `;
	  container.appendChild(newRow);
	}

	//품목 삭제
	function removeItemRow(button) {
	  const row = button.closest(".item-row");
	  row.remove();
	}
	
	//사업장 명 불러오기
	function filterCompanies() {
	     const type = document.getElementById("companyType").value;
	     const select = document.getElementById("companySelect");

	     const options = select.querySelectorAll("option");

	     options.forEach(opt => {
	       const optType = opt.dataset.type;
	       if (!optType) return; // 첫 번째 기본 옵션은 무시
	       opt.hidden = optType !== type;
	     });

	     // 선택된 값 초기화
	     select.value = "";
	   }


	function loadItemNames(select) {
	  const type = select.value;
	  const row = select.closest(".item-row");
	  const itemSelect = row.querySelector("[name*='itemId']");
	  fetch(`/api/items?type=${type}`)
	    .then(res => res.json())
	    .then(data => {
	      itemSelect.innerHTML = '<option value="">-- 품목명 --</option>';
	      data.forEach(item => {
	        const option = document.createElement("option");
	        option.value = item.id;
	        option.text = item.name;
	        option.dataset.code = item.code;
	        option.dataset.unit = item.unit;
	        option.dataset.price = item.unitPrice;
	        itemSelect.appendChild(option);
	      });
	    });
	}

	function fillItemDetails(select) {
	  const option = select.selectedOptions[0];
	  const row = select.closest(".item-row");
	  row.querySelector("[name*='itemCode']").value = option.dataset.code;
	  row.querySelector("[name*='unit']").value = option.dataset.unit;
	  row.querySelector("[name*='unitPrice']").value = option.dataset.price;
	  calculateItemTotal(row.querySelector("[name*='quantity']"));
	}

	function calculateItemTotal(qtyInput) {
	  const row = qtyInput.closest(".item-row");
	  const unitPrice = parseFloat(row.querySelector("[name*='unitPrice']").value || 0);
	  const quantity = parseInt(qtyInput.value || 0);
	  row.querySelector("[name*='unitPriceAll']").value = unitPrice * quantity;
	  calculateOrderTotals();
	}

	function calculateOrderTotals() {
	  let totalQty = 0, totalAmount = 0;
	  document.querySelectorAll(".item-row").forEach(row => {
	    const qty = parseInt(row.querySelector("[name*='quantity']").value || 0);
	    const priceAll = parseFloat(row.querySelector("[name*='unitPriceAll']").value || 0);
	    totalQty += qty;
	    totalAmount += priceAll;
	  });
	  document.getElementById("totalQty").innerText = totalQty;
	  document.getElementById("totalAmount").innerText = totalAmount.toLocaleString();
	}
