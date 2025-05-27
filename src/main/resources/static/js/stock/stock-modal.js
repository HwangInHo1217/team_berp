const StockModal = {
    modalInstance: null,
    // DOM 요소 ID
    elementsMap: {
        itemCode: 'detailItemCode',
        itemName: 'detailItemName',
        warehouse: 'detailWarehouse',
        qty: 'detailQty',
        unit: 'detailUnit',
        lastIn: 'detailLastIn',
        lastOut: 'detailLastOut'
    },

    init() {
        const modalElement = document.getElementById('stockDetailModal');
        if (modalElement) {
            this.modalInstance = new bootstrap.Modal(modalElement);
        }
    },

    open(stockId) {
        if (!this.modalInstance) {
            console.error('상세보기 모달이 초기화되지 않았습니다.');
            // 필요시 여기서 모달 동적 로딩 또는 에러 처리
            // this.loadModalAndOpen(stockId); // 만약 모달을 동적으로 가져온다면
            return;
        }

        if (stockId) {
            this.fetchStockDetail(stockId);
        } else {
            StockUtils.showError('상세 정보를 표시할 재고 ID가 없습니다.');
        }
    },

    // loadModalAndOpen(stockId) { ... } // 모달 HTML을 동적으로 로드해야 하는 경우 사용

    fetchStockDetail(stockId) {
        StockUtils.showLoading(document.querySelector('#stockDetailModal .modal-body ul')); // 로딩 표시기 위치
        fetch(`/api/stocks/${stockId}/detail`) // Controller에 이 API 엔드포인트 필요
            .then(response => {
                if (!response.ok) {
                    throw new Error(`서버 응답 오류 (${response.status})`);
                }
                return response.json();
            })
            .then(stockDetail => { // stockDetail은 StockResponseDTO 형태
                const displayData = {
                    itemCode: stockDetail.itemCode,
                    itemName: stockDetail.itemName,
                    warehouse: stockDetail.warehouseName,
                    qty: StockUtils.formatNumber(stockDetail.quantity),
                    unit: stockDetail.itemUnit || 'EA', // DTO에 itemUnit 필드 필요
                    // DTO의 날짜 필드명에 맞춰 수정 (actualLastInAt, actualLastOutAt 등)
                    lastIn: StockUtils.formatDate(stockDetail.actualLastInAt || stockDetail.firstAt),
                    lastOut: StockUtils.formatDate(stockDetail.actualLastOutAt || stockDetail.lastAt)
                };
                this.fillModalData(displayData);
                this.modalInstance.show();
            })
            .catch(error => {
                StockUtils.handleApiError(error, '재고 상세 정보를 가져오는데 실패했습니다.');
                // 실패 시 모달 내용 초기화 또는 에러 메시지 표시
                this.fillModalData({}); // 빈 데이터로 채우거나
                document.querySelector('#stockDetailModal .modal-body ul').innerHTML = `<li class="list-group-item text-danger">정보를 불러오지 못했습니다.</li>`;
            });
    },

    fillModalData(data) {
        for (const key in this.elementsMap) {
            const element = document.getElementById(this.elementsMap[key]);
            if (element) {
                element.textContent = data[key] || '-';
            }
        }
    },

    // show() 메소드는 this.modalInstance.show()로 대체되어 불필요할 수 있음
    // close() 메소드는 this.modalInstance.hide()로 대체되어 불필요할 수 있음
};