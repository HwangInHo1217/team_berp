/**
 * 창고 관리 JavaScript - 리팩토링 버전
 * 약 300줄 단축 (1000줄 → 700줄)
 */

// ========== 전역 변수 및 상태 ==========
//🔧 1. 전역 상태 변수 state
const state = {
    isWhsCodeVerified: false,     // 창고 코드 중복 확인 여부
    originalWhsCode: '',          // 기존 창고 코드 (수정 모드에서 비교용)
    currentPageNum: 1,            // 현재 페이지 번호 (페이징용)
    pageSize: 10                  // 한 페이지당 항목 수
};
//주요 셀렉터 모음
//📌 2. DOM 셀렉터 모음 selectors
const selectors = {
	form: '#warehouseForm',                      // 등록/수정 폼
	modal: '#warehouseRegisterModal',            // 모달 ID
	tbody: 'table tbody',                        // 테이블 본문
	totalCount: '#totalCount',                   // 총 개수 텍스트
	nav: 'nav',                                  // 페이징 영역
	inputs: {
	    warehouseCode: '#warehouseCode',         // 창고 코드 input
	    searchKeyword: '#searchKeyword',         // 검색어 input
	    useYnFilter: '#useYnFilter',             // 사용여부 필터
	    searchType: '#searchType',               // 검색 타입 select
	    isCodeChecked: '#isCodeChecked',         // 코드 확인 여부 hidden input
	    codeCheckResult: '#codeCheckResult'      // 코드 확인 메시지 영역
    }
};

// ========== 유틸리티 함수 ==========
//🧰 3. 유틸리티 함수들 utils
//DOM 생성, 셀렉터, fetch 래퍼, 메시지 출력 등 유틸리티 제공
const utils = {
	/**
	  * HTML 요소를 생성하고, 속성이나 이벤트를 지정해 반환하는 헬퍼 함수
	  * @param {string} tag - 생성할 태그명 (예: 'div', 'tr', 'button')
	  * @param {object} options - 요소에 적용할 속성들
	  * 예: { text: '클릭', class: 'btn', onClick: handler }
	  */
	createEl(tag, options = {}) { // DOM 요소를 동적으로 생성할 때 사용
        const el = document.createElement(tag); // 지정된 태그 요소 생성
        Object.entries(options).forEach(([key, value]) => {
            if (key === 'text') el.textContent = value; // 텍스트 설정
            else if (key === 'html') el.innerHTML = value; // HTML 설정
            else if (key.startsWith('on'))  // 이벤트 리스너 바인딩 (예: onClick)
				el.addEventListener(key.slice(2), value);
            else el.setAttribute(key, value); // 일반 속성 설정 (class, id 등)
        });
        return el;
    },

    // 쿼리 셀렉터 헬퍼
	/**
	 * document.querySelector 축약 함수 (하나만 선택)
	 * @param {string} selector - CSS 셀렉터
	 * @returns {Element} - 매칭된 요소
	 */
    $(selector) {
        return document.querySelector(selector);
    },

	/**
	 * document.querySelectorAll 축약 함수 (여러 개 선택)
	 * @param {string} selector - CSS 셀렉터
	 * @returns {NodeList} - 매칭된 요소들
	 */
    $$(selector) {
        return document.querySelectorAll(selector);
    },

    //fetchJson(url, options) - fetch 래퍼: 에러 처리 및 JSON 파싱 포함

	/**
	 * fetch 요청을 보내고 JSON 결과를 받아오는 헬퍼 함수
	 * - 기본적으로 'Content-Type: application/json' 헤더 포함
	 * - 204 No Content이면 null 반환
	 * - 오류 발생 시 JSON 내부의 message를 포함한 Error 발생
	 * 
	 * @param {string} url - 요청 보낼 API 경로
	 * @param {object} options - fetch 옵션 (method, body 등)
	 * @returns {Promise<object|null>} - JSON 결과 또는 null
	 */	
    async fetchJson(url, options = {}) {
        try {
            const response = await fetch(url, {
                headers: { 'Content-Type': 'application/json' },
                ...options
            });
			// 실패 응답 처리
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({})); // JSON 파싱 실패 방지
                throw new Error(errorData.message || `HTTP ${response.status}`);
            }
			// 성공 응답 처리
            return response.status === 204 ? null : await response.json();
        } catch (error) {
            console.error('API 오류:', error); // 콘솔에 에러 출력
            throw error; // 호출자에게 에러 전달
        }
    },

	/**
	 * 여러 요소의 이벤트를 한번에 바인딩해주는 헬퍼 함수
	 * @param {Array} eventMap - [selector, event, handler] 형태 배열
	 */
    // 이벤트 바인딩 헬퍼
    bindEvents(eventMap) {
        eventMap.forEach(([selector, event, handler]) => {
            const el = this.$(selector);
            if (el) el.addEventListener(event, handler);
        });
    },

    // 폼 데이터 수집
	/**
	 * 폼 입력값을 객체로 추출해 반환하는 함수
	 * @param {string} formSelector - form의 CSS 셀렉터
	 * @returns {object} - { name: value, ... } 형태의 객체 반환
	 */
    getFormData(formSelector) { // form의 입력값들을 객체로 추출
        const form = this.$(formSelector); // 폼 요소 선택
        const formData = new FormData(form); // FormData 객체 생성
        const data = {};
		
        for (let [key, value] of formData.entries()) {
            data[key] = typeof value === 'string' ? value.trim() : value; // 문자열이면 trim
        }
        return data;
    },

    // 알림 헬퍼
	/**
	 * 알림 메시지를 하단에 표시하는 함수 (코드 중복확인 결과 등)
	 * @param {string} message - 표시할 메시지
	 * @param {string} type - Bootstrap 스타일 클래스 (info / danger / success / warning)
	 */	
    showMessage(message, type = 'info') { // 창고코드 중복확인 결과 표시
        const resultEl = this.$(selectors.inputs.codeCheckResult); // 메시지를 보여줄 요소
        if (resultEl) {
            resultEl.textContent = message;
            resultEl.className = `form-text mt-1 text-${type}`; // Bootstrap 스타일 적용
        }
    }
};

