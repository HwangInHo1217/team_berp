/**
 * 창고 관리 JavaScript - 페이징 단계 전 버전
 * - 창고 등록/수정/검색 기능
 * - 사용여부 필터링 기능
 * - 실시간 코드 중복 검사
 * - 체크박스 전체선택/개별선택
 * - 선택 삭제 기능
 */

// ========== 전역 변수 ==========
let isWhsCodeVerified = false;  // 창고 코드 중복 확인 완료 여부
let originalWhsCode = '';       // 수정 모드에서 원본 창고 코드

// ========== 초기화 및 이벤트 리스너 등록 ==========
document.addEventListener("DOMContentLoaded", function () {
    initializeEventListeners();
    
    // 페이지 첫 로드 시 기본 데이터 표시
    setTimeout(function() {
        const initialFilter = document.getElementById('useYnFilter').value;
        loadWhsListByFilter(initialFilter);
    }, 300);
});

/**
 * 모든 이벤트 리스너 등록
 */
function initializeEventListeners() {
    // 폼 제출 이벤트
    const whsForm = document.querySelector("#warehouseForm");
    if (whsForm) {
        whsForm.addEventListener("submit", submitWhsForm);
    }

    // 중복확인 버튼
    const checkDuplicateBtn = document.getElementById('checkDuplicateBtn');
    if (checkDuplicateBtn) {
        checkDuplicateBtn.addEventListener('click', checkWhsCodeDuplicate);
    }

    // 창고 코드 입력 시 중복 확인 상태 초기화
    const whsCodeInput = document.getElementById('warehouseCode');
    if (whsCodeInput) {
        whsCodeInput.addEventListener('input', function() {
            resetCodeVerificationStatus();
        });
    }

    // 창고 유형 선택 시 자동 코드 생성
    const whsTypeSelect = document.getElementById('warehouseType');
    if (whsTypeSelect) {
        whsTypeSelect.addEventListener('change', function() {
            const selectedType = this.value;
            const form = document.getElementById('warehouseForm');
            const isEditMode = form.getAttribute('data-editing-id');
            
            if (selectedType && !isEditMode) {
                generateAndSetWhsCode(selectedType);
            }
        });
    }

    // 수정 버튼 클릭 (이벤트 위임 방식)
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('edit-btn')) {
            const whsData = extractWhsDataFromButton(e.target);
            showEditWhsModal(whsData);
        }
    });

    // 신규등록 버튼
    const newRegistrationButton = document.querySelector('button[data-bs-target="#warehouseRegisterModal"]');
    if (newRegistrationButton) {
        newRegistrationButton.addEventListener('click', showNewWhsModal);
    }

    // 검색 버튼
    const searchBtn = document.getElementById("searchBtn");
    if (searchBtn) {
        searchBtn.addEventListener("click", performSearch);
    }

    // 전체 버튼
    const showAllBtn = document.getElementById("showAllBtn");
    if (showAllBtn) {
        showAllBtn.addEventListener("click", showAllData);
    }

    // 사용여부 필터
    const useYnFilter = document.getElementById("useYnFilter");
    if (useYnFilter) {
        useYnFilter.addEventListener("change", filterByUseYn);
    }

    // 검색어 입력창에서 엔터키 처리
    const searchKeywordInput = document.getElementById("searchKeyword");
    if (searchKeywordInput) {
        searchKeywordInput.addEventListener("keypress", function(e) {
            if (e.key === "Enter") {
                performSearch();
            }
        });
    }

    // 모달 닫힐 때 폼 초기화
    const modal = document.getElementById('warehouseRegisterModal');
    if (modal) {
        modal.addEventListener('hidden.bs.modal', function () {
            resetWhsModalForm();
        });
    }
}

// ========== 체크박스 관련 함수 ==========

/**
 * 전체선택 함수
 * @param {boolean} ck - true: 전체선택, false: 전체해제
 */
function check_all(ck) {
    var ea = document.getElementsByName("ckbox");
    var w = 0;
    while(w < ea.length) {
        ea[w].checked = ck;
        w++;
    }
}

/**
 * 하나라도 체크 해제시 전체선택 체크 해제
 */
function checkdata() {
    var totalCheckboxes = document.getElementsByName("ckbox");
    var checkedCount = 0;
    var w = 0;
    
    // 체크된 개수 계산
    while(w < totalCheckboxes.length) {
        if(totalCheckboxes[w].checked == true) {
            checkedCount++;
        }
        w++;
    }
    
    // 전체선택 체크박스 상태 업데이트
    var selectAllCheckbox = document.querySelector('input[type="checkbox"][onclick*="check_all"]');
    if(selectAllCheckbox) {
        if(checkedCount === 0) {
            selectAllCheckbox.checked = false;
        } else if(checkedCount === totalCheckboxes.length) {
            selectAllCheckbox.checked = true;
        } else {
            selectAllCheckbox.checked = false;
        }
    }
}

