// 재고 목록 관리

const StockList = {
    
    init() {
        this.load();
        this.setupDetailButtons();
    },

    // 상세 버튼 이벤트 설정
    setupDetailButtons() {
        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('stock-detail-btn')) {
                const btn = e.target;
                const stockData = {
                    item_code: btn.dataset.itemCode,
                    item_name: btn.dataset.itemName,
                    warehouse: btn.dataset.warehouse,
                    qty: parseInt(btn.dataset.qty),
                    unit: 'EA',
                    last_in: btn.dataset.lastIn,
                    last_out: btn.dataset.lastOut,
                    stockId: parseInt(btn.dataset.stockId)
                };
                StockModal.open(stockData);
            }
        });
    },

    // 재고 목록 로딩
    load() {
        const keyword = StockSearch.getKeyword();
        const whs = StockSearch.getWarehouse();
        
        const url = `/api/stocks?page=${currentPage}&size=${pageSize}` +
                    (keyword ? `&keyword=${encodeURIComponent(keyword)}` : '') +
                    (whs !== '전체 창고' ? `&whs=${encodeURIComponent(whs)}` : '');
        
        fetch(url)
            .then(response => response.json())
            .then(data => {
                this.updateTable(data.content);
                this.updatePagination(data);
            })
            .catch(error => {
                console.error('재고 목록 조회 실패:', error);
                alert('재고 목록을 불러오는데 실패했습니다.');
            });
    },

    // 테이블 업데이트
    updateTable(stocks) {
        const tbody = document.querySelector('table tbody');
        if (!tbody) return;
        
        tbody.innerHTML = '';
        
        if (stocks.length === 0) {
            // 재고가 없을 때 메시지 표시
            const row = document.createElement('tr');
            row.className = 'no-data-row';
            row.innerHTML = `
                <td colspan="9" class="text-center text-muted py-5">
                    <div class="d-flex flex-column align-items-center">
                        <i class="fas fa-box-open fa-3x mb-3 text-secondary"></i>
                        <h5 class="mb-2">검색된 재고가 없습니다</h5>
                        <p class="mb-0">다른 검색 조건을 시도해보세요</p>
                    </div>
                </td>
            `;
            tbody.appendChild(row);
            return;
        }
        
        // 재고 데이터가 있을 때 테이블 생성
        stocks.forEach((stock, index) => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${currentPage * pageSize + index + 1}</td>
                <td>${stock.itemCode}</td>
                <td>${stock.itemName}</td>
                <td>${stock.warehouseName}</td>
                <td>${stock.quantity}</td>
                <td>EA</td>
                <td>${StockUtils.formatDate(stock.firstAt)}</td>
                <td>${StockUtils.formatDate(stock.lastAt)}</td>
                <td>
                    <button class="btn btn-info btn-sm stock-detail-btn"
                            data-stock-id="${stock.stockId}"
                            data-item-code="${stock.itemCode}"
                            data-item-name="${stock.itemName}"
                            data-warehouse="${stock.warehouseName}"
                            data-qty="${stock.quantity}"
                            data-last-in="${StockUtils.formatDate(stock.firstAt)}"
                            data-last-out="${StockUtils.formatDate(stock.lastAt)}">
                        상세
                    </button>
                </td>
            `;
            tbody.appendChild(row);
        });
    },

    // 페이징 업데이트
    updatePagination(pageData) {
        const paginationInfo = document.querySelector('.mt-3');
        
        if (pageData.totalElements === 0) {
            // 재고가 없으면 페이징 정보 숨기기
            if (paginationInfo) {
                paginationInfo.style.display = 'none';
            }
        } else {
            // 재고가 있으면 페이징 정보 표시
            if (paginationInfo) {
                paginationInfo.style.display = 'flex';
                const totalElements = paginationInfo.querySelector('span:first-child span:first-child');
                const currentPageSpan = paginationInfo.querySelector('span:first-child span:nth-child(2)');
                const totalPagesSpan = paginationInfo.querySelector('span:first-child span:nth-child(3)');
                
                if (totalElements) totalElements.textContent = pageData.totalElements;
                if (currentPageSpan) currentPageSpan.textContent = pageData.number + 1;
                if (totalPagesSpan) totalPagesSpan.textContent = pageData.totalPages;
            }
        }
        
        console.log(`총 ${pageData.totalElements}개, 현재 페이지: ${pageData.number + 1}/${pageData.totalPages}`);
    },

    // 새로고침
    refresh() {
        currentPage = 0;
        this.load();
    }
};