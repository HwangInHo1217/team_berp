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

// ✅ 발주 등록/수정 함수 (AJAX 방식) (자동 새로고침 추가)
function placeSubmit() {
    event.preventDefault();
    
    if (confirm("입력한 정보를 저장하시겠습니까?")) {
        const formData = collectFormData();
        const orderId = document.querySelector('#order_id').value;
        
        const url = orderId ? '/place/update' : '/place/add';
        const successMessage = orderId ? '수정되었습니다.' : '등록되었습니다.';
        
        // 저장 버튼 비활성화 및 로딩 표시
        const saveBtn = document.querySelector('#placeRegisterModal .btn-primary');
        const originalText = saveBtn.textContent;
        saveBtn.disabled = true;
        saveBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>처리중...';
        
        console.log('전송할 데이터:', formData);
        console.log('전송 URL:', url);
        
        fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=UTF-8'
            },
            body: JSON.stringify(formData)
        })
        .then(res => {
            console.log('응답 상태:', res.status);
            
            const contentType = res.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return res.json();
            } else {
                return res.text();
            }
        })
        .then(result => {
            console.log('성공 응답:', result);
            
            if (typeof result === 'object' && result.success) {
                alert(result.message);
            } else if (typeof result === 'string') {
                alert(successMessage);
            } else if (typeof result === 'object' && !result.success) {
                alert(result.message);
                saveBtn.disabled = false;
                saveBtn.textContent = originalText;
                return;
            } else {
                alert(successMessage);
            }
            
            // 모달 닫기
            const modal = bootstrap.Modal.getInstance(document.getElementById('placeRegisterModal'));
            if (modal) {
                modal.hide();
            }
            
            // 페이지 새로고침
            setTimeout(() => {
                window.location.reload();
            }, 500);
        })
        .catch(err => {
            console.error('저장 오류:', err);
            alert('저장 중 오류가 발생했습니다: ' + err.message);
            saveBtn.disabled = false;
            saveBtn.textContent = originalText;
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

// 🚀 스마트 삭제 함수
function smartDeleteItem(button) {
    const lineItemId = button.getAttribute('data-lineItemId');
    const orderId = button.getAttribute('data-orderId');
    const itemCount = parseInt(button.getAttribute('data-itemCount'));
    const itemName = button.getAttribute('data-itemName');
    const orderNum = button.getAttribute('data-orderNum');
    
    let confirmMessage = '';
    let warningMessage = '';
    
    if (itemCount > 1) {
        // 여러 품목 중 하나만 삭제
        confirmMessage = `품목 '${itemName}'을(를) 삭제하시겠습니까?`;
        warningMessage = `이 품목이 발주서 ${orderNum}에서 제거됩니다.\n남은 품목: ${itemCount - 1}개`;
    } else {
        // 마지막 품목 삭제 = 발주서 전체 삭제
        confirmMessage = `마지막 품목입니다.\n발주서 ${orderNum}을(를) 완전히 삭제하시겠습니까?`;
        warningMessage = '발주서와 모든 관련 데이터가 영구적으로 삭제됩니다.';
    }
    
    // 확인 대화상자
    if (!confirm(`${confirmMessage}\n\n⚠️ 주의: ${warningMessage}\n\n계속하시겠습니까?`)) {
        return;
    }
    
    // 버튼 비활성화 (중복 클릭 방지)
    button.disabled = true;
    const originalHTML = button.innerHTML;
    button.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
    
    // 삭제 실행
    fetch(`/place/item/${lineItemId}/delete`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('서버 통신 오류: ' + response.status);
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            alert(data.message);
            // 페이지 새로고침
            window.location.reload();
        } else {
            alert('삭제 실패: ' + data.message);
            // 버튼 복원
            button.disabled = false;
            button.innerHTML = originalHTML;
        }
    })
    .catch(error => {
        console.error('삭제 오류:', error);
        alert('삭제 중 오류가 발생했습니다: ' + error.message);
        // 버튼 복원
        button.disabled = false;
        button.innerHTML = originalHTML;
    });
}