/**
 * 선택삭제 버튼 클릭시 처리
 */
function check_del() {
    var ar = new Array(); // script 배열
    var ob = document.getElementsByName("ckbox");
    var w = 0;
    
    while(w < ob.length) {
        if(ob[w].checked == true) {
            ar.push(ob[w].value);
        }
        w++;
    }
    
    if(ar.length === 0) {
        alert('삭제할 항목을 선택해주세요.');
        return;
    }
    
    // 배열이 자동으로 문자열로 변해서 들어감 value="9,8,7,6,5"
    document.getElementById('ckdel').value = ar;
    
    if(confirm('선택한 ' + ar.length + '개의 창고를 삭제하시겠습니까?\n삭제된 데이터는 복구되지 않습니다.')) {
        // AJAX로 삭제 처리
        deleteSelectedWarehouses(ar);
    }
}

/**
 * 선택된 창고들 삭제 처리 (AJAX)
 */
function deleteSelectedWarehouses(idArray) {
    var deletePromises = [];
    
    for(var i = 0; i < idArray.length; i++) {
        var deletePromise = fetch('/api/warehouses/' + idArray[i], {
            method: 'DELETE'
        }).then(function(response) {
            return response.ok;
        }).catch(function(error) {
            console.error('삭제 오류:', error);
            return false;
        });
        
        deletePromises.push(deletePromise);
    }
    
    Promise.all(deletePromises).then(function(results) {
        var successCount = 0;
        var failCount = 0;
        
        for(var i = 0; i < results.length; i++) {
            if(results[i]) {
                successCount++;
            } else {
                failCount++;
            }
        }
        
        if(failCount === 0) {
            alert(successCount + '개 창고가 성공적으로 삭제되었습니다.');
        } else {
            alert(successCount + '개 삭제 성공, ' + failCount + '개 삭제 실패');
        }
        
        // 목록 새로고침
        var currentFilter = document.getElementById('useYnFilter').value;
        loadWhsListByFilter(currentFilter);
        
        // 전체선택 체크박스 해제
        var selectAllCheckbox = document.querySelector('input[type="checkbox"][onclick*="check_all"]');
        if(selectAllCheckbox) {
            selectAllCheckbox.checked = false;
        }
    });
}

/**
 * 총 개수 업데이트
 */
function updateTotalCount(count) {
    var totalCountEl = document.getElementById('totalCount');
    if(totalCountEl) {
        totalCountEl.textContent = '총 ' + count + '개의 창고가 등록되어 있습니다.';
    }
}

// ========== 검색 및 필터링 기능 ==========

/**
 * 키워드 검색 수행
 */
function performSearch() {
    const searchTypeElement = document.getElementById('searchType');
    const searchKeywordElement = document.getElementById('searchKeyword');
    
    if (!searchTypeElement || !searchKeywordElement) {
        alert("검색 요소를 찾을 수 없습니다.");
        return;
    }
    
    const searchType = searchTypeElement.value;
    const searchKeyword = searchKeywordElement.value;
    
    if (!searchKeyword || searchKeyword.trim() === '') {
        alert('검색어를 입력해주세요.');
        searchKeywordElement.focus();
        return;
    }
    
    loadWhsListWithSearch(searchKeyword.trim(), searchType);
}

/**
 * 전체 데이터 표시
 */
function showAllData() {
    document.getElementById('searchKeyword').value = '';
    
    const filterSelect = document.getElementById('useYnFilter');
    filterSelect.value = 'ALL';
    
    loadWhsListByFilter('ALL');
}

/**
 * 사용여부 필터 변경 처리
 */
function filterByUseYn() {
    const useYnFilter = document.getElementById('useYnFilter').value;
    document.getElementById('searchKeyword').value = '';
    loadWhsListByFilter(useYnFilter);
}

/**
 * 사용여부 필터에 따른 창고 목록 로드
 */
function loadWhsListByFilter(useYnFilter) {
    let apiUrl = '/api/warehouses';
    const params = new URLSearchParams();
    
    if (useYnFilter !== null && useYnFilter !== undefined) {
        params.append('useYnFilter', useYnFilter);
    }
    
    if (params.toString()) {
        apiUrl += '?' + params.toString();
    }

    fetchWhsData(apiUrl);
}

