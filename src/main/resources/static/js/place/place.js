// File: src/main/resources/static/js/place/place.js

// ========================
// Section 1: 전역 변수 및 함수
// ========================

let isSubmitting = false;

function searchOrders() {
    const keyword = document.getElementById('searchKeyword').value.trim();
    window.location.href = keyword ? `/place/search?keyword=${encodeURIComponent(keyword)}` : '/place';
}

function clearSearch() {
    document.getElementById('searchKeyword').value = '';
    window.location.href = '/place';
}

function goToInboundPage() {
    window.location.href = '/receive';
}

function filterCompanies() {
    try {
        const type = document.getElementById("orderType").value;
        const select = document.getElementById("companySelect");
        const options = select.querySelectorAll("option");
        options.forEach(opt => {
            if (opt.value === "") return;
            const optType = opt.dataset.type;
            opt.hidden = optType !== type;
        });
        if (select.options[select.selectedIndex]?.hidden) {
            select.value = "";
        }
    } catch (e) {
        console.error("filterCompanies 함수 실행 중 오류:", e);
    }
}

// ========================
// Section 2: 모달 관리
// ========================

function openplaceAddModal() {
    const form = document.getElementById('placeForm');
    form.reset();
    document.querySelector('#order_id').value = '';
    document.querySelectorAll('#placeForm input, #placeForm select, #placeForm textarea').forEach(field => {
        field.readOnly = false;
        field.disabled = false;
        field.style.backgroundColor = '';
    });
    document.querySelector('#placeRegisterModal .modal-title').textContent = '발주 등록';
    const alert = document.querySelector('.edit-restriction-alert');
    if (alert) alert.remove();
    filterCompanies();
    resetItemRows();
    calculateTotals();
}

// [추가] 모달 완전히 정리하는 함수
function cleanupModal() {
    const modalEl = document.getElementById('placeRegisterModal');
    if (modalEl) {
        const modalInstance = bootstrap.Modal.getInstance(modalEl);
        if (modalInstance) {
            modalInstance.dispose();
        }
        
        // backdrop 제거
        const backdrop = document.querySelector('.modal-backdrop');
        if (backdrop) {
            backdrop.remove();
        }
        
        // body 클래스 및 스타일 초기화
        document.body.classList.remove('modal-open');
        document.body.style.overflow = '';
        document.body.style.paddingRight = '';
    }
}

// [추가] 폼 초기화 함수
function placeReset() {
    if (!confirm("입력한 내용이 모두 초기화됩니다. 계속하시겠습니까?")) {
        return;
    }
    
    // 폼 리셋
    const form = document.getElementById('placeForm');
    form.reset();
    
    // hidden 필드 초기화
    document.querySelector('#order_id').value = '';
    
    // 모든 필드를 편집 가능 상태로 변경
    document.querySelectorAll('#placeForm input, #placeForm select, #placeForm textarea').forEach(field => {
        field.readOnly = false;
        field.disabled = false;
        field.style.backgroundColor = '';
    });
    
    // 제목 변경
    document.querySelector('#placeRegisterModal .modal-title').textContent = '발주 등록';
    
    // 경고 메시지 제거
    const alert = document.querySelector('.edit-restriction-alert');
    if (alert) alert.remove();
    
    // 품목 행 초기화
    resetItemRows();
    
    // 회사 필터링 다시 적용
    filterCompanies();
    
    // 합계 계산
    calculateTotals();
    
    alert("폼이 초기화되었습니다.");
}

// ========================
// Section 3: 폼 데이터 처리 및 제출
// ========================