// ✅ 발주 수정 모달 열기
function openPlaceEditModal(lineItemId) {
    console.log("✅ 개별 품목 수정 요청 - lineItemId:", lineItemId);

    // 먼저 해당 발주의 상태를 확인
    fetch(`/place/detail/${lineItemId}`)
        .then(res => {
            if (!res.ok) throw new Error('서버 통신 오류');
            return res.json();
        })
        .then(data => {
            // 수정 가능 상태인지 체크
            if (data.orderStatus !== 'WAITING') {
                const targetItem = data.lineItems.find(item => item.orderLineItemId == lineItemId);
                const itemName = targetItem ? targetItem.itemName : '해당 품목';
                
                alert(`'${itemName}'을(를) 수정할 수 없습니다.\n\n현재 상태: ${getStatusText(data.orderStatus)}\n\n발주 대기 상태에서만 수정이 가능합니다.`);
                return;
            }
            
            // 수정 가능한 상태면 수정 모달 로드
            loadRestrictedEditModalData(lineItemId);
        })
        .catch(err => {
            console.error("상태 확인 오류:", err);
            alert("발주 상태를 확인할 수 없습니다: " + err.message);
        });
}

// 기존 수정 로직을 별도 함수로 분리
function loadRestrictedEditModalData(lineItemId) {
    fetch(`/place/edit/${lineItemId}`)
        .then(res => {
            if (!res.ok) throw new Error('서버 통신 오류');
            return res.json();
        })
        .then(data => {
            console.log("불러온 수정 데이터: ", data);
            
            // orderId 설정 (수정 시 가장 중요!)
            document.querySelector('#order_id').value = data.orderId || '';
            
            // 🔒 발주 기본 정보는 읽기 전용으로 설정
            const orderDateField = document.querySelector('#placeForm [name="orderDate"]');
            const orderTypeField = document.querySelector('#placeForm [name="orderType"]');
            const companyIdField = document.querySelector('#placeForm [name="companyId"]');
            const empNameField = document.querySelector('#placeForm [name="empName"]');
            const empEmailField = document.querySelector('#placeForm [name="empEmail"]');
            const empHpField = document.querySelector('#placeForm [name="empHp"]');
            const noteField = document.querySelector('#placeForm [name="note"]');
            
            // 기본 정보 설정 (읽기 전용)
            orderDateField.value = data.orderDate;
            orderDateField.readOnly = true;
            orderDateField.style.backgroundColor = '#f8f9fa';
            
            orderTypeField.value = data.orderType;
            orderTypeField.disabled = true;
            orderTypeField.style.backgroundColor = '#f8f9fa';
            
            if (typeof filterCompanies === 'function') {
                filterCompanies();
            }
            companyIdField.value = data.companyId;
            companyIdField.disabled = true;
            companyIdField.style.backgroundColor = '#f8f9fa';
            
            empNameField.value = data.employeeName || '';
            empNameField.readOnly = true;
            empNameField.style.backgroundColor = '#f8f9fa';
            
            empEmailField.value = data.employeeEmail || '';
            empEmailField.readOnly = true;
            empEmailField.style.backgroundColor = '#f8f9fa';
            
            empHpField.value = data.employeeTel || '';
            empHpField.readOnly = true;
            empHpField.style.backgroundColor = '#f8f9fa';
            
            noteField.value = data.note || '';
            noteField.readOnly = true;
            noteField.style.backgroundColor = '#f8f9fa';

            // 품목 리스트 처리 (수정 대상만 편집 가능)
            const itemListContainer = document.getElementById("itemListContainer");
            itemListContainer.innerHTML = "";
            
            data.lineItems.forEach((item, index) => {
                const row = document.createElement("tr");
                row.className = "item-row";
                
                // ✅ 수정 대상 품목인지 확인
                const isTargetItem = item.orderLineItemId == lineItemId;
                const highlightClass = isTargetItem ? ' table-warning' : ' table-light';
                const readonlyAttr = isTargetItem ? '' : 'readonly';
                const disabledAttr = isTargetItem ? '' : 'disabled';
                const editableStyle = isTargetItem ? '' : 'background-color: #f8f9fa; cursor: not-allowed;';
                
                row.innerHTML = `
                    <td class="${highlightClass}">
                        <select name="lineItems[${index}].itemType" class="form-select item-type" ${disabledAttr} style="${editableStyle}">
                            <option value="자재" ${item.itemType === '자재' ? 'selected' : ''}>자재</option>
                        </select>
                    </td>
                    <td class="${highlightClass}">
                        <select name="lineItems[${index}].itemId" class="form-select item-name" ${disabledAttr} style="${editableStyle}">
                            <option value="${item.itemId}" selected>${item.itemName}</option>
                        </select>
                        ${isTargetItem ? '<small class="text-warning fw-bold">⭐ 수량만 수정 가능</small>' : '<small class="text-muted">🔒 수정 제한</small>'}
                    </td>
                    <td class="${highlightClass}"><input type="text" class="form-control item-code" name="lineItems[${index}].itemCode" value="${item.itemCode}" readonly style="${editableStyle}" /></td>
                    <td class="${highlightClass}"><input type="number" class="form-control unit-qty" name="lineItems[${index}].unitQty" value="${item.unitQty}" ${readonlyAttr} style="${editableStyle}" /></td>
                    <td class="${highlightClass}"><input type="text" class="form-control item-unit" name="lineItems[${index}].unit" value="${item.unit}" readonly style="${editableStyle}" /></td>
                    <td class="${highlightClass}"><input type="text" class="form-control unit-price" name="lineItems[${index}].unitPrice" value="${item.unitPrice}" readonly style="${editableStyle}" /></td>
                    <td class="${highlightClass}"><input type="text" class="form-control unit-price-all" name="lineItems[${index}].unitPriceAll" value="${item.unitPriceAll}" readonly style="${editableStyle}" /></td>
                    <input type="hidden" name="lineItems[${index}].orderLineItemId" class="order-line-item-id" value="${item.orderLineItemId || ''}" />
                    <td class="${highlightClass}">
                        ${isTargetItem ? 
                            '<button type="button" class="btn btn-danger btn-sm" onclick="removeItemRow(this)">삭제</button>' : 
                            '<button type="button" class="btn btn-secondary btn-sm" disabled>삭제</button>'
                        }
                    </td>
                `;
                itemListContainer.appendChild(row);
                
                if (typeof calculateItemTotal === 'function') {
                    calculateItemTotal(row);
                }
            });

            if (typeof calculateTotals === 'function') {
                calculateTotals();
            }
            
            // ✅ 모달 타이틀을 수정 대상 품목명으로 변경
            const targetItem = data.lineItems.find(item => item.orderLineItemId == lineItemId);
            const itemName = targetItem ? targetItem.itemName : '품목';
            document.querySelector('#placeRegisterModal .modal-title').textContent = `개별 수정: ${itemName}`;
            
            // 안내 메시지 추가
            const modalBody = document.querySelector('#placeRegisterModal .modal-body');
            let alertDiv = modalBody.querySelector('.edit-restriction-alert');
            if (!alertDiv) {
                alertDiv = document.createElement('div');
                alertDiv.className = 'alert alert-warning edit-restriction-alert';
                modalBody.insertBefore(alertDiv, modalBody.firstChild);
            }
            alertDiv.innerHTML = `
                <i class="bi bi-info-circle"></i>
                <strong>개별 수정 모드:</strong> '${itemName}' 품목만 수정할 수 있습니다. 
                다른 품목과 발주 기본 정보는 변경할 수 없습니다.
            `;
            
            const modal = new bootstrap.Modal(document.getElementById('placeRegisterModal'));
            modal.show();
        })
        .catch(err => {
            console.error("수정 모달 데이터 불러오기 오류:", err);
            alert("수정 정보를 불러오는 데 실패했습니다: " + err.message);
        });
}

