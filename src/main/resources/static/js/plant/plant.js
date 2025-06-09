function handlePlantEdit(button) {
    // 버튼의 data 속성에서 값들을 가져오기
    const workId = button.closest('tr').querySelector('input[name="work_id"]').value;
    const workName = button.getAttribute('data-work-name');
    const workCeonm = button.getAttribute('data-work-ceonm');
    const workNo = button.getAttribute('data-work-no');
    const workCond = button.getAttribute('data-work-cond');
    const workItem = button.getAttribute('data-work-item');
    const workTel = button.getAttribute('data-work-tel');
    const workFax = button.getAttribute('data-work-fax');
    const workAddr = button.getAttribute('data-work-addr');
    const workMainadd = button.getAttribute('data-work-mainadd');
    const workDetailadd = button.getAttribute('data-work-detailadd');
    const workManname = button.getAttribute('data-work-manname');
    const workManemail = button.getAttribute('data-work-manemail');
    const workMantel = button.getAttribute('data-work-mantel');

    console.log('수정할 work_id:', workId); // 디버깅용

    // 모달의 input 필드에 값 설정
    document.getElementById('modal_work_id').value = workId || '';
    document.getElementById('modal_work_name').value = workName || '';
    document.getElementById('modal_work_ceonm').value = workCeonm || '';
    document.getElementById('modal_work_no').value = workNo || '';
    document.getElementById('modal_work_cond').value = workCond || '';
    document.getElementById('modal_work_item').value = workItem || '';
    document.getElementById('modal_work_tel').value = workTel || '';
    document.getElementById('modal_work_fax').value = workFax || '';
    document.getElementById('modal_work_addr').value = workAddr || '';
    document.getElementById('modal_work_mainadd').value = workMainadd || '';
    document.getElementById('modal_work_detailadd').value = workDetailadd || '';
    document.getElementById('modal_work_manname').value = workManname || '';
    document.getElementById('modal_work_manemail').value = workManemail || '';
    document.getElementById('modal_work_mantel').value = workMantel || '';

    // 모달 열기
    const modal = new bootstrap.Modal(document.getElementById('plantRegisterModal'));
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