function placeSubmit(event) {
    if (event) {
        event.preventDefault();
    }
    
    if (!confirm("입력한 정보를 저장하시겠습니까?")) return;

    isSubmitting = true;

    const formData = collectFormData();
    const orderId = formData.orderId;
    const url = orderId ? '/place/update' : '/place/add';
    const successMessage = orderId ? '수정되었습니다.' : '등록되었습니다.';
    const saveBtn = document.querySelector('#placeRegisterModal .btn-primary');
    const originalText = saveBtn.textContent;
    saveBtn.disabled = true;
    saveBtn.innerHTML = `<span class="spinner-border spinner-border-sm me-2"></span>처리중...`;

    fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=UTF-8' },
        body: JSON.stringify(formData)
    })
    .then(res => {
        if (!res.ok) {
            return res.text().then(text => { throw new Error(text || '서버 응답 오류'); });
        }
        return res.json().catch(() => ({ success: true, message: successMessage }));
    })
    .then(result => {
        alert(result.message || successMessage);
        
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.has('items')) {
            window.location.href = '/place';
        } else {
            window.location.reload();
        }
    })
    .catch(err => {
        alert('저장 중 오류가 발생했습니다: ' + err.message);
        saveBtn.disabled = false;
        saveBtn.textContent = originalText;
        isSubmitting = false; 
    });
}

