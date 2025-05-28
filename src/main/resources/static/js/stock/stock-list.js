// ===== stock-list.js (디버깅 버전) =====
const StockList = {
    currentPage: 0,
    pageSize: globalPageSize || 10, // globalPageSize가 없을 경우 대비

    init() {
        console.log('StockList 초기화 중...');
        try {
            this.setupTableEvents();
            console.log('테이블 이벤트 설정 완료');
            
            this.loadStockData();
            console.log('데이터 로딩 시작');
        } catch (error) {
            console.error('StockList 초기화 오류:', error);
        }
    },

    setupTableEvents() {
        console.log('테이블 이벤트 설정 중...');
        
        // 정렬 가능한 컬럼 클릭 이벤트
        const sortableElements = document.querySelectorAll('.sortable');
        console.log('정렬 가능한 요소 수:', sortableElements.length);
        
        sortableElements.forEach(th => {
            th.style.cursor = 'pointer';
            th.addEventListener('click', (e) => {
                const sortField = th.dataset.sort;
                console.log('정렬 필드 클릭:', sortField);
                this.toggleSort(sortField);
            });
        });
        
        // 전체 선택 체크박스
        const selectAllCheckbox = document.getElementById('selectAll');
        if (selectAllCheckbox) {
            console.log('전체 선택 체크박스 찾음');
            selectAllCheckbox.addEventListener('change', (e) => {
                document.querySelectorAll('.stock-checkbox').forEach(cb => {
                    cb.checked = e.target.checked;
                });
            });
        } else {
            console.warn('전체 선택 체크박스를 찾을 수 없음');
        }
        
        this.setupDetailButtonListener();
    },

    setupDetailButtonListener() {
        const tableBody = document.getElementById('stockTableBody');
        if (tableBody) {
            console.log('테이블 바디 찾음');
            tableBody.addEventListener('click', (e) => {
                const detailButton = e.target.closest('.stock-detail-btn');
                if (detailButton) {
                    const stockId = parseInt(detailButton.dataset.stockId);
                    console.log('상세 버튼 클릭, stockId:', stockId);
                    if (stockId && typeof StockModal !== 'undefined') {
                        StockModal.open(stockId);
                    } else {
                        console.error('StockModal이 정의되지 않았거나 stockId가 유효하지 않음');
                    }
                }
            });
        } else {
            console.error('stockTableBody 요소를 찾을 수 없음');
        }
    },

    toggleSort(field) {
        console.log('정렬 토글:', field);
        if (typeof StockState === 'undefined') {
            console.error('StockState가 정의되지 않음');
            return;
        }
        
        const currentSort = StockState.sortBy.split(',');
        if (currentSort[0] === field) {
            StockState.sortBy = field + ',' + (currentSort[1] === 'ASC' ? 'DESC' : 'ASC');
        } else {
            StockState.sortBy = field + ',ASC';
        }
        this.loadStockData();
    },

    loadStockData() {
        console.log('데이터 로딩 시작...');
        
        if (typeof StockState === 'undefined') {
            console.error('StockState가 정의되지 않음');
            return;
        }
        
        const params = new URLSearchParams({
            page: StockState.currentPage,
            size: StockState.pageSize,
            sort: StockState.sortBy
        });
        
        // 필터 추가
        if (StockState.filters.keyword) params.append('keyword', StockState.filters.keyword);
        if (StockState.filters.warehouse) params.append('whs', StockState.filters.warehouse);
        if (StockState.filters.itemType) params.append('itemType', StockState.filters.itemType);
        if (StockState.filters.stockStatus) params.append('stockStatus', StockState.filters.stockStatus);

        const url = `/api/stocks?${params}`;
        console.log("✅ API 요청 URL:", url);

        fetch(url)
            .then(response => {
                console.log("응답 상태:", response.status);
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then(pageData => {
                console.log("✅ 받은 데이터:", pageData);
                console.log("데이터 개수:", pageData.content?.length);
                
                this.updateTable(pageData.content);
                this.updatePaginationControls(pageData);
                
                StockState.currentPage = pageData.number;
                console.log("데이터 로딩 완료");
            })
            .catch(error => {
                console.error('❌ 재고 목록 조회 중 오류 발생:', error);
                const tbody = document.getElementById('stockTableBody');
                if (tbody) {
                    tbody.innerHTML = `<tr><td colspan="12" class="text-center text-danger py-5">데이터를 불러오는데 실패했습니다: ${error.message}</td></tr>`;
                }
                const paginationNav = document.getElementById('paginationNav');
                if(paginationNav) paginationNav.style.display = 'none';
                const paginationInfoDiv = document.getElementById('paginationInfoDiv');
                 if(paginationInfoDiv) paginationInfoDiv.innerHTML = '';
            });
    },

    updateTable(stocks) {
        console.log('테이블 업데이트 시작, 데이터 수:', stocks?.length);
        
        const tbody = document.getElementById('stockTableBody');
        if (!tbody) {
            console.error('stockTableBody 요소를 찾을 수 없음');
            return;
        }

        tbody.innerHTML = '';

        if (!stocks || stocks.length === 0) {
            console.log('데이터가 없음 - 빈 테이블 표시');
            const noDataRow = `
                <tr class="no-data-row">
                    <td colspan="12" class="text-center text-muted py-5">
                        <div class="d-flex flex-column align-items-center">
                            <i class="fas fa-box-open fa-3x mb-3 text-secondary"></i>
                            <h5 class="mb-2">표시할 재고가 없습니다</h5>
                            <p class="mb-0">검색 조건을 확인하거나 데이터를 추가해주세요.</p>
                        </div>
                    </td>
                </tr>`;
            tbody.insertAdjacentHTML('beforeend', noDataRow);
            return;
        }

        console.log('데이터 있음 - 테이블 행 생성 중...');
        
        stocks.forEach((stock, index) => {
            try {
                const actualLastIn = stock.actualLastInAt ? StockUtils.formatDate(stock.actualLastInAt) : 
                                    (stock.firstAt ? StockUtils.formatDate(stock.firstAt) : 
                                    (stock.formattedLastInDate || '-'));
                const actualLastOut = stock.actualLastOutAt ? StockUtils.formatDate(stock.actualLastOutAt) : 
                                     (stock.lastAt ? StockUtils.formatDate(stock.lastAt) : 
                                     (stock.formattedLastOutDate || '-'));
                const itemUnit = stock.itemUnit || stock.unit || 'EA';

                const rowHtml = `
                    <tr>
                        <td><input type="checkbox" class="form-check-input stock-checkbox" data-stock-id="${stock.stockId}"></td>
                        <td>${StockState.currentPage * this.pageSize + index + 1}</td>
                        <td>${StockUtils.escapeHtml(stock.itemCode)}</td>
                        <td>${StockUtils.escapeHtml(stock.itemName)}</td>
                        <td>${StockUtils.escapeHtml(stock.itemType === 'raw' ? '자재' : (stock.itemType === 'product' ? '완제품' : stock.itemType))}</td>
                        <td>${StockUtils.escapeHtml(stock.warehouseName)}</td>
                        <td class="text-end"><strong>${StockUtils.formatNumber(stock.quantity)}</strong></td>
                        <td>${StockUtils.escapeHtml(itemUnit)}</td>
                        <td>${actualLastIn}</td>
                        <td>${actualLastOut}</td>
                        <td>${this.getStockStatusBadge(stock)}</td>
                        <td>
                            <button class="btn btn-info btn-sm stock-detail-btn"
                                    data-stock-id="${stock.stockId}">
                                상세
                            </button>
                        </td>
                    </tr>`;
                tbody.insertAdjacentHTML('beforeend', rowHtml);
            } catch (error) {
                console.error(`행 ${index} 생성 중 오류:`, error, stock);
            }
        });
        
        console.log('테이블 업데이트 완료');
    },

    getStockStatusBadge(stock) {
        if (typeof StockUtils === 'undefined') {
            console.error('StockUtils가 정의되지 않음');
            return '<span class="badge bg-secondary">-</span>';
        }
        
        const quantity = Number(stock.quantity);

        if (quantity === 0) {
            return '<span class="badge bg-danger">재고없음</span>';
        } else if (stock.isBelowSafety) {
            return '<span class="badge bg-warning">안전재고미달</span>';
        } else {
            return '<span class="badge bg-success">정상</span>';
        }
    },

    updatePaginationControls(pageData) {
        console.log('페이지네이션 업데이트');
        // 기존 코드와 동일하지만 간소화
        const paginationNav = document.getElementById('paginationNav');
        const paginationInfoDiv = document.getElementById('paginationInfoDiv');
        
        if (paginationInfoDiv) {
            paginationInfoDiv.innerHTML = `
                <span class="text-muted">
                    총 ${pageData.totalElements}개 
                    (현재 ${pageData.number + 1} / ${pageData.totalPages} 페이지)
                </span>`;
        }
        
        if (pageData.totalPages <= 1) {
            if (paginationNav) paginationNav.style.display = 'none';
            return;
        }
        
        // 간단한 페이지네이션만 표시
        if (paginationNav) paginationNav.style.display = 'block';
    },

    changePage(pageNumber) {
        console.log('페이지 변경:', pageNumber);
        this.currentPage = pageNumber;
        if (typeof StockState !== 'undefined') {
            StockState.currentPage = pageNumber;
        }
        this.loadStockData();
    },

    refresh() {
        console.log('목록 새로고침');
        this.currentPage = 0;
        if (typeof StockState !== 'undefined') {
            StockState.currentPage = 0;
        }
        this.loadStockData();
    }
};