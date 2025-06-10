// 파일: src/main/resources/static/js/place/place.js (최종 추천 버전)

// ========================
// Section 1: 페이지 이동 및 검색
// ========================

function searchOrders() {
    const keyword = document.getElementById('searchKeyword').value.trim();
    window.location.href = keyword ? `/place/search?keyword=${encodeURIComponent(keyword)}` : '/place';
}

function clearSearch() {
    document.getElementById('searchKeyword').value = '';
    window.location.href = '/place';
}

function goToInboundPage() {
    window.location.href = '/receive'; // 최신 코드의 라우팅 경로 반영
}


// ========================
// Section 2: 모달 관리 (등록, 수정, 상세)
// ========================

// ✅ 등록 모달 열기 (폼, 필드, 테이블 모두 초기화)
function openplaceAddModal() {
    const form = document.getElementById('placeForm');
    form.reset();
    document.querySelector('#order_id').value = '';

    // 모든 필드 편집 가능하도록 제한 해제
    document.querySelectorAll('#placeForm input, #placeForm select, #placeForm textarea').forEach(field => {
        field.readOnly = false;
        field.disabled = false;
        field.style.backgroundColor = '';
    });
    
    // 모달 타이틀 및 경고 메시지 초기화
    document.querySelector('#placeRegisterModal .modal-title').textContent = '발주 등록';
    const alert = document.querySelector('.edit-restriction-alert');
    if (alert) alert.remove();
    
    // 품목 테이블 새로 생성
    const itemListContainer = document.getElementById("itemListContainer");
    itemListContainer.innerHTML = `
        <tr class="item-row">
            <td>
                <select name="lineItems[0].itemType" class="form-select item-type" onchange="loadItems(this)">
                    <option value="자재" selected>자재</option>
                    <option value="완제품">완제품</option>
                </select>
            </td>
            <td>
                <select name="lineItems[0].itemId" class="form-select item-name" onchange="fillItemDetails(this)">
                    <option value="">-- 품목 선택 --</option>
                </select>
            </td>
            <td><input type="text" class="form-control item-code" readonly></td>
            <td><input type="number" class="form-control unit-qty" value="1" min="1"></td>
            <td><input type="text" class="form-control item-unit" readonly></td>
            <td><input type="text" class="form-control unit-price" readonly></td>
            <td><input type="text" class="form-control unit-price-all" readonly></td>
            <input type="hidden" class="order-line-item-id">
            <td><button type="button" class="btn btn-danger btn-sm" onclick="removeItemRow(this)">삭제</button></td>
        </tr>
    `;

    filterCompanies();
    calculateTotals();
    loadItems(itemListContainer.querySelector('.item-type')); // 첫 행 품목 로드
}


// ✅ 수정 모달 열기 (상태 확인 후 데이터 로드)
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
            alert(`'${itemName}'을(를) 수정할 수 없습니다.\n\n현재 상태: ${data.orderStatus}\n\n'발주 대기' 상태에서만 수정이 가능합니다.`);
            return;
        }
        loadRestrictedEditModalData(lineItemId);
    })
    .catch(err => {
        console.error("상태 확인 오류:", err);
        alert("발주 상태를 확인할 수 없습니다: " + err.message);
    });
}