function collectFormData() {
    const lineItems = [];
    document.querySelectorAll('.item-row').forEach(row => {
        const itemNameSelect = row.querySelector('.item-name');
        if (itemNameSelect && itemNameSelect.value) {
            lineItems.push({
                itemType: row.querySelector('.item-type').value,
                itemId: itemNameSelect.value,
                itemCode: row.querySelector('.item-code').value,
                unitQty: parseInt(row.querySelector('.unit-qty').value) || 1,
                unit: row.querySelector('.item-unit').value,
                unitPrice: parseFloat(row.querySelector('.unit-price').value) || 0,
                unitPriceAll: parseFloat(String(row.querySelector('.unit-price-all').value).replace(/,/g, '')) || 0,
                orderLineItemId: parseInt(row.querySelector('.order-line-item-id')?.value) || null
            });
        }
    });
    return {
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
}

// ========================
// Section 4: 품목 행(Row) 및 계산 유틸리티
// ========================

function resetItemRows() {
    const container = document.getElementById("itemListContainer");
    container.innerHTML = `
        <tr class="item-row">
            <td><select class="form-select item-type" onchange="loadItems(this)"><option value="자재" selected>자재</option></select></td>
            <td><select class="form-select item-name" onchange="fillItemDetails(this)"><option value="">-- 품목 선택 --</option></select></td>
            <td><input type="text" class="form-control item-code" readonly></td>
            <td><input type="number" class="form-control unit-qty" value="1" min="1"></td>
            <td><input type="text" class="form-control item-unit" readonly></td>
            <td><input type="text" class="form-control unit-price" readonly></td>
            <td><input type="text" class="form-control unit-price-all" readonly></td>
            <input type="hidden" class="order-line-item-id">
            <td><button type="button" class="btn btn-danger btn-sm" onclick="removeItemRow(this)">삭제</button></td>
        </tr>
    `;
    loadItems(container.querySelector('.item-type'));
}

function addItemRow() {
    const container = document.getElementById('itemListContainer');
    const newRow = document.createElement('tr');
    newRow.className = 'item-row';
    newRow.innerHTML = `
        <td><select class="form-select item-type" onchange="loadItems(this)"><option value="자재" selected>자재</option></select></td>
        <td><select class="form-select item-name" onchange="fillItemDetails(this)"><option value="">-- 품목 선택 --</option></select></td>
        <td><input type="text" class="form-control item-code" readonly></td>
        <td><input type="number" class="form-control unit-qty" value="1" min="1"></td>
        <td><input type="text" class="form-control item-unit" readonly></td>
        <td><input type="text" class="form-control unit-price" readonly></td>
        <td><input type="text" class="form-control unit-price-all" readonly></td>
        <input type="hidden" class="order-line-item-id">
        <td><button type="button" class="btn btn-danger btn-sm" onclick="removeItemRow(this)">삭제</button></td>
    `;
    container.appendChild(newRow);
    loadItems(newRow.querySelector('.item-type'));
}

function removeItemRow(btn) {
    const container = document.getElementById('itemListContainer');
    if (container.querySelectorAll('.item-row').length > 1) {
        btn.closest('tr').remove();
        calculateTotals();
    } else {
        alert('최소 하나의 품목은 필요합니다.');
    }
}

function loadItems(selectElement) {
    const itemType = selectElement.value;
    const row = selectElement.closest('tr');
    const itemSelect = row.querySelector('.item-name');
    itemSelect.innerHTML = '<option value="">-- 품목 선택 --</option>';
    if (!itemType) return Promise.resolve();
    return fetch(`/place/items?type=${itemType}`)
        .then(res => res.json())
        .then(items => {
            items.forEach(item => {
                itemSelect.innerHTML += `<option value="${item.id}" data-code="${item.code}" data-unit="${item.unit}" data-price="${item.itemPrice}">${item.name}</option>`;
            });
        });
}

function fillItemDetails(selectElement) {
    const option = selectElement.selectedOptions[0];
    const row = selectElement.closest('tr');
    row.querySelector('.item-code').value = option.dataset.code || '';
    row.querySelector('.item-unit').value = option.dataset.unit || '';
    row.querySelector('.unit-price').value = option.dataset.price || '';
    row.querySelector('.unit-qty').value = 1;
    calculateItemTotal(row);
    calculateTotals();
}

function calculateItemTotal(row) {
    let qtyInput = row.querySelector('.unit-qty');
    let priceInput = row.querySelector('.unit-price');
    let priceAllInput = row.querySelector('.unit-price-all');
    
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
    const price = parseFloat(priceInput?.value) || 0;
    
    if (priceAllInput) {
        priceAllInput.value = (qty * price).toLocaleString();
    }
}

function calculateTotals() {
    let totalQty = 0;
    let totalAmount = 0;
    document.querySelectorAll('#itemListContainer .item-row').forEach(row => {
        totalQty += parseInt(row.querySelector('.unit-qty')?.value) || 0;
        totalAmount += parseFloat(String(row.querySelector('.unit-price-all')?.value).replace(/,/g, '')) || 0;
    });
    document.getElementById("orderQty").textContent = totalQty.toLocaleString();
    document.getElementById("amount").textContent = totalAmount.toLocaleString();
}

// ========================
// Section 5: DOM 로드 후 실행
// ========================
document.addEventListener('DOMContentLoaded', function() {
    
    const urlParams = new URLSearchParams(window.location.search);
    const itemsParam = urlParams.get('items');
    
    if (itemsParam) {
        openplaceAddModal();
        const items = itemsParam.split(',').map(pair => {
            const [code, qty] = pair.split(':');
            return { itemCode: code, quantity: parseInt(qty, 10) || 1 };
        });
        
        const container = document.getElementById('itemListContainer');
        container.innerHTML = ''; 

        const promises = items.map(item => {
            const newRow = document.createElement('tr');
            newRow.className = 'item-row';
            newRow.innerHTML = `
                <td><select class="form-select item-type" onchange="loadItems(this)"><option value="자재" selected>자재</option></select></td>
                <td><select class="form-select item-name" onchange="fillItemDetails(this)"><option value="">-- 품목 선택 --</option></select></td>
                <td><input type="text" class="form-control item-code" readonly></td>
                <td><input type="number" class="form-control unit-qty" value="1" min="1"></td>
                <td><input type="text" class="form-control item-unit" readonly></td>
                <td><input type="text" class="form-control unit-price" readonly></td>
                <td><input type="text" class="form-control unit-price-all" readonly></td>
                <input type="hidden" class="order-line-item-id">
                <td><button type="button" class="btn btn-danger btn-sm" onclick="removeItemRow(this)">삭제</button></td>
            `;
            container.appendChild(newRow);

            return loadItems(newRow.querySelector('.item-type')).then(() => {
                const itemSelect = newRow.querySelector('.item-name');
                const option = Array.from(itemSelect.options).find(opt => opt.dataset.code === item.itemCode);
                if (option) {
                    itemSelect.value = option.value;
                    fillItemDetails(itemSelect);
                    newRow.querySelector('.unit-qty').value = item.quantity;
                    calculateItemTotal(newRow);
                }
            });
        });
        
        Promise.all(promises).then(() => {
            calculateTotals();
            new bootstrap.Modal(document.getElementById('placeRegisterModal')).show();
        });
    }

    // [수정] 모달 이벤트 처리 개선
    const placeModalEl = document.getElementById('placeRegisterModal');
    if(placeModalEl) {
        placeModalEl.addEventListener('hidden.bs.modal', (e) => {
            // 모달이 완전히 닫힌 후 처리
            setTimeout(() => {
                if (!isSubmitting && urlParams.has('items')) {
                    if (confirm("작성을 취소하고 이전 페이지로 돌아가시겠습니까?")) {
                        cleanupModal();
                        history.back();
                    } else {
                        // 사용자가 취소한 경우 모달을 다시 열기
                        const modal = new bootstrap.Modal(placeModalEl);
                        modal.show();
                    }
                } else {
                    cleanupModal();
                }
                isSubmitting = false;
            }, 100);
        });
    }

    // [수정] 등록 버튼 이벤트 처리
    document.getElementById('addBtn')?.addEventListener('click', () => {
        openplaceAddModal();
        const modal = new bootstrap.Modal(document.getElementById('placeRegisterModal'));
        modal.show();
    });

    // [추가] 초기화 버튼 이벤트 처리
    const resetBtn = document.querySelector('#placeRegisterModal .btn-secondary');
    if (resetBtn && resetBtn.textContent.includes('초기화')) {
        resetBtn.addEventListener('click', resetPlaceForm);
    }

    document.getElementById('companySelect')?.addEventListener('change', function () {
        const companyId = this.value;
        if (!companyId) {
            ['empName', 'empEmail', 'empHp'].forEach(id => document.getElementById(id).value = '');
            return;
        }
        fetch(`/place/employees/byCompany?companyId=${companyId}`)
            .then(response => response.json())
            .then(data => {
                document.getElementById('empName').value = data.empName || '';
                document.getElementById('empEmail').value = data.empEmail || '';
                document.getElementById('empHp').value = data.empHp || '';
            })
            .catch(() => ['empName', 'empEmail', 'empHp'].forEach(id => document.getElementById(id).value = ''));
    });

    document.getElementById('itemListContainer')?.addEventListener('input', e => {
        if (e.target.matches('.unit-qty, .unit-price')) {
            const row = e.target.closest('tr');
            if(row) {
                calculateItemTotal(row);
                calculateTotals();
            }
        }
    });
    
    document.getElementById('placeForm')?.addEventListener('keydown', e => {
        if (e.key === 'Enter' && e.target.tagName !== 'TEXTAREA') e.preventDefault();
    });
});

// 나머지 함수들은 기존 코드와 동일...
// (smartDeleteItem, openPlaceEditModal, loadRestrictedEditModalData, openPlaceDetailModal, confirmOrder, completeOrder, toggleSelectAll, updateBatchDeleteButton, deleteSelectedItems, batchDeleteItems 등)

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
        confirmMessage = `품목 '${itemName}'을(를) 삭제하시겠습니까?`;
        warningMessage = `이 품목이 발주서 ${orderNum}에서 제거됩니다.\n남은 품목: ${itemCount - 1}개`;
    } else {
        confirmMessage = `마지막 품목입니다.\n발주서 ${orderNum}을(를) 완전히 삭제하시겠습니까?`;
        warningMessage = '발주서와 모든 관련 데이터가 영구적으로 삭제됩니다.';
    }
    
    if (!confirm(`${confirmMessage}\n\n⚠️ 주의: ${warningMessage}\n\n계속하시겠습니까?`)) {
        return;
    }
    
    button.disabled = true;
    const originalHTML = button.innerHTML;
    button.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
    
    fetch(`/place/item/${lineItemId}/delete`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(response => {
        if (!response.ok) throw new Error('서버 통신 오류: ' + response.status);
        return response.json();
    })
    .then(data => {
        if (data.success) {
            alert(data.message);
            window.location.reload();
        } else {
            alert('삭제 실패: ' + data.message);
            button.disabled = false;
            button.innerHTML = originalHTML;
        }
    })
    .catch(error => {
        console.error('삭제 오류:', error);
        alert('삭제 중 오류가 발생했습니다: ' + error.message);
        button.disabled = false;
        button.innerHTML = originalHTML;
    });
}

