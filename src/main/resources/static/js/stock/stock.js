// ===== 통합 stock.js (완전한 버전 - 개선된 quickOut 포함) =====

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
            const lotNumber = stock.lotNumber || stock.lot || '-'; // LOT번호 처리 개선

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
                    <td><small class="text-muted">${StockUtils.escapeHtml(lotNumber)}</small></td>
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

// ===== StockModal (개선된 버전 - 창고이동 + 긴급출고 포함) =====
const StockModal = {
    modalInstance: null,
    currentStockId: null,
    currentWarehouseId: null,
    currentQuantity: null,
    currentItemName: null,
    
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
            console.log('✅ StockModal 정상 초기화');
        } else {
            console.warn('⚠️ stockDetailModal이 없음 - 나중에 동적 생성 예정');
        }
    },

    // 🆕 Detail Modal 동적 생성
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
                                        <dt class="col-sm-4">LOT번호</dt>
                                        <dd class="col-sm-8" id="detailLotNumber">-</dd>
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
                            <button type="button" class="btn btn-warning" onclick="StockActions.quickOut(StockModal.currentStockId, parseInt(document.getElementById('detailQty')?.textContent.replace(/,/g, '') || 0))">
                                <i class="fas fa-minus-circle"></i> 출고
                            </button>
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">닫기</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        
        // Modal 인스턴스 다시 생성
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
                
                // 현재 상태 저장 (버튼에서 사용하기 위해)
                this.currentStockId = stockDetail.stockId;
                this.currentWarehouseId = stockDetail.warehouseId;
                this.currentQuantity = stockDetail.quantity;
                this.currentItemName = stockDetail.itemName;
                
                const displayData = {
                    itemCode: stockDetail.itemCode,
                    itemName: stockDetail.itemName,
                    itemType: stockDetail.itemType === 'raw' ? '자재' : '완제품',
                    unit: stockDetail.itemUnit || stockDetail.unit || 'EA',
                    warehouse: stockDetail.warehouseName,
                    qty: StockUtils.formatNumber(stockDetail.quantity),
                    lotNumber: stockDetail.lotNumber || stockDetail.lot || '-',
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

    // ===== 🔄 창고이동 기능 =====
    
    openTransferModal(stockId, warehouseId, quantity, itemName) {
        this.currentStockId = stockId;
        this.currentWarehouseId = warehouseId;
        this.currentQuantity = quantity;
        this.currentItemName = itemName;
        
        // 창고이동 모달의 정보 채우기
        document.getElementById('transferStockInfo').textContent = itemName;
        document.getElementById('transferCurrentQty').textContent = StockUtils.formatNumber(quantity);
        
        // 창고 목록 로딩
        this.loadWarehousesForTransfer(warehouseId);
        
        // 폼 초기화
        document.getElementById('transferQty').value = '';
        document.getElementById('transferReason').value = '';
        document.getElementById('transferComment').value = '';
        
        // 모달 표시
        const transferModal = new bootstrap.Modal(document.getElementById('stockTransferModal'));
        transferModal.show();
    },

    loadWarehousesForTransfer(currentWarehouseId) {
        fetch('/api/stocks/warehouses?useYn=Y')
            .then(response => response.json())
            .then(warehouses => {
                const select = document.getElementById('toWarehouse');
                if (select) {
                    select.innerHTML = '<option value="">도착 창고를 선택하세요</option>';
                    
                    // 현재 창고는 제외하고 옵션 추가
                    warehouses
                        .filter(wh => wh.id !== currentWarehouseId)
                        .forEach(wh => {
                            select.innerHTML += `
                                <option value="${wh.id}" data-warehouse-name="${wh.warehouseName}">
                                    [${wh.warehouseCode}] ${wh.warehouseName}
                                </option>
                            `;
                        });
                }
            })
            .catch(error => {
                console.error('창고 목록 로딩 실패:', error);
            });
    },

    // 상세 모달에서 창고 이동 버튼 클릭시
    openTransferModalFromDetail() {
        if (this.currentStockId && this.currentWarehouseId && this.currentQuantity !== undefined) {
            this.openTransferModal(this.currentStockId, this.currentWarehouseId, this.currentQuantity, this.currentItemName);
        }
    },

    processTransfer() {
        const toWarehouseId = document.getElementById('toWarehouse').value;
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

        const transferData = {
            fromStockId: this.currentStockId,
            fromWarehouseId: this.currentWarehouseId,
            toWarehouseId: parseInt(toWarehouseId),
            itemId: this.currentStockId, // API 구조에 따라 조정 필요할 수 있음
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
                
                // 모달들 닫기
                const transferModal = bootstrap.Modal.getInstance(document.getElementById('stockTransferModal'));
                if (transferModal) transferModal.hide();
                
                const detailModal = bootstrap.Modal.getInstance(document.getElementById('stockDetailModal'));
                if (detailModal) detailModal.hide();
                
                // 목록 새로고침
                StockList.refresh();
            } else {
                StockUtils.showError(result.message || '창고 이동 실패');
            }
        })
        .catch(error => {
            console.error('창고 이동 오류:', error);
            StockUtils.showError('창고 이동 중 오류가 발생했습니다.');
        });
    }
};