// 수정 모달에 제한된 데이터 로드
function loadRestrictedEditModalData(lineItemId) {
    fetch(`/place/edit/${lineItemId}`)
    .then(res => {
        if (!res.ok) throw new Error('서버 통신 오류');
        return res.json();
    })
    .then(data => {
        document.querySelector('#order_id').value = data.orderId || '';

        // 기본 정보 필드 읽기 전용으로 설정
        const form = document.getElementById('placeForm');
        ['orderDate', 'empName', 'empEmail', 'empHp', 'note'].forEach(name => {
            const field = form.querySelector(`[name="${name}"]`);
            field.value = data[name] || (data.employee && data.employee[name]) || ''; // 데이터 구조에 맞게 조정
            field.readOnly = true;
            field.style.backgroundColor = '#f8f9fa';
        });
        
        const orderTypeField = form.querySelector('[name="orderType"]');
        orderTypeField.value = data.orderType;
        orderTypeField.disabled = true;

        filterCompanies();
        const companyIdField = form.querySelector('[name="companyId"]');
        companyIdField.value = data.companyId;
        companyIdField.disabled = true;

        // 품목 리스트 처리
        const itemListContainer = document.getElementById("itemListContainer");
        itemListContainer.innerHTML = "";
        data.lineItems.forEach((item, index) => {
            const isTargetItem = item.orderLineItemId == lineItemId;
            const row = document.createElement("tr");
            row.className = `item-row ${isTargetItem ? 'table-warning' : 'table-light'}`;
            row.innerHTML = `
                <td>
                    <select class="form-select item-type" disabled><option selected>${item.itemType}</option></select>
                </td>
                <td>
                    <select class="form-select item-name" disabled><option selected>${item.itemName}</option></select>
                    ${isTargetItem ? '<small class="text-primary fw-bold">⭐ 수량, 단가 수정 가능</small>' : '<small class="text-muted">🔒 수정 제한</small>'}
                </td>
                <td><input type="text" class="form-control item-code" value="${item.itemCode}" readonly></td>
                <td><input type="number" class="form-control unit-qty" value="${item.unitQty}" ${isTargetItem ? '' : 'readonly'}></td>
                <td><input type="text" class="form-control item-unit" value="${item.unit}" readonly></td>
                <td><input type="text" class="form-control unit-price" value="${item.unitPrice}" ${isTargetItem ? '' : 'readonly'}></td>
                <td><input type="text" class="form-control unit-price-all" value="${item.unitPriceAll}" readonly></td>
                <input type="hidden" class="order-line-item-id" value="${item.orderLineItemId || ''}">
                <td><button type="button" class="btn btn-secondary btn-sm" disabled>삭제</button></td>
            `;
            itemListContainer.appendChild(row);
        });
        
        calculateTotals();
        
        const targetItem = data.lineItems.find(item => item.orderLineItemId == lineItemId);
        document.querySelector('#placeRegisterModal .modal-title').textContent = `개별 수정: ${targetItem.itemName}`;
        
        const modal = new bootstrap.Modal(document.getElementById('placeRegisterModal'));
        modal.show();
    })
    .catch(err => {
        console.error("수정 모달 로드 오류:", err);
        alert("수정 정보를 불러오는 데 실패했습니다: " + err.message);
    });
}

// ✅ 상세 모달 열기
function openPlaceDetailModal(lineItemId) {
    fetch(`/place/detail/${lineItemId}`)
    .then(res => {
        if (!res.ok) throw new Error('서버 통신 오류: ' + res.status);
        return res.json();
    })
    .then(data => {
        document.getElementById('placeDetailDate').textContent = data.orderDate || '-';
        document.getElementById('placeDetailOrderNum').textContent = data.orderNum || '-';
        
        let companyText = data.companyName || '회사명 없음';
        if(data.companyName) companyText += (data.orderType === 'SUPPLIER' ? ' (거래처)' : ' (고객사)');
        document.getElementById('placeDetailCustomer').textContent = companyText;

        const targetItem = data.lineItems.find(item => item.orderLineItemId == lineItemId);
        if (targetItem) {
            document.getElementById('placeDetailItemCode').textContent = targetItem.itemCode || '-';
            document.getElementById('placeDetailItemName').textContent = targetItem.itemName || '-';
            document.getElementById('placeDetailQty').textContent = (targetItem.unitQty || 0).toLocaleString();
            document.getElementById('placeDetailUnit').textContent = targetItem.unit || '-';
            document.querySelector('#placeDetailModal .modal-title').textContent = `품목 상세: ${targetItem.itemName}`;
        }
        
        document.getElementById('placeDetailManager').textContent = data.employeeName || '-';
        document.getElementById('placeDetailNote').textContent = data.note || '-';

        const modal = bootstrap.Modal.getInstance(document.getElementById('placeDetailModal')) || new bootstrap.Modal(document.getElementById('placeDetailModal'));
        modal.show();
    })
    .catch(err => {
        console.error("상세 정보 로드 오류:", err);
        alert("상세 정보를 불러오는 데 실패했습니다: " + err.message);
    });
}


// ========================
// Section 3: 폼 데이터 처리 및 제출
// ========================

