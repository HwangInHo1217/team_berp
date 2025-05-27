// 재고 관리 메인 JavaScript - 완전 개선 버전

// 전역 상태 관리
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

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    StockList.init();
    StockSearch.init();
    StockModal.init();
    StockActions.init();
    StockFilter.init();
});

// 재고 목록 관리
const StockList = {
    
    init() {
        this.setupTableEvents();
        this.load();
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
        document.getElementById('selectAll').addEventListener('change', (e) => {
            document.querySelectorAll('.stock-checkbox').forEach(cb => {
                cb.checked = e.target.checked;
            });
        });
    },
    
    toggleSort(field) {
        const currentSort = StockState.sortBy.split(',');
        if (currentSort[0] === field) {
            StockState.sortBy = field + ',' + (currentSort[1] === 'ASC' ? 'DESC' : 'ASC');
        } else {
            StockState.sortBy = field + ',ASC';
        }
        this.load();
    },
    
    load() {
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
        
        fetch(`/api/stocks?${params}`)
            .then(response => response.json())
            .then(data => {
                this.renderTable(data.content);
                this.renderPagination(data);
            })
            .catch(error => {
                console.error('재고 목록 조회 실패:', error);
                StockUtils.showError('재고 목록을 불러오는데 실패했습니다.');
            });
    },
    
    renderTable(stocks) {
        const tbody = document.querySelector('#stockTable tbody');
        tbody.innerHTML = '';
        
        if (stocks.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="12" class="text-center text-muted py-5">
                        <i class="fas fa-box-open fa-3x mb-3 text-secondary"></i>
                        <h5>검색된 재고가 없습니다</h5>
                    </td>
                </tr>
            `;
            return;
        }
        
        stocks.forEach((stock, index) => {
            const row = this.createTableRow(stock, index);
            tbody.appendChild(row);
        });
    },
    
    createTableRow(stock, index) {
        const row = document.createElement('tr');
        
        // 재고 상태에 따른 행 스타일
        if (stock.quantity === 0) {
            row.classList.add('table-danger');
        } else if (stock.isBelowSafety) {
            row.classList.add('table-warning');
        }
        
        row.innerHTML = `
            <td>
                <input type="checkbox" class="stock-checkbox" data-stock-id="${stock.stockId}" />
            </td>
            <td>${StockState.currentPage * StockState.pageSize + index + 1}</td>
            <td>${stock.itemCode}</td>
            <td>${stock.itemName}</td>
            <td>
                <span class="badge bg-${stock.itemType === 'raw' ? 'info' : 'success'}">
                    ${stock.itemType === 'raw' ? '자재' : '완제품'}
                </span>
            </td>
            <td>${stock.warehouseName}</td>
            <td class="text-end">
                <strong>${StockUtils.formatNumber(stock.quantity)}</strong>
            </td>
            <td>${stock.unit || 'EA'}</td>
            <td>${stock.formattedLastInDate}</td>
            <td>${stock.formattedLastOutDate}</td>
            <td>
                ${this.getStatusBadge(stock)}
            </td>
            <td>
                <div class="dropdown">
                    <button class="btn btn-sm btn-secondary dropdown-toggle" data-bs-toggle="dropdown">
                        <i class="fas fa-ellipsis-v"></i>
                    </button>
                    <ul class="dropdown-menu">
                        <li>
                            <a class="dropdown-item" href="#" onclick="StockActions.showDetail(${stock.stockId})">
                                <i class="fas fa-eye"></i> 상세보기
                            </a>
                        </li>
                        <li>
                            <a class="dropdown-item" href="#" onclick="StockActions.showHistory(${stock.stockId})">
                                <i class="fas fa-history"></i> 이력조회
                            </a>
                        </li>
                        <li><hr class="dropdown-divider"></li>
                        <li>
                            <a class="dropdown-item" href="#" onclick="StockActions.quickOut(${stock.stockId}, ${stock.quantity})">
                                <i class="fas fa-minus-circle"></i> 빠른출고
                            </a>
                        </li>
                    </ul>
                </div>
            </td>
        `;
        
        return row;
    },
    
    getStatusBadge(stock) {
        if (stock.quantity === 0) {
            return '<span class="badge bg-danger">재고없음</span>';
        } else if (stock.isBelowSafety) {
            return '<span class="badge bg-warning">안전재고미달</span>';
        } else {
            return '<span class="badge bg-success">정상</span>';
        }
    },
    
    renderPagination(pageData) {
        const pagination = document.getElementById('pagination');
        pagination.innerHTML = '';
        
        // Previous 버튼
        const prevLi = document.createElement('li');
        prevLi.className = `page-item ${pageData.first ? 'disabled' : ''}`;
        prevLi.innerHTML = `<a class="page-link" href="#" onclick="StockList.goToPage(${pageData.number - 1})">이전</a>`;
        pagination.appendChild(prevLi);
        
        // 페이지 번호들
        const totalPages = pageData.totalPages;
        const currentPage = pageData.number;
        
        for (let i = 0; i < totalPages; i++) {
            if (i === 0 || i === totalPages - 1 || (i >= currentPage - 2 && i <= currentPage + 2)) {
                const li = document.createElement('li');
                li.className = `page-item ${i === currentPage ? 'active' : ''}`;
                li.innerHTML = `<a class="page-link" href="#" onclick="StockList.goToPage(${i})">${i + 1}</a>`;
                pagination.appendChild(li);
            } else if (i === currentPage - 3 || i === currentPage + 3) {
                const li = document.createElement('li');
                li.className = 'page-item disabled';
                li.innerHTML = '<span class="page-link">...</span>';
                pagination.appendChild(li);
            }
        }
        
        // Next 버튼
        const nextLi = document.createElement('li');
        nextLi.className = `page-item ${pageData.last ? 'disabled' : ''}`;
        nextLi.innerHTML = `<a class="page-link" href="#" onclick="StockList.goToPage(${pageData.number + 1})">다음</a>`;
        pagination.appendChild(nextLi);
    },
    
    goToPage(page) {
        if (page >= 0) {
            StockState.currentPage = page;
            this.load();
        }
    },
    
    refresh() {
        this.load();
    }
};

// 검색 및 필터 기능
const StockSearch = {
    
    init() {
        this.setupEvents();
    },
    
    setupEvents() {
        // 검색 버튼
        document.getElementById('searchBtn').addEventListener('click', () => this.search());
        
        // 초기화 버튼
        document.getElementById('resetBtn').addEventListener('click', () => this.reset());
        
        // 엔터키 검색
        document.getElementById('searchKeyword').addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.search();
        });
        
        // 필터 변경 시 자동 검색
        ['warehouseFilter', 'itemTypeFilter', 'stockStatusFilter'].forEach(id => {
            document.getElementById(id).addEventListener('change', () => this.search());
        });
    },
    
    search() {
        StockState.filters.keyword = document.getElementById('searchKeyword').value.trim();
        StockState.filters.warehouse = document.getElementById('warehouseFilter').value;
        StockState.filters.itemType = document.getElementById('itemTypeFilter').value;
        StockState.filters.stockStatus = document.getElementById('stockStatusFilter').value;
        StockState.currentPage = 0; // 검색 시 첫 페이지로
        
        StockList.refresh();
    },
    
    reset() {
        document.getElementById('searchKeyword').value = '';
        document.getElementById('warehouseFilter').value = '';
        document.getElementById('itemTypeFilter').value = '';
        document.getElementById('stockStatusFilter').value = '';
        
        StockState.filters = {
            keyword: '',
            warehouse: '',
            itemType: '',
            stockStatus: ''
        };
        StockState.currentPage = 0;
        
        StockList.refresh();
    }
};

// 재고 액션 기능
const StockActions = {
    
    init() {
        this.setupEvents();
    },
    
    setupEvents() {
        // 입고 버튼
        document.getElementById('stockInBtn').addEventListener('click', () => this.openStockInModal());
        
        // 출고 버튼
        document.getElementById('stockOutBtn').addEventListener('click', () => this.openBulkOutModal());
        
        // 재고조정 버튼
        document.getElementById('stockAdjustBtn').addEventListener('click', () => this.openAdjustModal());
        
        // 엑셀 다운로드
        document.getElementById('excelDownloadBtn').addEventListener('click', () => this.downloadExcel());
        
        // 입고 확인 버튼
        document.getElementById('confirmStockIn').addEventListener('click', () => this.processStockIn());
    },
    
    openStockInModal() {
        // 품목 및 창고 목록 로드
        this.loadItems();
        this.loadWarehouses();
        
        // 폼 초기화
        document.getElementById('stockInForm').reset();
        
        // 모달 열기
        const modal = new bootstrap.Modal(document.getElementById('stockInModal'));
        modal.show();
    },
    
    loadItems() {
        fetch('/api/items?useYn=Y')
            .then(response => response.json())
            .then(items => {
                const select = document.getElementById('inItemId');
                select.innerHTML = '<option value="">품목을 선택하세요</option>';
                
                items.forEach(item => {
                    select.innerHTML += `
                        <option value="${item.id}">
                            [${item.code}] ${item.name} (${item.type === 'raw' ? '자재' : '완제품'})
                        </option>
                    `;
                });
            })
            .catch(error => console.error('품목 로딩 실패:', error));
    },
    
    loadWarehouses() {
        fetch('/api/warehouses?useYn=Y')
            .then(response => response.json())
            .then(warehouses => {
                const select = document.getElementById('inWarehouseId');
                select.innerHTML = '<option value="">창고를 선택하세요</option>';
                
                warehouses.forEach(wh => {
                    select.innerHTML += `
                        <option value="${wh.id}">
                            [${wh.warehouseCode}] ${wh.warehouseName}
                        </option>
                    `;
                });
            })
            .catch(error => console.error('창고 로딩 실패:', error));
    },
    
    processStockIn() {
        const form = document.getElementById('stockInForm');
        if (!form.checkValidity()) {
            form.reportValidity();
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
    
    showDetail(stockId) {
        fetch(`/api/stocks/${stockId}`)
            .then(response => response.json())
            .then(stock => {
                StockModal.showDetail(stock);
            })
            .catch(error => {
                console.error('재고 상세 조회 실패:', error);
                StockUtils.showError('재고 상세 정보를 불러올 수 없습니다.');
            });
    },
    
    showHistory(stockId) {
        fetch(`/api/stocks/${stockId}`)
            .then(response => response.json())
            .then(stock => {
                // 이력 모달 헤더 정보 설정
                document.getElementById('historyInfo').innerHTML = `
                    <div class="alert alert-info mb-0">
                        <strong>[${stock.itemCode}] ${stock.itemName}</strong> - 
                        ${stock.warehouseName} 창고
                    </div>
                `;
                
                // 이력 데이터 로드
                return fetch(`/api/stocks/${stockId}/history`);
            })
            .then(response => response.json())
            .then(history => {
                this.renderHistory(history.content);
                
                // 모달 표시
                const modal = new bootstrap.Modal(document.getElementById('stockHistoryModal'));
                modal.show();
            })
            .catch(error => {
                console.error('재고 이력 조회 실패:', error);
                StockUtils.showError('재고 이력을 불러올 수 없습니다.');
            });
    },
    
    renderHistory(logs) {
        const tbody = document.getElementById('historyTableBody');
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
                <td>${log.formattedLogDatetime}</td>
                <td>
                    <span class="badge bg-${this.getLogTypeBadgeColor(log.logType)}">
                        ${log.logTypeLabel}
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
        
        // TODO: 일괄 출고 모달 구현
        StockUtils.showInfo('일괄 출고 기능은 준비 중입니다.');
    },
    
    openAdjustModal() {
        const selectedItems = document.querySelectorAll('.stock-checkbox:checked');
        if (selectedItems.length !== 1) {
            StockUtils.showError('재고 조정은 하나의 항목만 선택해주세요.');
            return;
        }
        
        // TODO: 재고 조정 모달 구현
        StockUtils.showInfo('재고 조정 기능은 준비 중입니다.');
    },
    
    downloadExcel() {
        const params = new URLSearchParams({
            keyword: StockState.filters.keyword,
            whs: StockState.filters.warehouse,
            itemType: StockState.filters.itemType,
            stockStatus: StockState.filters.stockStatus
        });
        
        window.location.href = `/api/stocks/excel?${params}`;
    }
};

// 모달 관리
const StockModal = {
    
    init() {
        // 모달 관련 초기화
    },
    
    showDetail(stock) {
        const modalBody = `
            <div class="row">
                <div class="col-md-6">
                    <h6 class="text-muted">품목 정보</h6>
                    <dl class="row mb-0">
                        <dt class="col-sm-4">품목코드</dt>
                        <dd class="col-sm-8">${stock.itemCode}</dd>
                        <dt class="col-sm-4">품목명</dt>
                        <dd class="col-sm-8">${stock.itemName}</dd>
                        <dt class="col-sm-4">품목유형</dt>
                        <dd class="col-sm-8">
                            <span class="badge bg-${stock.itemType === 'raw' ? 'info' : 'success'}">
                                ${stock.itemType === 'raw' ? '자재' : '완제품'}
                            </span>
                        </dd>
                        <dt class="col-sm-4">단위</dt>
                        <dd class="col-sm-8">${stock.unit || 'EA'}</dd>
                    </dl>
                </div>
                <div class="col-md-6">
                    <h6 class="text-muted">재고 정보</h6>
                    <dl class="row mb-0">
                        <dt class="col-sm-4">창고</dt>
                        <dd class="col-sm-8">${stock.warehouseName}</dd>
                        <dt class="col-sm-4">현재고</dt>
                        <dd class="col-sm-8">
                            <strong class="text-primary">${StockUtils.formatNumber(stock.quantity)}</strong>
                        </dd>
                        <dt class="col-sm-4">LOT번호</dt>
                        <dd class="col-sm-8">${stock.lotNumber || '-'}</dd>
                        <dt class="col-sm-4">재고상태</dt>
                        <dd class="col-sm-8">${this.getDetailStatusBadge(stock)}</dd>
                    </dl>
                </div>
            </div>
            <hr>
            <div class="row">
                <div class="col-md-6">
                    <dl class="row mb-0">
                        <dt class="col-sm-4">최초입고일</dt>
                        <dd class="col-sm-8">${stock.formattedFirstStockedDate}</dd>
                        <dt class="col-sm-4">최종입고일</dt>
                        <dd class="col-sm-8">${stock.formattedLastInDate}</dd>
                    </dl>
                </div>
                <div class="col-md-6">
                    <dl class="row mb-0">
                        <dt class="col-sm-4">최종변경일</dt>
                        <dd class="col-sm-8">${stock.formattedLastStockedDate}</dd>
                        <dt class="col-sm-4">최종출고일</dt>
                        <dd class="col-sm-8">${stock.formattedLastOutDate}</dd>
                    </dl>
                </div>
            </div>
        `;
        
        StockUtils.showModal('재고 상세 정보', modalBody);
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

// 필터 관리
const StockFilter = {
    
    init() {
        this.loadDynamicFilters();
    },
    
    loadDynamicFilters() {
        // 창고 목록 동적 로딩
        fetch('/api/stocks/warehouses')
            .then(response => response.json())
            .then(warehouses => {
                const select = document.getElementById('warehouseFilter');
                select.innerHTML = '<option value="">전체 창고</option>';
                
                warehouses.forEach(wh => {
                    select.innerHTML += `
                        <option value="${wh.warehouseCode}">${wh.warehouseName}</option>
                    `;
                });
            })
            .catch(error => console.error('창고 목록 로딩 실패:', error));
    }
};

// 유틸리티
const StockUtils = {
    
    formatNumber(num) {
        return new Intl.NumberFormat('ko-KR').format(num);
    },
    
    formatDate(dateStr) {
        if (!dateStr) return '-';
        return new Date(dateStr).toLocaleDateString('ko-KR');
    },
    
    formatDateTime(dateStr) {
        if (!dateStr) return '-';
        return new Date(dateStr).toLocaleString('ko-KR');
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
    
    showToast(message, type = 'info') {
        const toastHtml = `
            <div class="toast align-items-center text-white bg-${type} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `;
        
        const toastContainer = document.getElementById('toastContainer') || this.createToastContainer();
        toastContainer.insertAdjacentHTML('beforeend', toastHtml);
        
        const toastElement = toastContainer.lastElementChild;
        const toast = new bootstrap.Toast(toastElement);
        toast.show();
        
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    },
    
    createToastContainer() {
        const container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        document.body.appendChild(container);
        return container;
    },
    
    showModal(title, body, footer = '') {
        const modalHtml = `
            <div class="modal fade" tabindex="-1">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">${title}</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">${body}</div>
                        ${footer ? `<div class="modal-footer">${footer}</div>` : ''}
                    </div>
                </div>
            </div>
        `;
        
        const modalElement = document.createElement('div');
        modalElement.innerHTML = modalHtml;
        document.body.appendChild(modalElement.firstElementChild);
        
        const modal = new bootstrap.Modal(modalElement.firstElementChild);
        modal.show();
        
        modalElement.firstElementChild.addEventListener('hidden.bs.modal', () => {
            modalElement.firstElementChild.remove();
        });
    }
};