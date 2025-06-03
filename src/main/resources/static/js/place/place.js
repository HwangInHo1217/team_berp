// 등록 모드
function openplaceAddModal() {
  const form = document.getElementById('placeForm');
  form.reset();
  filterCompanies(); // 유형에 따른 사업장 필터링 재적용
  resetItemRows();   // 품목 행 하나만 남기고 초기화
  form.action = '/place/add';
  document.getElementById('submitBtn').textContent = '등록';
}

//초기 품목 행 클리어 함수
function resetItemRows() {
  const container = document.getElementById('itemListContainer');
  const firstRow = container.querySelector('.item-row');

  container.innerHTML = '';
  container.appendChild(firstRow.cloneNode(true));
  container.querySelectorAll('input, select').forEach(el => el.value = '');
}


//상세 모달 오픈 - 발주 상세정보를 표시하는 모달을 여는 함수
//data로 전달된 값들을 모달 내 span 요소에 출력
function openplaceDetailModal(data) {
  const modalEl = document.getElementById('placeDetailModal');
  const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);

  // 데이터 채우기
  document.getElementById('placeDetailDate').textContent = data.date;
  document.getElementById('placeDetailCustomer').textContent = data.customer;
  document.getElementById('placeDetailItemCode').textContent = data.itemCode;
  document.getElementById('placeDetailItemName').textContent = data.itemName;
  document.getElementById('placeDetailQty').textContent = data.quantity;
  document.getElementById('placeDetailUnit').textContent = data.unit;
  document.getElementById('placeDetailWarehouse').textContent = data.warehouse;
  document.getElementById('placeDetailEmployee').textContent = data.employee;
  document.getElementById('placeDetailNote').textContent = data.note || '';

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

  // 수정 모드
 /* function openplaceEditModal(data) {
    document.getElementById('placeDate').value = data.date;
    document.getElementById('placeNum').value = data.placeNum;
    document.getElementById('customerId').value = data.customer_id;
    document.getElementById('itemCode').value = data.item_code;
    document.getElementById('itemId').value = data.item_id;
    document.getElementById('quantity').value = data.quantity;
    document.getElementById('unit').value = data.unit;
    document.getElementById('warehouseId').value = data.warehouse_id;
    document.getElementById('employeeid').value = data.manager;
    document.getElementById('employeeEmail').value = data.manager_email;
    document.getElementById('employeeHp').value = data.manager_num;
    document.getElementById('note').value = data.note;

    document.getElementById('placeForm').action = `/place/update/${data.id}`;
    document.getElementById('submitBtn').textContent = '수정';

    const modal = new bootstrap.Modal(document.getElementById('placeRegisterModal'));
    modal.show();
  } */
  
  async function openplaceEditModal(data) {
    const form = document.getElementById('placeForm');

    form.querySelector('input[name="orderDate"]').value = data.date;
    document.getElementById('orderType').value = data.companyType;
    filterCompanies();
    document.getElementById('companySelect').value = data.companyId;

    document.getElementById('employeeSelect').value = data.employeeId;
    document.getElementById('empEmail').value = data.managerEmail || '';
    document.getElementById('empHp').value = data.managerNum || '';

    form.querySelector('textarea[name="note"]').value = data.note || '';

    const container = document.getElementById('itemListContainer');
    container.innerHTML = '';

    for (let item of data.lineItems) {
      addItemRow();
    }
    const rows = container.querySelectorAll('.item-row');

    for (let i = 0; i < data.lineItems.length; i++) {
      const item = data.lineItems[i];
      const row = rows[i];

      const itemTypeSelect = row.querySelector('.item-type');
      itemTypeSelect.value = item.itemType;
      await loadItems(itemTypeSelect);

      const itemNameSelect = row.querySelector('.item-name');
      itemNameSelect.value = item.itemId;

      row.querySelector('.item-code').value = item.itemCode;
      row.querySelector('.item-unit').value = item.unit;
      row.querySelector('.unit-price').value = item.unitPrice;
      row.querySelector('.unit-qty').value = item.unitQty;

      calculateItemTotal(row);
    }

    calculateTotals();

    form.action = `/place/update/${data.id}`;
    document.getElementById('submitBtn').textContent = '수정';

    const modal = new bootstrap.Modal(document.getElementById('placeRegisterModal'));
    modal.show();
  }



  //발주 등록 초기화 - 사용자에게 확인 후 등록 입력 폼을 초기화
  function placeReset(){
	if (confirm("입력한 정보를 초기화하시겠습니까?")) {
	       document.querySelector('#placeForm').reset();
	       alert("입력한 정보가 초기화되었습니다.");
		   calculateTotals(); // 리셋 후 총합도 초기화
	   } else {
	       alert("초기화가 취소되었습니다.");
	   }
  }
  
  //발주 등록 - 필수 항목이 비어있지 않은지 확인 후 제출
  // 발주 저장 버튼 클릭 시
  function placeSubmit() {
    if (confirm("입력한 정보를 저장하시겠습니까?")) {
      prepareFormBeforeSubmit();
      console.log("폼 제출 직전:", document.getElementById('placeForm'));
      document.getElementById('placeForm').submit();
    } else {
      alert("저장이 취소되었습니다.");
    }
  }


	  // 폼 제출 전 name 속성 동적 세팅
	  function prepareFormBeforeSubmit() {
	    document.querySelectorAll('.item-row').forEach((row, idx) => {
	      row.querySelector('.item-type').setAttribute('name', `lineItems[${idx}].itemType`);
	      row.querySelector('.item-name').setAttribute('name', `lineItems[${idx}].itemId`);
	      row.querySelector('.item-code').setAttribute('name', `lineItems[${idx}].itemCode`);
	      row.querySelector('.item-unit').setAttribute('name', `lineItems[${idx}].unit`);
	      row.querySelector('.unit-price').setAttribute('name', `lineItems[${idx}].unitPrice`);
	      row.querySelector('.unit-qty').setAttribute('name', `lineItems[${idx}].unitQty`);

	      // 쉼표 제거한 숫자만 전송되도록 처리
	      const priceAllInput = row.querySelector('.unit-price-all');
	      const rawValue = priceAllInput.value.replace(/,/g, '');
	      priceAllInput.value = rawValue;

	      priceAllInput.setAttribute('name', `lineItems[${idx}].unitPriceAll`);
	    });
	  }

		
	//품목 선택 
	//품목 리스트 행 하나씩 추가 - 기존 행의 input, select 값 초기화
	// 품목 행 추가
	  function addItemRow() {
	    const container = document.getElementById('itemListContainer');
	    const newRow = container.querySelector('.item-row').cloneNode(true);

	    newRow.querySelectorAll('input, select').forEach(el => el.value = '');
	    container.appendChild(newRow);
	  }

	  // 품목 행 삭제
	  function removeItemRow(btn) {
	    const row = btn.closest('tr');
	    const container = document.getElementById('itemListContainer');

	    if (container.querySelectorAll('.item-row').length > 1) {
	      row.remove();
	      calculateTotals();
	    } else {
	      alert('최소 하나의 품목은 필요합니다.');
	    }
	  }
	
	//사업장 명 불러오기
	//선택된 사업장 유형(supplier, customer)에 따라 select 옵션 필터링.
	//option에 있는 data-type 속성을 기반으로 필터링함
	function filterCompanies() {
	     const type = document.getElementById("orderType").value;
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

	   //select요소로 employeeSelect에서 select요소로 선택된 담당자의 Id를 가져와서
	   // /place/employee{employee}라는 경로로 Get 요청을 보냄
	   // 서버에서는 해당 ID에 해당하는 담당자의 상세 정보를 JSON 형태로 응답
	   // 받은 응답을 기반으로 empEmail, empHp, empName을 채움
	   /*function fillEmployeeDetails() {
	     const employeeId = document.getElementById("employeeSelect").value;
	     const employeeSelect = document.getElementById("employeeSelect");

	     const selectedOption = employeeSelect.selectedOptions[0];
	     const empName = selectedOption ? selectedOption.textContent : "";

	     if (!employeeId) {
	       document.getElementById("empName").value = "";
	       document.getElementById("empEmail").value = "";
	       document.getElementById("empHp").value = "";
	       return;
	     }

	     fetch(`/place/employee/${employeeId}`)
	       .then(response => {
	         if (!response.ok) {
	           throw new Error("서버 응답 실패");
	         }
	         return response.json();
	       })
	       .then(data => {
	         document.getElementById("empName").value = empName;
	         document.getElementById("empEmail").value = data.empEmail || "";
	         document.getElementById("empHp").value = data.empHp || "";
	       })
	       .catch(error => {
	         console.error("담당자 정보 로딩 실패:", error);
	         alert("담당자 정보를 불러오는 데 실패했습니다.");
	       });
	   }*/

	   //사업장 선택시 자동으로 담당자 정보 채우기
	   //company_name이라는 Id를 가진 select 요소에서 선택한 회사 Id를 기준으로, /place/employees?companyId=xx로 Ajax 요청보냄
	   //서버에서는 해당 회사의 담당자를 반환함. 그 중 data.name값을 employeeName input에 채워 넣음
	  
	    document.getElementById('companySelect').addEventListener('change', function () {
	     const companyId = this.value;

	     if (companyId) {
	       fetch(`/place/employees/byCompany?companyId=${companyId}`)
	         .then(response => response.json())
	         .then(data => {
	           if (data) {
	             document.getElementById('empName').value = data.empName || '';
	             document.getElementById('empEmail').value = data.empEmail || '';
	             document.getElementById('empHp').value = data.empHp || '';
	           } else {
	             // 데이터 없을 경우 초기화
	             document.getElementById('empName').value = '';
	             document.getElementById('empEmail').value = '';
	             document.getElementById('empHp').value = '';
	           }
	         })
	         .catch(error => {
	           console.error('담당자 정보를 불러오는 중 오류 발생:', error);
	           document.getElementById('empName').value = '';
	           document.getElementById('empEmail').value = '';
	           document.getElementById('empHp').value = '';
	         });
	     } else {
	       document.getElementById('empName').value = '';
	       document.getElementById('empEmail').value = '';
	       document.getElementById('empHp').value = '';
	     }
	   });



	   //품목 관련 처리
	   //품목명 동적 로딩(Ajax)
	   //품목 유형(자재, 완제품)을 선택하면 ajax로 해당 품목 목록을 받아와 select에 반영
	   //서버로 /api~~ 요청
	   // 품목 유형 선택 시 해당 품목명 목록 불러오기 (Ajax)
	    /* function loadItems(select) {
	       const itemType = select.value;
	       const row = select.closest('tr');
	       const itemSelect = row.querySelector('.item-name');

	       itemSelect.innerHTML = '<option value="">-- 품목 선택 --</option>';
	       if (!itemType) return;

	       fetch(`/place/items?type=${itemType}`)
	         .then(res => res.json())
	         .then(items => {
	           items.forEach(item => {
	             const option = document.createElement('option');
	             option.value = item.id;
	             option.textContent = item.name;
	             option.dataset.code = item.code;
	             option.dataset.unit = item.unit;
	             option.dataset.price = item.itemPrice;
	             itemSelect.appendChild(option);
	           });
	         });
	     } */
		 
		 function loadItems(select) {
		   return new Promise((resolve, reject) => {
		     const itemType = select.value;
		     const row = select.closest('tr');
		     const itemSelect = row.querySelector('.item-name');

		     itemSelect.innerHTML = '<option value="">-- 품목 선택 --</option>';
		     if (!itemType) {
		       resolve();
		       return;
		     }

		     fetch(`/place/items?type=${itemType}`)
		       .then(res => res.json())
		       .then(items => {
		         items.forEach(item => {
		           const option = document.createElement('option');
		           option.value = item.id;
		           option.textContent = item.name;
		           option.dataset.code = item.code;
		           option.dataset.unit = item.unit;
		           option.dataset.price = item.itemPrice;
		           itemSelect.appendChild(option);
		         });
		         resolve();
		       })
		       .catch(err => reject(err));
		   });
		 }

		 
	
	 //단위 설정이 누락되지 않도록 보완
	 // 품목명 선택 시 코드, 단위, 단가 자동 입력
	   function fillItemDetails(select) {
	     const row = select.closest('tr');
	     const option = select.selectedOptions[0];

	     row.querySelector('.item-code').value = option.dataset.code || '';
	     row.querySelector('.item-unit').value = option.dataset.unit || '';
	     row.querySelector('.unit-price').value = option.dataset.price || '';

	     row.querySelector('.unit-qty').value = 1;

	     calculateItemTotal(row);
	     calculateTotals();
	   }

	//수량 입력시 단가 합산
	//단가(unitPrice)와 수량(unitQty)으로 총액(unitPriceAll)계산
	//총합 업데이트 함수도 호출(calculateTotals())
		function calculateItemTotal(row) {
	    const qty = parseFloat(row.querySelector('.unit-qty').value) || 0;
	    const price = parseFloat(row.querySelector('.unit-price').value) || 0;
	    const total = qty * price;
		row.querySelector('.unit-price-all').value = Math.round(total).toLocaleString();
	  }

	  // 총합 계산
	  function calculateTotals() {
	    let orderQty = 0, totalAmount = 0;

	    document.querySelectorAll('.item-row').forEach(row => {
	      const qty = parseFloat(row.querySelector('.unit-qty').value) || 0;
		  const amt = parseFloat(row.querySelector('.unit-price-all').value.replace(/,/g, '')) || 0;
		   orderQty += qty;
	      totalAmount += amt;
	    });

	    document.getElementById('orderQty').textContent = orderQty;
	    document.getElementById('amount').textContent = totalAmount.toLocaleString();
	  }
	  
	  // 수량 또는 단가 입력 시 계산 실행
	  document.addEventListener('input', function (e) {
	    if (e.target.classList.contains('unit-qty') || e.target.classList.contains('unit-price')) {
	      const row = e.target.closest('.item-row');
	      calculateItemTotal(row);   // ✅ 한 줄 계산 함수 호출
	      calculateTotals();         // ✅ 총합 계산 함수 호출
	    }
	  });