// ✅ 발주 수정 모달 열기
function openPlaceEditModal(lineItemId) {
    fetch(`/place/detail/${lineItemId}`)
        .then(res => {
            if (!res.ok) throw new Error('서버 통신 오류');
            return res.json();
        })
        .then(data => {
            if (data.orderStatus !== 'WAITING') {
                const targetItem = data.lineItems.find(item => item.orderLineItemId == lineItemId);
                const itemName = targetItem ? targetItem.itemName : '해당 품목';
                alert(`'${itemName}'을(를) 수정할 수 없습니다.\n\n현재 상태: ${getStatusText(data.orderStatus)}\n\n발주 대기 상태에서만 수정이 가능합니다.`);
                return;
            }
            loadRestrictedEditModalData(lineItemId);
        })
        .catch(err => {
            console.error("상태 확인 오류:", err);
            alert("발주 상태를 확인할 수 없습니다: " + err.message);
        });
}

function loadRestrictedEditModalData(lineItemId) {
    fetch(`/place/edit/${lineItemId}`)
        .then(res => {
            if (!res.ok) throw new Error('서버 통신 오류');
            return res.json();
        })
        .then(data => {
            console.log("불러온 수정 데이터: ", data);
            document.querySelector('#order_id').value = data.orderId || '';
            const orderDateField = document.querySelector('#placeForm [name="orderDate"]');
            const orderTypeField = document.querySelector('#placeForm [name="orderType"]');
            const companyIdField = document.querySelector('#placeForm [name="companyId"]');
            const empNameField = document.querySelector('#placeForm [name="empName"]');
            const empEmailField = document.querySelector('#placeForm [name="empEmail"]');
            const empHpField = document.querySelector('#placeForm [name="empHp"]');
            const noteField = document.querySelector('#placeForm [name="note"]');
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
            const itemListContainer = document.getElementById("itemListContainer");
            itemListContainer.innerHTML = "";
            data.lineItems.forEach((item, index) => {
                const row = document.createElement("tr");
                row.className = "item-row";
                const isTargetItem = item.orderLineItemId == lineItemId;
                const highlightClass = isTargetItem ? ' table-warning' : ' table-light';
                const readonlyAttr = isTargetItem ? '' : 'readonly';
                const disabledAttr = isTargetItem ? '' : 'disabled';
                const editableStyle = isTargetItem ? '' : 'background-color: #f8f9fa; cursor: not-allowed;';
                row.innerHTML = `
                    <td class="${highlightClass}"><select name="lineItems[${index}].itemType" class="form-select item-type" ${disabledAttr} style="${editableStyle}"><option value="자재" ${item.itemType === '자재' ? 'selected' : ''}>자재</option></select></td>
                    <td class="${highlightClass}"><select name="lineItems[${index}].itemId" class="form-select item-name" ${disabledAttr} style="${editableStyle}"><option value="${item.itemId}" selected>${item.itemName}</option></select>${isTargetItem ? '<small class="text-warning fw-bold">⭐ 수량만 수정 가능</small>' : '<small class="text-muted">🔒 수정 제한</small>'}</td>
                    <td class="${highlightClass}"><input type="text" class="form-control item-code" name="lineItems[${index}].itemCode" value="${item.itemCode}" readonly style="${editableStyle}" /></td>
                    <td class="${highlightClass}"><input type="number" class="form-control unit-qty" name="lineItems[${index}].unitQty" value="${item.unitQty}" ${readonlyAttr} style="${editableStyle}" /></td>
                    <td class="${highlightClass}"><input type="text" class="form-control item-unit" name="lineItems[${index}].unit" value="${item.unit}" readonly style="${editableStyle}" /></td>
                    <td class="${highlightClass}"><input type="text" class="form-control unit-price" name="lineItems[${index}].unitPrice" value="${item.unitPrice}" readonly style="${editableStyle}" /></td>
                    <td class="${highlightClass}"><input type="text" class="form-control unit-price-all" name="lineItems[${index}].unitPriceAll" value="${item.unitPriceAll}" readonly style="${editableStyle}" /></td>
                    <input type="hidden" name="lineItems[${index}].orderLineItemId" class="order-line-item-id" value="${item.orderLineItemId || ''}" />
                    <td class="${highlightClass}">${isTargetItem ? '<button type="button" class="btn btn-danger btn-sm" onclick="removeItemRow(this)">삭제</button>' : '<button type="button" class="btn btn-secondary btn-sm" disabled>삭제</button>'}</td>
                `;
                itemListContainer.appendChild(row);
                if (typeof calculateItemTotal === 'function') calculateItemTotal(row);
            });
            if (typeof calculateTotals === 'function') calculateTotals();
            const targetItem = data.lineItems.find(item => item.orderLineItemId == lineItemId);
            const itemName = targetItem ? targetItem.itemName : '품목';
            document.querySelector('#placeRegisterModal .modal-title').textContent = `개별 수정: ${itemName}`;
            const modalBody = document.querySelector('#placeRegisterModal .modal-body');
            let alertDiv = modalBody.querySelector('.edit-restriction-alert');
            if (!alertDiv) {
                alertDiv = document.createElement('div');
                alertDiv.className = 'alert alert-warning edit-restriction-alert';
                modalBody.insertBefore(alertDiv, modalBody.firstChild);
            }
            alertDiv.innerHTML = `<i class="bi bi-info-circle"></i> <strong>개별 수정 모드:</strong> '${itemName}' 품목만 수정할 수 있습니다. 다른 품목과 발주 기본 정보는 변경할 수 없습니다.`;
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
            if (!res.ok) throw new Error('서버 통신 오류: ' + res.status);
            return res.json();
        })
        .then(data => {
            document.getElementById('placeDetailDate').textContent = data.orderDate || '-';
            const orderNumElement = document.getElementById('placeDetailOrderNum');
            if (orderNumElement) orderNumElement.textContent = data.orderNum || '-';
            let companyText = '-';
            if (data.companyName && data.companyName.trim() !== '') {
                companyText = data.companyName;
                if (data.orderType === 'SUPPLIER') companyText += ' (거래처)';
                else if (data.orderType === 'CUSTOMER') companyText += ' (고객사)';
            } else {
                companyText = '회사명 없음';
            }
            document.getElementById('placeDetailCustomer').textContent = companyText;
            if (data.lineItems && data.lineItems.length > 0) {
                const targetItem = data.lineItems.find(item => item.orderLineItemId == lineItemId);
                if (targetItem) {
                    document.getElementById('placeDetailItemCode').textContent = targetItem.itemCode || '-';
                    document.getElementById('placeDetailItemName').textContent = targetItem.itemName || '-';
                    document.getElementById('placeDetailQty').textContent = targetItem.unitQty || 0;
                    document.getElementById('placeDetailUnit').textContent = targetItem.unit || '-';
                    const modalTitle = document.querySelector('#placeDetailModal .modal-title');
                    if (modalTitle) modalTitle.textContent = `품목 상세: ${targetItem.itemName}`;
                } else {
                    const firstItem = data.lineItems[0];
                    document.getElementById('placeDetailItemCode').textContent = firstItem.itemCode || '-';
                    document.getElementById('placeDetailItemName').textContent = firstItem.itemName || '-';
                    document.getElementById('placeDetailQty').textContent = firstItem.unitQty || 0;
                    document.getElementById('placeDetailUnit').textContent = firstItem.unit || '-';
                }
            }
            document.getElementById('placeDetailManager').textContent = data.employeeName || '-';
            document.getElementById('placeDetailNote').textContent = data.note || '-';
            const modalEl = document.getElementById('placeDetailModal');
            const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
            modal.show();
        })
        .catch(err => {
            console.error("❌ 품목 상세 정보 불러오기 오류:", err);
            alert("품목 상세 정보를 불러오는 데 실패했습니다: " + err.message);
        });
}