// ========== 체크박스 관리 ==========
//✅ 4. 체크박스 제어 모듈 checkboxManager
// ✅ 창고 목록에서 체크박스를 전체 선택/해제하거나, 선택 항목 삭제 등을 제어하는 객체
const checkboxManager = {
	/**
	 * 전체 체크박스 일괄 체크/해제
	 * - '전체 선택' 체크박스를 클릭하면 호출됨
	 * @param {boolean} checked - true이면 전체 체크, false이면 전체 해제
	 */
    checkAll(checked) { // 전체 체크박스 체크/해제
        utils.$$('input[name="ckbox"]').forEach(cb => cb.checked = checked);
    },
	
	/**
	 * 개별 체크박스를 클릭했을 때
	 * - 전체 선택 체크박스 상태를 동기화함
	 * - 모든 개별 박스가 체크되어 있으면 '전체 체크'도 체크됨
	 */
    updateSelectAll() { // 개별 체크 시 전체 선택 상태 갱신
        const checkboxes = utils.$$('input[name="ckbox"]');
        const checkedCount = Array.from(checkboxes).filter(cb => cb.checked).length;
        const selectAllCb = utils.$('input[type="checkbox"][onclick*="check_all"]');
        
        if (selectAllCb) {
			// 전체 체크 상태로 판단되면 체크 유지, 일부만 체크되면 해제
            selectAllCb.checked = checkedCount > 0 && checkedCount === checkboxes.length;
        }
    },
	
	/**
	 * 현재 체크된 행의 창고 ID만 추출하여 배열로 반환
	 * @returns {string[]} 선택된 창고 ID 배열
	 */
    getSelectedIds() { // 선택된 창고 ID들 배열 반환
        return Array.from(utils.$$('input[name="ckbox"]:checked')).map(cb => cb.value);
    },

	/**
	 * ✅ 체크된 항목을 삭제 요청하는 핵심 함수
	 * - 1. 체크박스 선택 여부 확인
	 * - 2. 삭제 여부 사용자 확인 (confirm)
	 * - 3. 선택된 ID 각각에 대해 DELETE 요청 실행 (병렬)
	 * - 4. 삭제 후 alert 및 테이블 새로고침
	 */	
    async deleteSelected() { // 선택된 항목 DELETE 요청 보내기
        const selectedIds = this.getSelectedIds();
        
		// 아무것도 선택 안 된 경우 경고 후 종료
        if (selectedIds.length === 0) {
            alert('삭제할 항목을 선택해주세요.');
            return;
        }

		// 삭제 전 사용자 확인
        if (!confirm(`선택한 ${selectedIds.length}개의 창고를 삭제하시겠습니까?\n삭제된 데이터는 복구되지 않습니다.`)) {
            return;
        }

        try {
			// 각 ID에 대해 DELETE 요청을 비동기 병렬로 실행
            const deletePromises = selectedIds.map(id => 
                utils.fetchJson(`/api/warehouses/${id}`, { method: 'DELETE' })
            );
            
			// 전체 삭제 요청 완료될 때까지 대기
            await Promise.all(deletePromises);
            
			// 사용자에게 완료 안내
			alert(`${selectedIds.length}개 창고가 성공적으로 삭제되었습니다.`);
            
			// 삭제 후 현재 페이지 다시 조회
            dataManager.refreshCurrentView();
			
			// 전체 체크 상태도 다시 동기화
            this.updateSelectAll();
        } catch (error) {
			// 서버 오류 또는 네트워크 오류 처리
            alert('삭제 중 오류가 발생했습니다: ' + error.message);
        }
    }
};

