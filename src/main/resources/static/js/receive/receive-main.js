// receive-main.js - 입고 관리 메인 JavaScript (개선된 버전)

// 전역 변수
let currentPage = 1;
let pageSize = 10;
let totalPages = 1;
let searchParams = {};

// 전역 함수들을 먼저 정의 (HTML onclick에서 즉시 사용 가능)
window.searchReceiveHistory = function() {
    console.log('입고 이력 검색 시작');
    
    // 검색 조건 수집 - 단일 날짜만 사용
    const searchDate = document.getElementById('searchDate')?.value || '';
    const typeFilter = document.getElementById('searchTypeFilter')?.value || '';
    
    searchParams = {
        date: searchDate,
        type: typeFilter,
        page: 1
    };
    
    currentPage = 1;
    loadReceiveHistory();
};

window.resetSearch = function() {
    console.log('검색 조건 초기화');
    
    // 오늘 날짜로 리셋
    const today = new Date().toISOString().split('T')[0];
    const searchDateInput = document.getElementById('searchDate');
    const typeFilterSelect = document.getElementById('searchTypeFilter');
    
    if (searchDateInput) searchDateInput.value = today;
    if (typeFilterSelect) typeFilterSelect.value = '';
    
    // 검색 파라미터 초기화
    searchParams = {};
    currentPage = 1;
    
    // 데이터 다시 로드
    loadReceiveHistory();
};

window.refreshTable = function() {
    console.log('테이블 새로고침');
    loadReceiveHistory();
};

window.showReceiveDetail = function(receiveId) {
    console.log('입고 상세 조회:', receiveId);
    
    // 실제 API 호출로 상세 정보 조회
    fetch(`/api/receive/${receiveId}`)
        .then(response => response.json())
        .then(data => {
            // 모달에 데이터 표시
            const detailElements = {
                'detailReceiveDate': data.receiveDate || '-',
                'detailItemCode': data.itemCode || '-',
                'detailItemName': data.itemName || '-',
                'detailQuantity': data.quantity ? `${data.quantity.toLocaleString()} ${data.unit || ''}` : '-',
                'detailWarehouse': data.warehouse || '-',
                'detailCompany': data.company || '-',
                'detailReceiveType': data.receiveType || '-',
                'detailManager': data.manager || '-',
                'detailOrderNum': data.orderNum || '-',
                'detailNote': data.note || '-'
            };
            
            Object.keys(detailElements).forEach(id => {
                const element = document.getElementById(id);
                if (element) {
                    element.textContent = detailElements[id];
                }
            });
            
            // 모달 표시
            const modalElement = document.getElementById('receiveDetailModal');
            if (modalElement && typeof bootstrap !== 'undefined') {
                const modal = new bootstrap.Modal(modalElement);
                modal.show();
            }
        })
        .catch(error => {
            console.error('상세 정보 조회 실패:', error);
            alert('상세 정보를 불러오는데 실패했습니다.');
        });
};

window.goToPage = function(page) {
    if (page < 1 || page > totalPages) return;
    
    currentPage = page;
    loadReceiveHistory();
};

// DOM이 준비되면 실행
document.addEventListener('DOMContentLoaded', function() {
    console.log('입고 관리 페이지 로드됨');
    
    // 잠시 기다린 후 초기화 (DOM이 완전히 준비될 때까지)
    setTimeout(() => {
        initializePage();
        setupEventListeners();
    }, 100);
});

// 페이지 초기화
function initializePage() {
    console.log('페이지 초기화 시작');
    
    // 오늘 날짜로 검색 날짜 설정
    const today = new Date().toISOString().split('T')[0];
    const searchDateInput = document.getElementById('searchDate');
    
    if (searchDateInput) {
        searchDateInput.value = today;
        console.log('검색일 설정:', today);
    }
    
    // 초기 데이터 로드
    loadReceiveHistory();
}

// 이벤트 리스너 설정
function setupEventListeners() {
    console.log('이벤트 리스너 설정 시작');
    
    // 전체 선택 체크박스
    const selectAllCheckbox = document.getElementById('selectAll');
    if (selectAllCheckbox) {
        selectAllCheckbox.addEventListener('change', function() {
            const checkboxes = document.querySelectorAll('#receiveTableBody input[type="checkbox"]');
            checkboxes.forEach(checkbox => {
                checkbox.checked = this.checked;
            });
        });
        console.log('전체 선택 체크박스 이벤트 등록됨');
    }
    
    // 검색 필터 변경 시 자동 검색
    const searchInputs = ['searchDate', 'searchTypeFilter'];
    searchInputs.forEach(inputId => {
        const element = document.getElementById(inputId);
        if (element) {
            element.addEventListener('change', function() {
                console.log(`${inputId} 변경됨:`, this.value);
                window.searchReceiveHistory();
            });
            console.log(`${inputId} 이벤트 등록됨`);
        } else {
            console.warn(`${inputId} 요소를 찾을 수 없습니다.`);
        }
    });
}