function confirmOrder(button) {
    if (!confirm('등록확정하시겠습니까?')) return;
    const orderId = button.getAttribute('data-orderId');
    button.disabled = true;
    button.textContent = '처리중...';
    fetch(`/place/${orderId}/confirm`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
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

function completeOrder(orderId) {
    if (!confirm('입고 처리하시겠습니까?')) return;
    fetch(`/place/${orderId}/complete`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert(data.message);
            location.reload();
        } else {
            alert('오류: ' + data.message);
        }
    })
    .catch(error => {
        console.error('입고 처리 오류:', error);
        alert('입고 처리 중 오류가 발생했습니다.');
    });
}

function toggleSelectAll(selectAllCheckbox) {
    const itemCheckboxes = document.querySelectorAll('.item-checkbox:not(:disabled)');
    itemCheckboxes.forEach(checkbox => {
        checkbox.checked = selectAllCheckbox.checked;
    });
    updateBatchDeleteButton();
}

function updateBatchDeleteButton() {
    const checkedBoxes = document.querySelectorAll('.item-checkbox:checked');
    const totalBoxes = document.querySelectorAll('.item-checkbox:not(:disabled)');
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    const batchDeleteBtn = document.getElementById('batchDeleteBtn');
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
    if (checkedBoxes.length > 0) {
        batchDeleteBtn.disabled = false;
        batchDeleteBtn.className = 'btn btn-danger';
        batchDeleteBtn.innerHTML = `<i class="bi bi-trash"></i> 선택 삭제 (${checkedBoxes.length}개)`;
    } else {
        batchDeleteBtn.disabled = true;
        batchDeleteBtn.className = 'btn btn-outline-danger';
        batchDeleteBtn.innerHTML = '<i class="bi bi-trash"></i> 선택 삭제';
    }
}

