// 등록 모달을 열고 초기화
function openplaceAddModal() {
	const form = document.getElementById('placeForm');
	form.reset(); // 폼 초기화
	filterCompanies(); // 사업장 유형에 따른 필터링
	resetItemRows(); // 품목 행 초기화
	form.action = '/place/add'; // 폼 제출 경로 설정
}

// 첫 번째 품목 행을 제외한 나머지 행을 초기화
function resetItemRows() {
	const container = document.getElementById('itemListContainer');
	const firstRow = container.querySelector('.item-row');
	const newRow = firstRow.cloneNode(true); // 행 복사

	container.innerHTML = ''; // 전체 초기화
	container.appendChild(firstRow.cloneNode(true)); // 첫 행 추가
	newRow.classList.add('item-row');

	// 복사된 행의 입력 값 초기화
	container.querySelectorAll('input, select').forEach(el => el.value = '');
}

// 발주 등록 정보 전체 초기화
function placeReset(){
	if (confirm("입력한 정보를 초기화하시겠습니까?")) {
		document.querySelector('#placeForm').reset(); // 폼 초기화
		alert("입력한 정보가 초기화되었습니다.");
		calculateTotals(); // 합계 재계산
	} else {
		alert("초기화가 취소되었습니다.");
	}
}

// 발주 등록(폼 제출)
function placeSubmit() {
	if (confirm("입력한 정보를 저장하시겠습니까?")) {
		prepareFormBeforeSubmit(); // name 속성 지정
		console.log("폼 제출 직전:", document.getElementById('placeForm'));
		document.getElementById('placeForm').submit(); // 폼 제출
	} else {
		alert("저장이 취소되었습니다.");
	}
}

// 각 품목 행의 name 속성 동적으로 설정
function prepareFormBeforeSubmit() {
	document.querySelectorAll('.item-row').forEach((row, idx) => {
		row.querySelector('.item-type').setAttribute('name', `lineItems[${idx}].itemType`);
		row.querySelector('.item-name').setAttribute('name', `lineItems[${idx}].itemId`);
		row.querySelector('.item-code').setAttribute('name', `lineItems[${idx}].itemCode`);
		row.querySelector('.item-unit').setAttribute('name', `lineItems[${idx}].unit`);
		row.querySelector('.unit-price').setAttribute('name', `lineItems[${idx}].unitPrice`);
		row.querySelector('.unit-qty').setAttribute('name', `lineItems[${idx}].unitQty`);

		// 총금액에서 콤마 제거 후 name 지정
		const priceAllInput = row.querySelector('.unit-price-all');
		const rawValue = priceAllInput.value.replace(/,/g, '');
		priceAllInput.value = rawValue;
		priceAllInput.setAttribute('name', `lineItems[${idx}].unitPriceAll`);
	});
}

// 품목 행 추가
function addItemRow() {
	const container = document.getElementById('itemListContainer');
	const newRow = container.querySelector('.item-row').cloneNode(true);

	// 새 행의 입력값 초기화
	newRow.querySelectorAll('input, select').forEach(el => el.value = '');
	newRow.classList.add('item-row');
	container.appendChild(newRow); // 행 추가
}

// 품목 행 삭제
function removeItemRow(btn) {
	const row = btn.closest('tr');
	const container = document.getElementById('itemListContainer');

	if (container.querySelectorAll('.item-row').length > 1) {
		row.remove(); // 행 제거
		calculateTotals(); // 총합계 다시 계산
	} else {
		alert('최소 하나의 품목은 필요합니다.');
	}
}

// 사업장 유형 선택에 따라 해당 사업장명 옵션만 표시
function filterCompanies() {
	const type = document.getElementById("orderType").value;
	const select = document.getElementById("companySelect");

	const options = select.querySelectorAll("option");

	options.forEach(opt => {
		const optType = opt.dataset.type;
		if (!optType) return;
		opt.hidden = optType !== type; // 선택한 유형 외에는 숨김
	});

	select.value = ""; // 초기화
}

// 사업장명 선택 시 담당자 정보 자동 채우기
document.getElementById('companySelect').addEventListener('change', function () {
	const companyId = this.value;

	if (companyId) {
		fetch(`/place/employees/byCompany?companyId=${companyId}`)
		.then(response => response.json())
		.then(data => {
			// 데이터가 있으면 입력 필드 채우기
			if (data) {
				document.getElementById('empName').value = data.empName || '';
				document.getElementById('empEmail').value = data.empEmail || '';
				document.getElementById('empHp').value = data.empHp || '';
			} else {
				// 없으면 초기화
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
		// 선택하지 않은 경우 초기화
		document.getElementById('empName').value = '';
		document.getElementById('empEmail').value = '';
		document.getElementById('empHp').value = '';
	}
});

// item_type 선택 시 해당 타입의 품목 리스트를 불러와 item_name에 채움
function loadItems(select) {
	return new Promise((resolve, reject) => {
		const itemType = select.value;
		const row = select.closest('tr');
		const itemSelect = row.querySelector('.item-name');

		itemSelect.innerHTML = '<option value="">-- 품목 선택 --</option>';

		if (!itemType) {
			resolve(); // 선택 안 된 경우 종료
			return;
		}

		// 서버에서 해당 itemType의 품목 목록 가져오기
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
				itemSelect.appendChild(option); // 옵션 추가
			});
			resolve();
		})
		.catch(err => reject(err));
	});
}

