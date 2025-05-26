function handlePlantEdit(button) {
  // 버튼의 data 속성에서 값 추출
  const work_name = button.dataset.workName;           // data-workplace-name
  const work_ceonm = button.dataset.workCeonm; // data-workplace-president-nm
  const work_no = button.dataset.workNo;
  const work_cond = button.dataset.workCond;
  const work_item = button.dataset.workItem;
  const work_tel = button.dataset.workTel;
  const work_fax = button.dataset.workFax;
  const work_addr = button.dataset.workAddr;
  const work_mainadd = button.dataset.workMainadd;  // 여기도 데이터 속성 이름 맞춰야함
  const work_detailadd = button.dataset.workDetailadd; // 마찬가지
  const work_manname = button.dataset.workManname;
  const work_manemail = button.dataset.workManemail;
  const work_mantel = button.dataset.workMantel;

  // 모달 폼 입력값 설정
  document.getElementById("modal_work_name").value = work_name;
  document.getElementById("modal_work_ceonm").value = work_ceonm;
  document.getElementById("modal_work_no").value = work_no;
  document.getElementById("modal_work_cond").value = work_cond;
  document.getElementById("modal_work_item").value = work_item;
  document.getElementById("modal_work_tel").value = work_tel;
  document.getElementById("modal_work_fax").value = work_fax;
  document.getElementById("modal_work_addr").value = work_addr;
  document.getElementById("modal_work_mainadd").value = work_mainadd;
  document.getElementById("modal_work_detailadd").value = work_detailadd;
  document.getElementById("modal_work_manname").value = work_manname;
  document.getElementById("modal_work_manemail").value = work_manemail;
  document.getElementById("modal_work_mantel").value = work_mantel;

  // 모달 열기
  const modal = new bootstrap.Modal(document.getElementById("plantRegisterModal"));
  modal.show();
}



//주소 API - 모달창
function execDaumPostcodeModal() {
  new daum.Postcode({
    oncomplete: function(data) {
      // 주소 유형 (도로명 or 지번)
      const isRoad = data.userSelectedType === 'R';
      document.getElementById("modal_work_addr").value = isRoad ? "도로명" : "지번";

      // 기본 주소 값 (선택한 주소)
      const address = isRoad ? data.roadAddress : data.jibunAddress;
      document.getElementById("modal_work_mainadd").value = address;

      // 상세 주소는 사용자 입력 영역 → 초기화
      document.getElementById("modal_work_detailadd").value = "";
      document.getElementById("modal_work_detailadd").focus();
    }
  }).open();
}

//주소 API - 메인창
function execDaumPostcodeMain() {
  new daum.Postcode({
    oncomplete: function(data) {
      // 주소 유형 (도로명 or 지번)
      const isRoad = data.userSelectedType === 'R';
      document.getElementById("work_addr").value = isRoad ? "도로명" : "지번";

      // 기본 주소 값 (선택한 주소)
      const address = isRoad ? data.roadAddress : data.jibunAddress;
      document.getElementById("work_mainadd").value = address;

      // 상세 주소는 사용자 입력 영역 → 초기화
      document.getElementById("work_detailadd").value = "";
      document.getElementById("work_detailadd").focus();
    }
  }).open();
}

//초기화
function resetcheck(){
   if (confirm("입력한 정보를 초기화하시겠습니까?")) {
          document.querySelector('form[action="/plant/insert"]').reset();
          alert("입력한 정보가 초기화되었습니다.");
      } else {
          alert("초기화가 취소되었습니다.");
      }
}
  
/*function submitcheck() {
    if (confirm("입력한 정보를 저장하시겠습니까?")) {
        const inputs = document.querySelectorAll('input[required]');
        for (let input of inputs) { //필수 input 요소 검사
            if (!input.value.trim()) { //input.value: 해당 입력값, trim: 공백 제거
                alert("모든 필수 항목을 입력해주세요."); //입력이 비어있을시 출력
                return; //return으로 종료 -> submit 실행되지 않음
            }
        }
        alert("저장이 완료되었습니다.");
        document.querySelector('form[action="/plant/insert"]').submit();
    } else {
        alert("저장이 취소되었습니다.");
    }
}*/

//등록
function submitcheck() {
    if (confirm("입력한 정보를 저장하시겠습니까?")) {
        // 신규 등록 폼만 선택 (예: form[action="/plant/insert"])
        const form = document.querySelector('form[action="/plant/insert"]');
        const inputs = form.querySelectorAll('input[required]');

        for (let input of inputs) {
            if (!input.value.trim()) {
                alert("모든 필수 항목을 입력해주세요.");
                return;
            }
        }
        alert("저장이 완료되었습니다.");
        form.submit();
    } else {
        alert("저장이 취소되었습니다.");
    }
}

