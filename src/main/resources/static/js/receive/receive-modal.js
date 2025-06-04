// receive-modal.js - 입고 등록 모달 JavaScript (개선된 버전)

// 전역 변수
let currentReceiveData = {};
let availableItems = [];
let availableWarehouses = [];
let availableSuppliers = [];
let pendingOrders = [];

// 모달 열기 함수 - 전역으로 노출
window.openReceiveModal = function() {
    console.log('📦 입고 등록 모달 열기');
    
    // 모달 초기화
    resetReceiveModal();
    
    // 필요한 데이터 로드
    loadReceiveModalData();
    
    // 모달 표시
    const modalElement = document.getElementById('receiveRegisterModal');
    if (modalElement) {
        const modal = new bootstrap.Modal(modalElement);
        modal.show();
    }
};

// 모달 데이터 로드 (최적화된 버전)
async function loadReceiveModalData() {
    try {
        console.log('📡 입고 모달 데이터 로딩 시작');
        
        // 병렬로 데이터 로드 (빠른 로딩을 위해)
        const [itemsPromise, warehousesPromise, suppliersPromise, ordersPromise] = [
            loadItems(),
            loadWarehouses(), 
            loadSuppliers(),
            loadPendingOrders()
        ];
        
        // 모든 데이터 로딩 완료 대기
        await Promise.all([itemsPromise, warehousesPromise, suppliersPromise, ordersPromise]);
        
        console.log('✅ 모달 데이터 로딩 완료');
        console.log('  - 품목:', availableItems.length + '개');
        console.log('  - 창고:', availableWarehouses.length + '개');
        console.log('  - 공급업체:', availableSuppliers.length + '개');
        console.log('  - 미완료 발주:', pendingOrders.length + '개');
        
    } catch (error) {
        console.error('❌ 모달 데이터 로딩 실패:', error);
        showAlert('데이터 로딩에 실패했습니다.', 'danger');
    }
}

// 품목 데이터 로드
async function loadItems() {
    try {
        const response = await fetch('/api/receive/items');
        if (!response.ok) throw new Error('품목 데이터 로드 실패');
        
        availableItems = await response.json();
        updateItemOptions();
    } catch (error) {
        console.error('품목 로드 실패:', error);
        availableItems = [];
    }
}

// 창고 데이터 로드
async function loadWarehouses() {
    try {
        const response = await fetch('/api/receive/warehouses');
        if (!response.ok) throw new Error('창고 데이터 로드 실패');
        
        availableWarehouses = await response.json();
        updateWarehouseOptions();
    } catch (error) {
        console.error('창고 로드 실패:', error);
        availableWarehouses = [];
    }
}

// 공급업체 데이터 로드
async function loadSuppliers() {
    try {
        const response = await fetch('/api/receive/suppliers');
        if (!response.ok) throw new Error('공급업체 데이터 로드 실패');
        
        availableSuppliers = await response.json();
        updateSupplierOptions();
    } catch (error) {
        console.error('공급업체 로드 실패:', error);
        availableSuppliers = [];
    }
}

// 미완료 발주 데이터 로드
async function loadPendingOrders() {
    try {
        const response = await fetch('/api/receive/pending-orders');
        if (!response.ok) throw new Error('발주 데이터 로드 실패');
        
        pendingOrders = await response.json();
        updatePendingOrderOptions();
    } catch (error) {
        console.error('발주 로드 실패:', error);
        pendingOrders = [];
    }
}

// 품목 옵션 업데이트
function updateItemOptions() {
    const itemSelect = document.getElementById('itemSelect');
    if (itemSelect) {
        itemSelect.innerHTML = '<option value="">품목을 선택하세요</option>';
        
        availableItems.forEach(item => {
            const option = document.createElement('option');
            option.value = item.id;
            option.textContent = `[${item.code}] ${item.name}`;
            option.dataset.unit = item.unit;
            option.dataset.type = item.type;
            itemSelect.appendChild(option);
        });
    }
}

