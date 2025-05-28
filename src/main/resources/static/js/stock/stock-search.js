// ===== stock-search.js =====
const StockSearch = {
    searchInput: null,
    warehouseSelect: null,

    init() {
        console.log('StockSearch 초기화');
        this.searchInput = document.getElementById('searchInput'); 
        this.warehouseSelect = document.getElementById('warehouseSelect');
        this.setupEvents();
    },

    setupEvents() {
        const searchBtn = document.getElementById('searchBtn');
        if (searchBtn) {
            searchBtn.addEventListener('click', () => this.performSearch());
        }

        const resetBtn = document.getElementById('resetBtn');
        if (resetBtn) {
            resetBtn.addEventListener('click', () => this.reset());
        }

        if (this.searchInput) {
            this.searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.performSearch();
                }
            });
        }

        if (this.warehouseSelect) {
            this.warehouseSelect.addEventListener('change', () => this.performSearch());
        }

        ['itemTypeFilter', 'stockStatusFilter'].forEach(id => {
            const element = document.getElementById(id);
            if (element) {
                element.addEventListener('change', () => this.performSearch());
            }
        });
    },

    performSearch() {
        StockState.filters.keyword = this.getKeyword();
        StockState.filters.warehouse = this.getWarehouseCode();
        StockState.filters.itemType = document.getElementById('itemTypeFilter')?.value || '';
        StockState.filters.stockStatus = document.getElementById('stockStatusFilter')?.value || '';
        StockState.currentPage = 0;
        
        console.log('검색 필터:', StockState.filters);
        StockList.refresh();
    },

    reset() {
        if (this.searchInput) this.searchInput.value = '';
        if (this.warehouseSelect) this.warehouseSelect.value = '';
        
        const itemTypeFilter = document.getElementById('itemTypeFilter');
        if (itemTypeFilter) itemTypeFilter.value = '';
        
        const stockStatusFilter = document.getElementById('stockStatusFilter');
        if (stockStatusFilter) stockStatusFilter.value = '';
        
        StockState.filters = {
            keyword: '',
            warehouse: '',
            itemType: '',
            stockStatus: ''
        };
        StockState.currentPage = 0;
        
        StockList.refresh();
    },

    getKeyword() {
        return this.searchInput ? this.searchInput.value.trim() : '';
    },

    getWarehouseCode() {
        return this.warehouseSelect ? this.warehouseSelect.value : '';
    }
};
