// ===== 통합 stock.js (완전한 버전) =====

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

// ===== StockUtils =====
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

// ===== StockList =====
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

		// 🔥 창고명 정렬 필드 매핑 - 이 부분이 핵심!
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
                    tbody.innerHTML = `<tr><td colspan="12" class="text-center text-danger py-5">데이터를 불러오는데 실패했습니다: ${error.message}</td></tr>`;
                }
            });
    },

    updateTable(stocks) {
        const tbody = document.getElementById('stockTableBody');
        if (!tbody) return;

        tbody.innerHTML = '';

        if (!stocks || stocks.length === 0) {
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
                    총 ${pageData.totalElements}개 
                    (현재 ${pageData.number + 1} / ${pageData.totalPages} 페이지)
                </span>`;
        }
    },

    refresh() {
        this.currentPage = 0;
        StockState.currentPage = 0;
        this.loadStockData();
    }
};

// ===== StockSearch =====
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

// ===== StockModal =====
const StockModal = {
    modalInstance: null,
    currentStockId: null,
    
    elementsMap: {
        itemCode: 'detailItemCode',
        itemName: 'detailItemName',
        itemType: 'detailItemType',
        unit: 'detailUnit',
        warehouse: 'detailWarehouse',
        qty: 'detailQty',
        lotNumber: 'detailLotNumber',
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
        }
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
                
                const displayData = {
                    itemCode: stockDetail.itemCode,
                    itemName: stockDetail.itemName,
                    itemType: stockDetail.itemType === 'raw' ? '자재' : '완제품',
                    unit: stockDetail.itemUnit || stockDetail.unit || 'EA',
                    warehouse: stockDetail.warehouseName,
                    qty: StockUtils.formatNumber(stockDetail.quantity),
                    lotNumber: stockDetail.lotNumber || '-',
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
    }
};

// ===== StockActions =====
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
            lotNumber: document.getElementById('inLotNumber').value,
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
    
	quickOut(stockId, currentQty) {
	        if (currentQty === 0) {
	            StockUtils.showError('재고가 없어 출고할 수 없습니다.');
	            return;
	        }
	        
	        const qty = prompt(`출고 수량을 입력하세요 (현재고: ${StockUtils.formatNumber(currentQty)}개)`);
	        
	        if (qty && parseInt(qty) > 0) {
	            if (parseInt(qty) > currentQty) {
	                StockUtils.showError('출고 수량이 현재고보다 많습니다.');
	                return;
	            }
	            
	            fetch(`/api/stocks/${stockId}/out`, {
	                method: 'POST',
	                headers: { 'Content-Type': 'application/json' },
	                body: JSON.stringify({ quantity: parseInt(qty) })
	            })
	            .then(response => response.json())
	            .then(result => {
	                if (result.status === 'success') {
	                    StockUtils.showSuccess(result.message);
	                    StockList.refresh();
	                    // 모달이 열려있다면 닫기
	                    const modalElement = document.getElementById('stockDetailModal');
	                    if (modalElement) {
	                        const modalInstance = bootstrap.Modal.getInstance(modalElement);
	                        if (modalInstance) {
	                            modalInstance.hide();
	                        }
	                    }
	                } else {
	                    StockUtils.showError(result.message || '출고 처리 실패');
	                }
	            })
	            .catch(error => {
	                console.error('출고 처리 오류:', error);
	                StockUtils.showError('출고 처리 중 오류가 발생했습니다.');
	            });
	        }
	    },

    
	// stock.js에서 showHistory 함수만 이것으로 교체하세요


	showHistory(stockId) {
	    console.log('🔍 이력조회 시작 - stockId:', stockId);
	    
	    // ✅ 1. 일단 빈 모달이라도 표시하기
	    let modalElement = document.getElementById('stockHistoryModal');
	    
	    // 모달이 없으면 동적으로 생성
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
	        
	        // body에 모달 추가
	        document.body.insertAdjacentHTML('beforeend', modalHtml);
	        modalElement = document.getElementById('stockHistoryModal');
	    }
	    
	    // ✅ 2. 모달 무조건 표시
	    console.log('🚀 모달 표시!');
	    const modal = new bootstrap.Modal(modalElement, {
	        backdrop: 'static',
	        keyboard: true
	    });
	    modal.show();
	    
	    // ✅ 3. 데이터는 나중에 로딩
	    setTimeout(() => {
	        console.log('📡 데이터 로딩 시작...');
	        
	        // 재고 정보 조회
	        fetch(`/api/stocks/${stockId}`)
	            .then(response => {
	                if (!response.ok) {
	                    throw new Error(`재고 정보 조회 실패: ${response.status}`);
	                }
	                return response.json();
	            })
	            .then(stock => {
	                console.log('📦 재고 정보:', stock);
	                
	                // 재고 정보 표시
	                const historyInfo = document.getElementById('historyInfo');
	                if (historyInfo) {
	                    historyInfo.innerHTML = `
	                        <div class="alert alert-info mb-0">
	                            <strong>[${stock.itemCode}] ${stock.itemName}</strong> - 
	                            ${stock.warehouseName} 창고
	                        </div>
	                    `;
	                }
	                
	                // 이력 데이터 조회
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
	                
	                // 이력 테이블 렌더링
	                this.renderHistory(historyPage.content || []);
	                
	                console.log('✅ 이력 데이터 렌더링 완료!');
	            })
	            .catch(error => {
	                console.error('❌ 데이터 로딩 실패:', error);
	                
	                // 에러 메시지 표시
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
	    }, 100); // 0.1초 후 데이터 로딩
	    
	    // ✅ 4. 모달 표시 확인
	    modalElement.addEventListener('shown.bs.modal', () => {
	        console.log('✅ 모달이 성공적으로 표시됨!');
	    }, { once: true });
	},
    
    renderHistory(logs) {
        const tbody = document.getElementById('historyTableBody');
        if (!tbody) return;
        
        tbody.innerHTML = '';
        
        if (logs.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center text-muted py-3">
                        재고 변동 이력이 없습니다.
                    </td>
                </tr>
            `;
            return;
        }
        
        logs.forEach(log => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${StockUtils.formatDateTime(log.logDatetime)}</td>
                <td>
                    <span class="badge bg-${this.getLogTypeBadgeColor(log.logType)}">
                        ${log.logTypeLabel || log.logType}
                    </span>
                </td>
                <td class="text-end">
                    ${log.logType === 'OUT' || log.logType === 'DISPOSE' ? '-' : '+'}
                    ${StockUtils.formatNumber(log.quantity)}
                </td>
                <td>${log.comment || '-'}</td>
            `;
            tbody.appendChild(row);
        });
    },
    
    getLogTypeBadgeColor(logType) {
        return {
            'IN': 'success',
            'OUT': 'danger',
            'DISPOSE': 'warning',
            'RETURN_IN': 'info'
        }[logType] || 'secondary';
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
	}
};

// ===== StockFilter =====
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

// ===== 메인 초기화 =====
document.addEventListener('DOMContentLoaded', function() {
    console.log('재고 관리 시스템 초기화 시작');
    
    try {
        StockList.init();
        StockSearch.init();
        StockModal.init();
        StockActions.init();
        StockFilter.init();
        
        console.log('✅✅✅ 재고 관리 시스템 초기화 완료');
        
    } catch (error) {
        console.error('❌ 초기화 중 오류 발생:', error);
    }
});