/**
 * 키워드 검색으로 창고 목록 로드
 */
function loadWhsListWithSearch(searchKeyword, searchType) {
    let apiUrl = '/api/warehouses';
    const params = new URLSearchParams();

    if (searchKeyword && searchKeyword.trim() !== '') {
        params.append('keyword', searchKeyword);
        params.append('searchType', searchType);
    }
    
    if (params.toString()) {
        apiUrl += '?' + params.toString();
    }

    fetchWhsData(apiUrl);
}

/**
 * API에서 창고 데이터 가져오기
 */
function fetchWhsData(apiUrl) {
    fetch(apiUrl)
        .then(function(response) {
            if (!response.ok) {
                throw new Error('네트워크 오류: ' + response.status);
            }
            if (response.status === 204) {
                return [];
            }
            return response.json();
        })
        .then(function(whsList) {
            updateWhsTable(whsList);
        })
        .catch(function(error) {
            console.error('창고 목록 로드 오류:', error);
            alert('창고 목록을 불러오는 중 오류가 발생했습니다: ' + error.message);
        });
}

/**
 * 창고 목록 테이블 업데이트 (체크박스 포함)
 */
function updateWhsTable(whsList) {
    var tbody = document.querySelector('table tbody');
    if (!tbody) {
        console.error("테이블 tbody를 찾을 수 없습니다");
        return;
    }
    
    tbody.innerHTML = '';

    if (!whsList || whsList.length === 0) {
        tbody.innerHTML = 
            '<tr>' +
                '<td colspan="8" style="text-align: center; padding: 40px;">' +
                    '검색 결과가 없습니다.' +
                '</td>' +
            '</tr>';
        updateTotalCount(0);
        return;
    }

    for(var i = 0; i < whsList.length; i++) {
        var wh = whsList[i];
        var row = document.createElement('tr');
        row.innerHTML = 
            '<td>' +
                '<label class="d-flex justify-content-center align-items-center" style="cursor: pointer; min-height: 40px; margin: 0; width: 100%;">' +
                '<input type="checkbox" name="ckbox" value="' + (wh.warehouseId || '') + '" onchange="checkdata()" style="margin: 0;">' +
                '</label>' +
            '</td>' +
            '<td>' + (i + 1) + '</td>' +
            '<td>' + (wh.warehouseCode || '') + '</td>' +
            '<td>' + (wh.warehouseName || '') + '</td>' +
            '<td>' + (wh.warehouseType || '') + '</td>' +
            '<td>' + (wh.description || '') + '</td>' +
            '<td>' + (wh.useYn || '') + '</td>' +
            '<td>' +
                '<button type="button" class="btn btn-sm btn-warning edit-btn"' +
                    ' data-id="' + (wh.warehouseId || '') + '"' +
                    ' data-code="' + (wh.warehouseCode || '') + '"' +
                    ' data-name="' + (wh.warehouseName || '') + '"' + 
                    ' data-type="' + (wh.warehouseType || '') + '"' +
                    ' data-description="' + (wh.description || '') + '"' + 
                    ' data-useyn="' + (wh.useYn || '') + '">' +
                    '수정' +
                '</button>' +
            '</td>';
        tbody.appendChild(row);
    }
    
    // 총 개수 업데이트
    updateTotalCount(whsList.length);
    
    // 전체선택 체크박스 해제
    var selectAllCheckbox = document.querySelector('input[type="checkbox"][onclick*="check_all"]');
    if(selectAllCheckbox) {
        selectAllCheckbox.checked = false;
    }
}

// ========== 창고 코드 관리 ==========

/**
 * 창고 유형에 따른 자동 코드 생성 및 설정
 */
function generateAndSetWhsCode(whsType) {
    fetch(`/api/warehouses/generate-code?type=${whsType}`)
        .then(function(response) {
            return response.text();
        })
        .then(function(code) {
            if (code && code !== 'INVALID_TYPE') {
                document.getElementById('warehouseCode').value = code;
                document.getElementById('isCodeChecked').value = 'true';
                showCodeCheckResult('자동 생성된 코드입니다.', 'success');
                isWhsCodeVerified = true;
            }
        })
        .catch(function(error) {
            console.error('코드 생성 오류:', error);
            showCodeCheckResult('코드 생성 중 오류가 발생했습니다.', 'danger');
        });
}

/**
 * 창고 코드 중복 확인
 */
