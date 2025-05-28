// ===== stock-actions.js =====
const StockActions = {
    
    init() {
        console.log('StockActions 초기화');
        this.setupEvents();
    },
    
    setupEvents() {
        // 입고 버튼
        const stockInBtn = document.getElementById('stockInBtn');
        if (stockInBtn) {
            stockInBtn.addEventListener('click', () => this.openStockInModal());
        }
        
        // 출고 버튼
        const stockOutBtn = document.getElementById('stockOutBtn');
        if (stockOutBtn) {
            stockOutBtn.addEventListener('click', () => this.openBulkOutModal());
        }
        
        // 재고조정 버튼
        const stockAdjustBtn = document.getElementById('stockAdjustBtn');
        if (stockAdjustBtn) {
            stockAdjustBtn.addEventListener('click', () => this.openAdjustModal());
        }
        
        // 엑셀 다운로드
        const excelDownloadBtn = document.getElementById('excelDownloadBtn');
        if (excelDownloadBtn) {
            excelDownloadBtn.addEventListener('click', () => this.downloadExcel());
        }
        
        // 입고 확인 버튼
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
    
    showHistory(stockId) {
        fetch(`/api/stocks/${stockId}`)
            .then(response => response.json())
            .then(stock => {
                const historyInfo = document.getElementById('historyInfo');
                if (historyInfo) {
                    historyInfo.innerHTML = `
                        <div class="alert alert-info mb-0">
                            <strong>[${stock.itemCode}] ${stock.itemName}</strong> - 
                            ${stock.warehouseName} 창고
                        </div>
                    `;
                }
                
                return fetch(`/api/stocks/${stockId}/history`);
            })
            .then(response => response.json())
            .then(history => {
                this.renderHistory(history.content || []);
                
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
    
    openAdjustModal() {
        const selectedItems = document.querySelectorAll('.stock-checkbox:checked');
        if (selectedItems.length !== 1) {
            StockUtils.showError('재고 조정은 하나의 항목만 선택해주세요.');
            return;
        }
        
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