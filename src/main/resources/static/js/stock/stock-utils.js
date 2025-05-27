// 재고 관리 유틸리티

const StockUtils = {
    
    // 날짜 포맷팅
    formatDate(dateStr) {
        if (!dateStr) return '-';
        try {
            return new Date(dateStr).toISOString().split('T')[0];
        } catch (error) {
            return '-';
        }
    },

    // 날짜 + 시간 포맷팅
    formatDateTime(dateStr) {
        if (!dateStr) return '-';
        try {
            const date = new Date(dateStr);
            return date.toLocaleDateString('ko-KR') + ' ' + 
                   date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
        } catch (error) {
            return '-';
        }
    },

    // 숫자 포맷팅 (콤마)
    formatNumber(num) {
        if (num == null) return '0';
        return Number(num).toLocaleString();
    },

    // 문자열 안전 처리
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    },

    // 로딩 표시
    showLoading(element) {
        if (element) {
            element.innerHTML = '<div class="text-center"><div class="spinner-border" role="status"></div></div>';
        }
    },

    // 에러 메시지 표시
    showError(message) {
        console.error(message);
        alert(message);
    },

    // 성공 메시지 표시
    showSuccess(message) {
        console.log(message);
        // Toast나 다른 알림 방식으로 변경 가능
        alert(message);
    },

    // API 응답 에러 처리
    handleApiError(error, defaultMessage = '요청 처리에 실패했습니다.') {
        console.error('API Error:', error);
        const message = error.message || defaultMessage;
        this.showError(message);
    },

    // 페이지 번호 계산
    getPageNumber(currentPage, pageSize, index) {
        return currentPage * pageSize + index + 1;
    }
};