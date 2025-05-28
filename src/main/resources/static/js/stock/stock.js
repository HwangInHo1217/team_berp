// ===== 통합 stock.js (모든 기능 포함) =====

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
        
        const confirmStockIn = document.getElementById('confirmStockIn');
        if (confirmStockIn) {
            confirmStockIn.addEventListener('click', () => this.processStockIn());
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