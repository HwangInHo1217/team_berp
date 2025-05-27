const StockList = {
    currentPage: 0, // StockList 내부의 현재 페이지
    pageSize: globalPageSize, // 전역 또는 설정값 사용

    init() {
        this.loadStockData(); // 초기 데이터 로드 (Controller에서 이미 전달했다면, 해당 데이터로 초기 테이블 구성)
        this.setupDetailButtonListener(); // 이벤트 위임 방식으로 변경
    },

    setupDetailButtonListener() {
        const tableBody = document.getElementById('stockTableBody');
        if (tableBody) {
            tableBody.addEventListener('click', (e) => {
                // 클릭된 요소가 'stock-detail-btn' 클래스를 가지고 있거나 그 자식인지 확인
                const detailButton = e.target.closest('.stock-detail-btn');
                if (detailButton) {
                    const stockId = parseInt(detailButton.dataset.stockId);
                    if (stockId) {
                        StockModal.open(stockId);
                    }
                }
            });
        }
    },

    loadStockData() {
        const keyword = StockSearch.getKeyword();
        const whsCode = StockSearch.getWarehouseCode(); // 수정된 메소드 사용

        // API URL 구성
        let url = `/api/stocks?page=${this.currentPage}&size=${this.pageSize}`;
        if (keyword) {
            url += `&keyword=${encodeURIComponent(keyword)}`;
        }
        if (whsCode) { // 빈 문자열이 아닐 때만 파라미터 추가
            url += `&whsCode=${encodeURIComponent(whsCode)}`;
        }
        // 정렬 파라미터 추가 예시 (필요시)
        // url += `&sort=id,desc`;

        console.log("Requesting URL:", url); // 요청 URL 확인

        fetch(url)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then(pageData => {
                this.updateTable(pageData.content);
                this.updatePaginationControls(pageData);
            })
            .catch(error => {
                console.error('재고 목록 조회 중 오류 발생:', error);
                const tbody = document.getElementById('stockTableBody');
                if (tbody) {
                    tbody.innerHTML = `<tr><td colspan="9" class="text-center text-danger py-5">데이터를 불러오는데 실패했습니다: ${error.message}</td></tr>`;
                }
                const paginationNav = document.getElementById('paginationNav');
                if(paginationNav) paginationNav.style.display = 'none';
                const paginationInfoDiv = document.getElementById('paginationInfoDiv');
                 if(paginationInfoDiv) paginationInfoDiv.innerHTML = '';

            });
    },

    updateTable(stocks) {
        const tbody = document.getElementById('stockTableBody');
        if (!tbody) return;

        tbody.innerHTML = ''; // 기존 내용 초기화

        if (!stocks || stocks.length === 0) {
            const noDataRow = `
                <tr class="no-data-row">
                    <td colspan="9" class="text-center text-muted py-5">
                        <div class="d-flex flex-column align-items-center">
                            <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" fill="currentColor" class="bi bi-box2-heart mb-3 text-secondary" viewBox="0 0 16 16">
                                <path d="M8 7.982C9.664 6.309 11.828 5 12.5 5c.781 0 1.5.304 2.072.802L16 6.874V13.5a.5.5 0 0 1-.5.5h-1a.5.5 0 0 1-.5-.5v-1.268l-.207.207C13.146 13.118 11.8 13.5 10.5 13.5c-1.664 0-3.828-1.309-4.5-2.018L4 13.5V6.874l1.428-1.072C5.998 5.304 6.719 5 7.5 5c.781 0 1.5.304 2.072.802L8 4.606V1.5a.5.5 0 0 1 .5-.5h4a.5.5 0 0 1 .5.5v2.035L8.377 2.513a.517.517 0 0 0-.754 0L.123 6.965a.5.5 0 0 0 0 .707l4.5 4.5a.5.5 0 0 0 .707 0l1.293-1.293V13.5a.5.5 0 0 1-.5.5h-1a.5.5 0 0 1-.5-.5V13l-1.45-1.45A.5.5 0 0 1 2.5 11V7.559L1 6.276V3.5a.5.5 0 0 1 .5-.5h1a.5.5 0 0 1 .5.5v2.207l2.451-1.838A1.5 1.5 0 0 1 7.5 3zM12.5 6a1.11 1.11 0 0 0-1.044.72c-.05.146-.11.293-.184.445l-.035.068L8 9.586l-2.238-2.353-.035-.068a2.185 2.185 0 0 0-.184-.445A1.11 1.11 0 0 0 4.5 6c-.458 0-.848.17-1.132.434L2 7.559V11a.5.5 0 0 0 .293.45L4 12.276V7.559l.868-.651A1.5 1.5 0 0 1 7.5 6c.699 0 1.166.302 1.45.549l.003.002L10.451 8H12.5a1.5 1.5 0 0 1 0-3z"/>
                                <path d="m8 4.434 1.072-1.072c.24-.24.532-.402.868-.482A1.5 1.5 0 0 1 12.5 3c.699 0 1.166.302 1.45.549l.003.002L15 5.375V2a.5.5 0 0 0-.5-.5h-4a.5.5 0 0 0-.5.5v2.434z"/>
                            </svg>
                            <h5 class="mb-2">표시할 재고가 없습니다</h5>
                            <p class="mb-0">검색 조건을 확인하거나 데이터를 추가해주세요.</p>
                        </div>
                    </td>
                </tr>`;
            tbody.insertAdjacentHTML('beforeend', noDataRow);
            return;
        }

        stocks.forEach((stock, index) => {
            // StockResponseDTO에 actualLastInAt, actualLastOutAt, itemUnit 필드가 있다고 가정
            const actualLastIn = stock.actualLastInAt ? StockUtils.formatDate(stock.actualLastInAt) : (stock.firstAt ? StockUtils.formatDate(stock.firstAt) : '-');
            const actualLastOut = stock.actualLastOutAt ? StockUtils.formatDate(stock.actualLastOutAt) : (stock.lastAt ? StockUtils.formatDate(stock.lastAt) : '-');
            const itemUnit = stock.itemUnit || '-';

            const rowHtml = `
                <tr>
                    <td>${this.currentPage * this.pageSize + index + 1}</td>
                    <td>${StockUtils.escapeHtml(stock.itemCode)}</td>
                    <td>${StockUtils.escapeHtml(stock.itemName)}</td>
                    <td>${StockUtils.escapeHtml(stock.warehouseName)}</td>
                    <td>${StockUtils.formatNumber(stock.quantity)}</td>
                    <td>${StockUtils.escapeHtml(itemUnit)}</td>
                    <td>${actualLastIn}</td>
                    <td>${actualLastOut}</td>
                    <td>
                        <button class="btn btn-info btn-sm stock-detail-btn"
                                data-stock-id="${stock.stockId}">
                            상세
                        </button>
                    </td>
                </tr>`;
            tbody.insertAdjacentHTML('beforeend', rowHtml);
        });
    },

    updatePaginationControls(pageData) {
        const paginationNav = document.getElementById('paginationNav');
        const paginationUl = document.getElementById('paginationUl');
        const paginationInfoText = document.getElementById('paginationInfoText');
        const paginationInfoDiv = document.getElementById('paginationInfoDiv');


        if (!paginationNav || !paginationUl || !paginationInfoText || !paginationInfoDiv) return;

        paginationUl.innerHTML = ''; // 기존 페이지 버튼 초기화

        if (pageData.totalElements === 0) {
            paginationNav.style.display = 'none';
            paginationInfoDiv.innerHTML = '<span class="text-muted">표시할 데이터가 없습니다.</span>';
            return;
        }
        paginationNav.style.display = 'block';
        paginationInfoDiv.innerHTML = `
            <span class="text-muted">
                총 ${pageData.totalElements}개 
                (현재 ${pageData.number + 1} / ${pageData.totalPages} 페이지)
            </span>`;


        if (pageData.totalPages <= 1) {
             paginationNav.style.display = 'none'; // 페이지가 하나면 숨김
             return;
        }


        const maxPagesToShow = 5; // 한 번에 보여줄 최대 페이지 버튼 수
        let startPage, endPage;

        if (pageData.totalPages <= maxPagesToShow) {
            startPage = 0;
            endPage = pageData.totalPages - 1;
        } else {
            const maxPagesBeforeCurrent = Math.floor(maxPagesToShow / 2);
            const maxPagesAfterCurrent = Math.ceil(maxPagesToShow / 2) - 1;

            if (this.currentPage <= maxPagesBeforeCurrent) {
                startPage = 0;
                endPage = maxPagesToShow - 1;
            } else if (this.currentPage + maxPagesAfterCurrent >= pageData.totalPages) {
                startPage = pageData.totalPages - maxPagesToShow;
                endPage = pageData.totalPages - 1;
            } else {
                startPage = this.currentPage - maxPagesBeforeCurrent;
                endPage = this.currentPage + maxPagesAfterCurrent;
            }
        }

        // Previous button
        let li = document.createElement('li');
        li.className = `page-item ${pageData.first ? 'disabled' : ''}`;
        let a = document.createElement('a');
        a.className = 'page-link';
        a.href = '#';
        a.textContent = '이전';
        if (!pageData.first) {
            a.addEventListener('click', (e) => {
                e.preventDefault();
                this.changePage(this.currentPage - 1);
            });
        }
        li.appendChild(a);
        paginationUl.appendChild(li);

        // Page number buttons
        for (let i = startPage; i <= endPage; i++) {
            li = document.createElement('li');
            li.className = `page-item ${i === this.currentPage ? 'active' : ''}`;
            a = document.createElement('a');
            a.className = 'page-link';
            a.href = '#';
            a.textContent = i + 1;
            a.dataset.page = i;
            a.addEventListener('click', (e) => {
                e.preventDefault();
                this.changePage(parseInt(e.target.dataset.page));
            });
            li.appendChild(a);
            paginationUl.appendChild(li);
        }

        // Next button
        li = document.createElement('li');
        li.className = `page-item ${pageData.last ? 'disabled' : ''}`;
        a = document.createElement('a');
        a.className = 'page-link';
        a.href = '#';
        a.textContent = '다음';
        if (!pageData.last) {
            a.addEventListener('click', (e) => {
                e.preventDefault();
                this.changePage(this.currentPage + 1);
            });
        }
        li.appendChild(a);
        paginationUl.appendChild(li);
    },

    changePage(pageNumber) {
        this.currentPage = pageNumber;
        this.loadStockData();
    },

    refresh() {
        this.currentPage = 0; // 검색 시 첫 페이지로
        this.loadStockData();
    }
};