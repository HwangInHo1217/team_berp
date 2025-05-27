
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