function checkWhsCodeDuplicate() {
    const whsCodeInput = document.getElementById('warehouseCode');
    const whsCode = whsCodeInput.value.trim();
    const isCodeCheckedInput = document.getElementById('isCodeChecked');
    const editingId = document.getElementById('warehouseForm').getAttribute('data-editing-id');

    if (!whsCode) {
        showCodeCheckResult('창고 코드를 입력해주세요.', 'danger');
        isCodeCheckedInput.value = 'false';
        return;
    }

    let checkUrl = `/api/warehouses/check-duplicate?warehouseCode=${encodeURIComponent(whsCode)}`;
    if (editingId) {
        checkUrl += `&excludeId=${editingId}`;
    }

    fetch(checkUrl)
        .then(function(response) {
            return response.json();
        })
        .then(function(data) {
            if (data.status === 'duplicate') {
                showCodeCheckResult(data.message || '이미 사용 중인 창고 코드입니다.', 'danger');
                isCodeCheckedInput.value = 'false';
                isWhsCodeVerified = false;
            } else if (data.status === 'ok') {
                showCodeCheckResult(data.message || '사용 가능한 창고 코드입니다.', 'success');
                isCodeCheckedInput.value = 'true';
                isWhsCodeVerified = true;
            }
        })
        .catch(function(error) {
            console.error('중복 확인 오류:', error);
            showCodeCheckResult('중복 확인 중 오류가 발생했습니다.', 'danger');
            isCodeCheckedInput.value = 'false';
            isWhsCodeVerified = false;
        });
}

/**
 * 코드 확인 상태 초기화
 */
function resetCodeVerificationStatus() {
    document.getElementById('isCodeChecked').value = 'false';
    document.getElementById('codeCheckResult').textContent = '';
    isWhsCodeVerified = false;
}

/**
 * 코드 확인 결과 메시지 표시
 */
function showCodeCheckResult(message, type) {
    const resultEl = document.getElementById('codeCheckResult');
    resultEl.textContent = message;
    resultEl.className = `form-text mt-1 text-${type}`;
}

// ========== 모달 관리 ==========

/**
 * 신규 창고 등록 모달 표시
 */
function showNewWhsModal() {
    const form = document.getElementById('warehouseForm');
    form.reset();
    form.removeAttribute('data-editing-id');
    
    document.getElementById('warehouseRegisterModalLabel').textContent = '신규 창고 등록';
    form.querySelector('button[type="submit"]').textContent = '등록';
    
    document.getElementById('warehouseCode').readOnly = false;
    document.getElementById('checkDuplicateBtn').style.display = 'inline-block';
    showCodeCheckResult('창고 유형을 선택하면 코드가 자동 생성됩니다.', 'info');
    document.getElementById('isCodeChecked').value = 'false';
    
    // 신규 등록 시 미사용 옵션 비활성화
    const useYnSelect = document.getElementById('useYn');
    useYnSelect.value = 'Y';
    useYnSelect.querySelector('option[value="N"]').disabled = true;
    
    isWhsCodeVerified = false;
    originalWhsCode = '';
}

/**
 * 창고 수정 모달 표시
 */
function showEditWhsModal(whs) {
    const form = document.getElementById('warehouseForm');
    form.reset();
    form.setAttribute('data-editing-id', whs.warehouseId);
    
    document.getElementById('warehouseRegisterModalLabel').textContent = '창고 정보 수정';
    form.querySelector('button[type="submit"]').textContent = '수정';
    
    // 폼에 기존 데이터 채우기
    populateFormWithWhsData(whs);
    
    document.getElementById('warehouseCode').readOnly = false;
    document.getElementById('checkDuplicateBtn').style.display = 'inline-block';
    showCodeCheckResult('창고 코드 변경 시 중복확인을 해주세요.', 'warning');
    document.getElementById('isCodeChecked').value = 'true';
    
    // 수정 시에는 모든 옵션 활성화
    const useYnSelect = document.getElementById('useYn');
    useYnSelect.querySelector('option[value="N"]').disabled = false;
    
    isWhsCodeVerified = true;
    originalWhsCode = whs.warehouseCode;
    
    // 모달 표시
    const modalEl = document.getElementById('warehouseRegisterModal');
    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    modal.show();
}

/**
 * 폼에 창고 데이터 채우기
 */
function populateFormWithWhsData(whs) {
    document.getElementById('warehouseId').value = whs.warehouseId;
    document.getElementById('warehouseCode').value = whs.warehouseCode;
    document.getElementById('warehouseName').value = whs.warehouseName;
    document.getElementById('warehouseType').value = whs.warehouseType;
    document.getElementById('description').value = whs.description || '';
    document.getElementById('useYn').value = whs.useYn;
}

/**
 * 모달 폼 초기화
 */
