// order.js

// 상세 보기 모달 호출
function openOrderDetailModal(order) {
    if (!document.getElementById('orderDetailModal')) {
        fetch('/pages/order/order-detail-modal.html')
            .then(res => res.text())
            .then(html => {
                document.getElementById('orderDetailModalWrapper').innerHTML = html;
                setTimeout(() => openOrderDetailModal(order), 100);
            });
        return;
    }

    document.getElementById('detailCustomer').textContent = order.customer;
    document.getElementById('detailOrderDate').textContent = order.order_date;
    document.getElementById('detailDueDate').textContent = order.due_date;
    document.getElementById('detailManager').textContent = order.manager;
    document.getElementById('detailNote').textContent = order.note;

    const tbody = document.getElementById('orderDetailItems');
    tbody.innerHTML = '';
    order.items.forEach(item => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${item.item_code}</td>
            <td>${item.item_name}</td>
            <td>${item.quantity}</td>
            <td>${item.unit}</td>
            <td>${Number(item.unit_price).toLocaleString()}</td>
            <td>${(item.quantity * item.unit_price).toLocaleString()}</td>
        `;
        tbody.appendChild(row);
    });

    const modal = new bootstrap.Modal(document.getElementById('orderDetailModal'));
    modal.show();
}

// 수정 모드
function openOrderEditModal(order) {
    if (!document.getElementById('orderRegisterModal')) {
        fetch('/pages/order/order-register-modal.html')
            .then(res => res.text())
            .then(html => {
                document.getElementById('orderModals').innerHTML = html;
                setTimeout(() => openOrderEditModal(order), 100);
            });
        return;
    }

    document.querySelector('[name="customer_id"]').value = order.customer_id;
    document.querySelector('[name="order_date"]').value = order.order_date;
    document.querySelector('[name="due_date"]').value = order.due_date;
    document.querySelector('[name="manager"]').value = order.manager;
    document.querySelector('[name="note"]').value = order.note;

    const tbody = document.querySelector("#orderItemTable tbody");
    tbody.innerHTML = '';
    order.items.forEach(item => {
        const row = document.createElement("tr");
        row.innerHTML = `
            <td>
                <select class="form-select" name="item_id[]" required>
                    <option value="">-- 선택 --</option>
                    <option value="P001">P001 - 완제품 A</option>
                    <option value="P002">P002 - 완제품 B</option>
                </select>
            </td>
            <td><input type="number" name="quantity[]" class="form-control" value="${item.quantity}" required oninput="calculateRowTotal(this)" /></td>
            <td><input type="text" name="unit[]" class="form-control" value="${item.unit}" required /></td>
            <td><input type="number" name="unit_price[]" class="form-control" value="${item.unit_price}" required oninput="calculateRowTotal(this)" /></td>
            <td><input type="text" class="form-control total" readonly value="${(item.quantity * item.unit_price).toLocaleString()}" /></td>
            <td><button type="button" class="btn btn-sm btn-outline-danger" onclick="removeOrderItemRow(this)">삭제</button></td>
        `;
        tbody.appendChild(row);
        row.querySelector('select[name="item_id[]"]').value = item.item_id;
    });

    document.getElementById("orderForm").action = `/order/update/${order.id}`;
    document.getElementById("submitBtn").textContent = "수정";

    const modal = new bootstrap.Modal(document.getElementById('orderRegisterModal'));
    modal.show();
}

// 주문 추가 모달
function openOrderAddModal() {
    if (!document.getElementById('orderRegisterModal')) return;

    const form = document.getElementById('orderForm');
    if (form) {
        form.reset();
        form.action = '/order/add';
    }

    document.getElementById('submitBtn').textContent = '등록';

    // 품목 테이블 초기화 (기본 한 줄)
    const tbody = document.querySelector("#orderItemTable tbody");
    tbody.innerHTML = `
        <tr>
            <td>
                <select class="form-select" name="item_id[]" required>
                    <option value="">-- 선택 --</option>
                    <option value="P001">P001 - 완제품 A</option>
                    <option value="P002">P002 - 완제품 B</option>
                </select>
            </td>
            <td><input type="number" name="quantity[]" class="form-control" required oninput="calculateRowTotal(this)" /></td>
            <td><input type="text" name="unit[]" class="form-control" value="EA" required /></td>
            <td><input type="number" name="unit_price[]" class="form-control" required oninput="calculateRowTotal(this)" /></td>
            <td><input type="text" class="form-control total" readonly value="0" /></td>
            <td><button type="button" class="btn btn-sm btn-outline-danger" onclick="removeOrderItemRow(this)">삭제</button></td>
        </tr>
    `;
}