//상세 모달 오픈 - 발주 상세정보를 표시하는 모달을 여는 함수
function openPlaceDetailModal(lineItemId) {
    console.log("✅ 개별 품목 상세 조회 - lineItemId:", lineItemId);

    fetch(`/place/detail/${lineItemId}`)
        .then(res => {
            console.log("✅ 응답 상태:", res.status);
            if (!res.ok) throw new Error('서버 통신 오류: ' + res.status);
            return res.json();
        })
        .then(data => {
            console.log("✅ 받은 품목 상세 데이터:", data);
            
            // 기본 정보
            document.getElementById('placeDetailDate').textContent = data.orderDate || '-';
            
            // 주문번호
            const orderNumElement = document.getElementById('placeDetailOrderNum');
            if (orderNumElement) {
                orderNumElement.textContent = data.orderNum || '-';
            }
            
            // 고객사/거래처명
            let companyText = '-';
            if (data.companyName && data.companyName.trim() !== '') {
                companyText = data.companyName;
                if (data.orderType === 'SUPPLIER') {
                    companyText += ' (거래처)';
                } else if (data.orderType === 'CUSTOMER') {
                    companyText += ' (고객사)';
                }
            } else {
                companyText = '회사명 없음';
            }
            document.getElementById('placeDetailCustomer').textContent = companyText;
            
            // ✅ 핵심: 클릭한 품목의 정보만 표시
            if (data.lineItems && data.lineItems.length > 0) {
                // lineItemId와 일치하는 특정 품목 찾기
                const targetItem = data.lineItems.find(item => 
                    item.orderLineItemId == lineItemId
                );
                
                if (targetItem) {
                    document.getElementById('placeDetailItemCode').textContent = targetItem.itemCode || '-';
                    document.getElementById('placeDetailItemName').textContent = targetItem.itemName || '-';
                    document.getElementById('placeDetailQty').textContent = targetItem.unitQty || 0;
                    document.getElementById('placeDetailUnit').textContent = targetItem.unit || '-';
                    
                    // 모달 타이틀도 품목명으로 변경
                    const modalTitle = document.querySelector('#placeDetailModal .modal-title');
                    if (modalTitle) {
                        modalTitle.textContent = `품목 상세: ${targetItem.itemName}`;
                    }
                } else {
                    console.warn("⚠️ 해당 lineItemId의 품목을 찾을 수 없음:", lineItemId);
                    // 첫 번째 품목으로 대체
                    const firstItem = data.lineItems[0];
                    document.getElementById('placeDetailItemCode').textContent = firstItem.itemCode || '-';
                    document.getElementById('placeDetailItemName').textContent = firstItem.itemName || '-';
                    document.getElementById('placeDetailQty').textContent = firstItem.unitQty || 0;
                    document.getElementById('placeDetailUnit').textContent = firstItem.unit || '-';
                }
            }
            
            // 담당자 및 비고
            document.getElementById('placeDetailManager').textContent = data.employeeName || '-';
            document.getElementById('placeDetailNote').textContent = data.note || '-';

            // 모달 열기
            const modalEl = document.getElementById('placeDetailModal');
            const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
            modal.show();
        })
        .catch(err => {
            console.error("❌ 품목 상세 정보 불러오기 오류:", err);
            alert("품목 상세 정보를 불러오는 데 실패했습니다: " + err.message);
        });
}

