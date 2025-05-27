// 재고 관리 메인 JavaScript

// 전역 변수
let currentPage = 0;
const pageSize = 10;

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    StockList.init();
    StockSearch.init();
    StockModal.init();
});

// 전역 함수 (HTML onclick에서 호출)
function openStockDetailModal(stock) {
    StockModal.open(stock);
}