// ========== 데이터 관리 ==========
// ✅ 5. 창고 목록 조회 및 테이블 렌더링 담당 모듈
// fetch 요청으로 데이터를 불러오고, 화면에 반영하며, 페이징과 연결함
const dataManager = {
	
	/**
	 * 서버로부터 창고 데이터를 로드함 (기본 조회, 검색, 필터링 모두 여기서 처리)
	 * @param {Object} params - 검색어, 필터, 페이지 번호 등 query parameter
	 */
    async loadData(params = {}) { // /api/warehouses 호출하여 데이터 로드
		// 현재 페이지 번호(state.currentPageNum)를 기준으로 파라미터를 구성
		const urlParams = new URLSearchParams({
            page: state.currentPageNum,
            ...params
        });
        
		// 📡 /api/warehouses API 요청 후 응답 데이터 받아옴
        const data = await utils.fetchJson(`/api/warehouses?${urlParams}`);
		// 📋 받은 데이터를 테이블에 그려줌
		this.updateTable(data);
		// 📌 페이지네이션 정보도 함께 갱신
        paginationManager.update(data);
		
        return data;
    },

	/**
	 * 검색 기능 실행 - 키워드와 타입(code/name)을 바탕으로 서버에 요청
	 * - keyword가 없으면 alert
	 */
    async searchData(keyword, searchType) { // 키워드로 검색
        if (!keyword?.trim()) {
            alert('검색어를 입력해주세요.');
            utils.$(selectors.inputs.searchKeyword).focus();
            return;
        }
		// 검색 시 페이지는 항상 1페이지부터 시작
        state.currentPageNum = 1;
		// loadData 호출 시 keyword, searchType이 URL 파라미터로 들어감
        return this.loadData({ keyword: keyword.trim(), searchType });
    },

	/**
	 * 사용여부(Y/N/ALL) 필터에 따라 데이터를 다시 불러옴
	 * 필터를 바꾸면 페이지 번호도 1로 초기화
	 */
    async filterByUseYn(useYnFilter) { // 사용여부 필터
        state.currentPageNum = 1;
		
		// 검색창 초기화 (필터는 검색과 별개로 처리됨)
        utils.$(selectors.inputs.searchKeyword).value = '';
		// 필터 조건을 넣어 요청
        return this.loadData({ useYnFilter });
    },

	/**
	 * 테이블 tbody 영역에 데이터 행을 직접 렌더링하는 함수
	 * - 검색 결과가 없을 때는 빈 행 표시
	 * - 있으면 한 줄씩 행 생성해서 append
	 */
    updateTable(data) { // 테이블 갱신
        const tbody = utils.$(selectors.tbody); // <tbody> DOM 요소 가져오기
        if (!tbody) return;

        const whsList = data.whsList || []; // 받아온 창고 목록 (없으면 빈 배열)
		// 📭 데이터 없을 경우
        if (whsList.length === 0) {
            tbody.innerHTML = this.createEmptyRow(); // "검색 결과 없음" 표시
            this.updateTotalCount(0);
            return;
        }
		// 🧹 기존 테이블 초기화 후, 새로 렌더링
        tbody.innerHTML = '';
        whsList.forEach((whs, index) => {
            const row = this.createTableRow(whs, index, data.currentPage);
            tbody.appendChild(row);
        });
		
		// 상단 '총 N개' 문구 갱신
        this.updateTotalCount(data.totalElements || whsList.length);
		// 체크박스 상태 갱신 (전체선택 등)
		checkboxManager.updateSelectAll();
    },

	/**
	 * 검색 결과가 없을 때 표시할 기본 행 생성
	 * @returns {string} 빈 테이블 행 HTML
	 */
    createEmptyRow() {
        return '<tr><td colspan="8" style="text-align: center; padding: 40px;">검색 결과가 없습니다.</td></tr>';
    },

	/**
	 * 실제 테이블 한 줄을 생성하는 함수
	 * @param {Object} whs - 창고 DTO 데이터 (warehouseId, name, code 등 포함)
	 * @param {number} index - 현재 페이지에서 몇 번째 항목인지
	 * @param {number} currentPage - 현재 페이지 번호
	 */
    createTableRow(whs, index, currentPage) { // 한 줄씩 테이블 행 생성
        const rowNumber = ((currentPage || 1) - 1) * state.pageSize + index + 1;
        
		// HTML 문자열 기반으로 <tr> 요소 생성
        return utils.createEl('tr', {
            html: `
                <td>
                    <label class="d-flex justify-content-center align-items-center" style="cursor: pointer; min-height: 40px; margin: 0; width: 100%;">
                        <input type="checkbox" name="ckbox" value="${whs.warehouseId || ''}" onchange="checkdata()" style="margin: 0;">
                    </label>
                </td>
                <td>${rowNumber}</td>
                <td>${whs.warehouseCode || ''}</td>
                <td>${whs.warehouseName || ''}</td>
                <td>${whs.warehouseType || ''}</td>
                <td>${whs.description || ''}</td>
                <td>${whs.useYn || ''}</td>
                <td>
                    <button type="button" class="btn btn-sm btn-warning edit-btn"
                        data-whs='${JSON.stringify(whs)}'>
                        수정
                    </button>
                </td>
            `
        });
    },

	/**
	 * 상단에 총 등록 창고 수를 표시하는 함수
	 * @param {number} count - 전체 개수
	 */
    updateTotalCount(count) {
        const totalCountEl = utils.$(selectors.totalCount);
        if (totalCountEl) {
            totalCountEl.textContent = `총 ${count}개의 창고가 등록되어 있습니다.`;
        }
    },

	/**
	 * 현재 UI 상태(검색 or 필터)를 기준으로 테이블 다시 조회
	 * - 검색어가 있으면 검색 기준으로
	 * - 없으면 필터 기준으로 조회
	 */
    refreshCurrentView() { // 검색 or 필터링을 기준으로 다시 조회
        const keyword = utils.$(selectors.inputs.searchKeyword).value.trim();
        const useYnFilter = utils.$(selectors.inputs.useYnFilter).value;
        const searchType = utils.$(selectors.inputs.searchType).value;
        
        if (keyword) {
            this.loadData({ keyword, searchType, page: state.currentPageNum });
        } else {
            this.loadData({ useYnFilter, page: state.currentPageNum });
        }
    }
};

