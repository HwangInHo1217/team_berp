// ===== 기존 stock.js에 추가할 새로운 기능들 =====

// ===== StockTransfer (창고 이동 기능) =====
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
        this.loadStockForTransfer(stockId);
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
        const elements = {
            transferStockInfo: `[${stock.itemCode}] ${stock.itemName}`,
            fromWarehouse: stock.warehouseName,
            availableQty: stock.quantity
        };
        
        for (const [key, value] of Object.entries(elements)) {
            const element = document.getElementById(key);
            if (element) {
                element.textContent = value;
                if (element.tagName === 'INPUT') {
                    element.value = value;
                }
            }
        }
        
        // 폼 초기화
        document.getElementById('toWarehouse').value = '';
        document.getElementById('transferQty').value = '';
        document.getElementById('transferReason').value = '';
        document.getElementById('transferComment').value = '';
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

// ===== QuickOut (긴급 출고 기능) =====
const QuickOut = {
    selectedStock: null,
    
    init() {
        console.log('QuickOut 초기화');
        this.setupEvents();
    },
    
    setupEvents() {
        const quickOutBtn = document.getElementById('quickOutBtn');
        if (quickOutBtn) {
            quickOutBtn.addEventListener('click', () => this.openQuickOutModal());
        }
        
        const confirmBtn = document.getElementById('confirmQuickOut');
        if (confirmBtn) {
            confirmBtn.addEventListener('click', () => this.processQuickOut());
        }
        
        // 출고 수량 제한 검증
        const qtyInput = document.getElementById('quickOutQty');
        if (qtyInput) {
            qtyInput.addEventListener('input', () => this.validateQuickOutQty());
        }
    },
    
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
        this.loadStockForQuickOut(stockId);
    },
    
    loadStockForQuickOut(stockId) {
        fetch(`/api/stocks/${stockId}`)
            .then(response => response.json())
            .then(stock => {
                if (stock.quantity === 0) {
                    StockUtils.showError('재고가 없어 출고할 수 없습니다.');
                    return;
                }
                
                this.selectedStock = stock;
                this.fillQuickOutModal(stock);
                
                const modal = new bootstrap.Modal(document.getElementById('quickOutModal'));
                modal.show();
            })
            .catch(error => {
                StockUtils.handleApiError(error, '재고 정보를 불러오는데 실패했습니다.');
            });
    },
    
    fillQuickOutModal(stock) {
        const elements = {
            quickOutStockInfo: `[${stock.itemCode}] ${stock.itemName} - ${stock.warehouseName}`,
            quickCurrentQty: stock.quantity
        };
        
        for (const [key, value] of Object.entries(elements)) {
            const element = document.getElementById(key);
            if (element) {
                if (element.tagName === 'INPUT') {
                    element.value = value;
                } else {
                    element.textContent = value;
                }
            }
        }
        
        // 출고 수량 최대값 설정 (현재고와 10 중 작은 값)
        const qtyInput = document.getElementById('quickOutQty');
        if (qtyInput) {
            qtyInput.max = Math.min(stock.quantity, 10);
            qtyInput.value = '';
            qtyInput.placeholder = `최대 ${Math.min(stock.quantity, 10)}개`;
        }
        
        // 폼 초기화
        document.getElementById('quickOutReason').value = '';
    },
    
    validateQuickOutQty() {
        const qtyInput = document.getElementById('quickOutQty');
        const currentQtyInput = document.getElementById('quickCurrentQty');
        
        if (qtyInput && currentQtyInput) {
            const outQty = parseInt(qtyInput.value);
            const currentQty = parseInt(currentQtyInput.value);
            const maxQty = Math.min(currentQty, 10);
            
            if (outQty > maxQty) {
                StockUtils.showWarning(`긴급 출고는 최대 ${maxQty}개까지만 가능합니다.`);
                qtyInput.value = maxQty;
            }
            
            if (outQty > currentQty) {
                StockUtils.showWarning(`출고 수량이 현재고(${currentQty})보다 많습니다.`);
                qtyInput.value = currentQty;
            }
        }
    },
    
    processQuickOut() {
        const form = document.getElementById('quickOutForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        
        if (!this.selectedStock) {
            StockUtils.showError('선택된 재고 정보가 없습니다.');
            return;
        }
        
        const outQty = parseInt(document.getElementById('quickOutQty').value);
        const reason = document.getElementById('quickOutReason').value || '긴급 출고';
        
        // 최종 확인
        const confirmMsg = `정말로 ${outQty}개를 긴급 출고하시겠습니까?\n사유: ${reason}`;
        if (!confirm(confirmMsg)) return;
        
        const data = {
            itemId: this.selectedStock.itemId,
            warehouseId: this.selectedStock.warehouseId,
            quantity: outQty,
            comment: `[긴급출고] ${reason}`
        };
        
        fetch('/api/stocks/out', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        })
        .then(response => response.json())
        .then(result => {
            if (result.status === 'success') {
                StockUtils.showSuccess(result.message || '긴급 출고가 완료되었습니다.');
                bootstrap.Modal.getInstance(document.getElementById('quickOutModal')).hide();
                StockList.refresh();
            } else {
                StockUtils.showError(result.message || '긴급 출고에 실패했습니다.');
            }
        })
        .catch(error => {
            console.error('긴급 출고 오류:', error);
            StockUtils.showError('긴급 출고 중 오류가 발생했습니다.');
        });
    }
};

// ===== StockSummary (상단 통계 배지) =====
const StockSummary = {
    init() {
        console.log('StockSummary 초기화');
        this.loadSummary();
    },
    
    loadSummary() {
        fetch('/api/stocks/summary')
            .then(response => response.json())
            .then(summary => {
                this.updateSummaryBadges(summary);
            })
            .catch(error => {
                console.log('통계 조회 실패 (선택사항):', error.message);
                // 실패해도 무시 (선택적 기능)
            });
    },
    
    updateSummaryBadges(summary) {
        const elements = {
            totalStockCount: summary.totalItems || 0,
            belowSafetyCount: summary.belowSafety || 0,
            outOfStockCount: summary.outOfStock || 0
        };
        
        for (const [id, value] of Object.entries(elements)) {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = StockUtils.formatNumber(value);
            }
        }
    }
};

// ===== 기존 StockActions에 새 기능 추가 =====
// 기존 StockActions.init()에 다음 라인들 추가:
/*
StockTransfer.init();
QuickOut.init(); 
StockSummary.init();
*/

// ===== 메인 초기화 부분 수정 =====
document.addEventListener('DOMContentLoaded', function() {
    console.log('재고 관리 시스템 초기화 시작 (개선 버전)');
    
    try {
        StockList.init();
        StockSearch.init();
        StockModal.init();
        StockActions.init();
        StockFilter.init();
        
        // 새로운 기능들 초기화
        StockTransfer.init();
        QuickOut.init();
        StockSummary.init();
        
        console.log('✅✅✅ 재고 관리 시스템 초기화 완료 (개선 버전)');
        
    } catch (error) {
        console.error('❌ 초기화 중 오류 발생:', error);
    }
});