// ✅ 폼 데이터 수집
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
                orderLineItemId: parseInt(row.querySelector('.order-line-item-id').value) || null
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

// ✅ 발주 등록/수정 제출
function placeSubmit(event) {
    event.preventDefault();
    if (!confirm("입력한 정보를 저장하시겠습니까?")) return;

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
    .then(res => res.json().then(data => ({ ok: res.ok, data })))
    .then(({ ok, data }) => {
        if (!ok) throw new Error(data.message || '서버 오류');
        alert(data.message || successMessage);
        window.location.reload();
    })
    .catch(err => {
        alert('저장 중 오류가 발생했습니다: ' + err.message);
        saveBtn.disabled = false;
        saveBtn.textContent = originalText;
    });
}


// ========================
// Section 4: 품목 행(Row) 관리
// ========================

function addItemRow() {
    const container = document.getElementById('itemListContainer');
    const newIndex = container.querySelectorAll('.item-row').length;
    const newRow = document.createElement('tr');
    newRow.className = 'item-row';
    newRow.innerHTML = `
        <td>
            <select name="lineItems[${newIndex}].itemType" class="form-select item-type" onchange="loadItems(this)">
                <option value="자재" selected>자재</option>
                <option value="완제품">완제품</option>
            </select>
        </td>
        <td>
            <select name="lineItems[${newIndex}].itemId" class="form-select item-name" onchange="fillItemDetails(this)">
                <option value="">-- 품목 선택 --</option>
            </select>
        </td>
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

// 품목 타입에 따른 품목 목록 로드
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

// 품목 선택 시 상세 정보 자동 채우기
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


// ========================
// Section 5: 계산 로직 (최신 코드로 개선)
// ========================

// ✅ 개별 품목 합계 계산
function calculateItemTotal(row) {
    const qtyInput = row.querySelector('.unit-qty') || row.querySelector('input[name*="unitQty"]');
    const priceInput = row.querySelector('.unit-price') || row.querySelector('input[name*="unitPrice"]');
    const priceAllInput = row.querySelector('.unit-price-all') || row.querySelector('input[name*="unitPriceAll"]');
    const qty = parseInt(qtyInput?.value) || 0;
    const price = parseFloat(priceInput?.value) || 0;
    if (priceAllInput) priceAllInput.value = (qty * price).toLocaleString();
}

// ✅ 전체 합계 계산
function calculateTotals() {
    let totalQty = 0;
    let totalAmount = 0;
    document.querySelectorAll('#itemListContainer .item-row').forEach(row => {
        const qty = parseInt(row.querySelector('.unit-qty')?.value) || 0;
        const priceAll = parseFloat(String(row.querySelector('.unit-price-all')?.value).replace(/,/g, '')) || 0;
        totalQty += qty;
        totalAmount += priceAll;
    });
    document.getElementById("orderQty").textContent = totalQty.toLocaleString();
    document.getElementById("amount").textContent = totalAmount.toLocaleString();
}


// ========================
// Section 6: 발주 상태 변경 및 삭제
// ========================

// ✅ 발주 확정
function confirmOrder(button) {
    if (!confirm('등록을 확정하시겠습니까?')) return;
    const orderId = button.getAttribute('data-orderId');
    button.disabled = true;
    button.textContent = '처리중...';

    fetch(`/place/${orderId}/confirm`, { method: 'POST' })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            alert(data.message);
            window.location.reload();
        } else {
            throw new Error(data.message);
        }
    })
    .catch(error => {
        alert('오류: ' + error.message);
        button.disabled = false;
        button.textContent = '발주 대기';
    });
}

// ✅ 스마트 삭제 (개별 품목 또는 발주서 전체)
function smartDeleteItem(button) {
    const { lineitemid, ordernum, itemcount, itemname } = button.dataset;
    const isLastName = parseInt(itemcount) === 1;
    const confirmMessage = isLastName ? `마지막 품목입니다. 발주서 '${ordernum}'을(를) 완전히 삭제하시겠습니까?` : `품목 '${itemname}'을(를) 삭제하시겠습니까?`;
    const warningMessage = isLastName ? '발주서와 모든 관련 데이터가 영구적으로 삭제됩니다.' : `이 품목이 발주서 ${ordernum}에서 제거됩니다.`;

    if (!confirm(`${confirmMessage}\n\n⚠️ 주의: ${warningMessage}`)) return;

    button.disabled = true;
    button.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
    
    fetch(`/place/item/${lineitemid}/delete`, { method: 'DELETE' })
    .then(res => res.json().then(data => ({ ok: res.ok, data })))
    .then(({ ok, data }) => {
        if (!ok) throw new Error(data.message);
        alert(data.message);
        window.location.reload();
    })
    .catch(error => {
        alert('삭제 중 오류: ' + error.message);
        button.disabled = false; // 원래 버튼 텍스트 복원은 생략 (페이지가 리로드되므로)
    });
}

// ✅ 일괄 선택/삭제
function toggleSelectAll(selectAllCheckbox) {
    document.querySelectorAll('.item-checkbox:not(:disabled)').forEach(checkbox => {
        checkbox.checked = selectAllCheckbox.checked;
    });
    updateBatchDeleteButton();
}

function updateBatchDeleteButton() {
    const checkedBoxes = document.querySelectorAll('.item-checkbox:checked');
    const batchDeleteBtn = document.getElementById('batchDeleteBtn');
    if(!batchDeleteBtn) return;

    batchDeleteBtn.disabled = checkedBoxes.length === 0;
    batchDeleteBtn.innerHTML = checkedBoxes.length > 0 ? `<i class="bi bi-trash"></i> 선택 삭제 (${checkedBoxes.length}개)` : '<i class="bi bi-trash"></i> 선택 삭제';
    
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if(selectAllCheckbox) {
        const totalBoxes = document.querySelectorAll('.item-checkbox:not(:disabled)').length;
        selectAllCheckbox.checked = (totalBoxes > 0 && checkedBoxes.length === totalBoxes);
        selectAllCheckbox.indeterminate = (checkedBoxes.length > 0 && checkedBoxes.length < totalBoxes);
    }
}

function deleteSelectedItems() {
    const checkedBoxes = Array.from(document.querySelectorAll('.item-checkbox:checked'));
    if (checkedBoxes.length === 0) return alert('삭제할 품목을 선택해주세요.');

    if (!confirm(`선택된 ${checkedBoxes.length}개 품목을 정말 삭제하시겠습니까?`)) return;

    const deletePromises = checkedBoxes.map(cb => fetch(`/place/item/${cb.value}/delete`, { method: 'DELETE' }).then(res => res.json()));

    Promise.all(deletePromises)
    .then(results => {
        const successCount = results.filter(r => r.success).length;
        alert(`${successCount}개 품목이 성공적으로 삭제되었습니다.`);
        window.location.reload();
    })
    .catch(error => alert('일괄 삭제 중 오류가 발생했습니다: ' + error));
}


// ========================
// Section 7: DOM 로드 후 이벤트 리스너 통합 등록
// ========================

document.addEventListener('DOMContentLoaded', function() {
    // 등록 버튼 모달 열기
    const addBtn = document.getElementById('addBtn');
    if (addBtn) {
        addBtn.addEventListener('click', () => {
            openplaceAddModal();
            new bootstrap.Modal(document.getElementById('placeRegisterModal')).show();
        });
    }

    // 엔터 키로 폼 제출 방지
    const placeForm = document.getElementById('placeForm');
    if (placeForm) {
        placeForm.addEventListener('keydown', e => {
            if (e.key === 'Enter' && e.target.tagName !== 'TEXTAREA') {
                e.preventDefault();
            }
        });
    }

    // (개선) 이벤트 위임을 사용한 계산 트리거
    const itemListContainer = document.getElementById('itemListContainer');
    if (itemListContainer) {
        itemListContainer.addEventListener('input', e => {
            if (e.target.matches('.unit-qty, .unit-price')) {
                const row = e.target.closest('tr');
                if(row) {
                    calculateItemTotal(row);
                    calculateTotals();
                }
            }
        });
    }
    
    // 일괄 삭제 체크박스 이벤트
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', () => toggleSelectAll(selectAllCheckbox));
    }
    document.querySelectorAll('.item-checkbox').forEach(checkbox => {
        checkbox.addEventListener('change', updateBatchDeleteButton);
    });
    updateBatchDeleteButton(); // 초기 상태 설정
});