// ========== 페이지네이션 관리 ==========
//🔁 6. 페이징 관리 paginationManager
const paginationManager = {
	/**
	 * 서버에서 받은 페이징 정보(data)를 기반으로 내부 상태 갱신 및 UI 렌더링 수행
	 * @param {Object} data - 현재 페이지, 총 페이지 수, 다음/이전 여부, 총 개수 포함
	 */
    update(data) { // 현재 페이지, 총 페이지 등 상태 반영
        const { currentPage = 1, totalPages = 0, hasNext = false, hasPrevious = false, totalElements = 0 } = data;
        
		// 전역 상태에 현재 페이지 저장
        state.currentPageNum = currentPage;
		// UI에 페이지네이션 버튼 렌더링
		this.render({ currentPage, totalPages, hasNext, hasPrevious });
		// 상단 총 개수도 같이 갱신
		dataManager.updateTotalCount(totalElements);
    },
	
	/**
	 * 페이징 UI를 실제로 생성하여 <nav> 안에 삽입
	 * @param {Object} data - 페이징 상태 정보 (update에서 전달됨)
	 */
    render(data) { // 페이징 버튼 생성
        const nav = utils.$(selectors.nav); // 페이징이 들어갈 <nav> DOM 요소
        if (!nav) return; // 예외 처리
		// 전체 페이지 수가 0이면 페이징 UI 숨김
        if (data.totalPages === 0) {
            nav.style.display = 'none';
            return;
        }
		// 있으면 보여주고 HTML 삽입
        nav.style.display = 'block';
        nav.innerHTML = this.buildPaginationHtml(data);
    },

	/**
	 * HTML 페이징 버튼들 (이전, 숫자, 다음)을 구성하는 함수
	 */
    buildPaginationHtml({ currentPage, totalPages, hasNext, hasPrevious }) {
        const buttons = [];
        
        // 이전 버튼
        buttons.push(this.createPageButton('이전', currentPage - 1, !hasPrevious));
        
        // 숫자 페이지 버튼 계산
        const { startPage, endPage } = this.calculatePageRange(currentPage, totalPages);
		// 첫 페이지가 1이 아니면 1과 ... 버튼 추가
        if (startPage > 1) {
            buttons.push(this.createPageButton('1', 1));
            if (startPage > 2) buttons.push(this.createDisabledButton('...'));
        }
		// 중간 페이지 버튼들
        for (let i = startPage; i <= endPage; i++) {
            buttons.push(this.createPageButton(i, i, false, i === currentPage));
        }
        
		// 끝 페이지 처리
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) buttons.push(this.createDisabledButton('...'));
            buttons.push(this.createPageButton(totalPages, totalPages));
        }
        
        // 다음 버튼
        buttons.push(this.createPageButton('다음', currentPage + 1, !hasNext));
		// 전체 버튼을 <ul class="pagination">로 감싸서 반환
        return `<ul class="pagination justify-content-center mt-3">${buttons.join('')}</ul>`;
    },

	/**
	 * 현재 페이지를 기준으로 앞뒤로 몇 개 페이지를 보여줄지 계산
	 * - 기본적으로 현재 페이지 기준 ±2개의 페이지를 보여주는 방식
	 */
    calculatePageRange(currentPage, totalPages) {
        const startPage = Math.max(1, currentPage - 2); // 최소 1부터 시작
        const endPage = Math.min(totalPages, currentPage + 2); // 최대 totalPages까지만
        return { startPage, endPage };
    },

	/**
	 * 실제 페이지 버튼 생성 함수
	 * @param {string|number} text - 버튼에 보여질 텍스트 (숫자, 이전/다음 등)
	 * @param {number} page - 이동할 페이지 번호
	 * @param {boolean} disabled - 클릭 비활성화 여부 (예: "이전"이지만 현재 첫 페이지면 true)
	 * @param {boolean} active - 현재 선택된 페이지 여부 (CSS 강조용)
	 */
    createPageButton(text, page, disabled = false, active = false) {
        const classes = ['page-item'];
        if (disabled) classes.push('disabled'); // 회색 처리
        if (active) classes.push('active'); // 강조 처리
		// 클릭 가능한 경우 <a>, 아닌 경우 <span>
        const content = disabled || active 
            ? `<span class="page-link">${text}</span>`
            : `<a class="page-link" href="javascript:void(0)" onclick="goToPage(${page})">${text}</a>`;
            
        return `<li class="${classes.join(' ')}">${content}</li>`;
    },


	/**
	 * '...'처럼 클릭 불가능한 페이지 버튼을 만들 때 사용
	 * @param {string} text - 예: '...'
	 */
    createDisabledButton(text) {
        return `<li class="page-item disabled"><span class="page-link">${text}</span></li>`;
    }
};