function resetWhsModalForm() {
    const form = document.getElementById('warehouseForm');
    form.reset();
    form.removeAttribute('data-editing-id');
    
    document.getElementById('codeCheckResult').textContent = '';
    document.getElementById('isCodeChecked').value = 'false';
    
    // 사용여부 옵션 상태 초기화
    const useYnSelect = document.getElementById('useYn');
    useYnSelect.querySelector('option[value="N"]').disabled = false;
    
    isWhsCodeVerified = false;
    originalWhsCode = '';
}

// ========== 폼 제출 처리 ==========

/**
 * 창고 등록/수정 폼 제출 처리
 */
function submitWhsForm(event) {
    event.preventDefault();
    
    const form = event.target;
    const editingId = form.getAttribute('data-editing-id');
    
    // 폼 데이터 수집
    const formData = collectFormData();
    
    // 유효성 검사
    if (!validateFormData(formData, editingId)) {
        return;
    }

    // API 호출 설정
    const apiConfig = getApiConfig(editingId);
    
    // 서버로 데이터 전송
    submitToServer(apiConfig, formData);
}

/**
 * 폼 데이터 수집
 */
function collectFormData() {
    return {
        warehouseCode: document.getElementById('warehouseCode').value.trim(),
        warehouseName: document.getElementById('warehouseName').value.trim(),
        warehouseType: document.getElementById('warehouseType').value,
        useYn: document.getElementById('useYn').value,
        description: document.getElementById('description').value.trim()
    };
}

/**
 * 폼 데이터 유효성 검사
 */
function validateFormData(formData, editingId) {
    // 필수 필드 검사
    if (!formData.warehouseCode) {
        alert('창고 코드를 입력해주세요.');
        document.getElementById('warehouseCode').focus();
        return false;
    }
    
    if (!formData.warehouseName) {
        alert('창고명을 입력해주세요.');
        document.getElementById('warehouseName').focus();
        return false;
    }
    
    if (!formData.warehouseType) {
        alert('창고 유형을 선택해주세요.');
        document.getElementById('warehouseType').focus();
        return false;
    }

    // 중복 확인 검사
    const isCodeChecked = document.getElementById('isCodeChecked').value === 'true';
    
    if (editingId) {
        // 수정 모드: 코드가 변경된 경우에만 중복 확인 필요
        if (formData.warehouseCode !== originalWhsCode && !isWhsCodeVerified) {
            alert('변경된 창고 코드에 대한 중복 확인을 해주세요.');
            return false;
        }
    } else {
        // 등록 모드: 항상 중복 확인 필요
        if (!isCodeChecked) {
            alert('창고 코드 중복 확인을 해주세요.');
            return false;
        }
    }

    return true;
}

/**
 * API 호출 설정 정보 생성
 */
function getApiConfig(editingId) {
    if (editingId) {
        return {
            method: 'PUT',
            url: `/api/warehouses/${editingId}`,
            modeText: '수정',
            successStatus: 200
        };
    } else {
        return {
            method: 'POST',
            url: '/api/warehouses',
            modeText: '등록',
            successStatus: 201
        };
    }
}

/**
 * 서버로 데이터 전송
 */
function submitToServer(apiConfig, formData) {
    fetch(apiConfig.url, {
        method: apiConfig.method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData)
    })
    .then(function(response) {
        if (response.status === apiConfig.successStatus) {
            return response.json();
        }
        return response.json().then(function(errorData) {
            throw new Error(errorData.message || `${apiConfig.modeText} 실패`);
        });
    })
    .then(function(data) {
        alert(`창고가 성공적으로 ${apiConfig.modeText}되었습니다: ${data.warehouseName}`);
        
        // 모달 닫기
        const modalEl = document.getElementById('warehouseRegisterModal');
        const modalInst = bootstrap.Modal.getInstance(modalEl);
        modalInst.hide();
        
        // 현재 필터 상태에 맞춰 목록 새로고침
        const currentFilter = document.getElementById('useYnFilter').value;
        loadWhsListByFilter(currentFilter);
    })
    .catch(function(error) {
        console.error(`창고 ${apiConfig.modeText} 오류:`, error);
        alert('오류: ' + error.message);
    });
}

// ========== 유틸리티 함수 ==========

/**
 * 수정 버튼에서 창고 데이터 추출
 */
function extractWhsDataFromButton(button) {
    return {
        warehouseId: button.getAttribute('data-id'),
        warehouseCode: button.getAttribute('data-code'),
        warehouseName: button.getAttribute('data-name'),
        warehouseType: button.getAttribute('data-type'),
        description: button.getAttribute('data-description'),
        useYn: button.getAttribute('data-useyn')
    };
}
