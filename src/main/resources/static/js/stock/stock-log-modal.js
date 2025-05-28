// 📝 /static/js/stock/stock-log-modal.js - 재고 로그 모달

const StockLogModal = {
    
    init() {
        console.log('StockLogModal 초기화');
    },
    
    open(stockId) {
        // stockId가 없으면 StockModal에서 가져오기
        const targetStockId = stockId || StockModal.currentStockId;
        
        if (!targetStockId) {
            StockUtils.showError('로그를 조회할 재고 ID가 없습니다.');
            return;
        }

        fetch(`/api/stocks/${targetStockId}/logs`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then(logs => {
                this.renderLogs(logs);
                
                // 모달 표시
                const modal = new bootstrap.Modal(document.getElementById('stockLogModal'));
                modal.show();
            })
            .catch(error => {
                console.error('재고 로그 조회 실패:', error);
                StockUtils.showError('이력 조회에 실패했습니다: ' + error.message);
            });
    },
    
    renderLogs(logs) {
        const tbody = document.getElementById('logTableBody');
        if (!tbody) {
            console.error('logTableBody 요소를 찾을 수 없습니다.');
            return;
        }
        
        tbody.innerHTML = '';

        if (!logs || logs.length === 0) {
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
            const tr = document.createElement('tr');
            tr.innerHTML = `
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
            tbody.appendChild(tr);
        });
    },
    
    getLogTypeBadgeColor(logType) {
        const colorMap = {
            'IN': 'success',
            'OUT': 'danger', 
            'DISPOSE': 'warning',
            'RETURN_IN': 'info',
            'ADJUST': 'secondary'
        };
        return colorMap[logType] || 'secondary';
    }
};