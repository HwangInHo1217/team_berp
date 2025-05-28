// ===== stock-modal.js =====
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
            console.error('상세보기 모달이 초기화되지 않았습니다.');
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
                console.log("상세 데이터:", stockDetail); // 디버깅용
                
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
