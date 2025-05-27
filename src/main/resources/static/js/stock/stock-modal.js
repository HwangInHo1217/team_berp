// 재고 모달 관리

const StockModal = {
    
    init() {
        // 모달 관련 초기화 작업
    },

    // 모달 열기
    open(stock) {
        // 모달이 없으면 fetch후 재호출
        if (!document.getElementById('stockDetailModal')) {
            this.loadModal(() => this.open(stock));
            return;
        }

        // stockId가 있으면 API로 최신 데이터 조회
        if (stock.stockId) {
            this.fetchDetail(stock.stockId, stock);
        } else {
            this.fillData(stock);
            this.show();
        }
    },

    // 모달 동적 로딩
    loadModal(callback) {
        // 실제 동적 로딩이 필요한 경우
        // fetch('/pages/stock/stock-detail-modal.html')
        //     .then(res => res.text())
        //     .then(html => {
        //         document.getElementById('stockDetailModalWrapper').innerHTML = html;
        //         setTimeout(callback, 50);
        //     })
        //     .catch(error => {
        //         console.error('모달 로딩 실패:', error);
        //     });
        
        // 임시: Thymeleaf 모달이 이미 있으므로 바로 콜백 실행
        setTimeout(callback, 10);
    },

    // API로 상세 데이터 조회
    fetchDetail(stockId, fallbackStock) {
        fetch(`/api/stocks/${stockId}/detail`)
            .then(response => response.json())
            .then(latestStock => {
                const stockData = {
                    item_code: latestStock.itemCode,
                    item_name: latestStock.itemName,
                    warehouse: latestStock.warehouseName,
                    qty: latestStock.quantity,
                    unit: 'EA',
                    last_in: StockUtils.formatDate(latestStock.firstAt),
                    last_out: StockUtils.formatDate(latestStock.lastAt)
                };
                this.fillData(stockData);
                this.show();
            })
            .catch(error => {
                console.error('재고 상세 조회 실패:', error);
                // API 실패시 전달받은 데이터로 대체
                this.fillData(fallbackStock);
                this.show();
            });
    },

    // 모달 데이터 채우기
    fillData(stock) {
        const elements = {
            'detailItemCode': stock.item_code,
            'detailItemName': stock.item_name,
            'detailWarehouse': stock.warehouse,
            'detailQty': stock.qty,
            'detailUnit': stock.unit,
            'detailLastIn': stock.last_in,
            'detailLastOut': stock.last_out
        };

        Object.entries(elements).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = value || '-';
            }
        });
    },

    // 모달 표시
    show() {
        const modalElement = document.getElementById('stockDetailModal');
        if (modalElement) {
            const modal = new bootstrap.Modal(modalElement);
            modal.show();
        }
    },

    // 모달 닫기
    close() {
        const modalElement = document.getElementById('stockDetailModal');
        if (modalElement) {
            const modal = bootstrap.Modal.getInstance(modalElement);
            if (modal) {
                modal.hide();
            }
        }
    }
};