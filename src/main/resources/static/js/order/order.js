/**
 * order.js
 * - 모달 열기/닫기
 * - API 호출 (등록, 수정, 삭제, 조회)
 * - 동적 행 생성, 자동 계산
 */

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', () => {
  loadFilterData();   // 고객사, 품목 목록 로드
  loadOrderTable();   // 초기 주문 리스트 조회
});

// 모달 열기/닫기 공통 함수
function openRegisterModal() {
  fetchModal('registerModal');
}
function openUpdateModal(orderNum) {
  fetchOrderDetail(orderNum, populateUpdateModal);
}
function showDetail(orderNum) {
  fetchOrderDetail(orderNum, populateDetailModal);
}
function closeModal() {
  document.getElementById('modal-container').innerHTML = '';
}

// 고객사 변경 시 담당자/거래처담당자 자동 채움
function onCompanyChange(select) {
  const companyName = select.value;
  fetch(`/api/orders/company/${companyName}`)
    .then(res => res.json())
    .then(data => {
      document.getElementById(select.id === 'reg-company' ? 'reg-emp' : 'upd-emp').value = data.empName;
      document.getElementById(select.id === 'reg-company' ? 'reg-comp-emp' : 'upd-comp-emp').value = data.companyEmpName;
    });
}

// 행 추가 및 합계 계산
function addItemRow(bodyId) {
  // TODO: 동적 행 생성 후 이벤트 바인딩
}
function calcLineTotal(row) {
  // TODO: 단가 * 수량 계산하여 합계 표시
}

// 주문 CRUD 함수
function registerOrder(event) {
  event.preventDefault();
  // TODO: form 데이터 수집 -> API 호출
}
function updateOrder(event) {
  event.preventDefault();
  // TODO: form 데이터 수집 -> API 호출
}
function deleteSelected() {
  const checked = Array.from(document.querySelectorAll('.chk-one:checked'))
                     .map(cb => cb.value);
  if (checked.length === 0) return;
  fetch('/api/orders', { method: 'DELETE', body: JSON.stringify(checked) });
}
function loadOrderTable() {
  // TODO: 필터값 가져와서 조회
}
function searchOrders() {
  loadOrderTable();
}

// API 모달 로딩/데이터 파싱 헬퍼
function fetchModal(fragment) {
  fetch(`/orders/fragments/${fragment}`)
    .then(res => res.text())
    .then(html => document.getElementById('modal-container').innerHTML = html);
}
function fetchOrderDetail(orderNum, callback) {
  fetch(`/api/orders/${orderNum}`)
    .then(res => res.json())
    .then(callback);
}
function populateDetailModal(data) {
  document.getElementById('det-num').textContent = data.orderNum;
  // TODO: 나머지 필드 채우기
  document.getElementById('modal-container').innerHTML = document.querySelector('[th\:fragment="detailModal"]').outerHTML;
}
function populateUpdateModal(data) {
  // TODO: updateModal 채우기
  document.getElementById('modal-container').innerHTML = document.querySelector('[th\:fragment="updateModal"]').outerHTML;
}