function updateDeleteButtons() {
    updateBatchDeleteButton();
}

function deleteSelectedItems() {
    const checkedBoxes = document.querySelectorAll('.item-checkbox:checked');
    if (checkedBoxes.length === 0) {
        alert('삭제할 품목을 선택해주세요.');
        return;
    }
    
    const selectedItems = Array.from(checkedBoxes).map(checkbox => ({
        lineItemId: checkbox.value,
        itemName: checkbox.getAttribute('data-itemName'),
        orderNum: checkbox.getAttribute('data-orderNum'),
        orderId: checkbox.getAttribute('data-orderId'),
        itemCount: parseInt(checkbox.getAttribute('data-itemCount'))
    }));
    
    const orderGroups = {};
    selectedItems.forEach(item => {
        if (!orderGroups[item.orderId]) {
            orderGroups[item.orderId] = [];
        }
        orderGroups[item.orderId].push(item);
    });
    
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
    
    if (!confirm(confirmMessage)) return;
    
    batchDeleteItems(selectedItems);
}

function batchDeleteItems(selectedItems) {
    const batchDeleteBtn = document.getElementById('batchDeleteBtn');
    batchDeleteBtn.disabled = true;
    batchDeleteBtn.className = 'btn btn-secondary';
    batchDeleteBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>삭제 중...';
    
    const deletePromises = selectedItems.map(item => 
        fetch(`/place/item/${item.lineItemId}/delete`, {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' }
        }).then(response => response.json())
    );
    
    Promise.all(deletePromises)
        .then(results => {
            const successCount = results.filter(result => result.success).length;
            const failCount = results.length - successCount;
            if (failCount === 0) {
                alert(`${successCount}개 품목이 성공적으로 삭제되었습니다.`);
            } else {
                alert(`${successCount}개 성공, ${failCount}개 실패\n일부 품목 삭제에 실패했습니다.`);
            }
            window.location.reload();
        })
        .catch(error => {
            console.error('일괄 삭제 오류:', error);
            alert('일괄 삭제 중 오류가 발생했습니다.');
            updateBatchDeleteButton();
        });
}

// [추가] 체크박스 관련 DOM 로드 이벤트
document.addEventListener('DOMContentLoaded', function() {
    const itemCheckboxes = document.querySelectorAll('.item-checkbox');
    itemCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            updateBatchDeleteButton();
        });
    });
    
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            toggleSelectAll(this);
        });
    }
    
    updateBatchDeleteButton();
});