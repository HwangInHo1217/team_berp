// 검색 함수
function searchOrders() {
    const keyword = document.getElementById('searchKeyword').value.trim();
    
    console.log("✅ 검색 실행 - 키워드:", keyword);
    
    // 검색어가 있으면 검색 URL로, 없으면 기본 URL로 이동
    if (keyword) {
        window.location.href = `/place/search?keyword=${encodeURIComponent(keyword)}`;
    } else {
        // 검색어가 없으면 전체 목록 보기
        window.location.href = '/place';
    }
}

// 검색 초기화 함수 (선택사항)
function clearSearch() {
    document.getElementById('searchKeyword').value = '';
    window.location.href = '/place';
}

//입고 페이지로 이동
function goToInboundPage() {
    window.location.href = '/receive';
}

// ✅ 등록 모달을 열고 초기화
function openplaceAddModal() {
    const form = document.getElementById('placeForm');
    form.reset(); // 폼 초기화
    
    // orderId 초기화 (등록용)
    document.querySelector('#order_id').value = '';
    
    // 모달 타이틀 변경
    document.querySelector('#placeRegisterModal .modal-title').textContent = '발주 등록';
    
    filterCompanies(); // 사업장 유형에 따른 필터링
    resetItemRows(); // 품목 행 초기화
    calculateTotals(); // 합계 초기화
}

// 첫 번째 품목 행을 제외한 나머지 행을 초기화
function resetItemRows() {
    const container = document.getElementById('itemListContainer');
    const firstRow = container.querySelector('.item-row');

    container.innerHTML = ''; // 전체 초기화
    container.appendChild(firstRow.cloneNode(true)); // 첫 행 추가

    // 복사된 행의 입력 값 초기화
    container.querySelectorAll('input, select').forEach(el => el.value = '');
}

// ✅ 발주 등록 정보 전체 초기화
function placeReset(){
    if (confirm("입력한 정보를 초기화하시겠습니까?")) {
        document.querySelector('#placeForm').reset(); // 폼 초기화
        document.querySelector('#order_id').value = ''; // orderId도 초기화
        resetItemRows(); // 품목 행 초기화
        calculateTotals(); // 합계 재계산
        alert("입력한 정보가 초기화되었습니다.");
    } else {
        alert("초기화가 취소되었습니다.");
    }
}

// ✅ 발주 등록/수정 함수 (AJAX 방식)
// ✅ 발주 등록/수정 함수 (AJAX 방식) - 수정됨
function placeSubmit() {
    // 폼 기본 제출 이벤트 차단
    event.preventDefault();
    
    if (confirm("입력한 정보를 저장하시겠습니까?")) {
        const formData = collectFormData(); // 폼 데이터 수집
        const orderId = document.querySelector('#order_id').value;
        
        // URL과 메시지 결정
        const url = orderId ? '/place/update' : '/place/add';
        const successMessage = orderId ? '수정되었습니다.' : '등록되었습니다.';
        
        console.log('전송할 데이터:', formData);
        console.log('전송 URL:', url);
        
        // AJAX 전송
        fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=UTF-8'
            },
            body: JSON.stringify(formData)
        })
        .then(res => {
            console.log('응답 상태:', res.status);
            
            // ✅ 응답 타입 확인
            const contentType = res.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return res.json(); // JSON으로 파싱
            } else {
                return res.text(); // 텍스트로 파싱
            }
        })
        .then(result => {
            console.log('성공 응답:', result);
            
            // ✅ 응답 타입에 따라 처리
            if (typeof result === 'object' && result.success) {
                // JSON 응답 처리
                alert(result.message);
            } else if (typeof result === 'string') {
                // 텍스트 응답 처리
                alert(successMessage);
            } else if (typeof result === 'object' && !result.success) {
                // 에러 JSON 응답 처리
                alert(result.message);
                return; // 모달을 닫지 않음
            } else {
                // 기본 처리
                alert(successMessage);
            }
            
            // 모달 닫기
            const modal = bootstrap.Modal.getInstance(document.getElementById('placeRegisterModal'));
            if (modal) {
                modal.hide();
            }
            
            // 목록 새로고침
            loadPlaceList();
        })
        .catch(err => {
            console.error('저장 오류:', err);
            alert('저장 중 오류가 발생했습니다: ' + err.message);
        });
    } else {
        alert("저장이 취소되었습니다.");
    }
}

