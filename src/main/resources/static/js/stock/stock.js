// ===== 재고 관리 JavaScript (기존 유지 + 긴급출고 거래처 선택 추가) =====

// 전역 변수 및 상태
const globalPageSize = 10;

const StockState = {
    currentPage: 0,
    pageSize: 10,
    sortBy: 'id,DESC',
    filters: {
        keyword: '',
        warehouse: '',
        itemType: '',
        stockStatus: ''
    }
};

// ===== StockUtils (기존과 동일) =====
const StockUtils = {
    formatNumber(num) {
        if (num == null) return '0';
        return new Intl.NumberFormat('ko-KR').format(num);
    },
    
    formatDate(dateStr) {
        if (!dateStr) return '-';
        try {
            return new Date(dateStr).toLocaleDateString('ko-KR');
        } catch (error) {
            return '-';
        }
    },
    
    formatDateTime(dateStr) {
        if (!dateStr) return '-';
        try {
            return new Date(dateStr).toLocaleString('ko-KR', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (error) {
            return '-';
        }
    },
    
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    },
    
    showSuccess(message) {
        this.showToast(message, 'success');
    },
    
    showError(message) {
        this.showToast(message, 'danger');
    },
    
    showInfo(message) {
        this.showToast(message, 'info');
    },
    
    showWarning(message) {
        this.showToast(message, 'warning');
    },
    
    showToast(message, type = 'info') {
        const toastHtml = `
            <div class="toast align-items-center text-white bg-${type} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `;
        
        const toastContainer = this.getToastContainer();
        toastContainer.insertAdjacentHTML('beforeend', toastHtml);
        
        const toastElement = toastContainer.lastElementChild;
        const toast = new bootstrap.Toast(toastElement, {
            autohide: true,
            delay: 3000
        });
        toast.show();
        
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    },
    
    getToastContainer() {
        let container = document.getElementById('toastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toastContainer';
            container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            document.body.appendChild(container);
        }
        return container;
    },
    
    confirm(message, onConfirm, onCancel) {
        if (window.confirm(message)) {
            if (onConfirm) onConfirm();
        } else {
            if (onCancel) onCancel();
        }
    },
    
    handleApiError(error, defaultMessage = '요청 처리에 실패했습니다.') {
        console.error('API Error:', error);
        
        let message = defaultMessage;
        if (error.response && error.response.data && error.response.data.message) {
            message = error.response.data.message;
        } else if (error.message) {
            message = error.message;
        }
        
        this.showError(message);
    }
};

// ===== StockList (기존과 동일) =====
const StockList = {
    currentPage: 0,
    pageSize: globalPageSize,

    init() {
        console.log('StockList 초기화');
        this.setupTableEvents();
        this.loadStockData();
    },

    setupTableEvents() {
        // 정렬 가능한 컬럼 클릭 이벤트
        document.querySelectorAll('.sortable').forEach(th => {
            th.style.cursor = 'pointer';
            th.addEventListener('click', (e) => {
                const sortField = th.dataset.sort;
                this.toggleSort(sortField);
            });
        });
        
        // 전체 선택 체크박스
        const selectAllCheckbox = document.getElementById('selectAll');
        if (selectAllCheckbox) {
            selectAllCheckbox.addEventListener('change', (e) => {
                document.querySelectorAll('.stock-checkbox').forEach(cb => {
                    cb.checked = e.target.checked;
                });
            });
        }
        
        this.setupDetailButtonListener();
    },

    setupDetailButtonListener() {
        const tableBody = document.getElementById('stockTableBody');
        if (tableBody) {
            tableBody.addEventListener('click', (e) => {
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

    toggleSort(field) {
        console.log('🔄 toggleSort 호출 - 원본 필드:', field);

        // 창고명 정렬 필드 매핑
        if (field === 'warehouseName') {
            field = 'warehouse.warehouseName';
            console.log('🏢 창고 필드 매핑:', field);
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
        console.log("✅ API 요청:", url);

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
                this.updateTable(pageData.content);
                this.updatePaginationControls(pageData);
                StockState.currentPage = pageData.number;
            })
            .catch(error => {
                console.error('❌ 재고 목록 조회 오류:', error);
                const tbody = document.getElementById('stockTableBody');
                if (tbody) {
                    tbody.innerHTML = `<tr><td colspan="11" class="text-center text-danger py-5">데이터를 불러오는데 실패했습니다: ${error.message}</td></tr>`;
                }
            });
    },

	// 🆕 재고 테이블 업데이트 (NEW 배지 포함)
	updateTable(stocks) {
	    const tbody = document.getElementById('stockTableBody');
	    if (!tbody) return;

	    tbody.innerHTML = '';

	    if (!stocks || stocks.length === 0) {
	        const noDataRow = `
	            <tr class="no-data-row">
	                <td colspan="11" class="text-center text-muted py-5">
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

	    stocks.forEach((stock, index) => {
	        const actualLastIn = stock.actualLastInAt ? StockUtils.formatDate(stock.actualLastInAt) : 
	                            (stock.lastInDate ? StockUtils.formatDate(stock.lastInDate) :
	                            (stock.firstStockedDate ? StockUtils.formatDate(stock.firstStockedDate) : '-'));
	        const actualLastOut = stock.actualLastOutAt ? StockUtils.formatDate(stock.actualLastOutAt) : 
	                             (stock.lastOutDate ? StockUtils.formatDate(stock.lastOutDate) : '-');
	        const itemUnit = stock.itemUnit || stock.unit || 'EA';

	        // 🆕 최근 창고이동 입고 체크
	        const hasRecentTransfer = this.hasRecentTransferIn(stock.stockId);
	        const newBadge = hasRecentTransfer ? 
	            '<span class="badge bg-success ms-1 stock-new-badge" style="animation: pulse 1.5s infinite; font-size: 0.7em;">NEW</span>' : '';

	        const rowHtml = `
	            <tr data-stock-id="${stock.stockId}" ${hasRecentTransfer ? 'class="recent-transfer-stock"' : ''}>
	                <td><input type="checkbox" class="form-check-input stock-checkbox" data-stock-id="${stock.stockId}"></td>
	                <td>${StockState.currentPage * this.pageSize + index + 1}</td>
	                <td>${StockUtils.escapeHtml(stock.itemCode)}</td>
	                <td>
	                    ${StockUtils.escapeHtml(stock.itemName)}${newBadge}
	                </td>
	                <td>${StockUtils.escapeHtml(stock.itemType === 'raw' ? '자재' : (stock.itemType === 'product' ? '완제품' : stock.itemType))}</td>
	                <td>${StockUtils.escapeHtml(stock.warehouseName)}</td>
	                <td class="text-end"><strong>${StockUtils.formatNumber(stock.quantity)}</strong></td>
	                <td>${StockUtils.escapeHtml(itemUnit)}</td>
	                <td><small>${actualLastIn}</small></td>
	                <td><small>${actualLastOut}</small></td>
	                <td>${this.getStockStatusBadge(stock)}</td>
	                <td>
	                    <button class="btn btn-info btn-sm stock-detail-btn"
	                            data-stock-id="${stock.stockId}">
	                        상세
	                    </button>
	                </td>
	            </tr>`;
	        tbody.insertAdjacentHTML('beforeend', rowHtml);
	    });

	    // 🆕 최근 이동된 재고 행 클릭 이벤트 추가
	    this.setupRecentTransferClickEvents();
	},
	
	// 🆕 최근 이동된 재고 클릭 이벤트 설정
	setupRecentTransferClickEvents() {
	    document.querySelectorAll('.recent-transfer-stock').forEach(row => {
	        row.style.cursor = 'pointer';
	        row.style.backgroundColor = '#f8fff8'; // 연한 초록색
	        row.style.borderLeft = '4px solid #28a745';
	        
	        // 호버 효과
	        row.addEventListener('mouseenter', () => {
	            row.style.backgroundColor = '#e8f5e8';
	        });
	        row.addEventListener('mouseleave', () => {
	            row.style.backgroundColor = '#f8fff8';
	        });
	        
	        // 클릭 시 NEW 배지 제거
	        row.addEventListener('click', (e) => {
	            // 체크박스나 버튼 클릭이 아닌 경우만
	            if (!e.target.closest('input, button')) {
	                const stockId = row.dataset.stockId;
	                this.markStockAsViewed(stockId, row);
	            }
	        });
	    });
	},
	// 🆕 재고를 확인된 것으로 마킹
	markStockAsViewed(stockId, rowElement) {
	    const recentTransfers = JSON.parse(localStorage.getItem('recentTransferStocks') || '{}');
	    if (recentTransfers[stockId]) {
	        recentTransfers[stockId].viewed = true;
	        localStorage.setItem('recentTransferStocks', JSON.stringify(recentTransfers));
	    }

	    // NEW 배지 제거
	    const newBadge = rowElement.querySelector('.stock-new-badge');
	    if (newBadge) {
	        newBadge.style.animation = 'fadeOut 0.5s ease-out';
	        setTimeout(() => {
	            newBadge.remove();
	        }, 500);
	    }

	    // 행 스타일 원래대로 복원
	    rowElement.style.backgroundColor = '';
	    rowElement.style.borderLeft = '';
	    rowElement.style.cursor = '';
	    rowElement.classList.remove('recent-transfer-stock');

	    // 이벤트 리스너 제거
	    rowElement.replaceWith(rowElement.cloneNode(true));
	    
	    StockUtils.showToast('새로운 창고이동을 확인했습니다.', 'info', 2000);
	},
	
	
    updateTable(stocks) {
        const tbody = document.getElementById('stockTableBody');
        if (!tbody) return;

        tbody.innerHTML = '';

        if (!stocks || stocks.length === 0) {
            const noDataRow = `
                <tr class="no-data-row">
                    <td colspan="11" class="text-center text-muted py-5">
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

        stocks.forEach((stock, index) => {
            const actualLastIn = stock.actualLastInAt ? StockUtils.formatDate(stock.actualLastInAt) : 
                                (stock.lastInDate ? StockUtils.formatDate(stock.lastInDate) :
                                (stock.firstStockedDate ? StockUtils.formatDate(stock.firstStockedDate) : '-'));
            const actualLastOut = stock.actualLastOutAt ? StockUtils.formatDate(stock.actualLastOutAt) : 
                                 (stock.lastOutDate ? StockUtils.formatDate(stock.lastOutDate) : '-');
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
                    <td><small>${actualLastIn}</small></td>
                    <td><small>${actualLastOut}</small></td>
                    <td>${this.getStockStatusBadge(stock)}</td>
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

    getStockStatusBadge(stock) {
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
	    const paginationInfoDiv = document.getElementById('paginationInfoDiv');
	    if (paginationInfoDiv) {
	        paginationInfoDiv.innerHTML = `
	            <span class="text-muted">
	                총 ${StockUtils.formatNumber(pageData.totalElements)}개 
	                (현재 ${pageData.number + 1} / ${pageData.totalPages} 페이지)
	            </span>`;
	    }
	    
	    // 개선된 페이징 UI
	    const paginationUl = document.getElementById('paginationUl');
	    if (!paginationUl) return;

	    paginationUl.innerHTML = '';

	    const currentPage = pageData.number;
	    const totalPages = pageData.totalPages;

	    // 페이지가 1개 이하면 페이징 버튼 숨김
	    if (totalPages <= 1) {
	        paginationUl.style.display = 'none';
	        return;
	    }

	    paginationUl.style.display = 'flex';
	    paginationUl.className = 'pagination justify-content-center mb-3';

	    // 페이징 번호 범위 계산 (현재 페이지 기준 ±2)
	    let startPage = Math.max(0, currentPage - 2);
	    let endPage = Math.min(totalPages - 1, currentPage + 2);

	    // 범위 조정 (항상 5개 페이지 표시하려고 시도)
	    if (endPage - startPage < 4) {
	        if (startPage === 0) {
	            endPage = Math.min(totalPages - 1, startPage + 4);
	        } else if (endPage === totalPages - 1) {
	            startPage = Math.max(0, endPage - 4);
	        }
	    }

	    // 맨 처음 버튼
	    if (currentPage > 0) {
	        paginationUl.innerHTML += `
	            <li class="page-item">
	                <a class="page-link" href="#" onclick="StockList.goToPage(0)" title="첫 페이지">
	                    <i class="fas fa-angle-double-left"></i>
	                </a>
	            </li>`;
	    }

	    // 이전 버튼
	    if (pageData.hasPrevious) {
	        paginationUl.innerHTML += `
	            <li class="page-item">
	                <a class="page-link" href="#" onclick="StockList.goToPage(${currentPage - 1})" aria-label="이전">
	                    <i class="fas fa-angle-left"></i>
	                </a>
	            </li>`;
	    }

	    // 페이지 번호들
	    for (let i = startPage; i <= endPage; i++) {
	        const isActive = i === currentPage;
	        const pageNum = i + 1; // 1부터 시작하는 페이지 번호
	        
	        paginationUl.innerHTML += `
	            <li class="page-item ${isActive ? 'active' : ''}">
	                <a class="page-link" href="#" onclick="StockList.goToPage(${i})" 
	                   ${isActive ? 'aria-current="page"' : ''}>
	                    ${pageNum}
	                </a>
	            </li>`;
	    }

	    // 다음 버튼
	    if (pageData.hasNext) {
	        paginationUl.innerHTML += `
	            <li class="page-item">
	                <a class="page-link" href="#" onclick="StockList.goToPage(${currentPage + 1})" aria-label="다음">
	                    <i class="fas fa-angle-right"></i>
	                </a>
	            </li>`;
	    }

	    // 맨 끝 버튼
	    if (currentPage < totalPages - 1) {
	        paginationUl.innerHTML += `
	            <li class="page-item">
	                <a class="page-link" href="#" onclick="StockList.goToPage(${totalPages - 1})" title="마지막 페이지">
	                    <i class="fas fa-angle-double-right"></i>
	                </a>
	            </li>`;
	    }
	},
	
	// ===== 새로 추가: 페이지 이동 메서드 =====
	goToPage(pageNumber) {
	    console.log('📄 페이지 이동 요청:', pageNumber + 1 + '페이지');
	    
	    // 유효성 검사
	    if (pageNumber < 0) {
	        console.warn('⚠️ 잘못된 페이지 번호: ' + pageNumber);
	        return;
	    }
	    
	    // 상태 업데이트
	    StockState.currentPage = pageNumber;
	    
	    // 데이터 다시 로딩
	    this.loadStockData();
	},

    refresh() {
        this.currentPage = 0;
        StockState.currentPage = 0;
        this.loadStockData();
    }
};

// ===== StockSearch (기존과 동일) =====
const StockSearch = {
    init() {
        console.log('StockSearch 초기화');
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

        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.performSearch();
                }
            });
        }

        ['warehouseSelect', 'itemTypeFilter', 'stockStatusFilter'].forEach(id => {
            const element = document.getElementById(id);
            if (element) {
                element.addEventListener('change', () => this.performSearch());
            }
        });
    },

    performSearch() {
        const searchInput = document.getElementById('searchInput');
        const warehouseSelect = document.getElementById('warehouseSelect');
        const itemTypeFilter = document.getElementById('itemTypeFilter');
        const stockStatusFilter = document.getElementById('stockStatusFilter');

        StockState.filters.keyword = searchInput ? searchInput.value.trim() : '';
        StockState.filters.warehouse = warehouseSelect ? warehouseSelect.value : '';
        StockState.filters.itemType = itemTypeFilter ? itemTypeFilter.value : '';
        StockState.filters.stockStatus = stockStatusFilter ? stockStatusFilter.value : '';
        StockState.currentPage = 0;
        
        console.log('검색 필터:', StockState.filters);
        StockList.refresh();
    },

    reset() {
        const searchInput = document.getElementById('searchInput');
        const warehouseSelect = document.getElementById('warehouseSelect');
        const itemTypeFilter = document.getElementById('itemTypeFilter');
        const stockStatusFilter = document.getElementById('stockStatusFilter');

        if (searchInput) searchInput.value = '';
        if (warehouseSelect) warehouseSelect.value = '';
        if (itemTypeFilter) itemTypeFilter.value = '';
        if (stockStatusFilter) stockStatusFilter.value = '';
        
        StockState.filters = {
            keyword: '', warehouse: '', itemType: '', stockStatus: ''
        };
        StockState.currentPage = 0;
        
        StockList.refresh();
    }
};

// ===== StockModal (기존과 동일) =====
const StockModal = {
	
    modalInstance: null,
    currentStockId: null,
	currentItemId: null,      // 🆕 추가!
    currentWarehouseId: null,
    currentQuantity: null,
    currentItemName: null,
    currentItemType: null,
	currentWarehouseName: null,
	currentItemCode: null,
	
    elementsMap: {
        itemCode: 'detailItemCode',
        itemType: 'detailItemType',
        unit: 'detailUnit',
        warehouse: 'detailWarehouse',
        qty: 'detailQty',
        stockStatus: 'detailStockStatus',
        firstStocked: 'detailFirstStocked',
        lastIn: 'detailLastIn',
        lastStocked: 'detailLastStocked',
        lastOut: 'detailLastOut'
    },

    init() {
        console.log('StockModal 초기화');
        const modalElement = document.getElementById('stockDetailModal');
        if (modalElement) {
            this.modalInstance = new bootstrap.Modal(modalElement);
            console.log('✅ StockModal 정상 초기화');
        } else {
            console.warn('⚠️ stockDetailModal이 없음 - 나중에 동적 생성 예정');
        }
    },

    createDetailModalDynamically() {
        const modalHtml = `
            <div class="modal fade" id="stockDetailModal" tabindex="-1" aria-hidden="true">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">재고 상세</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <div class="row">
                                <div class="col-md-6">
                                    <h6 class="text-muted">품목 정보</h6>
                                    <dl class="row mb-0">
                                        <dt class="col-sm-4">품목코드</dt>
                                        <dd class="col-sm-8" id="detailItemCode">-</dd>
                                        <dt class="col-sm-4">품목명</dt>
                                        <dd class="col-sm-8" id="detailItemName">-</dd>
                                        <dt class="col-sm-4">품목유형</dt>
                                        <dd class="col-sm-8" id="detailItemType">-</dd>
                                        <dt class="col-sm-4">단위</dt>
                                        <dd class="col-sm-8" id="detailUnit">-</dd>
                                    </dl>
                                </div>
                                <div class="col-md-6">
                                    <h6 class="text-muted">재고 정보</h6>
                                    <dl class="row mb-0">
                                        <dt class="col-sm-4">창고</dt>
                                        <dd class="col-sm-8" id="detailWarehouse">-</dd>
                                        <dt class="col-sm-4">현재고</dt>
                                        <dd class="col-sm-8"><strong class="text-primary" id="detailQty">-</strong></dd>
                                        <dt class="col-sm-4">재고상태</dt>
                                        <dd class="col-sm-8" id="detailStockStatus">-</dd>
                                    </dl>
                                </div>
                            </div>
                            <hr>
                            <div class="row">
                                <div class="col-md-6">
                                    <dl class="row mb-0">
                                        <dt class="col-sm-4">최초입고일</dt>
                                        <dd class="col-sm-8" id="detailFirstStocked">-</dd>
                                        <dt class="col-sm-4">최종입고일</dt>
                                        <dd class="col-sm-8" id="detailLastIn">-</dd>
                                    </dl>
                                </div>
                                <div class="col-md-6">
                                    <dl class="row mb-0">
                                        <dt class="col-sm-4">최종변경일</dt>
                                        <dd class="col-sm-8" id="detailLastStocked">-</dd>
                                        <dt class="col-sm-4">최종출고일</dt>
                                        <dd class="col-sm-8" id="detailLastOut">-</dd>
                                    </dl>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-info" onclick="StockActions.showHistory(StockModal.currentStockId)">
                                <i class="fas fa-history"></i> 이력조회
                            </button>
                            <button type="button" class="btn btn-warning" onclick="StockModal.openTransferModalFromDetail()">
                                <i class="fas fa-exchange-alt"></i> 창고이동
                            </button>
                            <button type="button" class="btn btn-danger" onclick="StockActions.quickOut(StockModal.currentStockId, parseInt(document.getElementById('detailQty')?.textContent.replace(/,/g, '') || 0))">
                                <i class="fas fa-minus-circle"></i> 출고
                            </button>
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">닫기</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        
        const modalElement = document.getElementById('stockDetailModal');
        if (modalElement) {
            this.modalInstance = new bootstrap.Modal(modalElement);
        }
        
        console.log('✅ Detail Modal 동적 생성 완료');
    },

    open(stockId) {
        if (!this.modalInstance) {
            StockUtils.showError('상세보기 모달이 초기화되지 않았습니다.');
            return;
        }

        if (stockId) {
            this.currentStockId = stockId;
            this.fetchStockDetail(stockId);
        } else {
            StockUtils.showError('상세 정보를 표시할 재고 ID가 없습니다.');
        }
    },

	fetchStockDetail(stockId) {
	    fetch(`/api/stocks/${stockId}/detail`)
	        .then(response => {
	            if (!response.ok) {
	                throw new Error(`서버 응답 오류 (${response.status})`);
	            }
	            return response.json();
	        })
	        .then(stockDetail => {
	            console.log("상세 데이터:", stockDetail);
	            
				// 🔧 모든 필요한 정보를 저장 (itemId 중심으로)
				this.currentStockId = stockDetail.stockId;
				this.currentItemId = stockDetail.itemId || stockDetail.stockId; // ⚠️ fallback 처리
				this.currentWarehouseId = stockDetail.warehouseId;
				this.currentQuantity = stockDetail.quantity;
				this.currentItemName = stockDetail.itemName;
				this.currentItemType = stockDetail.itemType;
				this.currentWarehouseName = stockDetail.warehouseName;
				this.currentItemCode = stockDetail.itemCode;
				
				// 🔍 여기에 디버깅 로그 추가!
				console.log('📋 StockModal 데이터 저장 완료:');
				console.log('  - currentStockId:', this.currentStockId);
				console.log('  - currentItemId:', this.currentItemId);     // 🆕 추가!
				console.log('  - currentWarehouseId:', this.currentWarehouseId);
				console.log('  - currentWarehouseName:', this.currentWarehouseName);
				console.log('  - currentQuantity:', this.currentQuantity);
				console.log('  - currentItemName:', this.currentItemName);
				console.log('  - currentItemType:', this.currentItemType);
				console.log('  currentItemCode:', this.currentItemCode);
				// ⚠️ itemId 검증
				if (!this.currentItemId || this.currentItemId === this.currentStockId) {
				    console.warn('⚠️ itemId가 stockId와 같거나 없습니다. 서버 응답을 확인하세요.');
				    console.warn('서버 데이터의 itemId 필드:', stockDetail.itemId);
				}
	            const displayData = {
	                itemCode: stockDetail.itemCode,
	                itemName: stockDetail.itemName,
	                itemType: stockDetail.itemType === 'raw' ? '자재' : '완제품',
	                unit: stockDetail.itemUnit || stockDetail.unit || 'EA',
	                warehouse: stockDetail.warehouseName,
	                qty: StockUtils.formatNumber(stockDetail.quantity),
	                stockStatus: this.getDetailStatusBadge(stockDetail),
	                firstStocked: StockUtils.formatDate(stockDetail.firstStockedDate),
	                lastIn: StockUtils.formatDate(stockDetail.actualLastInAt || stockDetail.lastInDate),
	                lastStocked: StockUtils.formatDate(stockDetail.lastStockedDate),
	                lastOut: StockUtils.formatDate(stockDetail.actualLastOutAt || stockDetail.lastOutDate)
	            };
	            
	            this.fillModalData(displayData);
	            this.modalInstance.show();
	        })
	        .catch(error => {
	            StockUtils.handleApiError(error, '재고 상세 정보를 가져오는데 실패했습니다.');
	        });
	},

    fillModalData(data) {
        for (const key in this.elementsMap) {
            const element = document.getElementById(this.elementsMap[key]);
            if (element) {
                if (key === 'stockStatus') {
                    element.innerHTML = data[key] || '-';
                } else {
                    element.textContent = data[key] || '-';
                }
            }
        }
    },
    
    getDetailStatusBadge(stock) {
        if (stock.quantity === 0) {
            return '<span class="badge bg-danger">재고없음</span>';
        } else if (stock.isBelowSafety) {
            return '<span class="badge bg-warning">안전재고미달</span>';
        } else {
            return '<span class="badge bg-success">정상</span>';
        }
    },

    // 창고 이동 관련 메서드들은 기존과 동일하게 유지...
    openTransferModal(stockId, warehouseId, quantity, itemName, itemType) {
        this.currentStockId = stockId;
        this.currentWarehouseId = warehouseId;
        this.currentQuantity = quantity;
        this.currentItemName = itemName;
        this.currentItemType = itemType;
        
        document.getElementById('transferStockInfo').textContent = itemName;
        document.getElementById('transferCurrentQty').textContent = StockUtils.formatNumber(quantity);
        
        this.showTransferGuideMessage(itemType);
        this.loadWarehousesForTransfer(warehouseId, itemType);
        
        document.getElementById('transferQty').value = '';
        document.getElementById('transferReason').value = '';
        document.getElementById('transferComment').value = '';
        
        const transferModal = new bootstrap.Modal(document.getElementById('stockTransferModal'));
        transferModal.show();
    },

    showTransferGuideMessage(itemType) {
        const guideElement = document.getElementById('transferGuideMessage');
        if (!guideElement) {
            this.createTransferGuideElement();
        }
        
        const guide = document.getElementById('transferGuideMessage');
        if (guide) {
            let message = '';
            let className = 'alert alert-info small mt-2';
            
            if (itemType === 'product') {
                message = '완제품은 완제품 창고로만 이동 가능합니다.';
            } else if (itemType === 'raw') {
                message = '자재는 자재 창고로만 이동 가능합니다.';
            } else {
                message = '⚠️ 품목 유형에 맞는 창고를 선택해주세요.';
                className = 'alert alert-warning small mt-2';
            }
            
            guide.innerHTML = `
                <div class="${className}">
                    <i class="fas fa-info-circle me-2"></i>
                    ${message}
                </div>
            `;
        }
    },

    createTransferGuideElement() {
        const toWarehouseDiv = document.getElementById('toWarehouse')?.parentElement;
        if (toWarehouseDiv) {
            const guideDiv = document.createElement('div');
            guideDiv.id = 'transferGuideMessage';
            toWarehouseDiv.appendChild(guideDiv);
        }
    },

    loadWarehousesForTransfer(currentWarehouseId, itemType) {
        fetch('/api/stocks/warehouses?useYn=Y')
            .then(response => response.json())
            .then(warehouses => {
                const select = document.getElementById('toWarehouse');
                if (select) {
                    select.innerHTML = '<option value="">도착 창고를 선택하세요</option>';
                    
                    const filteredWarehouses = warehouses.filter(wh => {
                        if (wh.id === currentWarehouseId) return false;
                        if (wh.useYn !== 'Y') return false;
                        
                        if (itemType === 'product') {
                            return wh.warehouseType === 'PRODUCT';
                        } else if (itemType === 'raw') {
                            return wh.warehouseType === 'RAW';
                        }
                        
                        return true;
                    });

                    if (filteredWarehouses.length > 0) {
                        filteredWarehouses.forEach(wh => {
                            const typeLabel = wh.warehouseType === 'PRODUCT' ? '완제품' : '자재';
                            select.innerHTML += `
                                <option value="${wh.id}" 
                                        data-warehouse-name="${wh.warehouseName}"
                                        data-warehouse-type="${wh.warehouseType}">
                                    [${wh.warehouseCode}] ${wh.warehouseName} (${typeLabel})
                                </option>
                            `;
                        });
                    } else {
                        const noWarehouseMsg = itemType === 'product' 
                            ? '이동 가능한 완제품 창고가 없습니다'
                            : '이동 가능한 자재 창고가 없습니다';
                        
                        select.innerHTML += `<option value="" disabled>${noWarehouseMsg}</option>`;
                        
                        const confirmBtn = document.getElementById('confirmTransfer');
                        if (confirmBtn) {
                            confirmBtn.disabled = true;
                            confirmBtn.textContent = '이동 불가';
                        }
                    }
                }
            })
            .catch(error => {
                console.error('창고 목록 로딩 실패:', error);
                StockUtils.showError('창고 목록을 불러오는데 실패했습니다.');
            });
    },

    processTransfer() {
        const toWarehouseSelect = document.getElementById('toWarehouse');
        const toWarehouseId = toWarehouseSelect.value;
        const transferQty = parseInt(document.getElementById('transferQty').value);
        const reason = document.getElementById('transferReason').value;
        const comment = document.getElementById('transferComment').value;

        if (!toWarehouseId) {
            StockUtils.showError('도착 창고를 선택해주세요.');
            return;
        }

        if (!transferQty || transferQty <= 0) {
            StockUtils.showError('이동 수량을 입력해주세요.');
            return;
        }

        if (transferQty > this.currentQuantity) {
            StockUtils.showError('이동 수량이 현재고보다 많습니다.');
            return;
        }

        const selectedOption = toWarehouseSelect.selectedOptions[0];
        const selectedWarehouseType = selectedOption?.dataset.warehouseType;
        
        if (!this.validateWarehouseTypeMatch(this.currentItemType, selectedWarehouseType)) {
            return;
        }

        const transferData = {
            fromStockId: this.currentStockId,
            fromWarehouseId: this.currentWarehouseId,
            toWarehouseId: parseInt(toWarehouseId),
            itemId: this.currentStockId,
            quantity: transferQty,
            reason: reason,
            comment: comment
        };

        fetch('/api/stocks/transfer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(transferData)
        })
        .then(response => response.json())
        .then(result => {
            if (result.status === 'success') {
                StockUtils.showSuccess(result.message);
                
                const transferModal = bootstrap.Modal.getInstance(document.getElementById('stockTransferModal'));
                if (transferModal) transferModal.hide();
                
                const detailModal = bootstrap.Modal.getInstance(document.getElementById('stockDetailModal'));
                if (detailModal) detailModal.hide();
                
                StockList.refresh();
            } else {
                StockUtils.showError(result.message || '창고 이동 실패');
            }
        })
        .catch(error => {
            console.error('창고 이동 오류:', error);
            StockUtils.showError('창고 이동 중 오류가 발생했습니다.');
        });
    },

    validateWarehouseTypeMatch(itemType, warehouseType) {
        let isValid = false;
        let errorMessage = '';

        if (itemType === 'product' && warehouseType === 'PRODUCT') {
            isValid = true;
        } else if (itemType === 'raw' && warehouseType === 'RAW') {
            isValid = true;
        } else if (itemType === 'product' && warehouseType === 'RAW') {
            errorMessage = '❌ 완제품은 자재 창고로 이동할 수 없습니다.\n완제품 창고를 선택해주세요.';
        } else if (itemType === 'raw' && warehouseType === 'PRODUCT') {
            errorMessage = '❌ 자재는 완제품 창고로 이동할 수 없습니다.\n자재 창고를 선택해주세요.';
        } else {
            errorMessage = '⚠️ 품목 유형에 맞는 창고를 선택해주세요.';
        }

        if (!isValid) {
            StockUtils.showError(errorMessage);
            alert(errorMessage);
            document.getElementById('toWarehouse').value = '';
            return false;
        }

        return true;
    },

	    // 🔧 수정된 openTransferModalFromDetail 메서드
	    openTransferModalFromDetail() {
	        console.log('🔄 상세 모달에서 창고이동 요청');
			// 🔍 현재 저장된 데이터 상태 확인
			console.log('📦 현재 저장된 데이터:');
			console.log('  currentStockId:', this.currentStockId);
			console.log('  currentItemId:', this.currentItemId);
			console.log('  currentWarehouseId:', this.currentWarehouseId);
			console.log('  currentWarehouseName:', this.currentWarehouseName);
			console.log('  currentQuantity:', this.currentQuantity);
			console.log('  currentItemName:', this.currentItemName);
			console.log('  currentItemType:', this.currentItemType);
			console.log('  currentItemCode:', this.currentItemCode);

			// 🔧 수정된 유효성 검사
			if (!this.currentStockId) {
			    console.error('❌ currentStockId가 없습니다.');
			    StockUtils.showError('재고 ID 정보가 없습니다.');
			    return;
			}

			if (!this.currentItemId) {
			    console.error('❌ currentItemId가 없습니다.');
			    StockUtils.showError('품목 ID 정보가 없습니다.');
			    return;
			}

			if (!this.currentWarehouseId) {
			    console.error('❌ currentWarehouseId가 없습니다.');
			    StockUtils.showError('창고 ID 정보가 없습니다.');
			    return;
			}

			if (this.currentQuantity === undefined || this.currentQuantity === null) {
			    console.error('❌ currentQuantity가 없습니다.');
			    StockUtils.showError('재고 수량 정보가 없습니다.');
			    return;
			}

			if (!this.currentItemType) {
			    console.error('❌ currentItemType이 없습니다.');
			    StockUtils.showError('품목 유형 정보가 없습니다.');
			    return;
			}

			if (!this.currentItemCode) {
			    console.error('❌ currentItemCode가 없습니다.');
			    StockUtils.showError('품목 코드 정보가 없습니다.');
			    return;
			}
			
			 // ✅ 모든 유효성 검사 통과
			 console.log('✅ 모든 데이터 검증 완료, StockTransfer 호출');

			
			if (this.currentStockId && this.currentWarehouseId && 
			    this.currentQuantity !== undefined && this.currentItemType && this.currentItemCode) {
	            
	            // StockTransfer 모듈의 메서드 호출 (통일된 처리)
	            StockTransfer.openTransferModalWithData({
	                stockId: this.currentStockId,
					itemId: this.currentItemId,
	                warehouseId: this.currentWarehouseId,
	                warehouseName: this.currentWarehouseName,
	                quantity: this.currentQuantity,
	                itemName: this.currentItemName,
	                itemType: this.currentItemType,
					itemCode: this.currentItemCode
	            });
	            
	        } else {
	            console.error('❌ 필수 데이터 누락:', {
	                stockId: this.currentStockId,
					itemId: this.currentItemId,
	                warehouseId: this.currentWarehouseId,
	                quantity: this.currentQuantity,
	                itemType: this.currentItemType,
	                warehouseName: this.currentWarehouseName,
					itemCode: this.currentItemCode
	            });
	            StockUtils.showError('재고 정보가 부족하여 창고 이동을 진행할 수 없습니다.');
	        }
	    }
		
	};
	
// ===== StockActions (긴급출고 시 거래처 선택 추가) =====
const StockActions = {
    init() {
        console.log('StockActions 초기화');
        this.setupEvents();
    },
    
    setupEvents() {
        const stockInBtn = document.getElementById('stockInBtn');
        if (stockInBtn) {
            stockInBtn.addEventListener('click', () => this.openStockInModal());
        }
        
        const stockOutBtn = document.getElementById('stockOutBtn');
        if (stockOutBtn) {
            stockOutBtn.addEventListener('click', () => this.openBulkOutModal());
        }
        
        const confirmStockIn = document.getElementById('confirmStockIn');
        if (confirmStockIn) {
            confirmStockIn.addEventListener('click', () => this.processStockIn());
        }
        
        const excelDownloadBtn = document.getElementById('excelDownloadBtn');
        if (excelDownloadBtn) {
            excelDownloadBtn.addEventListener('click', () => this.downloadExcel());
        }

        const confirmTransferBtn = document.getElementById('confirmTransfer');
        if (confirmTransferBtn) {
            confirmTransferBtn.addEventListener('click', () => StockModal.processTransfer());
        }
        
        const quickOutBtn = document.getElementById('quickOutBtn');
        if (quickOutBtn) {
            console.log('✅ 긴급출고 버튼 이벤트 연결됨');
            quickOutBtn.addEventListener('click', () => {
                console.log('🚨 긴급출고 버튼 클릭됨!');
                this.openQuickOutModal();
            });
        } else {
            console.warn('⚠️ quickOutBtn을 찾을 수 없습니다!');
        }

        // 긴급출고 모달의 확인 버튼 이벤트
        const confirmQuickOut = document.getElementById('confirmQuickOut');
        if (confirmQuickOut) {
            confirmQuickOut.addEventListener('click', () => this.processQuickOutWithCustomer());
        }
    },
    
    openStockInModal() {
        console.log('입고 모달 열기');
        this.loadItems();
        this.loadWarehouses();
        
        const stockInForm = document.getElementById('stockInForm');
        if (stockInForm) {
            stockInForm.reset();
        }
        
        const modal = new bootstrap.Modal(document.getElementById('stockInModal'));
        modal.show();
    },
    
    loadItems() {
        fetch('/api/stocks/items?useYn=Y')
            .then(response => response.json())
            .then(items => {
                const select = document.getElementById('inItemId');
                if (select) {
                    select.innerHTML = '<option value="">품목을 선택하세요</option>';
                    items.forEach(item => {
                        select.innerHTML += `
                            <option value="${item.id}">
                                [${item.code}] ${item.name} (${item.type === 'raw' ? '자재' : '완제품'})
                            </option>
                        `;
                    });
                }
            })
            .catch(error => {
                console.error('품목 로딩 실패:', error);
                StockUtils.showError('품목 목록을 불러오는데 실패했습니다.');
            });
    },
    
    loadWarehouses() {
        fetch('/api/stocks/warehouses?useYn=Y')
            .then(response => response.json())
            .then(warehouses => {
                const select = document.getElementById('inWarehouseId');
                if (select) {
                    select.innerHTML = '<option value="">창고를 선택하세요</option>';
                    warehouses.forEach(wh => {
                        select.innerHTML += `
                            <option value="${wh.id}">
                                [${wh.warehouseCode}] ${wh.warehouseName}
                            </option>
                        `;
                    });
                }
            })
            .catch(error => {
                console.error('창고 로딩 실패:', error);
                StockUtils.showError('창고 목록을 불러오는데 실패했습니다.');
            });
    },
    
    processStockIn() {
        const form = document.getElementById('stockInForm');
        if (!form || !form.checkValidity()) {
            if (form) form.reportValidity();
            return;
        }
        
        const data = {
            itemId: parseInt(document.getElementById('inItemId').value),
            warehouseId: parseInt(document.getElementById('inWarehouseId').value),
            quantity: parseInt(document.getElementById('inQuantity').value),
            comment: document.getElementById('inComment').value
        };
        
        fetch('/api/stocks/in', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        })
        .then(response => response.json())
        .then(result => {
            if (result.status === 'success') {
                StockUtils.showSuccess(result.message);
                bootstrap.Modal.getInstance(document.getElementById('stockInModal')).hide();
                StockList.refresh();
            } else {
                StockUtils.showError(result.message || '입고 처리 실패');
            }
        })
        .catch(error => {
            console.error('입고 처리 오류:', error);
            StockUtils.showError('입고 처리 중 오류가 발생했습니다.');
        });
    },
    
    // 개선된 quickOut 메서드 (기존 유지)
    quickOut(stockId, currentQty) {
        if (currentQty === 0) {
            StockUtils.showError('재고가 없어 출고할 수 없습니다.');
            return;
        }

        const qty = prompt(`긴급 출고 수량을 입력하세요 (현재고: ${StockUtils.formatNumber(currentQty)}개)`);
        
        if (qty && parseInt(qty) > 0) {
            if (parseInt(qty) > currentQty) {
                StockUtils.showError('출고 수량이 현재고보다 많습니다.');
                return;
            }

            fetch(`/api/stocks/${stockId}/out`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ 
                    quantity: parseInt(qty),
                    comment: '긴급 출고'
                })
            })
            .then(response => response.json())
            .then(result => {
                if (result.status === 'success') {
                    StockUtils.showSuccess('긴급 출고가 완료되었습니다.');
                    StockList.refresh();
                    
                    const modalElement = document.getElementById('stockDetailModal');
                    if (modalElement) {
                        const modalInstance = bootstrap.Modal.getInstance(modalElement);
                        if (modalInstance) {
                            modalInstance.hide();
                        }
                    }
                } else {
                    StockUtils.showError(result.message || '긴급 출고 실패');
                }
            })
            .catch(error => {
                console.error('긴급 출고 오류:', error);
                StockUtils.showError('긴급 출고 처리 중 오류가 발생했습니다.');
            });
        }
    },
    
    // 재고 이력 조회 기능
    showHistory(stockId) {
        console.log('🔍 이력조회 시작 - stockId:', stockId);
        
        let modalElement = document.getElementById('stockHistoryModal');
        
        if (!modalElement) {
            console.log('🆕 모달이 없어서 동적 생성');
            const modalHtml = `
                <div class="modal fade" id="stockHistoryModal" tabindex="-1">
                    <div class="modal-dialog modal-lg">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">재고 입출고 이력</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <div id="historyInfo" class="mb-3">
                                    <div class="alert alert-info">
                                        <i class="fas fa-spinner fa-spin me-2"></i>
                                        데이터를 불러오는 중입니다...
                                    </div>
                                </div>
                                <div class="table-responsive">
                                    <table class="table table-bordered">
                                        <thead>
                                            <tr>
                                                <th>일시</th>
                                                <th>구분</th>
                                                <th>수량</th>
                                                <th>비고</th>
                                            </tr>
                                        </thead>
                                        <tbody id="historyTableBody">
                                            <tr>
                                                <td colspan="4" class="text-center py-3">
                                                    <i class="fas fa-spinner fa-spin me-2"></i>
                                                    로딩 중...
                                                </td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">닫기</button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            
            document.body.insertAdjacentHTML('beforeend', modalHtml);
            modalElement = document.getElementById('stockHistoryModal');
        }
        
        console.log('🚀 모달 표시!');
        const modal = new bootstrap.Modal(modalElement, {
            backdrop: 'static',
            keyboard: true
        });
        modal.show();
        
        setTimeout(() => {
            console.log('📡 데이터 로딩 시작...');
            
            fetch(`/api/stocks/${stockId}`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`재고 정보 조회 실패: ${response.status}`);
                    }
                    return response.json();
                })
                .then(stock => {
                    console.log('📦 재고 정보:', stock);
                    
                    const historyInfo = document.getElementById('historyInfo');
                    if (historyInfo) {
                        historyInfo.innerHTML = `
                            <div class="alert alert-info mb-0">
                                <strong>[${stock.itemCode}] ${stock.itemName}</strong> - 
                                ${stock.warehouseName} 창고
                            </div>
                        `;
                    }
                    
                    return fetch(`/api/stocks/${stockId}/history?page=0&size=50`);
                })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`이력 조회 실패: ${response.status}`);
                    }
                    return response.json();
                })
                .then(historyPage => {
                    console.log('📜 받은 이력 데이터:', historyPage);
                    
                    this.renderHistory(historyPage.content || []);
                    
                    console.log('✅ 이력 데이터 렌더링 완료!');
                })
                .catch(error => {
                    console.error('❌ 데이터 로딩 실패:', error);
                    
                    const historyInfo = document.getElementById('historyInfo');
                    const historyTableBody = document.getElementById('historyTableBody');
                    
                    if (historyInfo) {
                        historyInfo.innerHTML = `
                            <div class="alert alert-danger mb-0">
                                <i class="fas fa-exclamation-triangle me-2"></i>
                                오류: ${error.message}
                            </div>
                        `;
                    }
                    
                    if (historyTableBody) {
                        historyTableBody.innerHTML = `
                            <tr>
                                <td colspan="4" class="text-center text-danger py-3">
                                    데이터를 불러올 수 없습니다: ${error.message}
                                </td>
                            </tr>
                        `;
                    }
                    
                    StockUtils.showError('재고 이력을 불러올 수 없습니다: ' + error.message);
                });
        }, 100);
        
        modalElement.addEventListener('shown.bs.modal', () => {
            console.log('✅ 모달이 성공적으로 표시됨!');
        }, { once: true });
    },
	
	// 🆕 확인한 이력을 로컬스토리지에 저장
	markHistoryAsViewed(logId) {
	    const viewedLogs = JSON.parse(localStorage.getItem('viewedTransferLogs') || '[]');
	    if (!viewedLogs.includes(logId)) {
	        viewedLogs.push(logId);
	        localStorage.setItem('viewedTransferLogs', JSON.stringify(viewedLogs));
	    }
	},

	// 🆕 해당 이력이 이미 확인되었는지 체크
	isHistoryViewed(logId) {
	    const viewedLogs = JSON.parse(localStorage.getItem('viewedTransferLogs') || '[]');
	    return viewedLogs.includes(logId);
	},
    
	// 🆕 개선된 이력 렌더링 (NEW 배지 + 클릭 이벤트)
	renderHistory(logs) {
	    const tbody = document.getElementById('historyTableBody');
	    if (!tbody) return;
	    
	    tbody.innerHTML = '';

	    if (logs.length === 0) {
	        tbody.innerHTML = `
	            <tr>
	                <td colspan="4" class="text-center text-muted py-4">
	                    <i class="fas fa-inbox fa-2x mb-2 text-secondary"></i><br>
	                    재고 변동 이력이 없습니다.
	                </td>
	            </tr>
	        `;
	        return;
	    }
	    
	    logs.forEach((log, index) => {
	        const logTypeDisplay = this.getLogTypeDisplay(log.logType);
	        const quantityDisplay = this.getQuantityDisplay(log.logType, log.quantity);
	        const statusBadge = this.getStatusBadge(log.logStatus);

	        // 🆕 창고이동 입고 감지 및 최근 로그 판단
	        const isTransferIn = this.isTransferIncoming(log.comment);
	        const isRecent = this.isRecentLog(log.logDatetime); // 24시간 이내
	        const isViewed = this.isHistoryViewed(log.logId);

	        // 🆕 NEW 배지 (창고이동 입고이면서 최근이고 아직 안 본 경우만)
	        const shouldShowNew = isTransferIn && isRecent && !isViewed;
	        const newBadge = shouldShowNew ?
	            '<span class="badge bg-success ms-1 new-badge" style="animation: pulse 1.5s infinite;">NEW</span>' : '';

	        const row = document.createElement('tr');
	        
	        // 🆕 고유 ID 설정 (클릭 추적용)
	        row.setAttribute('data-log-id', log.logId);
	        
	        // 🆕 클릭 가능한 행 스타일 (NEW가 있는 경우만)
	        if (shouldShowNew) {
	            row.style.cursor = 'pointer';
	            row.classList.add('clickable-log-row');
	            row.style.transition = 'background-color 0.3s ease';
	            
	            // 호버 효과
	            row.addEventListener('mouseenter', () => {
	                row.style.backgroundColor = '#f8f9fa';
	            });
	            row.addEventListener('mouseleave', () => {
	                row.style.backgroundColor = '';
	            });
	            
	            // 🆕 클릭 이벤트 - NEW 배지 제거
	            row.addEventListener('click', () => {
	                this.handleLogRowClick(log.logId, row);
	            });
	        }

	        // 🆕 창고이동 입고인 경우 특별 스타일
	        if (isTransferIn) {
	            row.classList.add('transfer-incoming-row');
	            if (isRecent && !isViewed) {
	                row.style.backgroundColor = '#f8fff8'; // 연한 초록색 배경
	                row.style.borderLeft = '4px solid #28a745'; // 왼쪽 강조선
	            }
	        }

	        row.innerHTML = `
	            <td class="text-nowrap">
	                <small>${StockUtils.formatDateTime(log.logDatetime)}</small>
	                ${isRecent ? '<i class="fas fa-clock text-info ms-1" title="최근 기록"></i>' : ''}
	            </td>
	            <td class="text-center">
	                <span class="badge bg-${logTypeDisplay.color} rounded-pill">
	                    ${logTypeDisplay.label}${newBadge}
	                </span>
	                ${statusBadge ? `<br><small>${statusBadge}</small>` : ''}
	                ${shouldShowNew ? '<br><small class="text-muted">클릭하여 확인</small>' : ''}
	            </td>
	            <td class="text-end">
	                <strong class="${quantityDisplay.class}">
	                    ${quantityDisplay.text}
	                </strong>
	            </td>
	            <td>
	                <small class="text-muted">
	                    ${this.highlightTransferInfo(StockUtils.escapeHtml(log.comment || log.reason || '-'))}
	                </small>
	            </td>
	        `;
	        tbody.appendChild(row);
	    });
	    
	    // 🆕 CSS 애니메이션 동적 추가
	    this.addPulseAnimation();
	},
	
	// 🆕 로그 행 클릭 처리
	handleLogRowClick(logId, rowElement) {
	    console.log('📋 이력 행 클릭 - logId:', logId);
	    
	    // 확인 처리
	    this.markHistoryAsViewed(logId);
	    
	    // NEW 배지 제거
	    const newBadge = rowElement.querySelector('.new-badge');
	    if (newBadge) {
	        newBadge.style.animation = 'fadeOut 0.5s ease-out';
	        setTimeout(() => {
	            newBadge.remove();
	        }, 500);
	    }
	    
	    // 행 스타일 원래대로 복원
	    rowElement.style.backgroundColor = '';
	    rowElement.style.borderLeft = '';
	    rowElement.style.cursor = '';
	    rowElement.classList.remove('clickable-log-row');
	    
	    // "클릭하여 확인" 텍스트 제거
	    const clickText = rowElement.querySelector('small:contains("클릭하여 확인")');
	    if (clickText) {
	        clickText.remove();
	    }
	    
	    // 클릭 이벤트 제거
	    rowElement.replaceWith(rowElement.cloneNode(true));
	    
	    // 성공 피드백
	    StockUtils.showToast('이력을 확인했습니다.', 'info', 2000);
	},
	
	// 🆕 펄스 애니메이션 CSS 동적 추가
	addPulseAnimation() {
	    // 이미 추가되었는지 확인
	    if (document.getElementById('pulseAnimationStyle')) return;
	    
	    const style = document.createElement('style');
	    style.id = 'pulseAnimationStyle';
	    style.textContent = `
	        @keyframes pulse {
	            0% { opacity: 1; }
	            50% { opacity: 0.5; }
	            100% { opacity: 1; }
	        }
	        
	        @keyframes fadeOut {
	            0% { opacity: 1; }
	            100% { opacity: 0; }
	        }
	        
	        .new-badge {
	            font-size: 0.7em;
	            font-weight: bold;
	            text-shadow: 0 0 2px rgba(255,255,255,0.8);
	        }
	        
	        .clickable-log-row:hover {
	            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
	        }
	    `;
	    document.head.appendChild(style);
	},
	
	// 🆕 최근 로그 판단 (24시간 이내)
	isRecentLog(logDatetime) {
	    if (!logDatetime) return false;

	    const logTime = new Date(logDatetime);
	    const now = new Date();
	    const hoursDiff = (now - logTime) / (1000 * 60 * 60);

	    return hoursDiff <= 24;
	},
	
	// 🆕 창고이동 정보 하이라이트
	highlightTransferInfo(comment) {
	    if (!comment) return comment;

	    // 창고이동 관련 키워드 하이라이트
	    return comment
	        .replace(/\[창고이동-입고\]/g, '<span class="badge bg-success">창고이동-입고</span>')
	        .replace(/\[창고이동-출고\]/g, '<span class="badge bg-danger">창고이동-출고</span>')
	        .replace(/(\w+창고\s*\d*)\s*←\s*(\w+창고\s*\d*)/g,
	            '<strong class="text-success">$1</strong> ← <strong class="text-primary">$2</strong>')
	        .replace(/(\w+창고\s*\d*)\s*→\s*(\w+창고\s*\d*)/g,
	            '<strong class="text-primary">$1</strong> → <strong class="text-success">$2</strong>');
	},

	// 🆕 창고이동 입고 감지 메서드
	isTransferIncoming(comment) {
	    if (!comment) return false;

	    // 창고이동-입고 패턴 감지
	    const transferInPatterns = [
	        '[창고이동-입고]',
	        '창고간 이동으로 생성',
	        '← '  // "창고A ← 창고B" 패턴
	    ];
	    
	    return transferInPatterns.some(pattern => 
	        comment.includes(pattern)
	    );
	},
	
	// 상태 배지 표시 메서드 (수정됨)
	getStatusBadge(logStatus) {
	    if (!logStatus) return '';
	    
	    const statusMap = {
	        'PENDING': '<span class="badge bg-warning text-dark">대기</span>',
	        'CONFIRMED': '<span class="badge bg-success">확정</span>'
	    };
	    
	    return statusMap[logStatus] || '';
	},

	getLogTypeDisplay(logType) {
	    const types = {
	        'IN': { label: '입고', color: 'success' },
	        'OUT': { label: '출고', color: 'danger' },
	        'TRANSFER': { label: '창고이동', color: 'info' }
	    };
	    
	    return types[logType] || { label: logType, color: 'secondary' };
	},
	
	
	getQuantityDisplay(logType, quantity) {
	    const isDecrease = ['OUT', 'TRANSFER_OUT', 'DISPOSE'].includes(logType);
	    
	    return {
	        text: `${isDecrease ? '-' : '+'}${StockUtils.formatNumber(quantity)}`,
	        class: isDecrease ? 'text-danger' : 'text-success'
	    };
	},
    
    openBulkOutModal() {
        const selectedItems = document.querySelectorAll('.stock-checkbox:checked');
        if (selectedItems.length === 0) {
            StockUtils.showError('출고할 재고를 선택해주세요.');
            return;
        }
        
        StockUtils.showInfo('일괄 출고 기능은 준비 중입니다.');
    },
    
    downloadExcel() {
        const params = new URLSearchParams({
            keyword: StockState.filters.keyword || '',
            whs: StockState.filters.warehouse || '',
            itemType: StockState.filters.itemType || '',
            stockStatus: StockState.filters.stockStatus || ''
        });

        window.location.href = `/api/stocks/excel?${params}`;
    },
    
    // ===== 🆕 개선된 긴급출고 기능 (거래처 선택 포함) =====
    
    openQuickOutModal() {
        const selectedItems = document.querySelectorAll('.stock-checkbox:checked');
        
        if (selectedItems.length === 0) {
            StockUtils.showWarning('긴급 출고할 재고를 선택해주세요.');
            return;
        }
        
        if (selectedItems.length > 1) {
            StockUtils.showWarning('긴급 출고는 한 번에 하나의 재고만 처리할 수 있습니다.');
            return;
        }
        
        const stockId = selectedItems[0].dataset.stockId;
        console.log('🚨 긴급출고 요청 - stockId:', stockId);
        
        // 긴급출고 모달이 없으면 동적 생성
        if (!document.getElementById('quickOutModal')) {
            this.createQuickOutModal();
        }
        
        this.loadStockForQuickOut(stockId);
    },
	
	

    // 긴급출고 모달 동적 생성
    createQuickOutModal() {
        const modalHtml = `
            <div class="modal fade" id="quickOutModal" tabindex="-1" aria-hidden="true">
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">
                                <i class="fas fa-shipping-fast me-2 text-danger"></i>긴급 출고
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <form id="quickOutForm">
                                <!-- 재고 정보 표시 -->
                                <div class="alert alert-warning">
                                    <strong>🚨 긴급 출고</strong><br>
                                    <strong>품목:</strong> <span id="quickOutItemInfo">-</span><br>
                                    <strong>현재고:</strong> <span id="quickOutCurrentQty">0</span>개
                                </div>
                                
                                <!-- 거래처 선택 -->
                                <div class="mb-3">
                                    <label for="quickOutCustomer" class="form-label">
                                        거래처 <span class="text-danger">*</span>
                                    </label>
                                    <select class="form-select" id="quickOutCustomer" required>
                                        <option value="">거래처를 선택하세요</option>
                                    </select>
                                    <div class="form-text text-muted">
                                        <i class="fas fa-info-circle me-1"></i>
                                        출고할 거래처를 선택해주세요.
                                    </div>
                                </div>
                                
                                <!-- 출고 수량 -->
                                <div class="mb-3">
                                    <label for="quickOutQty" class="form-label">
                                        출고 수량 <span class="text-danger">*</span>
                                    </label>
                                    <input type="number" class="form-control" id="quickOutQty" 
                                           min="1" max="10" value="1" required>
                                    <div class="form-text">
                                        <span class="text-warning">
                                            <i class="fas fa-exclamation-triangle me-1"></i>
                                            긴급 출고는 최대 10개까지만 가능합니다.
                                        </span><br>
                                        현재고: <span id="quickOutAvailableQty">0</span>개
                                    </div>
                                </div>
                                
                                <!-- 출고 사유 -->
                                <div class="mb-3">
                                    <label for="quickOutReason" class="form-label">출고 사유</label>
                                    <select class="form-select" id="quickOutReason">
                                        <option value="긴급 출고">긴급 출고</option>
                                        <option value="긴급 주문">긴급 주문</option>
                                        <option value="생산 투입">생산 투입</option>
                                        <option value="품질 검사">품질 검사</option>
                                        <option value="기타">기타</option>
                                    </select>
                                </div>
                                
                                <!-- 비고 -->
                                <div class="mb-3">
                                    <label for="quickOutComment" class="form-label">비고</label>
                                    <textarea class="form-control" id="quickOutComment" rows="2" 
                                              placeholder="추가 설명이나 특이사항을 입력하세요"></textarea>
                                </div>
                            </form>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                            <button type="button" class="btn btn-danger" id="confirmQuickOut">
                                <i class="fas fa-shipping-fast me-2"></i>긴급 출고 실행
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        
        // 이벤트 리스너 연결
        const confirmBtn = document.getElementById('confirmQuickOut');
        if (confirmBtn) {
            confirmBtn.addEventListener('click', () => this.processQuickOutWithCustomer());
        }

        // 수량 입력 검증
        const qtyInput = document.getElementById('quickOutQty');
        if (qtyInput) {
            qtyInput.addEventListener('input', () => this.validateQuickOutQty());
        }
        
        console.log('✅ 긴급출고 모달 동적 생성 완료');
    },

    loadStockForQuickOut(stockId) {
        // 1. 재고 정보 조회
        fetch(`/api/stocks/${stockId}`)
            .then(response => response.json())
            .then(stock => {
                if (stock.quantity <= 0) {
                    StockUtils.showError('재고가 없어 출고할 수 없습니다.');
                    return;
                }
                
                // 모달에 재고 정보 표시
                this.fillQuickOutModal(stock);
                
                // 거래처 목록 로딩
                this.loadCustomersForQuickOut();
                
                // 모달 표시
                const modal = new bootstrap.Modal(document.getElementById('quickOutModal'));
                modal.show();
            })
            .catch(error => {
                StockUtils.handleApiError(error, '재고 정보를 불러오는데 실패했습니다.');
            });
    },

    fillQuickOutModal(stock) {
        // 재고 정보를 전역 변수에 저장
        this.currentQuickOutStock = stock;
        
        // 모달 필드 업데이트
        const updateElement = (id, value, isInput = false) => {
            const element = document.getElementById(id);
            if (element) {
                if (isInput) {
                    element.value = value;
                } else {
                    element.textContent = value;
                }
            }
        };

        updateElement('quickOutItemInfo', `[${stock.itemCode}] ${stock.itemName}`);
        updateElement('quickOutCurrentQty', StockUtils.formatNumber(stock.quantity));
        updateElement('quickOutAvailableQty', stock.quantity);
        
        // 수량 입력 최대값 설정
        const qtyInput = document.getElementById('quickOutQty');
        if (qtyInput) {
            qtyInput.max = Math.min(stock.quantity, 10);
            qtyInput.value = 1;
        }
        
        // 폼 초기화
        updateElement('quickOutCustomer', '', true);
        updateElement('quickOutReason', '긴급 출고', true);
        updateElement('quickOutComment', '', true);
    },

	// 거래처 목록 로딩
	loadCustomersForQuickOut() {
	    console.log('🏢 거래처 목록 로딩 시작...');
	    
	    // 올바른 API 엔드포인트 호출
	    fetch('/api/customers?useYn=Y')
	        .then(response => {
	            console.log('거래처 API 응답 상태:', response.status);
	                
	            if (!response.ok) {
	                throw new Error(`API 응답 오류: ${response.status} ${response.statusText}`);
	            }
	                
	            return response.json();
	        })
	        .then(customers => {
	            console.log('✅ 거래처 목록 수신 성공:', customers);

	            const select = document.getElementById('quickOutCustomer');
	            if (!select) {
	                console.error('❌ quickOutCustomer 엘리먼트를 찾을 수 없음');
	                return;
	            }

	            // 기본 옵션으로 초기화
	            select.innerHTML = '<option value="">거래처를 선택하세요</option>';

	            if (customers && Array.isArray(customers) && customers.length > 0) {
	                customers.forEach(customer => {
	                    // 안전한 데이터 추출
	                    const customerId = customer.id || 0;
	                    const customerCode = customer.customerCode || `CUST${customerId}`;
	                    const customerName = customer.customerName || '이름없음';
	                    const companyType = customer.companyType || 'CUSTOMER';

	                    // 옵션 추가
	                    const option = document.createElement('option');
	                    option.value = customerId;
	                    option.setAttribute('data-customer-code', customerCode);
	                    option.setAttribute('data-company-type', companyType);
	                    option.textContent = `[${customerCode}] ${customerName}`;

	                    select.appendChild(option);

	                    console.log(`  - 거래처 추가: ${customerName} (${customerCode})`);
	                });

	                console.log('✅ 거래처 옵션 생성 완료:', customers.length + '개');

	            } else {
	                console.warn('⚠️ 거래처 목록이 비어있거나 올바르지 않음:', customers);

	                select.innerHTML += `<option value="" disabled>사용 가능한 거래처가 없습니다</option>`;

	                // 긴급출고 버튼 비활성화
	                const confirmBtn = document.getElementById('confirmQuickOut');
	                if (confirmBtn) {
	                    confirmBtn.disabled = true;
	                    confirmBtn.textContent = '거래처 없음';
	                }
	            }
	        })
	        .catch(error => {
	            console.error('❌ 거래처 목록 로딩 실패:', error);
	            StockUtils.showError('거래처 목록을 불러오는데 실패했습니다: ' + error.message);

	            // 실패 시 기본 거래처만 표시
	            const select = document.getElementById('quickOutCustomer');
	            if (select) {
	                select.innerHTML = `
	                    <option value="">거래처를 선택하세요</option>
	                    <option value="0">기본 거래처</option>
	                `;
	            }
	        });
	},

    // 긴급출고 수량 검증
    validateQuickOutQty() {
        const qtyInput = document.getElementById('quickOutQty');
        const availableQtyElement = document.getElementById('quickOutAvailableQty');
        
        if (qtyInput && availableQtyElement && this.currentQuickOutStock) {
            const qty = parseInt(qtyInput.value);
            const availableQty = this.currentQuickOutStock.quantity;
            
            if (qty > 10) {
                StockUtils.showWarning('긴급 출고는 최대 10개까지만 가능합니다.');
                qtyInput.value = 10;
            } else if (qty > availableQty) {
                StockUtils.showWarning(`출고 수량이 현재고(${availableQty})보다 많습니다.`);
                qtyInput.value = availableQty;
            }
        }
    },

    // 🆕 거래처 포함 긴급출고 처리
    processQuickOutWithCustomer() {
        const form = document.getElementById('quickOutForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        
        if (!this.currentQuickOutStock) {
            StockUtils.showError('재고 정보가 없습니다.');
            return;
        }
		// 폼 데이터 수집
        const customerId = document.getElementById('quickOutCustomer').value;
        const quantity = parseInt(document.getElementById('quickOutQty').value);
        const reason = document.getElementById('quickOutReason').value;
        const comment = document.getElementById('quickOutComment').value;

		// 유효성 검사
		if (!customerId || customerId === '') {
		    StockUtils.showError('거래처를 선택해주세요.');
		    return;
		}

		if (!quantity || quantity <= 0) {
		    StockUtils.showError('출고 수량을 입력해주세요.');
		    return;
		}

		if (quantity > this.currentQuickOutStock.quantity) {
		    StockUtils.showError('출고 수량이 현재고보다 많습니다.');
		    return;
		}

		if (quantity > 10) {
		    StockUtils.showError('긴급 출고는 최대 10개까지만 가능합니다.');
		    return;
		}
		
		// 확인 메시지 생성
		const customerSelect = document.getElementById('quickOutCustomer');
		const selectedOption = customerSelect.selectedOptions[0];
		const customerText = selectedOption ? selectedOption.textContent : '선택된 거래처';
        
		const confirmMsg = `정말로 긴급 출고하시겠습니까?\n\n` +
		                  `품목: ${this.currentQuickOutStock.itemName}\n` +
		                  `거래처: ${customerText}\n` +
		                  `수량: ${quantity}개\n` +
		                  `사유: ${reason}`;

		if (!confirm(confirmMsg)) return;

        // 긴급출고 요청 데이터 구성
		const outData = {
		    itemId: this.currentQuickOutStock.itemId,
		    warehouseId: this.currentQuickOutStock.warehouseId,
		    companyId: parseInt(customerId),  // Long 타입으로 변환
		    quantity: quantity,
		    reason: reason || '긴급 출고',
		    comment: comment || ''
		};
		console.log('🚨 긴급출고 요청 데이터:', outData);
		
		// API 호출
        fetch('/api/stocks/emergency-out', {
            method: 'POST',
            headers: { 
				'Content-Type': 'application/json', 
				'Accept': 'application/json'
			},
            body: JSON.stringify(outData)
        })
		
		.then(response => {
		    console.log('긴급출고 API 응답 상태:', response.status);
		    
		    if (!response.ok) {
		        throw new Error(`서버 오류: ${response.status} ${response.statusText}`);
		    }
		    
		    return response.json();
		})
		
        .then(result => {
			
			console.log('긴급출고 API 응답:', result);
			
			if (result.status === 'success') {
			    StockUtils.showSuccess(`${this.currentQuickOutStock.itemName} ${quantity}개 긴급 출고가 완료되었습니다.`);
			    
			    // 모달 닫기
			    const modal = bootstrap.Modal.getInstance(document.getElementById('quickOutModal'));
			    if (modal) modal.hide();
                
                // 목록 새로고침
                StockList.refresh();
                
                // 선택 해제
                document.querySelectorAll('.stock-checkbox:checked').forEach(cb => {
                    cb.checked = false;
                });
            } else {
                StockUtils.showError(result.message || '긴급 출고 처리에 실패했습니다.');
            }
        })
        .catch(error => {
            console.error('긴급 출고 오류:', error);
            StockUtils.showError('긴급 출고 처리 중 오류가 발생했습니다.'+ error.message);
        });
    },

    // 기존 quickOut 메서드는 단순 prompt 방식으로 유지 (상세 모달에서 사용)
    quickOutSimple(stockId, currentQty) {
        if (currentQty === 0) {
            StockUtils.showError('재고가 없어 출고할 수 없습니다.');
            return;
        }

        const qty = prompt(`긴급 출고 수량을 입력하세요 (현재고: ${StockUtils.formatNumber(currentQty)}개)`);
        
        if (qty && parseInt(qty) > 0) {
            if (parseInt(qty) > currentQty) {
                StockUtils.showError('출고 수량이 현재고보다 많습니다.');
                return;
            }

            // 간단한 출고 처리 (거래처 없이)
            fetch(`/api/stocks/${stockId}/out`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ 
                    quantity: parseInt(qty),
                    comment: '긴급 출고'
                })
            })
            .then(response => response.json())
            .then(result => {
                if (result.status === 'success') {
                    StockUtils.showSuccess('긴급 출고가 완료되었습니다.');
                    StockList.refresh();
                    
                    const modalElement = document.getElementById('stockDetailModal');
                    if (modalElement) {
                        const modalInstance = bootstrap.Modal.getInstance(modalElement);
                        if (modalInstance) {
                            modalInstance.hide();
                        }
                    }
                } else {
                    StockUtils.showError(result.message || '긴급 출고 실패');
                }
            })
            .catch(error => {
                console.error('긴급 출고 오류:', error);
                StockUtils.showError('긴급 출고 처리 중 오류가 발생했습니다.');
            });
        }
    }
};

// ===== StockFilter (기존과 동일) =====
const StockFilter = {
    init() {
        console.log('StockFilter 초기화');
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

// ===== StockTransfer (기존 기능 유지) =====
const StockTransfer = {
    selectedStock: null,
    
    init() {
        console.log('StockTransfer 초기화');
        this.setupEvents();
    },
    
    setupEvents() {
        const transferBtn = document.getElementById('stockTransferBtn');
        if (transferBtn) {
            transferBtn.addEventListener('click', () => this.openTransferModal());
        }
        
        const confirmBtn = document.getElementById('confirmTransfer');
        if (confirmBtn) {
            confirmBtn.addEventListener('click', () => this.processTransfer());
        }
        
        const toWarehouseSelect = document.getElementById('toWarehouse');
        if (toWarehouseSelect) {
            toWarehouseSelect.addEventListener('change', () => this.validateWarehouseSelection());
        }
        
        const transferQtyInput = document.getElementById('transferQty');
        if (transferQtyInput) {
            transferQtyInput.addEventListener('input', () => this.validateTransferQty());
        }
    },
    
	// 🔧 기존 메서드는 유지하되, 새로운 메서드도 활용
	openTransferModal() {
	    const selectedItems = document.querySelectorAll('.stock-checkbox:checked');
	    
	    if (selectedItems.length === 0) {
	        StockUtils.showWarning('창고 이동할 재고를 선택해주세요.');
	        return;
	    }
	    
	    if (selectedItems.length > 1) {
	        StockUtils.showWarning('창고 이동은 한 번에 하나의 재고만 처리할 수 있습니다.');
	        return;
	    }
	    
	    const stockId = selectedItems[0].dataset.stockId;
	    console.log('🔄 목록에서 창고이동 요청 - stockId:', stockId);
	    
	    // 기존 방식: API 호출해서 데이터 가져오기
	    this.loadStockForTransfer(stockId);
	},

    createTransferModalDynamically() {
        const modalHtml = `
            <div class="modal fade" id="stockTransferModal" tabindex="-1" aria-hidden="true">
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">
                                <i class="fas fa-exchange-alt me-2"></i>창고 이동
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <form id="stockTransferForm">
                                <div class="alert alert-info">
                                    <strong>이동할 재고:</strong> <span id="transferStockInfo">-</span><br>
                                    <strong>현재 수량:</strong> <span id="transferCurrentQty">0</span>개
                                </div>
                                
                                <div class="mb-3">
                                    <label for="fromWarehouse" class="form-label">출발 창고</label>
                                    <input type="text" class="form-control" id="fromWarehouse" readonly>
                                </div>
                                
                                <div class="mb-3">
                                    <label for="toWarehouse" class="form-label">도착 창고 <span class="text-danger">*</span></label>
                                    <select class="form-select" id="toWarehouse" required>
                                        <option value="">도착 창고를 선택하세요</option>
                                    </select>
                                    <div id="transferGuideMessage"></div>
                                    <div class="form-text text-muted mt-2">
                                        <i class="fas fa-lightbulb me-1"></i>
                                        품목 유형에 맞는 창고만 선택 가능합니다.
                                    </div>
                                </div>
                                
                                <div class="mb-3">
                                    <label for="transferQty" class="form-label">이동 수량 <span class="text-danger">*</span></label>
                                    <input type="number" class="form-control" id="transferQty" min="1" required>
                                    <div class="form-text">현재고: <span id="availableQty">0</span>개</div>
                                </div>
                                
                                <div class="mb-3">
                                    <label for="transferReason" class="form-label">이동 사유</label>
                                    <select class="form-select" id="transferReason">
                                        <option value="">사유를 선택하세요</option>
                                        <option value="PRODUCTION">생산 투입</option>
                                        <option value="SHIPPING_PREPARE">출하 준비</option>
                                        <option value="REORGANIZATION">창고 정리</option>
                                        <option value="MAINTENANCE">창고 보수</option>
                                        <option value="OTHER">기타</option>
                                    </select>
                                </div>
                                
                                <div class="mb-3">
                                    <label for="transferComment" class="form-label">비고</label>
                                    <textarea class="form-control" id="transferComment" rows="3" 
                                              placeholder="추가 설명이나 특이사항을 입력하세요"></textarea>
                                </div>
                            </form>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                            <button type="button" class="btn btn-primary" id="confirmTransfer">
                                <i class="fas fa-exchange-alt me-2"></i>이동 실행
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        
        const confirmBtn = document.getElementById('confirmTransfer');
        if (confirmBtn) {
            confirmBtn.addEventListener('click', () => this.processTransfer());
        }
        
        console.log('✅ 창고이동 모달 동적 생성 완료');
    },
    
    loadStockForTransfer(stockId) {
        fetch(`/api/stocks/${stockId}`)
            .then(response => response.json())
            .then(stock => {
                this.selectedStock = stock;
                this.fillTransferModal(stock);
                this.loadWarehousesForTransfer(stock.warehouseId, stock.itemType);
                
                const modal = new bootstrap.Modal(document.getElementById('stockTransferModal'));
                modal.show();
            })
            .catch(error => {
                StockUtils.handleApiError(error, '재고 정보를 불러오는데 실패했습니다.');
            });
    },
    
    fillTransferModal(stock) {
        const updateElement = (id, value, isInput = false) => {
            const element = document.getElementById(id);
            if (element) {
                if (isInput || element.tagName === 'INPUT' || element.tagName === 'SELECT' || element.tagName === 'TEXTAREA') {
                    element.value = value;
                } else {
                    element.textContent = value;
                }
                return true;
            }
            console.warn(`⚠️ Element not found: ${id}`);
            return false;
        };

        updateElement('transferStockInfo', `[${stock.itemCode}] ${stock.itemName}`);
        updateElement('transferCurrentQty', StockUtils.formatNumber(stock.quantity));
        updateElement('fromWarehouse', stock.warehouseName, true);
        updateElement('availableQty', stock.quantity);
        
        updateElement('toWarehouse', '', true);
        updateElement('transferQty', '', true);
        updateElement('transferReason', '', true);
        updateElement('transferComment', '', true);
    },
    
    loadWarehousesForTransfer(currentWarehouseId, itemType) {
        fetch('/api/stocks/warehouses?useYn=Y')
            .then(response => response.json())
            .then(warehouses => {
                const select = document.getElementById('toWarehouse');
                if (select) {
                    select.innerHTML = '<option value="">도착 창고를 선택하세요</option>';
                    
                    const filteredWarehouses = warehouses.filter(wh => {
                        if (wh.id === currentWarehouseId) return false;
                        if (wh.useYn !== 'Y') return false;
                        
                        if (itemType === 'product') {
                            return wh.warehouseType === 'PRODUCT';
                        } else if (itemType === 'raw') {
                            return wh.warehouseType === 'RAW';
                        }
                        
                        return true;
                    });

                    if (filteredWarehouses.length > 0) {
                        filteredWarehouses.forEach(wh => {
                            const typeLabel = wh.warehouseType === 'PRODUCT' ? '완제품' : '자재';
                            select.innerHTML += `
                                <option value="${wh.id}" 
                                        data-warehouse-name="${wh.warehouseName}"
                                        data-warehouse-type="${wh.warehouseType}">
                                    [${wh.warehouseCode}] ${wh.warehouseName} (${typeLabel})
                                </option>
                            `;
                        });
                    } else {
                        const noWarehouseMsg = itemType === 'product' 
                            ? '이동 가능한 완제품 창고가 없습니다'
                            : '이동 가능한 자재 창고가 없습니다';
                        
                        select.innerHTML += `<option value="" disabled>${noWarehouseMsg}</option>`;
                    }
                    
                    this.showTransferGuideMessage(itemType);
                }
            })
            .catch(error => {
                console.error('창고 목록 로딩 실패:', error);
            });
    },

    showTransferGuideMessage(itemType) {
        const guideElement = document.getElementById('transferGuideMessage');
        if (!guideElement) {
            this.createTransferGuideElement();
        }
        
        const guide = document.getElementById('transferGuideMessage');
        if (guide) {
            let message = '';
            
            if (itemType === 'product') {
                message = '완제품은 완제품 창고로만 이동 가능합니다.';
            } else if (itemType === 'raw') {
                message = '자재는 자재 창고로만 이동 가능합니다.';
            } else {
                message = '⚠️ 품목 유형에 맞는 창고를 선택해주세요.';
            }
            
            guide.innerHTML = `
                <div class="alert alert-info small mt-2">
                    <i class="fas fa-info-circle me-2"></i>
                    ${message}
                </div>
            `;
        }
    },

    createTransferGuideElement() {
        const toWarehouseDiv = document.getElementById('toWarehouse')?.parentElement;
        if (toWarehouseDiv) {
            const guideDiv = document.createElement('div');
            guideDiv.id = 'transferGuideMessage';
            toWarehouseDiv.appendChild(guideDiv);
        }
    },
    
    validateWarehouseSelection() {
        const toWarehouse = document.getElementById('toWarehouse');
        const fromWarehouse = document.getElementById('fromWarehouse');
        
        if (toWarehouse && fromWarehouse) {
            const selectedOption = toWarehouse.selectedOptions[0];
            if (selectedOption && selectedOption.dataset.warehouseName === fromWarehouse.value) {
                StockUtils.showWarning('출발 창고와 도착 창고가 같을 수 없습니다.');
                toWarehouse.value = '';
            }
        }
    },
    
    validateTransferQty() {
        const transferQtyInput = document.getElementById('transferQty');
        const availableQtyInput = document.getElementById('availableQty');
        
        if (transferQtyInput && availableQtyInput) {
            const transferQty = parseInt(transferQtyInput.value);
            const availableQty = parseInt(availableQtyInput.value);
            
            if (transferQty > availableQty) {
                StockUtils.showWarning(`이동 수량이 현재고(${availableQty})보다 많습니다.`);
                transferQtyInput.value = availableQty;
            }
        }
    },
	
	// 🆕 창고이동 성공 시 로컬스토리지에 기록
	recordSuccessfulTransfer(stockData, transferData) {
	    const transferRecord = {
	        stockId: stockData.stockId,
	        itemId: stockData.itemId,
	        itemName: stockData.itemName,
	        fromWarehouse: transferData.fromWarehouseName,
	        toWarehouse: transferData.toWarehouseName,
	        quantity: transferData.quantity,
	        timestamp: new Date().toISOString(),
	        viewed: false
	    };

	    // 최근 창고이동 목록에 추가
	    const recentTransfers = JSON.parse(localStorage.getItem('recentTransferStocks') || '{}');
	    recentTransfers[stockData.stockId] = transferRecord;
	    localStorage.setItem('recentTransferStocks', JSON.stringify(recentTransfers));

	    console.log('✅ 창고이동 추적 기록됨:', transferRecord);
	},
	
	// 🆕 수정된 창고이동 처리 (추적 기능 포함)
	processTransfer() {
	    const form = document.getElementById('stockTransferForm');
	    if (!form.checkValidity()) {
	        form.reportValidity();
	        return;
	    }
	    
	    if (!this.selectedStock) {
	        StockUtils.showError('선택된 재고 정보가 없습니다.');
	        return;
	    }

	    const toWarehouseSelect = document.getElementById('toWarehouse');
	    const selectedOption = toWarehouseSelect.selectedOptions[0];
	    const selectedWarehouseType = selectedOption?.dataset.warehouseType;
	    
	    if (!this.validateWarehouseTypeMatch(this.selectedStock.itemType, selectedWarehouseType)) {
	        return;
	    }
	    
	    const data = {
	        fromStockId: this.selectedStock.stockId,
	        fromWarehouseId: this.selectedStock.warehouseId,
	        toWarehouseId: parseInt(document.getElementById('toWarehouse').value),
	        itemId: this.selectedStock.itemId,
	        quantity: parseInt(document.getElementById('transferQty').value),
	        reason: document.getElementById('transferReason').value,
	        comment: document.getElementById('transferComment').value
	    };
	    
	    const fromWarehouseName = this.selectedStock.warehouseName;
	    const toWarehouseName = selectedOption?.text.split(']')[1]?.split('(')[0]?.trim() || '선택된 창고';
	    
	    const confirmBtn = document.getElementById('confirmTransfer');
	    const originalText = confirmBtn.textContent;
	    confirmBtn.disabled = true;
	    confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>처리 중...';
	    
	    fetch('/api/stocks/transfer', {
	        method: 'POST',
	        headers: { 'Content-Type': 'application/json' },
	        body: JSON.stringify(data)
	    })
	    .then(response => {
	        if (!response.ok) {
	            throw new Error(`서버 오류: ${response.status}`);
	        }
	        return response.json();
	    })
	    .then(result => {
	        if (result.status === 'success') {
	            // 🆕 창고이동 추적 기록
	            this.recordSuccessfulTransfer(this.selectedStock, {
	                fromWarehouseName,
	                toWarehouseName,
	                quantity: data.quantity
	            });
	            
	            // 🆕 특별 성공 알림
	            StockUtils.showTransferSuccess(
	                fromWarehouseName, 
	                toWarehouseName, 
	                data.quantity, 
	                this.selectedStock.itemName
	            );
	            
	            bootstrap.Modal.getInstance(document.getElementById('stockTransferModal')).hide();
	            StockList.refresh();
	            
	            // 선택 해제
	            document.querySelectorAll('.stock-checkbox:checked').forEach(cb => {
	                cb.checked = false;
	            });
	        } else {
	            StockUtils.showError(result.message || '창고 이동에 실패했습니다.');
	        }
	    })
	    .catch(error => {
	        console.error('창고 이동 오류:', error);
	        StockUtils.showError('창고 이동 중 오류가 발생했습니다.');
	    })
	    .finally(() => {
	        confirmBtn.disabled = false;
	        confirmBtn.textContent = originalText;
	    });
	},

    validateWarehouseTypeMatch(itemType, warehouseType) {
        let isValid = false;
        let errorMessage = '';

        if (itemType === 'product' && warehouseType === 'PRODUCT') {
            isValid = true;
        } else if (itemType === 'raw' && warehouseType === 'RAW') {
            isValid = true;
        } else if (itemType === 'product' && warehouseType === 'RAW') {
            errorMessage = '❌ 완제품은 자재 창고로 이동할 수 없습니다.\n완제품 창고를 선택해주세요.';
        } else if (itemType === 'raw' && warehouseType === 'PRODUCT') {
            errorMessage = '❌ 자재는 완제품 창고로 이동할 수 없습니다.\n자재 창고를 선택해주세요.';
        } else {
            errorMessage = '⚠️ 품목 유형에 맞는 창고를 선택해주세요.';
        }

        if (!isValid) {
            StockUtils.showError(errorMessage);
            alert(errorMessage);
            document.getElementById('toWarehouse').value = '';
            return false;
        }

        return true;
    },
	
	// 🆕 데이터 객체로 모달 열기 (통일된 처리)
	openTransferModalWithData(stockData) {
	    console.log('🔄 창고이동 모달 열기 - 받은 데이터:', stockData);
	    
	    // 가짜 재고 객체 생성 (기존 구조와 호환)
	    this.selectedStock = {
	        stockId: stockData.stockId,
	        itemId: stockData.itemId || stockData.stockId, // 🔧 실제 itemId 사용 (fallback)
	        itemCode: stockData.itemCode || 'Unknown',
	        itemName: stockData.itemName,
	        itemType: stockData.itemType,
	        warehouseId: stockData.warehouseId,
	        warehouseName: stockData.warehouseName,
	        quantity: stockData.quantity
	    };
		// 🔍 디버깅 로그
		console.log('📋 selectedStock 설정:');
		console.log('  stockId:', this.selectedStock.stockId);
		console.log('  itemId:', this.selectedStock.itemId);
		console.log('  itemName:', this.selectedStock.itemName);

	    // 모달이 없으면 생성
	    let transferModal = document.getElementById('stockTransferModal');
	    if (!transferModal) {
	        console.warn('⚠️ Transfer Modal이 없어서 동적 생성합니다.');
	        this.createTransferModalDynamically();
	    }

	    // 모달 데이터 채우기
	    this.fillTransferModal(this.selectedStock);
	    
	    // 창고 목록 로딩
	    this.loadWarehousesForTransfer(stockData.warehouseId, stockData.itemType);
	    
	    // 모달 표시
	    const modal = new bootstrap.Modal(document.getElementById('stockTransferModal'));
	    modal.show();
	},
	
	
	
	
};