// ========== 모달 및 폼 관리 ==========
//7. 모달/폼 제어 modalManager : 창고 등록/수정 모달을 열고 폼 상태를 설정하는 UI 제어 객체
const modalManager = {

	/**
	 * 모달을 보여주는 함수. 등록 모드 or 수정 모드를 구분해서 처리
	 * @param {string} mode - 'new' 또는 'edit'
	 * @param {Object|null} whsData - 수정할 경우, 기존 창고 데이터
	 */
    show(mode = 'new', whsData = null) { // 'new' 또는 'edit' 모달 띄우기
        const form = utils.$(selectors.form); // 폼 요소
        const modal = bootstrap.Modal.getOrCreateInstance(utils.$(selectors.modal));
		// 폼 초기화 (기존 데이터 제거)
        this.resetForm();
		// 모드에 따라 신규 or 수정 셋업
        if (mode === 'edit' && whsData) {
            this.setupEditMode(whsData);
        } else {
            this.setupNewMode();
        }
        
		// 실제로 모달 열기
        modal.show();
    },

	/**
	 * 신규 등록 모드로 모달 설정
	 * - 필드 초기화
	 * - '등록' 모드 표시
	 * - 창고 유형 선택 시 자동 코드 생성 안내
	 */
    setupNewMode() { // 신규 등록 시 UI 초기화
        const form = utils.$(selectors.form);
        form.removeAttribute('data-editing-id'); // 수정 모드 아님 표시
        
		// 창고코드 수동 입력 가능 (자동 생성 안내 메시지도 같이)
        utils.$('#warehouseRegisterModalLabel').textContent = '신규 창고 등록';
        form.querySelector('button[type="submit"]').textContent = '등록';
        
        // 코드 입력 활성화 및 안내
        utils.$(selectors.inputs.warehouseCode).readOnly = false;
        utils.$('#checkDuplicateBtn').style.display = 'inline-block';
        utils.showMessage('창고 유형을 선택하면 코드가 자동 생성됩니다.', 'info');
        
        // 신규 등록 시에는 '미사용(N)' 선택 불가능하게 비활성화
        const useYnSelect = utils.$('#useYn');
        useYnSelect.value = 'Y';
        useYnSelect.querySelector('option[value="N"]').disabled = true;
		// 상태 초기화
        state.isWhsCodeVerified = false;
        state.originalWhsCode = '';
    },

	/**
	 * 수정 모드로 모달 설정
	 * - 기존 데이터 채움
	 * - '수정' 모드 표시
	 * - 코드 변경 가능 + 중복 확인 유도
	 */
    setupEditMode(whsData) { // 수정 시 기존 데이터 채움
        const form = utils.$(selectors.form);
        form.setAttribute('data-editing-id', whsData.warehouseId); // 수정 중인 ID 저장
        
        utils.$('#warehouseRegisterModalLabel').textContent = '창고 정보 수정';
        form.querySelector('button[type="submit"]').textContent = '수정';
        
        // 기존 창고 정보 폼에 채우기
        this.populateForm(whsData);
        
        // 창고코드는 수정 가능 (주의문 표시)
        utils.$(selectors.inputs.warehouseCode).readOnly = false;
        utils.$('#checkDuplicateBtn').style.display = 'inline-block';
        utils.showMessage('창고 코드 변경 시 중복확인을 해주세요.', 'warning');
		// 이미 중복 확인된 것으로 가정
		utils.$(selectors.inputs.isCodeChecked).value = 'true';
        
        // 수정 시 '미사용'도 선택 가능하게 열어둠
        utils.$('#useYn option[value="N"]').disabled = false;
        
        state.isWhsCodeVerified = true;
        state.originalWhsCode = whsData.warehouseCode;
    },

	/**
	 * 전달받은 창고 데이터를 폼 입력창에 채워 넣음
	 * @param {Object} whsData - 서버에서 받아온 Warehouse DTO
	 */
    populateForm(whsData) { // 모달 닫힐 때 폼 초기화
        const fields = ['warehouseId', 'warehouseCode', 'warehouseName', 'warehouseType', 'description', 'useYn'];
        fields.forEach(field => {
            const el = utils.$(`#${field}`);
            if (el) el.value = whsData[field] || '';
        });
    },

	/**
	 * 모달 닫히거나 새로 열릴 때 폼 초기화
	 * - 입력 값 제거, 상태 리셋, 에러 메시지 제거
	 */
    resetForm() { // 모달 닫힐 때 폼 초기화
        const form = utils.$(selectors.form);
        form.reset();
        form.removeAttribute('data-editing-id');
        
		// 코드 중복 메시지 제거
        utils.$(selectors.inputs.codeCheckResult).textContent = '';
        utils.$(selectors.inputs.isCodeChecked).value = 'false';
		// '미사용' 옵션 다시 활성화
		utils.$('#useYn option[value="N"]').disabled = false;
		// 상태값 초기화
        state.isWhsCodeVerified = false;
        state.originalWhsCode = '';
    }
};

