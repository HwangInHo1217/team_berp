function openplantEditModal(workplace_name, workplace_president_nm, workplace_no, workplace_cond,
	  workplace_item, workplace_tel, workplace_fax, workplace_addr, workplace_manname,
	  workplace_manemail, workplace_mantel) {
  // 폼에 값 채우기 
  document.getElementById("workplace_name").value = workplace_name;
  document.getElementById("workplace_president_nm").value = workplace_president_nm;
  document.getElementById("workplace_no").value = workplace_no;
  document.getElementById("workplace_cond").value = workplace_cond;
  document.getElementById("workplace_item").value = workplace_item;
  document.getElementById("workplace_tel").value = workplace_tel;
  document.getElementById("workplace_fax").value = workplace_fax;
  document.getElementById("workplace_addr").value = workplace_addr;
  document.getElementById("workplace_manname").value = workplace_manname;
  document.getElementById("workplace_manemail").value = workplace_manemail;
  document.getElementById("workplace_mantel").value = workplace_mantel;

  // 등록/수정 상태 구분용 (선택사항)
  // document.getElementById("mode").value = "edit";

  // 모달 띄우기
  const modal = new bootstrap.Modal(document.getElementById("plantRegisterModal"));
  modal.show();
}
 
function resetcheck(){
	if (confirm("입력한 정보를 초기화하시겠습니까?")) {
	       document.querySelector('form[action="/plant/insert"]').reset();
	       alert("입력한 정보가 초기화되었습니다.");
	   } else {
	       alert("초기화가 취소되었습니다.");
	   }
}
  
function submitcheck() {
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
}