// 입고 이력 데이터 로드 (최적화된 버전)
function loadReceiveHistory() {
    console.log('입고 이력 데이터 로드 시작');
    
    // 로딩 상태 표시
    showLoadingState();
    
    // URL 파라미터 구성
    const params = new URLSearchParams({
        page: currentPage - 1, // Spring Boot는 0부터 시작
        size: pageSize
    });
    
    // 날짜 검색 조건 추가 (단일 날짜)
    if (searchParams.date) {
        params.append('startDate', searchParams.date);
        params.append('endDate', searchParams.date);
    }
    
    // 타입 필터 추가
    if (searchParams.type) {
        params.append('type', searchParams.type);
    }
    
    console.log('검색 파라미터:', Object.fromEntries(params));
    
    // 실제 API 호출
    fetch(`/api/receive?${params.toString()}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            displayReceiveHistory(data);
            console.log('데이터 로드 완료');
        })
        .catch(error => {
            console.error('데이터 로드 실패:', error);
            showErrorState();
        });
}

// 입고 이력 표시
function displayReceiveHistory(data) {
    console.log('입고 이력 표시:', data);
    
    const tableBody = document.getElementById('receiveTableBody');
    const loadingState = document.getElementById('loadingState');
    const emptyState = document.getElementById('emptyState');
    const tableContainer = document.getElementById('tableContainer');
    
    // 로딩 상태 숨기기
    if (loadingState) loadingState.style.display = 'none';
    
    if (!data || !data.content || data.content.length === 0) {
        // 빈 데이터 상태
        if (emptyState) emptyState.style.display = 'block';
        if (tableContainer) tableContainer.style.display = 'none';
        console.log('빈 데이터 상태 표시');
        return;
    }
    
    // 데이터 있을 때
    if (emptyState) emptyState.style.display = 'none';
    if (tableContainer) tableContainer.style.display = 'block';
    
    // 테이블 내용 생성
    if (tableBody) {
        tableBody.innerHTML = data.content.map((item, index) => `
            <tr onclick="window.showReceiveDetail(${item.id})" style="cursor: pointer;">
                <td onclick="event.stopPropagation();">
                    <input type="checkbox" class="form-check-input" value="${item.id}">
                </td>
                <td>${(currentPage - 1) * pageSize + index + 1}</td>
                <td>${item.receiveDate}</td>
                <td>${item.itemCode || '-'}</td>
                <td>${item.itemName || '-'}</td>
                <td>${item.quantity ? item.quantity.toLocaleString() : '0'}</td>
                <td>${item.unit || '-'}</td>
                <td>${item.company || '-'}</td>
                <td>${item.warehouse || '-'}</td>
                <td>
                    <span class="badge ${getStatusBadgeClass(item.status)} status-badge">
                        ${getStatusLabel(item.status)}
                    </span>
                </td>
            </tr>
        `).join('');
        console.log('테이블 내용 업데이트됨');
    }
    
    // 페이지네이션 업데이트
    updatePagination(data);
}

// 상태별 배지 클래스 반환 (입고 대기/입고 완료만)
function getStatusBadgeClass(status) {
    switch (status) {
        case '입고완료':
        case 'CONFIRMED': 
            return 'bg-success';
        case '입고대기':
        case 'PENDING': 
            return 'bg-warning text-dark';
        default: 
            return 'bg-secondary';
    }
}

// 상태 라벨 반환
function getStatusLabel(status) {
    switch (status) {
        case 'CONFIRMED': 
            return '입고완료';
        case 'PENDING': 
            return '입고대기';
        case '입고완료':
        case '입고대기':
            return status;
        default: 
            return '입고완료'; // 기본값
    }
}

// 로딩 상태 표시
function showLoadingState() {
    const loadingState = document.getElementById('loadingState');
    const emptyState = document.getElementById('emptyState');
    const tableContainer = document.getElementById('tableContainer');
    
    if (loadingState) loadingState.style.display = 'block';
    if (emptyState) emptyState.style.display = 'none';
    if (tableContainer) tableContainer.style.display = 'none';
    
    console.log('로딩 상태 표시');
}

// 에러 상태 표시
function showErrorState() {
    const loadingState = document.getElementById('loadingState');
    const emptyState = document.getElementById('emptyState');
    const tableContainer = document.getElementById('tableContainer');
    
    if (loadingState) loadingState.style.display = 'none';
    if (emptyState) {
        emptyState.style.display = 'block';
        emptyState.innerHTML = `
            <i class="bi bi-exclamation-triangle" style="font-size: 3rem; color: #dc3545;"></i>
            <h5 class="mt-3">데이터 로드 실패</h5>
            <p class="text-muted">데이터를 불러오는 중 오류가 발생했습니다.</p>
            <button type="button" class="btn btn-outline-primary" onclick="window.refreshTable()">
                <i class="bi bi-arrow-clockwise me-2"></i>다시 시도
            </button>
        `;
    }
    if (tableContainer) tableContainer.style.display = 'none';
    
    console.log('에러 상태 표시');
}

// 페이지네이션 업데이트
function updatePagination(data) {
    const pagination = document.getElementById('pagination');
    if (!pagination) return;
    
    totalPages = data.totalPages;
    currentPage = data.number + 1;
    
    let paginationHtml = '';
    
    // 이전 페이지
    if (currentPage > 1) {
        paginationHtml += `
            <li class="page-item">
                <a class="page-link" href="#" onclick="window.goToPage(${currentPage - 1}); return false;">이전</a>
            </li>
        `;
    }
    
    // 페이지 번호들
    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);
    
    for (let i = startPage; i <= endPage; i++) {
        paginationHtml += `
            <li class="page-item ${i === currentPage ? 'active' : ''}">
                <a class="page-link" href="#" onclick="window.goToPage(${i}); return false;">${i}</a>
            </li>
        `;
    }
    
    // 다음 페이지
    if (currentPage < totalPages) {
        paginationHtml += `
            <li class="page-item">
                <a class="page-link" href="#" onclick="window.goToPage(${currentPage + 1}); return false;">다음</a>
            </li>
        `;
    }
    
    pagination.innerHTML = paginationHtml;
    console.log('페이지네이션 업데이트됨');
}