// ========== 창고 코드 관리 ==========
//🔐 8. 창고 코드 처리 codeManager
// 창고 유형을 기반으로 자동 코드 생성 + 입력된 창고 코드의 중복 여부 확인 담당
const codeManager = {
	/**
	 * 창고 유형(RAW/PRODUCT)에 따라 자동 창고 코드를 서버에서 생성 받아 입력창에 세팅
	 * @param {string} whsType - 창고 구분 코드 (RAW 또는 PRODUCT)
	 */
    async generateCode(whsType) { // 유형에 따라 자동 코드 생성
        try {
			// 백엔드에서 생성된 코드 요청
            const response = await fetch(`/api/warehouses/generate-code?type=${whsType}`, { 
                method: 'GET',
                headers: { 'Content-Type': 'application/json' }
            });
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`); // 응답 에러 처리
            }
            
            const code = await response.text(); // 서버에서는 JSON이 아니라 text로 응답함
            
			// 응답된 코드가 유효하면 입력창에 자동 채움
            if (code && code !== 'INVALID_TYPE' && code.trim() !== '') {
                utils.$(selectors.inputs.warehouseCode).value = code; // 코드 입력창에 자동 입력
                utils.$(selectors.inputs.isCodeChecked).value = 'true'; // 중복확인 상태 true로 설정
                utils.showMessage('자동 생성된 코드입니다.', 'success');
                state.isWhsCodeVerified = true;
            } else {
                throw new Error('유효하지 않은 창고 유형입니다.');
            }
        } catch (error) {
            console.error('코드 생성 오류:', error);
            utils.showMessage('코드 생성 중 오류가 발생했습니다.', 'danger');
            state.isWhsCodeVerified = false;
        }
    },

	/**
	 * 현재 입력된 창고 코드가 기존 데이터와 중복되는지 서버에 확인
	 * 수정 시에는 자기 자신의 코드 제외
	 */
    async checkDuplicate() { // 중복 확인 API 호출
        const whsCode = utils.$(selectors.inputs.warehouseCode).value.trim(); // 입력된 창고 코드
        const editingId = utils.$(selectors.form).getAttribute('data-editing-id'); // 수정 중인 ID

        if (!whsCode) {
			// 공란 방지
            utils.showMessage('창고 코드를 입력해주세요.', 'danger');
            utils.$(selectors.inputs.isCodeChecked).value = 'false';
            return;
        }

        try {
			// 중복 확인 요청 URL 구성 (수정 중일 경우, 자기 자신 제외)
            let checkUrl = `/api/warehouses/check-duplicate?warehouseCode=${encodeURIComponent(whsCode)}`;
            if (editingId) checkUrl += `&excludeId=${editingId}`;

            const result = await utils.fetchJson(checkUrl); // 중복 여부 확인
            
            if (result.status === 'duplicate') {
				// 중복일 경우 안내
                utils.showMessage(result.message || '이미 사용 중인 창고 코드입니다.', 'danger');
                utils.$(selectors.inputs.isCodeChecked).value = 'false';
                state.isWhsCodeVerified = false;
            } else if (result.status === 'ok') {
				// 사용 가능
                utils.showMessage(result.message || '사용 가능한 창고 코드입니다.', 'success');
                utils.$(selectors.inputs.isCodeChecked).value = 'true';
                state.isWhsCodeVerified = true;
            }
        } catch (error) {
            utils.showMessage('중복 확인 중 오류가 발생했습니다.', 'danger');
            state.isWhsCodeVerified = false;
        }
    },

	/**
	 * 코드 중복확인 상태 초기화 (사용자가 입력값 변경 시 호출됨)
	 * - 중복확인 결과 메시지도 제거
	 * - 상태값도 false로 되돌림
	 */
    resetVerification() { // 중복확인 플래그 초기화
        utils.$(selectors.inputs.isCodeChecked).value = 'false';
        utils.$(selectors.inputs.codeCheckResult).textContent = '';
        state.isWhsCodeVerified = false;
    }
};

// ========== 폼 제출 관리 ==========
//📤 9. 폼 제출 formManager
// 창고 등록/수정 모달에서 "등록" 또는 "수정" 버튼 클릭 시 작동
const formManager = {
	
	/**
	 * [submit] 폼 제출 핸들러
	 * 1. 기본 form submit 이벤트 방지
	 * 2. form 내의 입력값 추출
	 * 3. 유효성 검사
	 * 4. 백엔드 API 호출 (등록/수정)
	 * 5. 성공 시 모달 닫고 테이블 새로고침
	 */
    async submit(event) { // 등록/수정 폼 제출 핸들러
        event.preventDefault(); // 폼 제출로 인한 새로고침 방지
        
        const form = event.target; // 현재 제출 중인 form 객체
        const editingId = form.getAttribute('data-editing-id'); // 수정이면 ID 존재, 신규면 null
        const formData = utils.getFormData(selectors.form); // 폼의 모든 입력값을 객체 형태로 수집

		// 입력값 검증 (필수 항목, 중복 확인 여부 등)
        if (!this.validate(formData, editingId)) return;

        try {
            const config = this.getApiConfig(editingId); // 등록/수정 여부에 따라 API 정보 결정
            const result = await utils.fetchJson(config.url, {
                method: config.method,
                body: JSON.stringify(formData)
            });
            
            alert(`창고가 성공적으로 ${config.modeText}되었습니다: ${result.warehouseName}`);
            
			// 모달 닫기 + 테이블 새로고침
            bootstrap.Modal.getInstance(utils.$(selectors.modal)).hide();
            dataManager.refreshCurrentView();
        } catch (error) {
            alert('오류: ' + error.message);
        }
    },

	/**
	 * [validate] 유효성 검사
	 * - 필수 입력값 확인
	 * - 중복 확인 여부 검사
	 * @param formData - 입력된 폼 데이터
	 * @param editingId - 수정 중이면 해당 창고 ID
	 * @returns true/false
	 */
    validate(formData, editingId) { // 필수값 + 중복확인 여부 검증
		
		// 필수 필드가 비었는지 확인
        const requiredFields = [
            ['warehouseCode', '창고 코드를 입력해주세요.'],
            ['warehouseName', '창고명을 입력해주세요.'],
            ['warehouseType', '창고 유형을 선택해주세요.']
        ];

        for (let [field, message] of requiredFields) {
            if (!formData[field]) {
                alert(message);
                utils.$(`#${field}`).focus(); // 해당 입력 필드로 포커스 이동
                return false;
            }
        }

        // 중복 확인 검사
        const isCodeChecked = utils.$(selectors.inputs.isCodeChecked).value === 'true';
		// 수정인데 창고 코드를 변경한 경우 → 중복 확인 필수
        if (editingId) { // POST or PUT API 구성 반환
            if (formData.warehouseCode !== state.originalWhsCode && !state.isWhsCodeVerified) {
                alert('변경된 창고 코드에 대한 중복 확인을 해주세요.');
                return false;
            }
        } else {
			// 신규 등록일 경우 → 무조건 중복 확인 필요
            if (!isCodeChecked) {
                alert('창고 코드 중복 확인을 해주세요.');
                return false; 
            }
        }

        return true; // 통과 시 true 반환
    },

	/**
	 * [getApiConfig] 등록/수정에 따라 API method 및 URL 반환
	 * @param editingId - 수정 시 창고 ID
	 * @returns { method, url, modeText, successStatus }
	 */
    getApiConfig(editingId) {
        return editingId 
            ? { 
				method: 'PUT', 
				url: `/api/warehouses/${editingId}`, 
				modeText: '수정', 
				successStatus: 200 
			}
            : { 
				method: 'POST', 
				url: '/api/warehouses', 
				modeText: '등록', 
				successStatus: 201 
			};
    }
};

