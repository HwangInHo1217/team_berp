/*
 * order.js
 * 주문 관리 모듈의 전체 JavaScript 로직을 담고 있는 파일입니다.
 * jQuery를 사용하여 DOM 조작, AJAX 요청, Modal 제어, 계산 등을 수행합니다.
 */

$(function() {
    // 초기 로딩: 고객사, 품목 목록 불러오기
    loadCompanies();
    loadItems();

    // +주문등록 버튼 클릭 시 폼 초기화 후 모달 오픈
    $('#openRegisterModal').click(function() {
        $('#registerForm')[0].reset();
        // 기존에 추가된 품목 행 제거 후 기본 1행 생성
        $('#itemsTable tbody').empty();
        addItemRow();
        // 총합계 초기화
        $('#totalAmount').text('0');
        $('#registerModal').modal('show');
    });

    // 고객사 선택 시 담당자(empName), 거래처담당자(companyEmpName) 자동 채움
    $('#companySelect').change(function() {
        const companyId = $(this).val();
        if (!companyId) {
            $('#empName, #companyEmpName').val('');
            return;
        }
        $.get(`/api/companies/${companyId}/info`, data => {
            $('#empName').val(data.empName);
            $('#companyEmpName').val(data.companyEmpName);
        });
    });

    // 품목 추가 버튼 클릭
    $('#addItem').click(() => addItemRow());

    // 동적으로 추가된 행 내 버튼, 셀렉트, 입력 이벤트 위임 처리
    $('#itemsTable')
        .on('click', '.removeItem', function() {
            // 품목 행 삭제
            $(this).closest('tr').remove();
            updateTotals();
        })
        .on('change', '.itemSelect', function() {
            // 품목 선택 시 단위(unit) 자동 로딩
            loadUnit($(this));
        })
        .on('input', '.unitPrice, .unitQty', function() {
            // 단가 또는 수량 입력 시 해당 행 합계 계산
            updateRowTotal($(this).closest('tr'));
        });

    // 주문 등록 저장
    $('#saveOrder').click(function() {
        const payload = $('#registerForm').serialize();
        $.post('/api/orders', payload)
            .done(() => location.reload())
            .fail(err => alert('저장 중 오류가 발생했습니다.'));        
    });

    // 상세 보기 버튼
    $('.detailBtn').click(function() {
        const orderId = $(this).data('id');
        $.get(`/api/orders/${orderId}`, data => {
            populateDetailModal(data);
            $('#detailModal').modal('show');
        });
    });

    // 수정 버튼
    $('.editBtn').click(function() {
        const orderId = $(this).data('id');
        $.get(`/api/orders/${orderId}`, data => {
            populateFixedForm(data);
            $('#fixedModal').modal('show');
        });
    });
    
    // 수정 저장
    $('#updateOrder').click(() => {
        const payload = $('#fixedForm').serialize();
        $.ajax({
            url: '/api/orders',
            method: 'PUT',
            data: payload
        })
        .done(() => location.reload())
        .fail(err => alert('수정 중 오류가 발생했습니다.'));
    });

    // 전체 선택 / 해제
    $('#selectAll').change(function() {
        $('.selectBox').prop('checked', this.checked);
    });

    // 선택 삭제
    $('#deleteSelected').click(function() {
        const ids = $('.selectBox:checked').map((i, el) => el.value).get();
        if (ids.length === 0) { alert('삭제할 주문을 선택하세요.'); return; }
        if (!confirm('선택한 주문을 삭제하시겠습니까?')) return;
        $.ajax({
            url: '/api/orders',
            method: 'DELETE',
            traditional: true,  // 배열 전송 지원
            data: { ids }
        })
        .done(() => location.reload())
        .fail(err => alert('삭제 중 오류가 발생했습니다.'));
    });

    // 검색 버튼
    $('#searchBtn').click(() => $('#filterForm').submit());
});

/**
 * 고객사 목록을 API에서 조회하여 select 요소에 옵션으로 추가합니다.
 */
function loadCompanies() {
    $.get('/api/companies', data => {
        const $select = $('#companySelect, #searchCompanySelect');
        $select.empty().append('<option value="">전체</option>');
        data.forEach(c => {
            $select.append(`<option value="${c.companyId}">${c.companyName}</option>`);
        });
    });
}

/**
 * 품목 목록을 API에서 조회하여 itemSelect 옵션으로 사용합니다.
 */
function loadItems() {
    $.get('/api/items', data => {
        window.ALL_ITEMS = data;  // 전역에 캐싱
        // 검색용 셀렉트
        const $search = $('#searchItemSelect');
        $search.empty().append('<option value="">전체</option>');
        data.forEach(i => $search.append(`<option value="${i.id}">${i.itemName}</option>`));
        
        // 등록/수정 모달의 첫 행이 없을 수 있어, addItemRow 시 ALL_ITEMS 사용
    });
}

/**
 * itemsTable에 새로운 행을 추가합니다.
 */