// ✅ 폼 데이터를 JSON 형태로 수집하는 함수
// ✅ 폼 데이터를 JSON 형태로 수집하는 함수 (수정)
function collectFormData() {
    const lineItems = [];
    
    // 품목 행 데이터 수집
    document.querySelectorAll('.item-row').forEach((row, index) => {
        const itemTypeSelect = row.querySelector('.item-type');
        const itemNameSelect = row.querySelector('.item-name');
        const itemCodeInput = row.querySelector('.item-code');
        const unitQtyInput = row.querySelector('.unit-qty');
        const itemUnitInput = row.querySelector('.item-unit');
        const unitPriceInput = row.querySelector('.unit-price');
        const unitPriceAllInput = row.querySelector('.unit-price-all');
        const orderLineItemIdInput = row.querySelector('.order-line-item-id');
        
        // 값이 있는 행만 추가
        if (itemTypeSelect && itemTypeSelect.value && itemNameSelect && itemNameSelect.value) {
            const lineItem = {
                itemType: itemTypeSelect.value,
                itemId: itemNameSelect.value,
                itemCode: itemCodeInput ? itemCodeInput.value : '',
                unitQty: unitQtyInput ? parseInt(unitQtyInput.value) || 1 : 1,
                unit: itemUnitInput ? itemUnitInput.value : '',
                unitPrice: unitPriceInput ? parseFloat(unitPriceInput.value) || 0 : 0,
                unitPriceAll: unitPriceAllInput ? parseFloat(unitPriceAllInput.value.replace(/,/g, '')) || 0 : 0
            };
            
            // ✅ orderLineItemId가 있으면 추가 (수정 시에만)
            if (orderLineItemIdInput && orderLineItemIdInput.value) {
                lineItem.orderLineItemId = parseInt(orderLineItemIdInput.value);
            }
            
            lineItems.push(lineItem);
            console.log('수집된 품목:', lineItem); // 디버깅용
        }
    });
    
    const formData = {
        orderId: document.querySelector('#order_id').value || null,
        orderDate: document.querySelector('[name="orderDate"]').value,
        orderType: document.querySelector('[name="orderType"]').value,
        companyId: document.querySelector('[name="companyId"]').value,
        empName: document.querySelector('[name="empName"]').value,
        empEmail: document.querySelector('[name="empEmail"]').value,
        empHp: document.querySelector('[name="empHp"]').value,
        note: document.querySelector('[name="note"]').value || '',
        lineItems: lineItems
    };
    
    console.log('최종 전송 데이터:', formData); // 디버깅용
    return formData;
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

// ✅ 이벤트 위임 - 수량 변경 시 계산 (class와 name 속성 모두 지원)
document.getElementById('itemListContainer').addEventListener('input', function (e) {
    // class나 name 속성으로 수량 입력 필드 감지
    if (e.target.classList.contains('unit-qty') || 
        (e.target.name && e.target.name.includes('unitQty'))) {
        const row = e.target.closest('tr');
        calculateItemTotal(row);
        calculateTotals();
    }
});

// ✅ 개별 품목 합계 계산 함수 (class와 name 속성 모두 지원)
function calculateItemTotal(row) {
    // class 기반으로 찾기 시도
    let qtyInput = row.querySelector('.unit-qty');
    let priceInput = row.querySelector('.unit-price');
    let priceAllInput = row.querySelector('.unit-price-all');
    
    // class로 못 찾으면 name 속성으로 찾기
    if (!qtyInput) {
        qtyInput = row.querySelector('input[name*="unitQty"]');
    }
    if (!priceInput) {
        priceInput = row.querySelector('input[name*="unitPrice"]:not([name*="unitPriceAll"])');
    }
    if (!priceAllInput) {
        priceAllInput = row.querySelector('input[name*="unitPriceAll"]');
    }

    const qty = parseInt(qtyInput?.value) || 0;
    const price = parseInt(priceInput?.value) || 0;
    
    if (priceAllInput) {
        priceAllInput.value = qty * price;
    }
}

// ✅ 전체 합계 계산 함수 (class와 name 속성 모두 지원)
function calculateTotals() {
    const rows = document.querySelectorAll('#itemListContainer tr.item-row');
    let totalQty = 0;
    let totalAmount = 0;

    rows.forEach(row => {
        // class 기반으로 찾기 시도
        let qtyInput = row.querySelector('.unit-qty');
        let priceAllInput = row.querySelector('.unit-price-all');
        
        // class로 못 찾으면 name 속성으로 찾기
        if (!qtyInput) {
            qtyInput = row.querySelector('input[name*="unitQty"]');
        }
        if (!priceAllInput) {
            priceAllInput = row.querySelector('input[name*="unitPriceAll"]');
        }
        
        const qty = parseInt(qtyInput?.value) || 0;
        const priceAll = parseInt(priceAllInput?.value) || 0;
        totalQty += qty;
        totalAmount += priceAll;
    });

    document.getElementById("orderQty").textContent = totalQty;
    document.getElementById("amount").textContent = totalAmount.toLocaleString();
}

// ✅ 발주 수정 모달 열기
function openplaceEditModal() {
    const checkedItems = document.querySelectorAll('input[name="lineItemIds"]:checked');

    if (checkedItems.length === 0) {
        alert("수정할 품목을 선택하세요.");
        return;
    }

    if (checkedItems.length > 1) {
        alert("하나의 품목만 선택하세요.");
        return;
    }

    const lineItemId = checkedItems[0].value;

    // 서버에서 수정할 발주 데이터 요청
    fetch(`/place/edit/${lineItemId}`)
        .then(res => {
            if (!res.ok) throw new Error('서버 통신 오류');
            return res.json();
        })
        .then(data => {
            console.log("불러온 데이터: ", data);
            
            // ✅ orderId 설정 (수정 시 가장 중요!)
            document.querySelector('#order_id').value = data.orderId || '';
            
            // 발주일자
            document.querySelector('#placeForm [name="orderDate"]').value = data.orderDate;

            // 사업장 유형 및 이름
            document.querySelector('#placeForm [name="orderType"]').value = data.orderType;
            filterCompanies(); // 사업장 필터링 적용
            document.querySelector('#placeForm [name="companyId"]').value = data.companyId;

            // 담당자 정보
            document.querySelector('#placeForm [name="empName"]').value = data.employeeName || '';
            document.querySelector('#placeForm [name="empEmail"]').value = data.employeeEmail || '';
            document.querySelector('#placeForm [name="empHp"]').value = data.employeeTel || '';

            // 비고
            document.querySelector('#placeForm [name="note"]').value = data.note || '';

            // 품목 리스트 반복 처리
            const itemListContainer = document.getElementById("itemListContainer");
            itemListContainer.innerHTML = "";
            
            data.lineItems.forEach((item, index) => {
                const row = document.createElement("tr");
                row.className = "item-row";
                row.innerHTML = `
                    <td>
                        <select name="lineItems[${index}].itemType" class="form-select item-type">
                            <option value="자재" ${item.itemType === '자재' ? 'selected' : ''}>자재</option>
                            <option value="완제품" ${item.itemType === '완제품' ? 'selected' : ''}>완제품</option>
                        </select>
                    </td>
                    <td>
                        <select name="lineItems[${index}].itemId" class="form-select item-name">
                            <option value="${item.itemId}" selected>${item.itemName}</option>
                        </select>
                    </td>
                    <td><input type="text" class="form-control item-code" name="lineItems[${index}].itemCode" value="${item.itemCode}" readonly /></td>
                    <td><input type="number" class="form-control unit-qty" name="lineItems[${index}].unitQty" value="${item.unitQty}" /></td>
                    <td><input type="text" class="form-control item-unit" name="lineItems[${index}].unit" value="${item.unit}" readonly /></td>
                    <td><input type="text" class="form-control unit-price" name="lineItems[${index}].unitPrice" value="${item.unitPrice}" readonly /></td>
                    <td><input type="text" class="form-control unit-price-all" name="lineItems[${index}].unitPriceAll" value="${item.unitPriceAll}" readonly /></td>
                    <input type="hidden" name="lineItems[${index}].orderLineItemId" class="order-line-item-id" value="${item.orderLineItemId || ''}" />
                    <td><button type="button" class="btn btn-danger btn-sm" onclick="removeItemRow(this)">삭제</button></td>
                `;
                itemListContainer.appendChild(row);
                
                // 각 행 생성 후 즉시 합계 계산
                calculateItemTotal(row);
            });

            // 전체 합계 계산
            calculateTotals();
            
            // 모달 타이틀 변경
            document.querySelector('#placeRegisterModal .modal-title').textContent = '발주 수정';
            
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
/*function openplaceDetailModal(data) {
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
}*/

//상세 모달 오픈 - 발주 상세정보를 표시하는 모달을 여는 함수
//상세 모달 오픈 - 발주 상세정보를 표시하는 모달을 여는 함수
//상세 모달 오픈 - 발주 상세정보를 표시하는 모달을 여는 함수
function openplaceDetailModal() {
    const checkedItems = document.querySelectorAll('input[name="lineItemIds"]:checked');

    if (checkedItems.length === 0) {
        alert("상세 정보를 볼 품목을 선택하세요.");
        return;
    }

    if (checkedItems.length > 1) {
        alert("하나의 품목만 선택하세요.");
        return;
    }

    const lineItemId = checkedItems[0].value;
    console.log("✅ 상세 조회 요청 - lineItemId:", lineItemId);

    // 서버에서 상세 데이터 요청
    fetch(`/place/detail/${lineItemId}`)
        .then(res => {
            console.log("✅ 응답 상태:", res.status);
            if (!res.ok) throw new Error('서버 통신 오류: ' + res.status);
            return res.json();
        })
        .then(data => {
            console.log("✅ 받은 상세 데이터:", data);
            console.log("✅ companyName 값:", data.companyName);
            console.log("✅ orderType 값:", data.orderType);
            
            // ✅ 기본 정보
            document.getElementById('placeDetailDate').textContent = data.orderDate || '-';
            
            // 주문번호 (HTML 수정 필요시)
            const orderNumElement = document.getElementById('placeDetailOrderNum');
            if (orderNumElement) {
                orderNumElement.textContent = data.orderNum || '-';
            } else {
                // id가 없다면 3번째 li의 span을 찾아서 설정
                const orderNumSpan = document.querySelector('#placeDetailModal .list-group-item:nth-child(3) span');
                if (orderNumSpan) {
                    orderNumSpan.textContent = data.orderNum || '-';
                }
            }
            
            // ✅ 고객사/거래처명
            let companyText = '-';
            
            if (data.companyName && data.companyName.trim() !== '') {
                companyText = data.companyName;
                if (data.orderType === 'SUPPLIER') {
                    companyText += ' (거래처)';
                } else if (data.orderType === 'CUSTOMER') {
                    companyText += ' (고객사)';
                }
            } else {
                console.warn("⚠️ companyName이 비어있거나 없습니다");
                companyText = '회사명 없음';
            }
            
            console.log("✅ 설정할 companyText:", companyText);
            document.getElementById('placeDetailCustomer').textContent = companyText;
            
            // ✅ 품목 정보 (첫 번째 품목)
            if (data.lineItems && data.lineItems.length > 0) {
                const firstItem = data.lineItems[0];
                console.log("✅ 첫 번째 품목:", firstItem);
                
                document.getElementById('placeDetailItemCode').textContent = firstItem.itemCode || '-';
                document.getElementById('placeDetailItemName').textContent = firstItem.itemName || '-';
                document.getElementById('placeDetailQty').textContent = firstItem.unitQty || 0;
                document.getElementById('placeDetailUnit').textContent = firstItem.unit || '-';
            } else {
                console.warn("⚠️ 품목 정보가 없습니다");
                document.getElementById('placeDetailItemCode').textContent = '-';
                document.getElementById('placeDetailItemName').textContent = '-';
                document.getElementById('placeDetailQty').textContent = '-';
                document.getElementById('placeDetailUnit').textContent = '-';
            }
            
            // ✅ 담당자 및 비고
            document.getElementById('placeDetailManager').textContent = data.employeeName || '-';
            document.getElementById('placeDetailNote').textContent = data.note || '-';

            // 모달 열기
            const modalEl = document.getElementById('placeDetailModal');
            const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
            modal.show();
        })
        .catch(err => {
            console.error("❌ 상세 정보 불러오기 오류:", err);
            alert("상세 정보를 불러오는 데 실패했습니다: " + err.message);
        });
}

// ✅ 모달이 닫힐 때 기본 상태로 초기화
const modalEl = document.getElementById('placeRegisterModal');
if (modalEl) {
    modalEl.addEventListener('hidden.bs.modal', () => {
        document.getElementById('placeForm').reset();
        document.querySelector('#order_id').value = '';
        document.querySelector('#placeRegisterModal .modal-title').textContent = '발주 등록';
    });
}

// ✅ 엔터키로 폼 제출 방지
document.addEventListener('DOMContentLoaded', function() {
    const placeForm = document.getElementById('placeForm');
    if (placeForm) {
        placeForm.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && e.target.tagName !== 'TEXTAREA') {
                e.preventDefault();
                return false;
            }
        });
    }
});

// ✅ DOM이 로드된 후 이벤트 리스너 등록
document.addEventListener('DOMContentLoaded', function() {
    // 상세 버튼 이벤트
    const detailBtn = document.getElementById('detailBtn');
    if (detailBtn) {
        detailBtn.addEventListener('click', function() {
            // 선택된 항목이 있는지 확인하고 상세 모달 열기
            const checkedItems = document.querySelectorAll('input[name="lineItemIds"]:checked');
            if (checkedItems.length === 0) {
                alert("상세 정보를 볼 품목을 선택하세요.");
                return;
            }
            if (checkedItems.length > 1) {
                alert("하나의 품목만 선택하세요.");
                return;
            }
            
            // 상세 모달 열기 (구현 필요시)
            openplaceDetailModal();
        });
    }

    // 수정 버튼 이벤트
    const editBtn = document.getElementById('editBtn');
    if (editBtn) {
        editBtn.addEventListener('click', function() {
            openplaceEditModal();
        });
    }

    // 등록 버튼 이벤트
    const addBtn = document.getElementById('addBtn');
    if (addBtn) {
        addBtn.addEventListener('click', function() {
            openplaceAddModal();
            // 모달 열기
            const modal = new bootstrap.Modal(document.getElementById('placeRegisterModal'));
            modal.show();
        });
    }

    // 엔터키로 폼 제출 방지
    const placeForm = document.getElementById('placeForm');
    if (placeForm) {
        placeForm.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && e.target.tagName !== 'TEXTAREA') {
                e.preventDefault();
                return false;
            }
        });
    }

    // 전체 선택 체크박스 기능
    const checkAll = document.getElementById("checkAll");
    if (checkAll) {
        checkAll.addEventListener("change", function () {
            const checkboxes = document.querySelectorAll('input[name="lineItemIds"]');
            checkboxes.forEach(cb => {
                cb.checked = checkAll.checked;
            });
        });
    }
});