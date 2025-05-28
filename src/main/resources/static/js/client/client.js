document.addEventListener('DOMContentLoaded', () => {
  let currentType = 'CUSTOMER'; // ''(전체), CUSTOMER, SUPPLIER, BOTH
  let currentPage = 0;
  let currentKeyword = '';
  let currentSearchType = 'name';
  let currentSortType = '';
  let isEdit = false;
  let editCompanyId = null;
  let employeeList = []; // 담당자 캐싱

  // 리스트 불러오기 (검색/정렬/페이징/논리삭제 적용)
  function loadClients(type = currentType, page = 0, keyword = '', searchType = 'name', sortType = '') {
    let url = `/api/clients?page=${page}&size=5&searchType=${searchType}`;
    if (type && type !== '') url += `&type=${type}`;
    if (keyword && keyword.trim() !== '') url += `&keyword=${encodeURIComponent(keyword)}`;
    if (sortType && sortType !== '') url += `&sort=${sortType}`;
    fetch(url)
      .then(res => {
        if (!res.ok) throw new Error('[loadClients] API 오류 status: ' + res.status);
        return res.json();
      })
      .then(data => {
        renderTable(data.content);
        renderPagination(data.totalPages, data.number);
      })
      .catch(err => {
        console.error('[loadClients] 에러:', err);
        renderTable([]);
      });
  }

  // 테이블 렌더링
  function renderTable(list) {
    const tbody = document.getElementById('client-table');
    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="10">데이터가 없습니다.</td></tr>`;
      return;
    }
    tbody.innerHTML = list.map(row => `
      <tr>
        <td><input type="checkbox" class="row-check" value="${row.companyId}" /></td>
        <td>${row.companyName}</td>
        <td>${row.presidentNm || '-'}</td>
        <td>${row.companyNo || '-'}</td>
        <td>${row.companyTel || '-'}</td>
        <td>${row.employeeName || '-'}</td>
        <td>
          ${
            row.useYn === 'Y'
              ? `<span class="badge bg-success">사용</span>
                 <button class="btn btn-sm btn-outline-secondary status-btn ms-1" data-id="${row.companyId}" data-next="N">정지</button>`
              : `<button class="btn btn-sm btn-outline-success status-btn" data-id="${row.companyId}" data-next="Y">복구</button>
                 <span class="badge bg-secondary ms-1">정지</span>`
          }
        </td>
        <td>
          <button class="btn btn-sm btn-info detail-btn" data-id="${row.companyId}">상세</button>
        </td>
        <td>
          <button class="btn btn-sm btn-warning edit-btn" data-row='${encodeURIComponent(JSON.stringify(row))}'>수정</button>
        </td>
        <td>
          <button class="btn btn-sm btn-danger" onclick="deleteClient(${row.companyId})">삭제</button>
        </td>
      </tr>
    `).join('');
    // 상태 변경 바인딩
    document.querySelectorAll('.status-btn').forEach(btn => {
      btn.onclick = function() {
        const companyId = this.getAttribute('data-id');
        const nextYn = this.getAttribute('data-next');
        const msg = nextYn === 'N' ? '정지하시겠습니까?' : '복구(사용)로 전환하시겠습니까?';
        if (!confirm(msg)) return;
        fetch(`/api/clients/${companyId}/status?useYn=${nextYn}`, { method: 'PATCH' })
          .then(res => {
            if (res.ok) {
              alert(nextYn === 'N' ? '정지 처리 완료' : '복구(사용) 처리 완료');
              loadClients(currentType, currentPage, currentKeyword, currentSearchType, currentSortType);
            } else {
              alert('상태 변경 실패');
            }
          });
      }
    });
    // 상세/수정 바인딩
    document.querySelectorAll('.detail-btn').forEach(btn => {
      btn.onclick = function() {
        const companyId = this.getAttribute('data-id');
        fetch(`/api/clients/${companyId}`)
          .then(res => res.json())
          .then(data => {
            let html = `
              <ul class="list-group list-group-flush">
                <li class="list-group-item"><b>회사명:</b> ${data.companyName}</li>
                <li class="list-group-item"><b>대표자명:</b> ${data.presidentNm || '-'}</li>
                <li class="list-group-item"><b>사업자번호:</b> ${data.companyNo || '-'}</li>
                <li class="list-group-item"><b>전화번호:</b> ${data.companyTel || '-'}</li>
                <li class="list-group-item"><b>담당자:</b> ${data.employeeName || '-'}</li>
                <li class="list-group-item"><b>팩스:</b> ${data.companyFax || '-'}</li>
                <li class="list-group-item"><b>주소:</b> ${data.companyAddr || '-'}</li>
                <li class="list-group-item"><b>업태:</b> ${data.companyCond || '-'}</li>
                <li class="list-group-item"><b>종목:</b> ${data.companyItem || '-'}</li>
                <li class="list-group-item"><b>유형:</b> ${data.companyType || '-'}</li>
                <li class="list-group-item"><b>등록여부:</b> ${data.useYn == 'Y' ? '사용' : '정지'}</li>
              </ul>
            `;
            document.getElementById('clientDetailBody').innerHTML = html;
            new bootstrap.Modal(document.getElementById("clientDetailModal")).show();
          });
      }
    });
    document.querySelectorAll('.edit-btn').forEach(btn => {
      btn.onclick = function() {
        const rowStr = decodeURIComponent(this.getAttribute('data-row'));
        openEditModal(rowStr);
      }
    });
  }
  
  // 주소 입력칸 클릭시 다음 주소 검색 API 연동
  document.getElementById('mainAddress').addEventListener('click', function() {
      new daum.Postcode({
          oncomplete: function(data) {
              // 도로명, 지번 등 주소 결과
              var addr = data.roadAddress ? data.roadAddress : data.jibunAddress;
              document.getElementById('postcode').value = data.zonecode;
              document.getElementById('mainAddress').value = addr;
              document.getElementById('detailAddress').focus();
          }
      }).open();
  });

  // 페이징
  function renderPagination(totalPages, currentPageNum) {
    const pagination = document.querySelector('.pagination');
    if (!pagination) return;
    let html = '';
    for (let i = 0; i < totalPages; i++) {
      html += `<li class="page-item ${i === currentPageNum ? 'active' : ''}">
        <a class="page-link" href="#" data-page="${i}">${i+1}</a>
      </li>`;
    }
    pagination.innerHTML = html;
    document.querySelectorAll('.pagination .page-link').forEach(a => {
      a.onclick = function(e) {
        e.preventDefault();
        currentPage = parseInt(this.dataset.page);
        loadClients(currentType, currentPage, currentKeyword, currentSearchType, currentSortType);
      }
    });
  }

  // 검색/정렬/고급조건 동기화
  window.searchClients = function() {
    currentSearchType = document.getElementById('searchType').value;
    currentKeyword = document.getElementById('searchKeyword').value;
    currentSortType = document.getElementById('sortType').value;
    currentPage = 0;
    loadClients(currentType, 0, currentKeyword, currentSearchType, currentSortType);
  };

  // 전체 선택/해제
  document.getElementById('all-check').addEventListener('change', function() {
    const checked = this.checked;
    document.querySelectorAll('.row-check').forEach(chk => chk.checked = checked);
  });

  // 일괄 삭제(논리삭제)
  window.deleteChecked = function() {
    const checkedIds = Array.from(document.querySelectorAll('.row-check:checked')).map(chk => chk.value);
    if (checkedIds.length === 0) return alert('삭제할 데이터를 선택하세요.');
    if (!confirm('정말 삭제하시겠습니까?')) return;
    Promise.all(checkedIds.map(id =>
      fetch(`/api/clients/${id}`, { method: 'DELETE' })
    )).then(() => loadClients(currentType, currentPage, currentKeyword, currentSearchType, currentSortType));
  };

  // 개별 삭제(논리삭제)
  window.deleteClient = function(companyId) {
    if (!confirm("정말 삭제하시겠습니까?")) return;
    fetch(`/api/clients/${companyId}`, { method: 'DELETE' })
      .then(res => {
        if (res.ok) loadClients(currentType, currentPage, currentKeyword, currentSearchType, currentSortType);
        else alert('삭제 실패!');
      });
  };

  // 탭/유형/정렬 변화 시
  document.getElementById('filterType').addEventListener('change', function() {
    currentType = this.value;
    currentPage = 0;
    loadClients(currentType, 0, currentKeyword, currentSearchType, currentSortType);
  });
  document.getElementById('sortType').addEventListener('change', function() {
    currentSortType = this.value;
    loadClients(currentType, currentPage, currentKeyword, currentSearchType, currentSortType);
  });
  document.getElementById('tab-client').addEventListener('click', () => {
    currentType = 'CUSTOMER';
    document.getElementById('filterType').value = 'CUSTOMER';
    currentPage = 0;
    loadClients(currentType, 0, '', currentSearchType, currentSortType);
    document.getElementById('tab-client').classList.add('active', 'btn-outline-primary');
    document.getElementById('tab-client').classList.remove('btn-outline-secondary');
    document.getElementById('tab-supplier').classList.remove('active', 'btn-outline-primary');
    document.getElementById('tab-supplier').classList.add('btn-outline-secondary');
  });
  document.getElementById('tab-supplier').addEventListener('click', () => {
    currentType = 'SUPPLIER';
    document.getElementById('filterType').value = 'SUPPLIER';
    currentPage = 0;
    loadClients(currentType, 0, '', currentSearchType, currentSortType);
    document.getElementById('tab-supplier').classList.add('active', 'btn-outline-primary');
    document.getElementById('tab-supplier').classList.remove('btn-outline-secondary');
    document.getElementById('tab-client').classList.remove('active', 'btn-outline-primary');
    document.getElementById('tab-client').classList.add('btn-outline-secondary');
  });

  // 직원목록 가져와서 담당자 드롭다운 채우기 (최초1회/모달open시)
  function loadEmployees(selectedId) {
    // 캐싱된 데이터 활용, 없으면 fetch
    if (employeeList.length > 0) {
      setEmployeeOptions(employeeList, selectedId);
      return;
    }
    fetch('/api/employee/list')
      .then(response => response.json())
      .then(data => {
        employeeList = data;
        setEmployeeOptions(employeeList, selectedId);
      });
  }
  function setEmployeeOptions(data, selectedId) {
    const select = document.getElementById('employeeId');
    select.innerHTML = '<option value="">담당자 선택</option>';
    data.forEach(emp => {
      let option = document.createElement('option');
      option.value = emp.employeeId;
      option.text = emp.empName;
      if (selectedId && selectedId == emp.employeeId) option.selected = true;
      select.appendChild(option);
    });
  }

  // 거래처 등록/수정 모달 열릴 때
  document.getElementById('clientRegisterModal').addEventListener('show.bs.modal', function () {
    // 등록이면 선택X, 수정이면 미리 선택
    if (isEdit && editCompanyId) {
      // 수정상태에서는 employeeId 세팅
      loadEmployees(document.getElementById('employeeId').value);
    } else {
      loadEmployees();
    }
    document.getElementById('duplicateWarning').classList.add('d-none');
  });

  // 등록/수정 모달 폼 submit (중복등록 방지)
  document.getElementById('clientRegisterForm').onsubmit = function(e) {
    e.preventDefault();
    const form = e.target;
    const data = {
      companyName: form.companyName.value.trim(),
      presidentNm: form.ceoName.value.trim(),
      companyNo: form.bizNum.value.trim(),
      companyTel: form.phone.value.trim(),
      companyFax: form.fax.value.trim(),
      companyAddr: form.mainAddress.value.trim(),
      companyCond: form.type.value.trim(),
      companyItem: form.item.value.trim(),
      companyType: document.querySelector('input[name="clientType"]:checked').value,
      useYn: "Y",
      employeeId: form.employeeId.value
    };
    // 중복검사(회사명/사업자번호)
    fetch(`/api/clients/duplicate?companyName=${encodeURIComponent(data.companyName)}&companyNo=${encodeURIComponent(data.companyNo)}${isEdit && editCompanyId ? '&excludeId='+editCompanyId : ''}`)
      .then(res => res.json())
      .then(dup => {
        if (dup.exists) {
          document.getElementById('duplicateWarning').classList.remove('d-none');
          return;
        }
        let method = 'POST', url = '/api/clients';
        if (isEdit && editCompanyId) {
          method = 'PUT';
          url = `/api/clients/${editCompanyId}`;
        }
        fetch(url, {
          method,
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(data)
        }).then(res => {
          if (res.ok) {
            alert(isEdit ? '수정 완료!' : '등록 완료!');
            bootstrap.Modal.getInstance(document.getElementById('clientRegisterModal')).hide();
            loadClients(currentType, currentPage, currentKeyword, currentSearchType, currentSortType);
            isEdit = false;
            editCompanyId = null;
            form.reset();
            document.getElementById('clientRegisterModalLabel').innerText = '거래처 등록';
            document.getElementById('modalSubmitBtn').innerText = '등록';
          } else {
            res.text().then(txt => {
              alert('실패했습니다: ' + txt);
            });
          }
        });
      });
  };

  // 수정 모달 열기
  window.openEditModal = function(rowStr) {
    const row = JSON.parse(decodeURIComponent(rowStr));
    isEdit = true;
    editCompanyId = row.companyId;
    document.getElementById('editCompanyId').value = row.companyId;
    document.getElementById('companyName').value = row.companyName || '';
    document.getElementById('ceoName').value = row.presidentNm || '';
    document.getElementById('bizNum').value = row.companyNo || '';
    document.getElementById('phone').value = row.companyTel || '';
    document.getElementById('fax').value = row.companyFax || '';
    document.getElementById('mainAddress').value = row.companyAddr || '';
    document.getElementById('type').value = row.companyCond || '';
    document.getElementById('item').value = row.companyItem || '';
    document.querySelector(`input[name="clientType"][value="${row.companyType}"]`).checked = true;
    // 담당자 select 미리 선택
    setTimeout(() => loadEmployees(row.employeeId), 80); // 모달 열릴 때 셀렉트 세팅
    document.getElementById('clientRegisterModalLabel').innerText = '거래처 수정';
    document.getElementById('modalSubmitBtn').innerText = '수정';
    document.getElementById('duplicateWarning').classList.add('d-none');
    const modal = new bootstrap.Modal(document.getElementById("clientRegisterModal"));
    modal.show();
  };

  // 모달 닫힐 때 초기화
  document.getElementById('clientRegisterModal').addEventListener('hidden.bs.modal', () => {
    document.getElementById('clientRegisterForm').reset();
    isEdit = false;
    editCompanyId = null;
    document.getElementById('clientRegisterModalLabel').innerText = '거래처 등록';
    document.getElementById('modalSubmitBtn').innerText = '등록';
    document.getElementById('duplicateWarning').classList.add('d-none');
  });

  // 최초 로드
  currentType = 'CUSTOMER';
  document.getElementById('filterType').value = 'CUSTOMER';
  loadClients(currentType, currentPage, '', currentSearchType, currentSortType);
});
