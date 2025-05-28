
// ===== stock-filter.js =====
const StockFilter = {
    
    init() {
        console.log('StockFilter 초기화');
        this.loadDynamicFilters();
    },
    
    loadDynamicFilters() {
        this.loadWarehouses();
    },
    
    loadWarehouses() {
        fetch('/api/stocks/warehouses')
            .then(response => response.json())
            .then(warehouses => {
                const select = document.getElementById('warehouseSelect');
                if (select) {
                    select.innerHTML = '<option value="">전체 창고</option>';
                    
                    warehouses.forEach(wh => {
                        select.innerHTML += `
                            <option value="${wh.warehouseCode}">${wh.warehouseName}</option>
                        `;
                    });
                }
            })
            .catch(error => {
                console.error('창고 목록 로딩 실패:', error);
            });
    }
};