// ========== 전역 함수들 (기존 호환성 유지) ==========
//🔁 10. 전역 함수 (기존 HTML의 onclick용) 
// HTML 템플릿에서 직접 사용하는 함수들을 유지하기 위한 global 정의

// 전체 체크박스 상태를 한 번에 변경 (모두 선택/해제)
function check_all(ck) { checkboxManager.checkAll(ck); } // 전체 체크

// 개별 체크박스 상태를 기준으로 "전체 선택" 체크박스를 갱신
function checkdata() { checkboxManager.updateSelectAll(); } // 체크박스 상태 갱신

// 체크된 항목 삭제 수행
function check_del() { checkboxManager.deleteSelected(); } // 삭제 확인 및 실행

// 페이지 번호 클릭 시 해당 페이지로 이동
function goToPage(page) {  // 페이징 이동
    state.currentPageNum = page; 
    dataManager.refreshCurrentView();  // 현재 키워드/필터 유지하며 재조회
}

// ========== 초기화 ==========
// 🚀 11. 초기화 로직 (DOMContentLoaded 시점)
// - 페이지 로드 완료 후 상태 초기화 + 이벤트 바인딩 수행
/*
DOMContentLoaded 시:
- page 상태 설정
- 페이징 정보 설정
- 이벤트들 바인딩
- 수정 버튼 위임
*/
document.addEventListener("DOMContentLoaded", function() {
    // 페이지 정보 초기화
	// 📌 현재 페이지 번호 초기화 (hidden input으로 전달된 값 기준)
    const pageInput = utils.$('#currentPage');
    if (pageInput?.value) {
        state.currentPageNum = parseInt(pageInput.value); // 예: 1, 2, ...
    }
    
    // 초기 페이징 설정
	// 📌 초기 페이징 관련 정보 셋업 (페이지 수, 다음/이전 여부 등)
    const pageData = {
        currentPage: parseInt(utils.$('#currentPage')?.value) || 1,
        totalPages: parseInt(utils.$('#totalPages')?.value) || 0,
        hasNext: utils.$('#hasNext')?.value === 'true',
        hasPrevious: utils.$('#hasPrevious')?.value === 'true',
        totalElements: parseInt(utils.$('#totalElements')?.value) || 0
    };
    paginationManager.update(pageData); // 페이징 버튼 렌더링 + 총개수 표시

    // 이벤트 바인딩
	// 📌 주요 이벤트 핸들러 바인딩 (selector, eventType, handler 함수)
    utils.bindEvents([  // [selector, event, handler] 배열 바인딩 헬퍼
		
		// 창고 등록/수정 form 제출 시
        [selectors.form, 'submit', formManager.submit.bind(formManager)],
        
		// 창고코드 중복 확인 버튼 클릭 시
		['#checkDuplicateBtn', 'click', codeManager.checkDuplicate],
        
		// 창고코드 입력값 변경 시 → 중복확인 상태 초기화
		[selectors.inputs.warehouseCode, 'input', codeManager.resetVerification],
        
		// 창고 구분 선택 시 → 신규 등록일 경우 자동 코드 생성
		['#warehouseType', 'change', (e) => {
            const form = utils.$(selectors.form);
            if (e.target.value && !form.getAttribute('data-editing-id')) {
                codeManager.generateCode(e.target.value);
            }
        }],
		// 등록 버튼 누르면 모달 열기 (신규 등록 모드)
        ['button[data-bs-target="#warehouseRegisterModal"]', 'click', () => modalManager.show('new')],
		// 검색 버튼 클릭 시
		['#searchBtn', 'click', () => {
            const keyword = utils.$(selectors.inputs.searchKeyword).value;
            const searchType = utils.$(selectors.inputs.searchType).value;
            dataManager.searchData(keyword, searchType);
        }],
		
		// 전체 버튼 클릭 시 검색조건 초기화
        ['#showAllBtn', 'click', () => {
            utils.$(selectors.inputs.searchKeyword).value = '';
            utils.$(selectors.inputs.useYnFilter).value = 'ALL';
            dataManager.filterByUseYn('ALL');
        }],
		
		// 사용여부 필터 변경 시
        [selectors.inputs.useYnFilter, 'change', (e) => dataManager.filterByUseYn(e.target.value)],
		// 검색어 입력 후 엔터 누르면 검색 실행
		[selectors.inputs.searchKeyword, 'keypress', (e) => {
            if (e.key === 'Enter') {
                const keyword = e.target.value;
                const searchType = utils.$(selectors.inputs.searchType).value;
                dataManager.searchData(keyword, searchType);
            }
        }],
		
		// 모달이 닫힐 때 폼 리셋
        [selectors.modal, 'hidden.bs.modal', modalManager.resetForm]
    ]);

    // 수정 버튼 클릭 (이벤트 위임)
	// 수정 버튼 클릭 시 이벤트 위임 (동적으로 추가되는 행을 위해 document에 위임)
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('edit-btn')) {
            const whsData = JSON.parse(e.target.getAttribute('data-whs')) // 버튼의 data-whs 속성(JSON);
            modalManager.show('edit', whsData); // 수정 모드로 모달 열기
        }
    });
});