function addItemRow() {
    const rowId = Date.now();  // 유니크 ID
    const $tbody = $('#itemsTable tbody');
    const $tr = $(
        `<tr data-rowid="${rowId}">` +
            '<td>' +
                '<select name="orderLineItems[#index].itemId" class="form-control itemSelect" required>' +
                    '<option value="">선택</option>' +
                '</select>' +
            '</td>' +
            '<td><input type="text" name="orderLineItems[#index].unit" class="form-control unit" readonly /></td>' +
            '<td><input type="number" name="orderLineItems[#index].unitPrice" class="form-control unitPrice" step="0.01" required /></td>' +
            '<td><input type="number" name="orderLineItems[#index].unitQty" class="form-control unitQty" step="1" required /></td>' +
            '<td><input type="text" name="orderLineItems[#index].unitPriceall" class="form-control unitTotal" readonly /></td>' +
            '<td><button type="button" class="btn btn-danger removeItem">삭제</button></td>' +
        '</tr>'
    );
    // ALL_ITEMS를 이용해 옵션 추가
    ALL_ITEMS.forEach(i => {
        $tr.find('.itemSelect').append(`<option value="${i.id}" data-unit="${i.unit}">${i.itemName}</option>`);
    });
    $tbody.append($tr);
    refreshIndexes();
}

/**
 * 폼 내 배열 인덱스를 순서대로 갱신하여 Spring MVC binding 시 index 오류를 방지합니다.
 */
function refreshIndexes() {
    $('#itemsTable tbody tr').each(function(idx) {
        $(this).find('[name]').each(function() {
            const name = $(this).attr('name');
            const newName = name.replace(/\[.*?\]/, '[' + idx + ']');
            $(this).attr('name', newName);
        });
    });
}

/**
 * 해당 행의 itemSelect 변경 시 단위(unit)를 자동으로 채웁니다.
 */
function loadUnit($select) {
    const $tr = $select.closest('tr');
    const unit = $select.find('option:selected').data('unit') || '';
    $tr.find('.unit').val(unit);
}

/**
 * 단가와 수량이 입력된 후 행의 합계(unitPriceall)를 계산하고 갱신합니다.
 */
function updateRowTotal($tr) {
    const price = parseFloat($tr.find('.unitPrice').val()) || 0;
    const qty   = parseFloat($tr.find('.unitQty').val())   || 0;
    const total = (price * qty).toFixed(2);
    $tr.find('.unitTotal').val(total);
    updateTotals();
}

/**
 * 모든 행의 합계(unitTotal)를 합산하여 총합계(totalAmount)를 갱신합니다.
 */
function updateTotals() {
    let sum = 0;
    $('#itemsTable tbody tr').each(function() {
        sum += parseFloat($(this).find('.unitTotal').val()) || 0;
    });
    $('#totalAmount').text(sum.toFixed(2));
}

/**
 * 상세 모달을 주어진 data로 채워 줍니다.
 */
function populateDetailModal(data) {
    $('#detailOrderNum').text(data.orderNum);
    $('#detailOrderDate').text(data.orderDate);
    $('#detailCompanyName').text(data.companyName);
    $('#detailEmpName').text(data.empName);
    $('#detailCompanyEmp').text(data.companyEmpName);
    $('#detailOrderQty').text(data.orderLineItems.length);
    $('#detailAmount').text(data.amount.toFixed(2));
    
    // 품목 테이블 채우기
    const $tb = $('#detailItemsTable tbody').empty();
    data.orderLineItems.forEach(item => {
        $tb.append(
            `<tr>` +
                `<td>${item.itemName}</td>` +
                `<td>${item.unit}</td>` +
                `<td>${item.unitPrice.toFixed(2)}</td>` +
                `<td>${item.unitQty}</td>` +
                `<td>${item.unitPriceall.toFixed(2)}</td>` +
            `</tr>`
        );
    });
}

/**
 * 수정 모달의 폼(#fixedForm)을 주어진 data로 채워 줍니다.
 */
function populateFixedForm(data) {
    const $form = $('#fixedForm')[0];
    $form.reset();
    // 기본 정보
    $('#fixedOrderId').val(data.orderId);
    $('#fixedOrderDate').val(data.orderDate);
    $('#fixedCompanySelect').val(data.companyId).trigger('change');
    $('#fixedEmpName').val(data.empName);
    $('#fixedCompanyEmpName').val(data.companyEmpName);
    
    // 기존 행 삭제 후 새로 추가
    $('#fixedItemsTable tbody').empty();
    data.orderLineItems.forEach((item, idx) => {
        addItemRow();  // registerForm과 동일하지만 name prefix는 fixedForm으로 동일 구조여야 함
        const $tr = $('#fixedItemsTable tbody tr').last();
        $tr.find('.itemSelect').val(item.itemId).change();
        $tr.find('.unitPrice').val(item.unitPrice);
        $tr.find('.unitQty').val(item.unitQty);
        $tr.find('.unitTotal').val(item.unitPriceall.toFixed(2));
    });
    updateTotals();
}