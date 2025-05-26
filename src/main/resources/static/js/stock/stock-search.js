// 재고 검색 기능

const StockSearch = {
    
    init() {
        this.setupEvents();
    },

    // 이벤트 설정
    setupEvents() {
        // 검색 버튼
        const searchBtn = document.querySelector('.btn-outline-secondary');
        if (searchBtn) {
            searchBtn.addEventListener('click', () => this.search());
        }

        // 엔터키 검색
        const searchInput = this.getSearchInput();
        if (searchInput) {
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.search();
                }
            });
        }

        // 창고 선택 변경
        const whsSelect = this.getWarehouseSelect();
        if (whsSelect) {
            whsSelect.addEventListener('change', () => this.search());
        }
    },

    // 검색 실행
    search() {
        const keyword = this.getKeyword();
        const warehouse = this.getWarehouse();
        
        // 검색 조건 표시
        this.showSearchCondition(keyword, warehouse);
        
        StockList.refresh();
    },

    // 검색 조건 표시
    showSearchCondition(keyword, warehouse) {
        let conditionText = '';
        
        if (keyword && warehouse !== '전체 창고') {
            conditionText = `"${keyword}" 키워드 + "${warehouse}" 창고`;
        } else if (keyword) {
            conditionText = `"${keyword}" 키워드`;
        } else if (warehouse !== '전체 창고') {
            conditionText = `"${warehouse}" 창고`;
        }
        
        // 검색 조건을 어딘가에 표시할 수 있음 (나중에 UI 추가시)
        if (conditionText) {
            console.log(`검색 조건: ${conditionText}`);
        }
    },

    // 검색어 가져오기
    getKeyword() {
        const input = this.getSearchInput();
        return input ? input.value.trim() : '';
    },

    // 창고 가져오기
    getWarehouse() {
        const select = this.getWarehouseSelect();
        return select ? select.value : '전체 창고';
    },

    // DOM 요소 가져오기
    getSearchInput() {
        return document.querySelector('input[placeholder*="품목명"]');
    },

    getWarehouseSelect() {
        return document.querySelector('select.form-select');
    },

    // 검색 조건 초기화
    reset() {
        const input = this.getSearchInput();
        const select = this.getWarehouseSelect();
        
        if (input) input.value = '';
        if (select) select.selectedIndex = 0;
        
        this.search();
    }
};