// 품목명 선택 시 코드, 단위, 단가 입력 필드 자동 입력
function fillItemDetails(select) {
	const row = select.closest('tr');
	const option = select.selectedOptions[0];

	row.querySelector('.item-code').value = option.dataset.code || '';
	row.querySelector('.item-unit').value = option.dataset.unit || '';
	row.querySelector('.unit-price').value = option.dataset.price || '';
	row.querySelector('.unit-qty').value = 1;

	calculateItemTotal(row); // 행 합계
	calculateTotals(); // 전체 합계
}

// 단가 * 수량 = 금액 계산
function calculateItemTotal(row) {
	const qty = parseFloat(row.querySelector('.unit-qty').value) || 0;
	const price = parseFloat(row.querySelector('.unit-price').value) || 0;
	const total = qty * price;
	row.querySelector('.unit-price-all').value = Math.round(total).toLocaleString(); // 콤마 포함 출력
}

// 전체 품목 수량 및 금액 합계 계산
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

// 수량이나 단가 입력 시 실시간으로 합계 계산
document.addEventListener('input', function (e) {
	if (e.target.classList.contains('unit-qty') || e.target.classList.contains('unit-price')) {
		const row = e.target.closest('.item-row');
		calculateItemTotal(row);
		calculateTotals();
	}
});

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
      
function openplaceEditModal() {
	const checked = document.querySelector('input[name="lineItemIds"]:checked');
        if (!checked) {
         	 alert("수정할 품목을 선택하세요.");
          	return;
        }

        const lineItemId = checked.value;

// 서버에서 수정할 발주 데이터 요청 (예: DTO를 JSON 형태로 반환)
	fetch(`/place/edit/${lineItemId}`)
       .then(res => {
       if (!res.ok) throw new Error('서버 통신 오류');
			return res.json();
        })
        .then(data => {
        	console.log("불러온 데이터: ", data);
            // ✅ 발주일자
            document.querySelector('#placeForm [name="orderDate"]').value = data.orderDate;

            // ✅ 사업장 유형 및 이름
            document.querySelector('#placeForm [name="orderType"]').value = data.orderType;
            document.querySelector('#placeForm [name="companyId"]').value = data.companyId;

            // ✅ 담당자 정보
	        document.querySelector('#placeForm [name="empName"]').value = data.employeeName || '';
	        document.querySelector('#placeForm [name="empEmail"]').value = data.employeeEmail || '';
	        document.querySelector('#placeForm [name="empHp"]').value = data.employeeTel || '';

            // ✅ 품목 리스트 반복 처리
        const itemListContainer = document.getElementById("itemListContainer");
            itemListContainer.innerHTML = "";
            data.lineItems.forEach((item, index) => {
              const row = document.createElement("tr");
              row.innerHTML = `
                <td>
                  <select name="lineItems[${index}].itemType" class="form-select">
                    <option value="자재" ${item.itemType === '자재' ? 'selected' : ''}>자재</option>
                    <option value="완제품" ${item.itemType === '완제품' ? 'selected' : ''}>완제품</option>
                  </select>
                </td>
                <td>
                  <select name="lineItems[${index}].itemId" class="form-select">
                    <option value="${item.itemId}" selected>${item.itemName}</option>
                  </select>
                </td>
                <td><input type="text" class="form-control" name="lineItems[${index}].itemCode" value="${item.itemCode}" readonly /></td>
                <td><input type="number" class="form-control" name="lineItems[${index}].unitQty" value="${item.unitQty}" /></td>
                <td><input type="text" class="form-control" name="lineItems[${index}].unit" value="${item.unit}" readonly /></td>
                <td><input type="number" class="form-control" name="lineItems[${index}].unitPrice" value="${item.unitPrice}" readonly /></td>
                <td><input type="number" class="form-control" name="lineItems[${index}].unitPriceAll" value="${item.unitPriceAll}" readonly /></td>
                <td><button type="button" class="btn btn-danger btn-sm" onclick="removeEditRow(this)">삭제</button></td>
              `;
              itemListContainer.appendChild(row);
            });

            document.getElementById("orderQty").textContent = data.orderQty || 0;
            document.getElementById("amount").textContent = data.amount || 0;

            const modal = new bootstrap.Modal(document.getElementById('placeRegisterModal'));
            modal.show();
          })
          .catch(err => {
            console.error("수정 모달 데이터 불러오기 오류:", err);
            alert("수정 정보를 불러오는 데 실패했습니다.");
          });
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
        });
}