// 창고 옵션 업데이트
function updateWarehouseOptions() {
    const warehouseSelect = document.getElementById('warehouseSelect');
    if (warehouseSelect) {
        warehouseSelect.innerHTML = '<option value="">창고를 선택하세요</option>';
        
        availableWarehouses.forEach(warehouse => {
            const option = document.createElement('option');
            option.value = warehouse.id;
            option.textContent = `[${warehouse.code}] ${warehouse.name}`;
            option.dataset.type = warehouse.type;
            warehouseSelect.appendChild(option);
        });
    }
}

// 공급업체 옵션 업데이트
function updateSupplierOptions() {
    const supplierSelect = document.getElementById('supplierSelect');
    if (supplierSelect) {
        supplierSelect.innerHTML = '<option value="">공급업체를 선택하세요</option>';
        
        availableSuppliers.forEach(supplier => {
            const option = document.createElement('option');
            option.value = supplier.companyId;
            option.textContent = supplier.companyName;
            
            // 담당자 정보를 data 속성에 저장
            option.dataset.supplierData = JSON.stringify({
                companyId: supplier.companyId,
                managerName: supplier.managerName || '담당자 미지정',
                managerEmail: supplier.managerEmail || '',
                managerPhone: supplier.managerPhone || ''
            });
            
            supplierSelect.appendChild(option);
        });
    }
}

// 미완료 발주 옵션 업데이트 (수정된 버전)
function updatePendingOrderOptions() {
    const orderSelect = document.getElementById('pendingOrderSelect');
    if (orderSelect) {
        orderSelect.innerHTML = '<option value="">발주를 선택하세요</option>';
        
        pendingOrders.forEach(order => {
            const option = document.createElement('option');
            option.value = order.orderLineItemId;
            option.textContent = `${order.orderNum || '주문번호 없음'} - [${order.itemCode}] ${order.itemName} (${order.unitQty}${order.itemUnit || ''})`;
            
            // 발주 데이터를 JSON 형태로 저장
            option.dataset.orderData = JSON.stringify({
                orderLineItemId: order.orderLineItemId,
                itemId: order.itemId,
                itemCode: order.itemCode,
                itemName: order.itemName,
                unitQty: order.unitQty,
                receivedQty: order.receivedQty || 0,
                remainingQty: order.remainingQty || order.unitQty,
                unit: order.itemUnit,
                companyId: order.companyId,
                companyName: order.companyName,
                managerName: order.managerName || '담당자 미지정'
            });
            
            orderSelect.appendChild(option);
        });
    }
}

// 입고 유형 변경 처리
window.toggleReceiveType = function() {
    const orderBased = document.getElementById('receiveTypeOrder').checked;
    const orderSection = document.getElementById('orderBasedSection');
    const independentSection = document.getElementById('independentSection');
    const remainingQtyText = document.getElementById('remainingQtyText');
    
    if (orderBased) {
        orderSection.style.display = 'block';
        independentSection.style.display = 'none';
        remainingQtyText.style.display = 'block';
        
        // 발주 목록이 이미 로드되어 있으면 표시, 아니면 로드
        if (pendingOrders.length === 0) {
            loadPendingOrders();
        }
        
        // 독립적 입고 필드 초기화
        resetIndependentFields();
    } else {
        orderSection.style.display = 'none';
        independentSection.style.display = 'block';
        remainingQtyText.style.display = 'none';
        
        // 발주 기반 필드 초기화
        resetOrderBasedFields();
    }
};