// ===== 재고 통계 및 요약 =====
const StockSummary = {
    init() {
        console.log('StockSummary 초기화');
        this.loadSummary();
    },

    loadSummary() {
        fetch('/api/stocks/summary')
            .then(response => response.json())
            .then(summary => {
                this.updateSummaryDisplay(summary);
            })
            .catch(error => {
                console.error('재고 요약 조회 실패:', error);
            });
    },

    updateSummaryDisplay(summary) {
        const totalItemsElement = document.getElementById('totalItems');
        const outOfStockElement = document.getElementById('outOfStock');
        const belowSafetyElement = document.getElementById('belowSafety');
        const normalStockElement = document.getElementById('normalStock');

        if (totalItemsElement) totalItemsElement.textContent = summary.totalItems || 0;
        if (outOfStockElement) outOfStockElement.textContent = summary.outOfStock || 0;
        if (belowSafetyElement) belowSafetyElement.textContent = summary.belowSafety || 0;
        if (normalStockElement) normalStockElement.textContent = summary.normalStock || 0;
    }
};


// ===== 메인 초기화 =====
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 재고 관리 시스템 초기화 시작');
    
    setTimeout(() => {
        console.log('⏰ Fragment 로딩 대기 후 초기화 시작');
        
        try {
            StockList.init();
            StockSearch.init();
            StockModal.init();
            StockActions.init();
            StockFilter.init();
            StockTransfer.init();
            StockSummary.init();
            
            const transferModal = document.getElementById('stockTransferModal');
            const detailModal = document.getElementById('stockDetailModal');
            const historyModal = document.getElementById('stockHistoryModal');
            const quickOutModal = document.getElementById('quickOutModal');
            
            console.log('📋 모달 상태 체크:');
            console.log('  - Transfer Modal:', transferModal ? '✅ 존재' : '❌ 없음');
            console.log('  - Detail Modal:', detailModal ? '✅ 존재' : '❌ 없음');
            console.log('  - History Modal:', historyModal ? '✅ 존재' : '❌ 없음');
            console.log('  - Quick Out Modal:', quickOutModal ? '✅ 존재' : '❌ 없음');
            
            if (!transferModal) {
                console.warn('⚠️ Transfer Modal이 없어서 강제 생성합니다.');
                StockTransfer.createTransferModalDynamically();
            }
            
            if (!detailModal) {
                console.warn('⚠️ Detail Modal이 없어서 강제 생성합니다.');
                StockModal.createDetailModalDynamically();
            }
            
            console.log('✅✅✅ 재고 관리 시스템 초기화 완료');
            
        } catch (error) {
            console.error('❌ 초기화 중 오류 발생:', error);
        }
    }, 500);
});

setTimeout(() => {
    console.log('🔍 StockModal에서 관리하는 현재 데이터:');
    console.log('  - currentStockId:', StockModal.currentStockId);
    console.log('  - currentWarehouseId:', StockModal.currentWarehouseId);
    console.log('  - currentWarehouseName:', StockModal.currentWarehouseName);
    console.log('  - currentQuantity:', StockModal.currentQuantity);
    console.log('  - currentItemName:', StockModal.currentItemName);
    console.log('  - currentItemType:', StockModal.currentItemType);
}, 1000); // 1초 후 실행
//아니 왜 안되냐고