// ===== StockActions (개선된 quickOut 포함) =====
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

        // 🔄 창고이동 버튼 이벤트
        const confirmTransferBtn = document.getElementById('confirmTransfer');
        if (confirmTransferBtn) {
            confirmTransferBtn.addEventListener('click', () => StockModal.processTransfer());
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
    
    // ===== 🔥 개선된 quickOut 메서드 =====
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
                    
                    // 상세 모달이 열려있다면 닫기
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
    
	// ===== 📜 재고 이력 조회 기능 =====
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
                    <td colspan="4" class="text-center text-muted py-4">
                        <i class="fas fa-inbox fa-2x mb-2 text-secondary"></i><br>
                        재고 변동 이력이 없습니다.
                    </td>
                </tr>
            `;
            return;
        }
        
        logs.forEach(log => {
            const logTypeDisplay = this.getLogTypeDisplay(log.logType);
            const quantityDisplay = this.getQuantityDisplay(log.logType, log.quantity);
            
            const row = document.createElement('tr');
            row.innerHTML = `
                <td class="text-nowrap">
                    <small>${StockUtils.formatDateTime(log.logDatetime)}</small>
                </td>
                <td class="text-center">
                    <span class="badge bg-${logTypeDisplay.color} rounded-pill">
                        ${logTypeDisplay.label}
                    </span>
                </td>
                <td class="text-end">
                    <strong class="${quantityDisplay.class}">
                        ${quantityDisplay.text}
                    </strong>
                </td>
                <td>
                    <small class="text-muted">
                        ${StockUtils.escapeHtml(log.comment || log.reason || '-')}
                    </small>
                </td>
            `;
            tbody.appendChild(row);
        });
    },
    
    getLogTypeDisplay(logType) {
        const types = {
            'IN': { label: '입고', color: 'success' },
            'OUT': { label: '출고', color: 'danger' },
            'TRANSFER_IN': { label: '이동입고', color: 'info' },
            'TRANSFER_OUT': { label: '이동출고', color: 'warning' },
            'ADJUST': { label: '조정', color: 'primary' },
            'DISPOSE': { label: '폐기', color: 'dark' },
            'RETURN_IN': { label: '반품입고', color: 'secondary' }
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

// ===== StockTransfer (기존 유지) =====
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
        
        // 도착 창고 선택시 같은 창고 체크
        const toWarehouseSelect = document.getElementById('toWarehouse');
        if (toWarehouseSelect) {
            toWarehouseSelect.addEventListener('change', () => this.validateWarehouseSelection());
        }
        
        // 이동 수량 입력시 검증
        const transferQtyInput = document.getElementById('transferQty');
        if (transferQtyInput) {
            transferQtyInput.addEventListener('input', () => this.validateTransferQty());
        }
    },
    
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
        console.log('🔄 창고이동 요청 - stockId:', stockId);
        
        // 🔥 더 안전한 모달 체크 및 생성
        let transferModal = document.getElementById('stockTransferModal');
        
        if (!transferModal) {
            console.warn('⚠️ stockTransferModal이 없어서 동적 생성합니다.');
            this.createTransferModalDynamically();
            transferModal = document.getElementById('stockTransferModal');
        }
        
        if (!transferModal) {
            StockUtils.showError('창고이동 모달을 생성할 수 없습니다. 페이지를 새로고침해주세요.');
            console.error('❌ 모달 생성 실패!');
            return;
        }
        
        console.log('✅ 모달 확인됨, 재고 정보 로딩 시작');
        this.loadStockForTransfer(stockId);
    },

    // 🆕 동적 모달 생성 메서드
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
                                        <option value="재고 조정">재고 조정</option>
                                        <option value="창고 정리">창고 정리</option>
                                        <option value="생산 계획">생산 계획</option>
                                        <option value="기타">기타</option>
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
        
        // 이벤트 리스너 다시 연결
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
                this.loadWarehousesForTransfer(stock.warehouseId);
                
                const modal = new bootstrap.Modal(document.getElementById('stockTransferModal'));
                modal.show();
            })
            .catch(error => {
                StockUtils.handleApiError(error, '재고 정보를 불러오는데 실패했습니다.');
            });
    },
    
    fillTransferModal(stock) {
        // 🔥 안전한 element 업데이트
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

        // 재고 정보 표시
        updateElement('transferStockInfo', `[${stock.itemCode}] ${stock.itemName}`);
        updateElement('transferCurrentQty', StockUtils.formatNumber(stock.quantity));
        updateElement('fromWarehouse', stock.warehouseName, true);
        updateElement('availableQty', stock.quantity);
        
        // 폼 필드 초기화 (안전하게)
        updateElement('toWarehouse', '', true);
        updateElement('transferQty', '', true);
        updateElement('transferReason', '', true);
        updateElement('transferComment', '', true);
    },
    
    loadWarehousesForTransfer(currentWarehouseId) {
        fetch('/api/stocks/warehouses?useYn=Y')
            .then(response => response.json())
            .then(warehouses => {
                const select = document.getElementById('toWarehouse');
                if (select) {
                    select.innerHTML = '<option value="">도착 창고를 선택하세요</option>';
                    
                    // 현재 창고는 제외하고 옵션 추가
                    warehouses
                        .filter(wh => wh.id !== currentWarehouseId)
                        .forEach(wh => {
                            select.innerHTML += `
                                <option value="${wh.id}" data-warehouse-name="${wh.warehouseName}">
                                    [${wh.warehouseCode}] ${wh.warehouseName}
                                </option>
                            `;
                        });
                }
            })
            .catch(error => {
                console.error('창고 목록 로딩 실패:', error);
            });
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
        
        const data = {
            fromStockId: this.selectedStock.stockId,
            fromWarehouseId: this.selectedStock.warehouseId,
            toWarehouseId: parseInt(document.getElementById('toWarehouse').value),
            itemId: this.selectedStock.itemId,
            quantity: parseInt(document.getElementById('transferQty').value),
            reason: document.getElementById('transferReason').value,
            comment: document.getElementById('transferComment').value
        };
        
        // 확인 메시지
        const toWarehouseName = document.getElementById('toWarehouse').selectedOptions[0]?.text || '';
        const confirmMsg = `정말로 ${data.quantity}개를 ${toWarehouseName}로 이동하시겠습니까?`;
        
        if (!confirm(confirmMsg)) return;
        
        fetch('/api/stocks/transfer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        })
        .then(response => response.json())
        .then(result => {
            if (result.status === 'success') {
                StockUtils.showSuccess(result.message || '창고 이동이 완료되었습니다.');
                bootstrap.Modal.getInstance(document.getElementById('stockTransferModal')).hide();
                StockList.refresh();
            } else {
                StockUtils.showError(result.message || '창고 이동에 실패했습니다.');
            }
        })
        .catch(error => {
            console.error('창고 이동 오류:', error);
            StockUtils.showError('창고 이동 중 오류가 발생했습니다.');
        });
    }
};

// ===== 📊 재고 통계 및 요약 (선택 사항) =====
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
                // 실패해도 계속 진행 (선택적 기능)
            });
    },

    updateSummaryDisplay(summary) {
        // 상단 배지 업데이트
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
    
    // 🔥 Fragment 로딩을 위해 더 긴 대기 시간
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
            
            // 🔍 모달 존재 여부 상세 확인
            const transferModal = document.getElementById('stockTransferModal');
            const detailModal = document.getElementById('stockDetailModal');
            const historyModal = document.getElementById('stockHistoryModal');
            
            console.log('📋 모달 상태 체크:');
            console.log('  - Transfer Modal:', transferModal ? '✅ 존재' : '❌ 없음');
            console.log('  - Detail Modal:', detailModal ? '✅ 존재' : '❌ 없음');
            console.log('  - History Modal:', historyModal ? '✅ 존재' : '❌ 없음');
            
            // Fragment가 로드되지 않은 경우 강제 생성
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
    }, 500); // 0.5초 대기로 증가
});