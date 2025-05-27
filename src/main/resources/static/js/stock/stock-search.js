const StockSearch = {
    searchInput: null,
    warehouseSelect: null,

    init() {
        this.searchInput = document.getElementById('searchInput'); // input에 id 추가
        this.warehouseSelect = document.getElementById('warehouseSelect'); // select에 id 추가
        this.setupEvents();
    },

    setupEvents() {
        const searchBtn = document.getElementById('searchBtn'); // button에 id 추가
        if (searchBtn) {
            searchBtn.addEventListener('click', () => this.performSearch());
        }

        if (this.searchInput) {
            this.searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.performSearch();
                }
            });
        }

        if (this.warehouseSelect) {
            // 초기 로딩 시 창고 목록이 동적으로 채워진 후 검색이 필요하면,
            // 창고 목록 로딩 완료 이벤트에 맞춰서 초기 검색을 하거나,
            // 사용자가 직접 검색 버튼을 누르도록 유도할 수 있음.
            // 여기서는 변경 시 바로 검색하도록 설정.
            this.warehouseSelect.addEventListener('change', () => this.performSearch());
        }
    },

    performSearch() {
        StockList.refresh(); // 검색 시 첫 페이지부터 다시 로드
    },

    getKeyword() {
        return this.searchInput ? this.searchInput.value.trim() : '';
    },

    getWarehouseCode() { // 메소드명 변경 및 반환값 일관성
        return this.warehouseSelect ? this.warehouseSelect.value : ''; // 선택된 value (창고 코드) 반환
    }
    // reset() 메소드는 필요시 추가
};