document.addEventListener('DOMContentLoaded', () => {
  let currentType       = '';
  let currentPage       = 0;
  let currentKeyword    = '';
  let currentSearchType = 'name';
  let currentSortType   = '';
  let isEdit            = false;
  let editCompanyId     = null;
  let employeeList      = [];

  // ■ 페이징 제어용
  let lastTotalPages = 0;

  const registerModalEl = document.getElementById('clientRegisterModal');
  const detailModalEl   = document.getElementById('clientDetailModal');
  const searchInput     = document.getElementById('searchKeyword');
  const filterSelect    = document.getElementById('filterType');
  const sortSelect      = document.getElementById('sortType'); // 정렬 기준 select

  // ■ 하이픈 제거
  const stripHyphens = s => (s||'').replace(/-/g,'');

  // ■ 사업자번호 포맷터 (3-2-5)
  function formatBiz(v) {
    const s = (v||'').replace(/\D/g,'');
    if (s.length > 5) return `${s.slice(0,3)}-${s.slice(3,5)}-${s.slice(5)}`;
    if (s.length > 3) return `${s.slice(0,3)}-${s.slice(3)}`;
    return s;
  }

  // ■ 전화번호 포맷터 (02 2-3-4, 그 외 3-4-4)
  function formatPhone(v) {
    const s = (v||'').replace(/\D/g,'');
    if (s.startsWith('02')) {
      if (s.length > 6) return `${s.slice(0,2)}-${s.slice(2,5)}-${s.slice(5,9)}`;
      if (s.length > 2) return `${s.slice(0,2)}-${s.slice(2)}`;
      return s;
    }
    if (s.length > 10) return `${s.slice(0,3)}-${s.slice(3,7)}-${s.slice(7,11)}`;
    if (s.length > 7)  return `${s.slice(0,3)}-${s.slice(3,7)}-${s.slice(7)}`;
    if (s.length > 3)  return `${s.slice(0,3)}-${s.slice(3)}`;
    return s;
  }

  // 직원 로드
  function loadEmployees(selId='') {
    if (employeeList.length) return setEmployeeOptions(employeeList, selId);
    fetch('/api/employee/list')
      .then(r => r.json())
      .then(list => {
        employeeList = list;
        setEmployeeOptions(list, selId);
      });
  }
  function setEmployeeOptions(list, selId) {
    const sel = document.getElementById('employeeId');
    sel.innerHTML = '<option value="">거래처 담당자 선택</option>';
    list.forEach(e => {
      const o = document.createElement('option');
      o.value = e.employeeId;
      o.text  = e.empName;
      if (e.employeeId == selId) o.selected = true;
      sel.appendChild(o);
    });
  }

  // 리스트 로드 (페이징 단위를 10개로 변경)
  function loadClients(type = currentType, page = 0, kw = '', st = 'name', sort = '') {
    console.log('▶ loadClients', { type, page, kw, st, sort });
    // size=10으로 변경
    let url = `/api/clients?page=${page}&size=10&searchType=${st}`;
    if (type) url += `&type=${type}`;
    if (kw)   url += `&keyword=${encodeURIComponent(kw)}`;
    if (sort) url += `&sort=${sort}`;
    fetch(url)
      .then(r => r.ok ? r.json() : Promise.reject(r.status))
      .then(({ content, totalPages, number }) => {
        renderTable(content);
        renderPagination(totalPages, number);
      })
      .catch(e => {
        console.error('loadClients error:', e);
        renderTable([]);
      });
  }

  // 테이블 그리기
  function renderTable(list) {
    const tb = document.getElementById('client-table');
    if (!list.length) {
      tb.innerHTML = '<tr><td colspan="10" class="text-center">데이터가 없습니다.</td></tr>';
      return;
    }
    tb.innerHTML = list.map(r => {
      // r.useYn 값이 'N'(정지)인 경우에도 그대로 보여줌
      const d = encodeURIComponent(JSON.stringify(r));
      return `
        <tr data-id="${r.companyId}">
          <td><input type="checkbox" class="row-check" value="${r.companyId}"></td>
          <td>${r.companyName}</td>
          <td>${r.presidentNm||'-'}</td>
          <td>${formatBiz(r.companyNo)||'-'}</td>
          <td>${formatPhone(r.companyTel)||'-'}</td>
          <td>${r.employeeName||'-'}</td>
          <td>
            ${r.useYn==='Y'
              ? `<span class="badge bg-success">사용</span>
                 <button class="btn btn-sm btn-outline-secondary status-btn ms-1"
                         data-id="${r.companyId}" data-next="N">정지</button>`
              : `<button class="btn btn-sm btn-outline-success status-btn"
                         data-id="${r.companyId}" data-next="Y">복구</button>
                 <span class="badge bg-secondary ms-1">정지</span>`}
          </td>
          <td><button class="btn btn-sm btn-info detail-btn" data-row="${d}">상세</button></td>
          <td><button class="btn btn-sm btn-warning edit-btn"  data-row="${d}">수정</button></td>
          <td><button class="btn btn-sm btn-danger delete-btn" data-id="${r.companyId}">삭제</button></td>
        </tr>`;
    }).join('');
    bindRowButtons();
  }

  // 행 버튼 바인딩
  function bindRowButtons() {
    // 상태 토글
    document.querySelectorAll('.status-btn').forEach(b => {
      b.onclick = () => {
        const { id, next } = b.dataset;
        if (!confirm(next==='N'?'정지하시겠습니까?':'복구하시겠습니까?')) return;
        fetch(`/api/clients/${id}/status?useYn=${next}`, { method:'PATCH' })
          .then(r => r.ok && loadClients(currentType, currentPage, currentKeyword, currentSearchType, currentSortType));
      };
    });
    // 상세
    document.querySelectorAll('.detail-btn').forEach(b => {
      b.onclick = () => {
        const row = JSON.parse(decodeURIComponent(b.dataset.row));
        const html = `
          <ul class="list-group list-group-flush text-start">
            <li class="list-group-item"><b>회사명:</b> ${row.companyName}</li>
            <li class="list-group-item"><b>담당자명:</b> ${row.presidentNm}</li>
            <li class="list-group-item"><b>사업자번호:</b> ${formatBiz(row.companyNo)}</li>
            <li class="list-group-item"><b>전화번호:</b> ${formatPhone(row.companyTel)}</li>
            <li class="list-group-item"><b>거래처 담당자:</b> ${row.employeeName}</li>
            <li class="list-group-item"><b>팩스:</b> ${row.companyFax}</li>
            <li class="list-group-item"><b>우편번호:</b> ${row.postcode||'-'}</li>
            <li class="list-group-item"><b>주소:</b> ${row.companyAddr||'-'}</li>
            <li class="list-group-item"><b>상세주소:</b> ${row.detailAddress||'-'}</li>
            <li class="list-group-item"><b>업태:</b> ${row.companyCond}</li>
            <li class="list-group-item"><b>종목:</b> ${row.companyItem}</li>
            <li class="list-group-item"><b>유형:</b> ${row.companyType}</li>
            <li class="list-group-item"><b>등록여부:</b> ${row.useYn==='Y'?'사용':'정지'}</li>
          </ul>`;
        document.getElementById('clientDetailBody').innerHTML = html;
        new bootstrap.Modal(detailModalEl).show();
      };
    });
    // 수정
    document.querySelectorAll('.edit-btn').forEach(b => {
      b.onclick = () => openEditModal(JSON.parse(decodeURIComponent(b.dataset.row)));
    });
    // 개별 삭제
    document.querySelectorAll('.delete-btn').forEach(b => {
      b.onclick = () => {
        const id = b.dataset.id;
        if (!document.querySelector(`tr[data-id="${id}"] .row-check`).checked) {
          return alert('삭제하려면 먼저 체크하세요.');
        }
        if (!confirm('정말 삭제하시겠습니까?')) return;
        fetch(`/api/clients/${id}`, { method:'DELETE' })
          .then(r => r.ok && loadClients(currentType, currentPage, currentKeyword, currentSearchType, currentSortType));
      };
    });
  }

  // 수정 모달 열기
  function openEditModal(row) {
    isEdit = true;
    editCompanyId = row.companyId;
    const $ = id => document.getElementById(id);
    $('companyName'      ).value = row.companyName   || '';
    $('ceoName'          ).value = row.presidentNm   || '';
    $('bizNum'           ).value = formatBiz(row.companyNo);
    $('phone'            ).value = formatPhone(row.companyTel);
    $('postcode'         ).value = row.postcode      || '';
    $('companyAddr'      ).value = row.companyAddr   || '';
    $('detailAddress'    ).value = row.detailAddress || '';
    $('fax'              ).value = row.companyFax    || '';
    $('type'             ).value = row.companyCond   || '';
    $('item'             ).value = row.companyItem   || '';
    document.querySelector(`input[name="clientType"][value="${row.companyType}"]`).checked = true;
    loadEmployees(row.employeeId);
    $('clientRegisterModalLabel').innerText = '거래처 수정';
    $('modalSubmitBtn'           ).innerText = '수정';
    new bootstrap.Modal(registerModalEl).show();
  }

  // 페이징 렌더링
  function renderPagination(total, cur) {
    lastTotalPages = total;
    const pg = document.querySelector('.pagination');

    // 페이지 번호
    pg.innerHTML = Array.from({length: total}, (_, i) => `
      <li class="page-item ${i === cur ? 'active' : ''}">
        <a class="page-link" href="#" data-page="${i}">${i + 1}</a>
      </li>`).join('');

    // 번호 클릭 이벤트
    pg.querySelectorAll('.page-link').forEach(a => {
      a.onclick = e => {
        e.preventDefault();
        const p = +a.dataset.page;
        currentPage = p;
        loadClients(currentType, p, currentKeyword, currentSearchType, currentSortType);
      };
    });

    // <<, <, >, >> 버튼
    document.getElementById('firstPageBtn').onclick = () => {
      if (currentPage > 0) {
        currentPage = 0;
        loadClients(currentType, 0, currentKeyword, currentSearchType, currentSortType);
      }
    };
    document.getElementById('prevPageBtn').onclick = () => {
      if (currentPage > 0) {
        currentPage--;
        loadClients(currentType, currentPage, currentKeyword, currentSearchType, currentSortType);
      }
    };
    document.getElementById('nextPageBtn').onclick = () => {
      if (currentPage < total - 1) {
        currentPage++;
        loadClients(currentType, currentPage, currentKeyword, currentSearchType, currentSortType);
      }
    };
    document.getElementById('lastPageBtn').onclick = () => {
      if (currentPage < total - 1) {
        currentPage = total - 1;
        loadClients(currentType, currentPage, currentKeyword, currentSearchType, currentSortType);
      }
    };

    // 직접 이동 Go
    const gotoInput = document.getElementById('gotoPageInput');
    document.getElementById('gotoPageBtn').onclick = () => {
      const v = parseInt(gotoInput.value, 10);
      if (!isNaN(v) && v >= 1 && v <= total) {
        currentPage = v - 1;
        loadClients(currentType, currentPage, currentKeyword, currentSearchType, currentSortType);
        gotoInput.value = '';
      } else {
        alert(`1부터 ${total} 사이 숫자를 입력하세요.`);
      }
    };
  }

  // ─────────────────────────────────────────────────────────────────────

  // ■ 검색 폼(submit) 이벤트 바인딩
  document.getElementById('searchForm').addEventListener('submit', e => {
    e.preventDefault();
    searchClients();
  });

  // 검색 & 필터
  function searchClients() {
    currentSearchType = document.getElementById('searchType').value;
    currentKeyword    = searchInput.value.trim();
    if (currentSearchType === 'biznum') currentKeyword = stripHyphens(currentKeyword);
    currentSortType   = document.getElementById('sortType').value;
    currentPage       = 0;
    loadClients(currentType, 0, currentKeyword, currentSearchType, currentSortType);
  }

  // Enter 키로 검색
  searchInput.addEventListener('keydown', e => {
    if (e.key === 'Enter') {
      e.preventDefault();
      searchClients();
    }
  });

  // 필터(유형) 변경 즉시 호출
  filterSelect.addEventListener('change', () => {
    currentType = filterSelect.value;
    currentPage = 0;
    loadClients(currentType, 0, currentKeyword, currentSearchType, currentSortType);
  });

  // 정렬 기준 변경 즉시 호출
  sortSelect.addEventListener('change', () => {
    currentSortType = sortSelect.value;
    currentPage = 0;
    loadClients(currentType, 0, currentKeyword, currentSearchType, currentSortType);
  });

  // ------------------------ Bulk delete ------------------------
  window.deleteChecked = () => {
    const ids = [...document.querySelectorAll('.row-check:checked')].map(c=>c.value);
    if (!ids.length) return alert('하나 이상 체크하세요.');
    if (!confirm(`${ids.length}개 삭제하시겠습니까?`)) return;
    Promise.all(ids.map(id =>
      fetch(`/api/clients/${id}`,{method:'DELETE'})
    )).then(() => loadClients(currentType, currentPage, currentKeyword, currentSearchType, currentSortType));
  };
  document.getElementById('all-check').addEventListener('change', function(){
    document.querySelectorAll('.row-check').forEach(c=>c.checked = this.checked);
  });

  // 핸드폰/사업자번호 입력 시 포맷팅
  document.getElementById('phone').addEventListener('input', e => {
    e.target.value = formatPhone(e.target.value);
  });
  document.getElementById('bizNum').addEventListener('input', e => {
    e.target.value = formatBiz(e.target.value);
  });

  // 카카오 우편번호 API
  registerModalEl.addEventListener('shown.bs.modal', () => {
    document.getElementById('companyAddr').onclick = () => {
      new daum.Postcode({ oncomplete: data => {
        document.getElementById('postcode').value    = data.zonecode;
        document.getElementById('companyAddr').value = data.roadAddress || data.jibunAddress;
        document.getElementById('detailAddress').focus();
      }}).open();
    };
    if (!isEdit) loadEmployees();
  });

  // 거래처 등록/수정 폼 submit (중복 체크 포함)
  document.getElementById('clientRegisterForm').onsubmit = e => {
    e.preventDefault();
    const f   = e.target;
    const tel = stripHyphens(f.phone.value.trim());
    const biz = stripHyphens(f.bizNum.value.trim());

    // (0) 주소 필수 검사
    if (!f.postcode.value.trim() || !f.companyAddr.value.trim()) {
      alert('주소를 반드시 입력해주세요.');
      return;
    }

    // (1) 거래처명 단독 중복 체크
    const nameDupUrl = `/api/clients/duplicateName?companyName=${encodeURIComponent(f.companyName.value.trim())}` +
                       (isEdit ? `&excludeId=${editCompanyId}` : '');
    fetch(nameDupUrl)
      .then(r => r.json())
      .then(nameDup => {
        if (nameDup.exists) {
          alert('이미 등록된 거래처명입니다.');
          return;
        }

        // (2) 사업자번호 단독 중복 체크
        let bizDupUrl = `/api/clients/duplicate/biznum?companyNo=${encodeURIComponent(biz)}`;
        if (isEdit) bizDupUrl += `&excludeId=${editCompanyId}`;
        return fetch(bizDupUrl)
          .then(r => r.json())
          .then(bizDup => {
            if (bizDup.exists) {
              alert('이미 등록된 사업자번호입니다.');
              return;
            }

            // (3) 회사명+사업자번호 복합 중복 체크
            let dupUrl = `/api/clients/duplicate?companyName=${encodeURIComponent(f.companyName.value.trim())}&companyNo=${biz}`;
            if (isEdit) dupUrl += `&excludeId=${editCompanyId}`;
            return fetch(dupUrl)
              .then(r => r.json())
              .then(dup => {
                if (dup.exists) {
                  document.getElementById('duplicateWarning').classList.remove('d-none');
                  return;
                }

                // (4) 모두 통과 → 실제 저장
                const data = {
                  companyName:   f.companyName.value.trim(),
                  presidentNm:   f.ceoName.value.trim(),
                  companyNo:     biz,
                  companyTel:    tel,
                  companyFax:    f.fax.value.trim(),
                  postcode:      f.postcode.value.trim(),
                  companyAddr:   f.companyAddr.value.trim(),
                  detailAddress: f.detailAddress.value.trim(),
                  companyCond:   f.type.value.trim(),
                  companyItem:   f.item.value.trim(),
                  companyType:   document.querySelector('input[name="clientType"]:checked').value,
                  useYn:         'Y',
                  employeeId:    f.employeeId.value
                };
                const url    = isEdit ? `/api/clients/${editCompanyId}` : '/api/clients';
                const method = isEdit ? 'PUT' : 'POST';
                return fetch(url, {
                  method, headers:{ 'Content-Type':'application/json' }, body:JSON.stringify(data)
                }).then(r => {
                  if (r.ok) {
                    alert(isEdit ? '수정 완료!' : '등록 완료!');
                    bootstrap.Modal.getInstance(registerModalEl).hide();
                    loadClients(currentType, currentPage, currentKeyword, currentSearchType, currentSortType);
                  } else {
                    r.text().then(txt=>alert('실패: '+txt));
                  }
                });
              });
          });
      });
  };

  // 모달 닫힐 때 폼 리셋
  registerModalEl.addEventListener('hidden.bs.modal', () => {
    document.getElementById('clientRegisterForm').reset();
    isEdit = false;
    editCompanyId = null;
    document.getElementById('clientRegisterModalLabel').innerText = '거래처 등록';
    document.getElementById('modalSubmitBtn').innerText           = '등록';
    document.getElementById('duplicateWarning').classList.add('d-none');
  });

  // 최초 로드
  loadClients();
});
