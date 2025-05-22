

// ✅ HTML 문서의 DOM 트리가 완전히 로드된 후 실행되도록 설정
document.addEventListener("DOMContentLoaded", function () {
  
  // 🔹 모달 및 폼 관련 DOM 요소 가져오기
  var warehouseForm = document.querySelector("#warehouseForm"); // 모달 내부 <form>
  var warehouseModal = document.querySelector("#warehouseRegisterModal"); // 모달 전체
  var warehouseCodeInput = document.querySelector("#warehouseId"); // 창고 ID (수정 시 필요)
  var warehouseNameInput = document.querySelector("#warehouseName"); // 창고명 입력 필드
  var warehouseTypeSelect = document.querySelector("#warehouseType"); // 창고 구분 select
  var descriptionTextarea = document.querySelector("#description"); // 설명 입력칸
  var useYnSelect = document.querySelector("#useYn"); // 사용 여부 select

  // 🔹 모달 하단 등록/수정 버튼
  var submitBtn = warehouseForm.querySelector(".modal-footer button[type='submit']");

  // 🔹 검색 기능 관련 DOM 요소
  var searchBtn = document.querySelector("button.btn-outline-secondary"); // 검색 버튼
  var searchKeywordInput = document.querySelector("#searchKeyword"); // 검색어 입력 필드
  var searchTypeSelect = document.querySelector("#searchType"); // 검색 기준 (코드/이름)
  
  
  //창고 구분 선택 시 코드 예시 자동 업데이트
  	// 창고 구분에 따른 코드 예시 변경
  	warehouseTypeSelect.addEventListener("change", function () {
  	  const selected = warehouseTypeSelect.value;
  	  if (selected == "RAW") {
  	    warehouseCodeInput.placeholder = "예: RWWH001";
  	  } else if (selected == "PRODUCT") {
  	    warehouseCodeInput.placeholder = "예: PRWH001";
  	  } else {
  	    warehouseCodeInput.placeholder = "예: RWWH001, PRWH001";
  	  }
  	});
	
	// 중복 확인 버튼
	document.getElementById("checkDuplicateBtn").addEventListener("click", function () {
	  const whCd = warehouseCodeInput.value.trim();

	  if (!whCd) {
	    alert("창고 코드를 입력해주세요.");
	    warehouseCodeInput.focus();
	    return;
	  }

	  fetch(`/api/warehouses/check-duplicate?warehouseCode=${encodeURIComponent(whCd)}`)
	    .then(res => res.json())
	    .then(result => {
	      if (result.status === "ok") {
	        alert("사용 가능한 창고 코드입니다.");
	        warehouseCodeInput.readOnly = true;
	      } else {
	        alert("이미 존재하는 창고 코드입니다.");
	      }
	    })
	    .catch(err => {
	      console.error("중복 확인 오류:", err);
	      alert("중복 확인 중 오류가 발생했습니다.");
	    });
	});

  
  
  // 🔹 모달이 닫힐 때마다 입력 값 초기화
  warehouseModal.addEventListener("hidden.bs.modal", function () {
    resetModalPage(); // 모달 리셋 함수 호출
  });

  // 🔹 창고 등록/수정 제출 시 실행
  warehouseForm.addEventListener("submit", function (e) {
    e.preventDefault(); // 폼의 기본 동작(새로고침) 막기

    // 📦 입력한 데이터를 JSON 형태로 준비
    var data = {
      warehouseName: warehouseNameInput.value,
      warehouseType: warehouseTypeSelect.value,
      useYn: useYnSelect.value,
      description: descriptionTextarea.value
    };

    // 📡 fetch로 백엔드 API에 POST 요청
    fetch("/api/warehouses", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    })
      .then(function (res) {
        if (!res.ok) throw new Error("등록 실패"); // 200번대 아닌 응답일 때 예외 발생
        return res.json(); // JSON 응답 파싱
      })
      .then(function (result) {
        alert("창고가 등록되었습니다."); // 성공 메시지
        location.reload(); // 새로고침으로 목록 반영
      })
      .catch(function (err) {
        alert("에러 발생: " + err.message); // 에러 메시지 알림
      });
  });

  // 🔎 검색 버튼 클릭 이벤트
  searchBtn.addEventListener("click", function () {
    var keyword = searchKeywordInput.value;
    var type = searchTypeSelect.value;

    if (keyword == "") {
      alert("검색어를 입력해주세요.");
      searchKeywordInput.focus(); // 입력 필드에 포커스
      return;
    }

    // 여기에 실제 검색 기능 구현 필요
    console.log("검색어:", keyword, "| 타입:", type);
  });

  // 🔄 모달 리셋 함수 (등록 모드 초기화 용도)
  function resetModalPage() {
    warehouseForm.reset(); // 입력 값 초기화
    warehouseCodeInput.readOnly = false; // ID 입력 가능하도록
    submitBtn.textContent = "등록"; // 버튼 텍스트 되돌리기
    warehouseForm.action = "/warehouse/add"; // 폼 액션 되돌리기
  }
});
