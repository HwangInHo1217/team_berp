document.addEventListener('DOMContentLoaded', () => {
  let currentType = 'CUSTOMER'; // ""는 전체, CUSTOMER/SUPPLIER/BOTH 도 가능
  let currentPage = 0;
  let currentKeyword = '';
  let currentSearchType = 'name';
  let isEdit = false;
  let editCompanyId = null;

  // 리스트 불러오기
  function loadClients(type = currentType, page = 0, keyword = '', searchType = 'name') {
	let url = `/api/clients?page=${page}&size=10&searchType=${searchType}`;
	if (type && type !== '') url += `&type=${type}`;
	if (keyword && keyword.trim() !== '') url += `&keyword=${encodeURIComponent(keyword)}`;
    fetch(url)
      .then(res => {
        if (!res.ok) {
          res.text().then(txt => console.error('[loadClients] fetch error body:', txt));
          throw new Error('[loadClients] API 오류 status: ' + res.status);
        }
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
      tbody.innerHTML = `<tr><td colspan="9">데이터가 없습니다.</td></tr>`;
      return;
    }
    tbody.innerHTML = list.map(row => `
      <tr>
        <td><input type="checkbox" class="row-check" value="${row.companyId}" /></td>
        <td>${row.companyName}</td>
        <td>${row.presidentNm || '-'}</td>
        <td>${row.companyTel || '-'}</td>
        <td>${row.companyFax || '-'}</td>
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
	
	// 필터 셀렉트 change 이벤트 등록
	document.getElementById('filterType').addEventListener('change', function() {
	  currentType = this.value; // ''(전체) / 'CUSTOMER' / 'SUPPLIER' / 'BOTH'
	  currentPage = 0;
	  loadClients(currentType, 0, currentKeyword, currentSearchType);
	});

    // 상태 버튼 이벤트 바인딩
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
              loadClients(currentType, currentPage, currentKeyword, currentSearchType);
            } else {
              alert('상태 변경 실패');
            }
          });
      }
    });

    // 상세 버튼 바인딩
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

    // 수정 버튼 바인딩
    document.querySelectorAll('.edit-btn').forEach(btn => {
      btn.onclick = function() {
        const rowStr = decodeURIComponent(this.getAttribute('data-row'));
        openEditModal(rowStr);
      }
    });
  }

  // 페이징 렌더링
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
        loadClients(currentType, currentPage, currentKeyword, currentSearchType);
      }
    });
  }

  // 거래처 유형 셀렉트 변경 이벤트 (전체/고객사/매입처/겸용)
  document.getElementById('filterType').addEventListener('change', function() {
    currentType = this.value;
    currentPage = 0;
    loadClients(currentType, 0, currentKeyword, currentSearchType);
  });

  // 탭 이벤트 (탭과 필터 동기화)
  document.getElementById('tab-client').addEventListener('click', () => {
    currentType = 'CUSTOMER';
    document.getElementById('filterType').value = 'CUSTOMER';
    currentPage = 0;
    loadClients(currentType, 0, '', currentSearchType);
    document.getElementById('tab-client').classList.add('active', 'btn-outline-primary');
    document.getElementById('tab-client').classList.remove('btn-outline-secondary');
    document.getElementById('tab-supplier').classList.remove('active', 'btn-outline-primary');
    document.getElementById('tab-supplier').classList.add('btn-outline-secondary');
  });
  document.getElementById('tab-supplier').addEventListener('click', () => {
    currentType = 'SUPPLIER';
    document.getElementById('filterType').value = 'SUPPLIER';
    currentPage = 0;
    loadClients(currentType, 0, '', currentSearchType);
    document.getElementById('tab-supplier').classList.add('active', 'btn-outline-primary');
    document.getElementById('tab-supplier').classList.remove('btn-outline-secondary');
    document.getElementById('tab-client').classList.remove('active', 'btn-outline-primary');
    document.getElementById('tab-client').classList.add('btn-outline-secondary');
  });

  // 검색
  window.searchClients = function() {
    currentSearchType = document.getElementById('searchType').value;
    currentKeyword = document.getElementById('searchKeyword').value;
    loadClients(currentType, 0, currentKeyword, currentSearchType);
  };

  // 전체 선택/해제
  document.getElementById('all-check').addEventListener('change', function() {
    const checked = this.checked;
    document.querySelectorAll('.row-check').forEach(chk => chk.checked = checked);
  });

  // 일괄 삭제
  window.deleteChecked = function() {
    const checkedIds = Array.from(document.querySelectorAll('.row-check:checked')).map(chk => chk.value);
    if (checkedIds.length === 0) return alert('삭제할 데이터를 선택하세요.');
    if (!confirm('정말 삭제하시겠습니까?')) return;
    Promise.all(checkedIds.map(id =>
      fetch(`/api/clients/${id}`, { method: 'DELETE' })
    )).then(() => loadClients(currentType, currentPage, currentKeyword, currentSearchType));
  };

  // 개별 삭제
  window.deleteClient = function(companyId) {
    if (!confirm("정말 삭제하시겠습니까?")) return;
    fetch(`/api/clients/${companyId}`, { method: 'DELETE' })
      .then(res => {
        if (res.ok) loadClients(currentType, currentPage, currentKeyword, currentSearchType);
        else alert('삭제 실패!');
      });
  };

  // 등록/수정 모달 열기
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
    document.getElementById('clientRegisterModalLabel').innerText = '거래처 수정';
    document.getElementById('modalSubmitBtn').innerText = '수정';
    const modal = new bootstrap.Modal(document.getElementById("clientRegisterModal"));
    modal.show();
  };

  // 등록/수정 모달 폼 submit
  document.getElementById('clientRegisterForm').onsubmit = function(e) {
    e.preventDefault();
    const form = e.target;
    const data = {
      companyName: form.companyName.value,
      presidentNm: form.ceoName.value,
      companyNo: form.bizNum.value,
      companyTel: form.phone.value,
      companyFax: form.fax.value,
      companyAddr: form.mainAddress.value,
      companyCond: form.type.value,
      companyItem: form.item.value,
      companyType: document.querySelector('input[name="clientType"]:checked').value,
      useYn: "Y"
    };
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
        loadClients(currentType, currentPage, currentKeyword, currentSearchType);
        // 초기화
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
  };

  // 모달 닫힐 때 초기화
  document.getElementById('clientRegisterModal').addEventListener('hidden.bs.modal', () => {
    document.getElementById('clientRegisterForm').reset();
    isEdit = false;
    editCompanyId = null;
    document.getElementById('clientRegisterModalLabel').innerText = '거래처 등록';
    document.getElementById('modalSubmitBtn').innerText = '등록';
  });

  // 최초 로드 (고객사만)
  currentType = 'CUSTOMER';
  document.getElementById('filterType').value = 'CUSTOMER';
  loadClients(currentType, currentPage, '', currentSearchType);
});