// 발주 선택 시 자동 데이터 입력 (수정된 버전)
window.onPendingOrderSelect = function() {
    const select = document.getElementById('pendingOrderSelect');
    const selectedOption = select.selectedOptions[0];
    
    if (!selectedOption || !selectedOption.value) {
        clearOrderBasedData();
        return;
    }
    
    try {
        // 선택된 발주의 데이터를 자동으로 입력
        const orderData = JSON.parse(selectedOption.dataset.orderData || '{}');
        console.log('선택된 발주 데이터:', orderData);
        
        // 숨겨진 필드 설정
        document.getElementById('orderLineItemId').value = orderData.orderLineItemId || '';
        document.getElementById('selectedItemCode').value = orderData.itemCode || '';
        document.getElementById('selectedItemName').value = orderData.itemName || '';
        document.getElementById('selectedCompanyId').value = orderData.companyId || '';
        
        // 수량 정보 표시
        const remainingQty = orderData.remainingQty || orderData.unitQty || 0;
        document.getElementById('remainingQty').textContent = remainingQty.toLocaleString();
        
        const quantityInput = document.getElementById('quantityInput');
        if (quantityInput) {
            quantityInput.max = remainingQty;
            quantityInput.value = remainingQty;
        }
        
        // 담당자 정보 표시 (이메일, 연락처는 편집 가능하도록 유지)
        const managerNameInput = document.getElementById('managerNameInput');
        const managerEmailInput = document.getElementById('managerEmailInput');
        const managerPhoneInput = document.getElementById('managerPhoneInput');
        
        if (managerNameInput) {
            managerNameInput.value = orderData.managerName || '담당자 미지정';
        }
        
        // 이메일과 연락처는 기존 값이 있으면 표시하되, 편집 가능하도록 유지
        if (managerEmailInput) {
            managerEmailInput.value = orderData.managerEmail || '';
            managerEmailInput.placeholder = '이메일을 입력하세요';
        }
        
        if (managerPhoneInput) {
            managerPhoneInput.value = orderData.managerPhone || '';
            managerPhoneInput.placeholder = '연락처를 입력하세요';
        }
        
        console.log('발주 정보 자동 입력 완료 (이메일/연락처 편집 가능)');
    } catch (error) {
        console.error('발주 데이터 파싱 오류:', error);
        clearOrderBasedData();
    }
};

// 공급업체 선택 시 담당자 정보 자동 입력 (수정된 버전)
window.onSupplierSelect = function() {
    const select = document.getElementById('supplierSelect');
    const selectedOption = select.selectedOptions[0];
    
    if (!selectedOption || !selectedOption.value) {
        clearManagerInfo();
        return;
    }
    
    try {
        const supplierData = JSON.parse(selectedOption.dataset.supplierData || '{}');
        console.log('선택된 공급업체 데이터:', supplierData);
        
        // 회사 ID 저장
        document.getElementById('selectedCompanyId').value = supplierData.companyId || '';
        
        // 담당자 정보 자동 입력 (편집 가능하도록)
        const managerNameInput = document.getElementById('managerNameInput');
        const managerEmailInput = document.getElementById('managerEmailInput');
        const managerPhoneInput = document.getElementById('managerPhoneInput');
        
        if (managerNameInput) {
            managerNameInput.value = supplierData.managerName || '담당자 미지정';
        }
        
        // 이메일과 연락처는 기존 값 표시하되 편집 가능
        if (managerEmailInput) {
            managerEmailInput.value = supplierData.managerEmail || '';
            managerEmailInput.placeholder = '이메일을 입력하세요';
        }
        
        if (managerPhoneInput) {
            managerPhoneInput.value = supplierData.managerPhone || '';
            managerPhoneInput.placeholder = '연락처를 입력하세요';
        }
        
        console.log('공급업체 담당자 정보 자동 입력 완료 (이메일/연락처 편집 가능)');
    } catch (error) {
        console.error('공급업체 데이터 파싱 오류:', error);
        clearManagerInfo();
    }
};

// 입고 등록 제출
window.submitReceive = async function() {
    try {
        console.log('📦 입고 등록 제출 시작');
        
        // 폼 유효성 검사
        const receiveData = collectReceiveFormData();
        if (!validateReceiveData(receiveData)) {
            return;
        }
        
        console.log('📤 입고 데이터 전송:', receiveData);
        
        // API 호출
        const response = await fetch('/api/receive', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(receiveData)
        });
        
        const result = await response.json();
        
        if (response.ok && result.status === 'success') {
            console.log('✅ 입고 등록 성공');
            showAlert(result.message, 'success');
            
            // 모달 닫기
            const modal = bootstrap.Modal.getInstance(document.getElementById('receiveRegisterModal'));
            if (modal) {
                modal.hide();
            }
            
            // 테이블 새로고침
            if (typeof window.refreshTable === 'function') {
                window.refreshTable();
            }
            
        } else {
            console.error('❌ 입고 등록 실패:', result.message);
            showAlert(result.message || '입고 등록에 실패했습니다.', 'danger');
        }
        
    } catch (error) {
        console.error('❌ 입고 등록 오류:', error);
        showAlert('입고 등록 중 오류가 발생했습니다.', 'danger');
    }
};