// ✅ 모달이 닫힐 때 기본 상태로 초기화
// ✅ 모달 초기화 이벤트
// ✅ 발주 등록 버튼 클릭 시 품목 테이블 완전 재생성
document.addEventListener('DOMContentLoaded', function() {
    const addBtn = document.getElementById('addBtn');
    if (addBtn) {
        addBtn.addEventListener('click', function() {
            // 폼 리셋
            document.getElementById('placeForm').reset();
            document.querySelector('#order_id').value = '';
            
            // 🔥 품목 테이블 완전히 새로 생성
            const itemListContainer = document.getElementById("itemListContainer");
            itemListContainer.innerHTML = `
                <tr class="item-row">
                    <td>
                        <select name="lineItems[0].itemType" class="form-select item-type">
                            <option value="자재" selected>자재</option>
                            <option value="완제품">완제품</option>
                        </select>
                    </td>
                    <td>
                        <select name="lineItems[0].itemId" class="form-select item-name">
                            <option value="">-- 품목 선택 --</option>
                        </select>
                    </td>
                    <td><input type="text" class="form-control item-code" name="lineItems[0].itemCode" readonly /></td>
                    <td><input type="number" class="form-control unit-qty" name="lineItems[0].unitQty" value="1" /></td>
                    <td><input type="text" class="form-control item-unit" name="lineItems[0].unit" readonly /></td>
                    <td><input type="text" class="form-control unit-price" name="lineItems[0].unitPrice" readonly /></td>
                    <td><input type="text" class="form-control unit-price-all" name="lineItems[0].unitPriceAll" readonly /></td>
                    <input type="hidden" name="lineItems[0].orderLineItemId" value="" />
                    <td><button type="button" class="btn btn-danger btn-sm" onclick="removeItemRow(this)">삭제</button></td>
                </tr>
            `;
            
            // 전체 폼 필드 제한 해제
            document.querySelectorAll('#placeForm input, #placeForm select, #placeForm textarea').forEach(field => {
                field.readOnly = false;
                field.disabled = false;
                field.style.backgroundColor = '';
            });
            
            // 모달 타이틀 및 알림 초기화
            document.querySelector('#placeRegisterModal .modal-title').textContent = '발주 등록';
            const alert = document.querySelector('.edit-restriction-alert');
            if (alert) alert.remove();
        });
    }
});
	
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
});

