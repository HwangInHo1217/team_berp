function execDaumPostcode() {
  new daum.Postcode({
    oncomplete: function(data) {
      // 주소 유형 (도로명 or 지번)
      const isRoad = data.userSelectedType === 'R';
      document.getElementById("addressType").value = isRoad ? "도로명" : "지번";

      // 기본 주소 값 (선택한 주소)
      const address = isRoad ? data.roadAddress : data.jibunAddress;
      document.getElementById("mainAddress").value = address;

      // 상세 주소는 사용자 입력 영역 → 초기화
      document.getElementById("detailAddress").value = "";
      document.getElementById("detailAddress").focus();
    }
  }).open();
}

// 공통 레이아웃 불러오기
//fetch('/layouts/header.html').then(res => res.text()).then(data => document.getElementById('header').innerHTML = data);
//fetch('/layouts/sidebar.html').then(res => res.text()).then(data => document.getElementById('sidebar').innerHTML = data);
//fetch('/layouts/footer.html').then(res => res.text()).then(data => document.getElementById('footer').innerHTML = data);

// 탭 버튼 기능 예시
document.addEventListener('DOMContentLoaded', () => {
  const tabClient = document.getElementById('tab-client');
  const tabSupplier = document.getElementById('tab-supplier');
  const tableBody = document.getElementById('client-table');

  tabClient.addEventListener('click', () => {
    tabClient.classList.add('btn-outline-primary', 'active');
    tabClient.classList.remove('btn-outline-secondary');
    tabSupplier.classList.remove('active');
    tabSupplier.classList.add('btn-outline-secondary');
    tableBody.innerHTML = `
      <tr><td><input type="checkbox" /></td><td>삼성전자</td><td>이재용</td><td>02-1234-5678</td><td>samsung@example.com</td>
        <td><button class="btn btn-sm btn-warning">수정</button></td><td><button class="btn btn-sm btn-danger">삭제</button></td></tr>
        <tr><td><input type="checkbox" /></td><td>LG화학	</td><td>구광모</td><td>02-9876-5432</td><td>lgchem@example.com</td>
        <td><button class="btn btn-sm btn-warning">수정</button></td><td><button class="btn btn-sm btn-danger">삭제</button></td></tr>
    `;
  });

  tabSupplier.addEventListener('click', () => {
    tabSupplier.classList.add('btn-outline-primary', 'active');
    tabSupplier.classList.remove('btn-outline-secondary');
    tabClient.classList.remove('active');
    tabClient.classList.add('btn-outline-secondary');
    tableBody.innerHTML = `
      <tr><td><input type="checkbox" /></td><td>대한유통</td><td>김유통</td><td>031-222-1111</td><td>daehan@example.com</td>
        <td><button class="btn btn-sm btn-warning">수정</button></td><td><button class="btn btn-sm btn-danger">삭제</button></td></tr>
    `;
  });
});


//수정 모달 스크립트
function openEditModal(company, ceo, biznum, phone, email, fax, address, type, item, clientType) {
  // 폼에 값 채우기
  document.getElementById("companyName").value = company;
  document.getElementById("ceoName").value = ceo;
  document.getElementById("bizNum").value = biznum;
  document.getElementById("phone").value = phone;
  document.getElementById("email").value = email;
  document.getElementById("fax").value = fax;
  document.getElementById("mainAddress").value = address;
  document.getElementById("type").value = type;
  document.getElementById("item").value = item;

  // 거래처 유형 선택
  if (clientType === 'CLIENT') {
    document.getElementById("client").checked = true;
  } else {
    document.getElementById("supplier").checked = true;
  }

  // 등록/수정 상태 구분용 (선택사항)
  // document.getElementById("mode").value = "edit";

  // 모달 띄우기
  const modal = new bootstrap.Modal(document.getElementById("clientRegisterModal"));
  modal.show();
}

function searchClients() {
  const type = document.getElementById("searchType").value;
  const keyword = document.getElementById("searchKeyword").value;

  if (!keyword.trim()) {
    alert("검색어를 입력해주세요.");
    return;
  }

  console.log("검색 조건:", type);
  console.log("검색 키워드:", keyword);

  // ✅ 실제 검색 로직은 여기에 추가
  // 예) fetch(`/api/client/search?type=${type}&keyword=${keyword}`)
  //     .then(res => res.json()).then(data => renderTable(data));

  alert(`[테스트용] "${type}" 기준으로 "${keyword}" 검색합니다.`);
}