// 폼 데이터 수집 (수정된 버전)
function collectReceiveFormData() {
    const receiveType = document.querySelector('input[name="receiveType"]:checked')?.value;
    
    // 수량 입력 필드를 입고 유형에 따라 선택
    let quantity;
    if (receiveType === 'ORDER_BASED') {
        // 발주 기반일 때는 quantityInput 사용
        quantity = parseInt(document.getElementById('quantityInput')?.value);
    } else {
        // 독립적 입고일 때는 quantityInputIndependent 사용
        quantity = parseInt(document.getElementById('quantityInputIndependent')?.value);
    }
    
    // 품목 ID 결정
    let itemId;
    if (receiveType === 'ORDER_BASED') {
        // 발주 기반일 때는 선택된 발주의 품목 ID 사용
        const orderSelect = document.getElementById('pendingOrderSelect');
        const selectedOrderOption = orderSelect?.selectedOptions[0];
        if (selectedOrderOption && selectedOrderOption.dataset.orderData) {
            try {
                const orderData = JSON.parse(selectedOrderOption.dataset.orderData);
                itemId = parseInt(orderData.itemId);
            } catch (e) {
                console.error('발주 데이터 파싱 오류:', e);
                itemId = null;
            }
        }
    } else {
        // 독립적 입고일 때는 직접 선택한 품목 사용
        itemId = parseInt(document.getElementById('itemSelect')?.value);
    }
    
    const data = {
        receiveType: receiveType,
        itemId: itemId,
        warehouseId: parseInt(document.getElementById('warehouseSelect')?.value),
        quantity: quantity,
        note: document.getElementById('noteInput')?.value?.trim()
    };
    
    // 발주 기반인 경우 추가 정보
    if (receiveType === 'ORDER_BASED') {
        const orderLineItemId = document.getElementById('pendingOrderSelect')?.value;
        if (orderLineItemId) {
            data.orderLineItemId = parseInt(orderLineItemId);
        }
    }
    
    // 독립적 입고인 경우 공급업체 정보
    if (receiveType === 'INDEPENDENT') {
        const supplierId = document.getElementById('supplierSelect')?.value;
        if (supplierId) {
            data.supplierId = parseInt(supplierId);
        }
    }
    
    console.log('수집된 폼 데이터:', data);
    return data;
}

// 데이터 유효성 검사 (수정된 버전)
function validateReceiveData(data) {
    console.log('유효성 검사 데이터:', data);
    
    if (!data.receiveType) {
        showAlert('입고 유형을 선택해주세요.', 'warning');
        return false;
    }
    
    if (!data.itemId || isNaN(data.itemId)) {
        if (data.receiveType === 'ORDER_BASED') {
            showAlert('발주를 선택해주세요.', 'warning');
        } else {
            showAlert('품목을 선택해주세요.', 'warning');
        }
        return false;
    }
    
    if (!data.warehouseId || isNaN(data.warehouseId)) {
        showAlert('창고를 선택해주세요.', 'warning');
        return false;
    }
    
    if (!data.quantity || isNaN(data.quantity) || data.quantity <= 0) {
        showAlert('올바른 입고 수량을 입력해주세요.', 'warning');
        return false;
    }
    
    if (data.quantity > 10000) {
        showAlert('입고 수량이 너무 큽니다. (최대 10,000개)', 'warning');
        return false;
    }
    
    // 발주기반 입고인 경우 orderLineItemId 필수
    if (data.receiveType === 'ORDER_BASED' && (!data.orderLineItemId || isNaN(data.orderLineItemId))) {
        showAlert('발주를 선택해주세요.', 'warning');
        return false;
    }
    
    console.log('✅ 유효성 검사 통과');
    return true;
}