//발주 처리현황 변경
// 발주 확정 함수
// ✅ 발주 확정 함수 (자동 새로고침 추가)
function confirmOrder(button) {
    if (!confirm('등록확정하시겠습니까?')) {
        return;
    }
    
    const orderId = button.getAttribute('data-orderId');
    
    // 버튼 비활성화 (중복 클릭 방지)
    button.disabled = true;
    button.textContent = '처리중...';
    
    fetch(`/place/${orderId}/confirm`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert(data.message);
            window.location.reload();
        } else {
            alert('오류: ' + data.message);
            button.disabled = false;
            button.textContent = '발주 대기';
        }
    })
    .catch(error => {
        console.error('발주 확정 오류:', error);
        alert('발주 확정 중 오류가 발생했습니다.');
        button.disabled = false;
        button.textContent = '발주 대기';
    });
}

// 입고 처리 함수 (입고 페이지에서 사용)
function completeOrder(orderId) {
    if (!confirm('입고 처리하시겠습니까?')) {
        return;
    }
    
    fetch(`/place/${orderId}/complete`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert(data.message);
            location.reload(); // 페이지 새로고침
        } else {
            alert('오류: ' + data.message);
        }
    })
    .catch(error => {
        console.error('입고 처리 오류:', error);
        alert('입고 처리 중 오류가 발생했습니다.');
    });
}

// ✅ 전체 선택/해제 토글
function toggleSelectAll(selectAllCheckbox) {
    const itemCheckboxes = document.querySelectorAll('.item-checkbox:not(:disabled)');
    
    itemCheckboxes.forEach(checkbox => {
        checkbox.checked = selectAllCheckbox.checked;
    });
    
    updateBatchDeleteButton();
}

// ✅ 전체 선택/해제 토글
function toggleSelectAll(selectAllCheckbox) {
    const itemCheckboxes = document.querySelectorAll('.item-checkbox:not(:disabled)');
    
    itemCheckboxes.forEach(checkbox => {
        checkbox.checked = selectAllCheckbox.checked;
    });
    
    updateBatchDeleteButton();
}

// ✅ 일괄 삭제 버튼 상태 업데이트 (항상 표시, 활성화/비활성화만 변경)
function updateBatchDeleteButton() {
    const checkedBoxes = document.querySelectorAll('.item-checkbox:checked');
    const totalBoxes = document.querySelectorAll('.item-checkbox:not(:disabled)');
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    const batchDeleteBtn = document.getElementById('batchDeleteBtn');
    
    // 전체 선택 체크박스 상태 업데이트
    if (checkedBoxes.length === 0) {
        selectAllCheckbox.indeterminate = false;
        selectAllCheckbox.checked = false;
    } else if (checkedBoxes.length === totalBoxes.length) {
        selectAllCheckbox.indeterminate = false;
        selectAllCheckbox.checked = true;
    } else {
        selectAllCheckbox.indeterminate = true;
        selectAllCheckbox.checked = false;
    }
    
    // ✅ 삭제 버튼 상태 업데이트 (항상 표시)
    if (checkedBoxes.length > 0) {
        // 활성화
        batchDeleteBtn.disabled = false;
        batchDeleteBtn.className = 'btn btn-danger';
        batchDeleteBtn.innerHTML = `<i class="bi bi-trash"></i> 선택 삭제 (${checkedBoxes.length}개)`;
    } else {
        // 비활성화
        batchDeleteBtn.disabled = true;
        batchDeleteBtn.className = 'btn btn-outline-danger';
        batchDeleteBtn.innerHTML = '<i class="bi bi-trash"></i> 선택 삭제';
    }
}

