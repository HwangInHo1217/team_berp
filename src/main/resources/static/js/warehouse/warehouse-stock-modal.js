/**
 * 창고별 재고 모달 관리 JavaScript
 * 특정 창고에 속한 재고 목록을 모달로 표시
 */

const WarehouseStockModal = {
    modalInstance: null,
    currentWarehouseId: null,
    currentWarehouseData: null,
    currentPage: 0,
    pageSize: 10,
    
    // 초기화
    init() {
        console.log('WarehouseStockModal 초기화');
        const modalElement = document.getElementById('warehouseStockModal');
        if (modalElement) {
            this.modalInstance = new bootstrap.Modal(modalElement);
            this.setupEvents();
            console.log('✅ WarehouseStockModal 정상 초기화');
        } else {
            console.warn('⚠️ warehouseStockModal 엘리먼트를 찾을 수 없음');
        }
    },
    
    // 이벤트 설정
    setupEvents() {
        // 검색 버튼 클릭
        const searchBtn = document.getElementById('modalSearchBtn');
        if (searchBtn) {
            searchBtn.addEventListener('click', () => this.performSearch());
        }
        
        // 검색 입력에서 엔터 키
        const searchInput = document.getElementById('modalSearchInput');
        if (searchInput) {
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.performSearch();
                }
            });
        }
        
        // 필터 변경
        ['modalItemTypeFilter', 'modalStockStatusFilter'].forEach(id => {
            const element = document.getElementById(id);
            if (element) {
                element.addEventListener('change', () => this.performSearch());
            }
        });
        
        // 엑셀 다운로드
        const excelBtn = document.getElementById('modalExcelDownloadBtn');
        if (excelBtn) {
            excelBtn.addEventListener('click', () => this.downloadExcel());
        }
    },
    
    // 모달 열기 (창고 ID와 창고 정보를 받아서)
    open(warehouseId, warehouseData) {
        console.log('🏢 창고 재고 모달 열기 - warehouseId:', warehouseId);
        console.log('창고 데이터:', warehouseData);
        
        if (!warehouseId) {
            StockUtils.showError('창고 정보가 없습니다.');
            return;
        }
        
		// 🔧 미사용 창고 체크
		if (warehouseData && warehouseData.useYn === 'N') {
		    console.warn('⚠️ 미사용 창고 접근 시도:', warehouseData.warehouseName);
		    StockUtils.showError(`'${warehouseData.warehouseName}'은(는) 사용하지 않는 창고입니다.\n재고 조회가 제한됩니다.`);
		    return;
		}
		
        this.currentWarehouseId = warehouseId;
        this.currentWarehouseData = warehouseData;
        this.currentPage = 0;
        
        // 모달 헤더 정보 설정
        this.updateModalHeader(warehouseData);
        
        // 검색 조건 초기화
        this.resetSearchConditions();
        
        // 재고 데이터 로드
        this.loadWarehouseStocks();
        
        // 모달 표시
        if (this.modalInstance) {
            this.modalInstance.show();
        }
    },
    
    // 모달 헤더 정보 업데이트
    updateModalHeader(warehouseData) {
        const elements = {
            modalWarehouseName: warehouseData.warehouseName || '알 수 없는 창고',
            modalWarehouseCode: warehouseData.warehouseCode || '-',
            modalWarehouseType: this.getWarehouseTypeLabel(warehouseData.warehouseType)
        };
        
        Object.entries(elements).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = value;
				
				// 🔧 미사용 창고인 경우 스타일 변경
				if (warehouseData.useYn === 'N') {
				    element.style.color = '#6c757d';
				    element.style.textDecoration = 'line-through';
				}
				
            }
        });
		
		// 🔧 미사용 창고 경고 메시지 추가
		if (warehouseData.useYn === 'N') {
		    const modalTitle = document.querySelector('#warehouseStockModal .modal-title');
		    if (modalTitle) {
		        modalTitle.innerHTML += ' <span class="badge bg-secondary ms-2">미사용</span>';
		    }
		}
		
    },
    
    // 창고 유형 라벨 변환
    getWarehouseTypeLabel(warehouseType) {
        switch (warehouseType) {
            case 'RAW': return '자재창고';
            case 'PRODUCT': return '완제품창고';
            default: return warehouseType || '-';
        }
    },
    
    // 검색 조건 초기화
    resetSearchConditions() {
        const searchInput = document.getElementById('modalSearchInput');
        const itemTypeFilter = document.getElementById('modalItemTypeFilter');
        const stockStatusFilter = document.getElementById('modalStockStatusFilter');
        
        if (searchInput) searchInput.value = '';
        if (itemTypeFilter) itemTypeFilter.value = '';
        if (stockStatusFilter) stockStatusFilter.value = '';
    },
    
    // 창고별 재고 데이터 로드
    loadWarehouseStocks() {
        if (!this.currentWarehouseId) {
            console.error('❌ 창고 ID가 설정되지 않음');
            return;
        }
		
		// 🔧 미사용 창고 재체크
		if (this.currentWarehouseData && this.currentWarehouseData.useYn === 'N') {
		    this.showError('사용하지 않는 창고의 재고는 조회할 수 없습니다.');
		    return;
		}
        
        // 검색 조건 수집
        const keyword = document.getElementById('modalSearchInput')?.value.trim() || '';
        const itemType = document.getElementById('modalItemTypeFilter')?.value || '';
        const stockStatus = document.getElementById('modalStockStatusFilter')?.value || '';
        
        // API 파라미터 구성
        const params = new URLSearchParams({
            whs: this.currentWarehouseData.warehouseCode, // 창고 코드로 필터링
            page: this.currentPage,
            size: this.pageSize,
            sort: 'item.name,ASC' // 품목명 오름차순
        });
        
        if (keyword) params.append('keyword', keyword);
        if (itemType) params.append('itemType', itemType);
        if (stockStatus) params.append('stockStatus', stockStatus);
        
        const url = `/api/stocks?${params}`;
        console.log('📡 API 요청:', url);
        
        // 로딩 상태 표시
        this.showLoading();
        
        fetch(url)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                return response.json();
            })
            .then(pageData => {
                console.log('✅ 재고 데이터 수신:', pageData);
                this.updateStockTable(pageData.content || []);
                this.updateSummaryInfo(pageData);
                this.updatePagination(pageData);
            })
			.catch(error => {
			    console.error('❌ 재고 데이터 로드 실패:', error);
			    
			    // 🔧 미사용 창고 접근 시 특별 처리
			    if (error.message.includes('사용하지 않는 창고') || 
			        error.message.includes('제한됩니다')) {
			        this.showError('사용하지 않는 창고입니다. 재고 조회가 제한됩니다.');
			    } else {
			        this.showError('재고 데이터를 불러오는데 실패했습니다: ' + error.message);
			    }
			});
    },
    
    // 로딩 상태 표시
    showLoading() {
        const tbody = document.getElementById('modalStockTableBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-4">
                        <i class="fas fa-spinner fa-spin me-2"></i>
                        로딩 중...
                    </td>
                </tr>
            `;
        }
    },
    
    // 에러 상태 표시
    showError(message) {
        const tbody = document.getElementById('modalStockTableBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-4 text-danger">
                        <i class="fas fa-exclamation-triangle me-2"></i>
                        ${message}
                    </td>
                </tr>
            `;
        }
    },
    
    // 재고 테이블 업데이트
    updateStockTable(stocks) {
        const tbody = document.getElementById('modalStockTableBody');
        if (!tbody) return;
        
        if (!stocks || stocks.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-4 text-muted">
                        <i class="fas fa-inbox fa-2x mb-2"></i><br>
                        해당 조건의 재고가 없습니다.
                    </td>
                </tr>
            `;
            return;
        }
        
        tbody.innerHTML = '';
        
        stocks.forEach((stock, index) => {
            const rowNumber = this.currentPage * this.pageSize + index + 1;
            const row = this.createStockRow(stock, rowNumber);
            tbody.appendChild(row);
        });
    },
    
    // 재고 행 생성
    createStockRow(stock, rowNumber) {
        const tr = document.createElement('tr');
        
        const lastInDate = stock.actualLastInAt ? 
            this.formatDate(stock.actualLastInAt) : 
            (stock.lastInDate ? this.formatDate(stock.lastInDate) : '-');
        
        const itemTypeLabel = stock.itemType === 'raw' ? '자재' : 
                             (stock.itemType === 'product' ? '완제품' : stock.itemType);
        
        const statusBadge = this.getStockStatusBadge(stock);
        
        tr.innerHTML = `
            <td class="text-center">${rowNumber}</td>
            <td><code>${stock.itemCode || '-'}</code></td>
            <td><strong>${stock.itemName || '-'}</strong></td>
            <td class="text-center">${itemTypeLabel}</td>
            <td class="text-end"><strong>${this.formatNumber(stock.quantity)}</strong></td>
            <td class="text-center">${stock.unit || stock.itemUnit || 'EA'}</td>
            <td class="text-center"><small>${lastInDate}</small></td>
            <td class="text-center">${statusBadge}</td>
        `;
        
        return tr;
    },
    
    // 재고 상태 배지 생성
    getStockStatusBadge(stock) {
        const quantity = Number(stock.quantity);
        if (quantity === 0) {
            return '<span class="badge bg-danger">재고없음</span>';
        } else if (stock.isBelowSafety || quantity < 10) {
            return '<span class="badge bg-warning">안전재고미달</span>';
        } else {
            return '<span class="badge bg-success">정상</span>';
        }
    },
    
    // 요약 정보 업데이트
    updateSummaryInfo(pageData) {
        const totalItems = pageData.totalElements || 0;
        
        // 총 재고량 계산 (현재 페이지 기준)
        const currentPageTotalQty = (pageData.content || [])
            .reduce((sum, stock) => sum + (stock.quantity || 0), 0);
        
        const totalItemsElement = document.getElementById('modalTotalItems');
        const totalQtyElement = document.getElementById('modalTotalQty');
        
        if (totalItemsElement) {
            totalItemsElement.textContent = this.formatNumber(totalItems);
        }
        
        if (totalQtyElement) {
            // 실제로는 전체 재고량을 계산하려면 별도 API가 필요하지만,
            // 일단 현재 페이지 기준으로 표시
            totalQtyElement.textContent = this.formatNumber(currentPageTotalQty);
        }
    },
    
    // 페이징 업데이트
    updatePagination(pageData) {
        const paginationInfo = document.getElementById('modalPaginationInfo');
        const paginationUl = document.getElementById('modalPaginationUl');
        
        if (paginationInfo) {
            const totalElements = pageData.totalElements || 0;
            const currentPage = (pageData.number || 0) + 1;
            const totalPages = pageData.totalPages || 0;
            
            paginationInfo.innerHTML = `
                총 ${this.formatNumber(totalElements)}개 품목 
                (${currentPage} / ${totalPages} 페이지)
            `;
        }
        
        if (paginationUl) {
            this.renderPaginationButtons(pageData);
        }
    },
    
    // 페이징 버튼 렌더링
    renderPaginationButtons(pageData) {
        const paginationUl = document.getElementById('modalPaginationUl');
        if (!paginationUl) return;
        
        const currentPage = pageData.number || 0;
        const totalPages = pageData.totalPages || 0;
        
        if (totalPages <= 1) {
            paginationUl.innerHTML = '';
            return;
        }
        
        let html = '';
        
        // 이전 버튼
        if (pageData.hasPrevious) {
            html += `
                <li class="page-item">
                    <a class="page-link" href="#" onclick="WarehouseStockModal.goToPage(${currentPage - 1})">
                        <i class="fas fa-chevron-left"></i>
                    </a>
                </li>
            `;
        }
        
        // 페이지 번호들 (간단하게 현재 ±2 페이지만)
        const startPage = Math.max(0, currentPage - 2);
        const endPage = Math.min(totalPages - 1, currentPage + 2);
        
        for (let i = startPage; i <= endPage; i++) {
            const isActive = i === currentPage;
            html += `
                <li class="page-item ${isActive ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="WarehouseStockModal.goToPage(${i})">
                        ${i + 1}
                    </a>
                </li>
            `;
        }
        
        // 다음 버튼
        if (pageData.hasNext) {
            html += `
                <li class="page-item">
                    <a class="page-link" href="#" onclick="WarehouseStockModal.goToPage(${currentPage + 1})">
                        <i class="fas fa-chevron-right"></i>
                    </a>
                </li>
            `;
        }
        
        paginationUl.innerHTML = html;
    },
    
    // 페이지 이동
    goToPage(pageNumber) {
        if (pageNumber < 0) return;
        
        this.currentPage = pageNumber;
        this.loadWarehouseStocks();
    },
    
    // 검색 실행
    performSearch() {
        this.currentPage = 0; // 검색 시 첫 페이지로
        this.loadWarehouseStocks();
    },
    
    // 엑셀 다운로드
    downloadExcel() {
        if (!this.currentWarehouseData) {
            StockUtils.showError('창고 정보가 없습니다.');
            return;
        }
        
        const keyword = document.getElementById('modalSearchInput')?.value.trim() || '';
        const itemType = document.getElementById('modalItemTypeFilter')?.value || '';
        const stockStatus = document.getElementById('modalStockStatusFilter')?.value || '';
        
        const params = new URLSearchParams({
            whs: this.currentWarehouseData.warehouseCode
        });
        
        if (keyword) params.append('keyword', keyword);
        if (itemType) params.append('itemType', itemType);
        if (stockStatus) params.append('stockStatus', stockStatus);
        
        const fileName = `${this.currentWarehouseData.warehouseName}_재고현황.csv`;
        
        // 기존 엑셀 다운로드 API 활용
        window.location.href = `/api/stocks/excel?${params}`;
        
        StockUtils.showSuccess('엑셀 다운로드를 시작합니다.');
    },
    
    // 유틸리티 메서드들
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
    }
};

// 전역 함수로 노출 (HTML onclick에서 사용)
window.WarehouseStockModal = WarehouseStockModal;

// DOM 로드 완료 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    // 약간의 지연 후 초기화 (다른 모듈들과의 충돌 방지)
    setTimeout(() => {
        WarehouseStockModal.init();
    }, 100);
});