// 모달 초기화
function resetReceiveModal() {
    console.log('🧹 입고 모달 초기화');
    
    // 폼 리셋
    const form = document.getElementById('receiveForm');
    if (form) {
        form.reset();
    }
    
    // 기본값 설정 (독립적 입고)
    const independentRadio = document.getElementById('receiveTypeIndependent');
    if (independentRadio) {
        independentRadio.checked = true;
        window.toggleReceiveType(); // 초기 상태 설정
    }
    
    // 담당자 정보 초기화
    resetManagerFields();
    
    // 숨겨진 필드 초기화
    clearHiddenFields();
}

// 발주 기반 필드 초기화
function resetOrderBasedFields() {
    const pendingOrderSelect = document.getElementById('pendingOrderSelect');
    if (pendingOrderSelect) {
        pendingOrderSelect.value = '';
    }
    clearOrderBasedData();
}

// 독립적 입고 필드 초기화
function resetIndependentFields() {
    const selects = ['itemSelect', 'supplierSelect'];
    selects.forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.value = '';
        }
    });
    
    // 독립적 입고용 수량 입력 필드 초기화
    const quantityInputIndependent = document.getElementById('quantityInputIndependent');
    if (quantityInputIndependent) {
        quantityInputIndependent.value = '';
        quantityInputIndependent.removeAttribute('max');
    }
    
    resetManagerFields();
    document.getElementById('selectedCompanyId').value = '';
}

// 담당자 정보 초기화
function resetManagerFields() {
    const managerFields = ['managerNameInput', 'managerEmailInput', 'managerPhoneInput'];
    managerFields.forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.value = '';
        }
    });
}

// 숨겨진 필드 초기화
function clearHiddenFields() {
    const hiddenFields = ['orderLineItemId', 'selectedItemCode', 'selectedItemName', 'selectedCompanyId'];
    hiddenFields.forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.value = '';
        }
    });
}

// 담당자 정보 초기화
function clearManagerInfo() {
    resetManagerFields();
    document.getElementById('selectedCompanyId').value = '';
}

// 발주 기반 데이터 초기화
function clearOrderBasedData() {
    const fields = ['orderLineItemId', 'selectedItemCode', 'selectedItemName', 'selectedCompanyId'];
    fields.forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.value = '';
        }
    });
    
    // 발주 기반용 수량 입력 필드 초기화
    const quantityInput = document.getElementById('quantityInput');
    if (quantityInput) {
        quantityInput.max = 10000;
        quantityInput.value = '';
    }
    
    const remainingQty = document.getElementById('remainingQty');
    if (remainingQty) {
        remainingQty.textContent = '0';
    }
    
    clearManagerInfo();
}

// 알림 표시 함수
function showAlert(message, type = 'info') {
    console.log(`알림 (${type}): ${message}`);
    
    // 간단한 alert 사용 (나중에 Toast로 교체 가능)
    alert(message);
}

// DOM 로드 완료 시 이벤트 리스너 등록
document.addEventListener('DOMContentLoaded', function() {
    console.log('📦 입고 모달 JavaScript 로드 완료');
    
    // 입고 유형 라디오 버튼 이벤트
    const receiveTypeRadios = document.querySelectorAll('input[name="receiveType"]');
    receiveTypeRadios.forEach(radio => {
        radio.addEventListener('change', window.toggleReceiveType);
    });
    
    // 발주 선택 이벤트
    const pendingOrderSelect = document.getElementById('pendingOrderSelect');
    if (pendingOrderSelect) {
        pendingOrderSelect.addEventListener('change', window.onPendingOrderSelect);
    }
    
    // 공급업체 선택 이벤트
    const supplierSelect = document.getElementById('supplierSelect');
    if (supplierSelect) {
        supplierSelect.addEventListener('change', window.onSupplierSelect);
    }
    
    console.log('✅ 입고 모달 이벤트 리스너 등록 완료');
});

// 전역 함수 노출
window.toggleReceiveType = window.toggleReceiveType;
window.onPendingOrderSelect = window.onPendingOrderSelect;
window.onSupplierSelect = window.onSupplierSelect;
window.submitReceive = window.submitReceive;