// ✅ HTML에서 호출하는 함수명과 일치시키기 위한 별칭 함수
function updateDeleteButtons() {
    updateBatchDeleteButton();
}

// ✅ DOM이 로드된 후 이벤트 리스너 등록 (개별 체크박스용)
document.addEventListener('DOMContentLoaded', function() {
    // 모든 개별 체크박스에 이벤트 리스너 추가
    const itemCheckboxes = document.querySelectorAll('.item-checkbox');
    itemCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            updateBatchDeleteButton();
        });
    });
    
    // 전체 선택 체크박스에도 이벤트 리스너 추가
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            toggleSelectAll(this);
        });
    }
    
    // 초기 상태 설정
    updateBatchDeleteButton();
});

// ✅ 선택된 품목들 일괄 삭제
function deleteSelectedItems() {
    const checkedBoxes = document.querySelectorAll('.item-checkbox:checked');
    
    if (checkedBoxes.length === 0) {
        alert('삭제할 품목을 선택해주세요.');
        return;
    }
    
    // 선택된 품목 정보 수집
    const selectedItems = Array.from(checkedBoxes).map(checkbox => ({
        lineItemId: checkbox.value,
        itemName: checkbox.getAttribute('data-itemName'),
        orderNum: checkbox.getAttribute('data-orderNum'),
        orderId: checkbox.getAttribute('data-orderId'),
        itemCount: parseInt(checkbox.getAttribute('data-itemCount'))
    }));
    
    // 발주서별로 그룹화
    const orderGroups = {};
    selectedItems.forEach(item => {
        if (!orderGroups[item.orderId]) {
            orderGroups[item.orderId] = [];
        }
        orderGroups[item.orderId].push(item);
    });
    
    // 확인 메시지 생성
    let confirmMessage = `선택된 ${selectedItems.length}개 품목을 삭제하시겠습니까?\n\n`;
    
    Object.entries(orderGroups).forEach(([orderId, items]) => {
        const orderNum = items[0].orderNum;
        const totalItemsInOrder = items[0].itemCount;
        
        if (items.length === totalItemsInOrder) {
            confirmMessage += `📋 ${orderNum}: 전체 삭제 (발주서 삭제)\n`;
        } else {
            confirmMessage += `📋 ${orderNum}: ${items.length}/${totalItemsInOrder}개 품목 삭제\n`;
        }
        
        items.forEach(item => {
            confirmMessage += `   • ${item.itemName}\n`;
        });
    });
    
    confirmMessage += `\n⚠️ 삭제된 데이터는 복구할 수 없습니다.`;
    
    if (!confirm(confirmMessage)) {
        return;
    }
    
    // 일괄 삭제 실행
    batchDeleteItems(selectedItems);
}

// ✅ 실제 일괄 삭제 처리
function batchDeleteItems(selectedItems) {
    const batchDeleteBtn = document.getElementById('batchDeleteBtn');
    
    // 버튼 로딩 상태로 변경
    batchDeleteBtn.disabled = true;
    batchDeleteBtn.className = 'btn btn-secondary';
    batchDeleteBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>삭제 중...';
    
    // 삭제 요청 배열
    const deletePromises = selectedItems.map(item => 
        fetch(`/place/item/${item.lineItemId}/delete`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            }
        }).then(response => response.json())
    );
    
    // 모든 삭제 요청 병렬 처리
    Promise.all(deletePromises)
        .then(results => {
            const successCount = results.filter(result => result.success).length;
            const failCount = results.length - successCount;
            
            if (failCount === 0) {
                alert(`${successCount}개 품목이 성공적으로 삭제되었습니다.`);
            } else {
                alert(`${successCount}개 성공, ${failCount}개 실패\n일부 품목 삭제에 실패했습니다.`);
            }
            
            // 페이지 새로고침
            window.location.reload();
        })
        .catch(error => {
            console.error('일괄 삭제 오류:', error);
            alert('일괄 삭제 중 오류가 발생했습니다.');
            
            // 버튼 상태 복원
            updateBatchDeleteButton();
        });
}