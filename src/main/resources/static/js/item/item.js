function submitItem() {
    const form = document.getElementById('addItemForm'); // 폼 요소 가져오기
	const formData = new FormData(form); // FormData 객체 생성
	
    const jsonData = {}; // 전송할 JSON 객체 초기화
    formData.forEach((value, key) => jsonData[key] = value); // 폼 데이터를 JSON 형태로 변환
    

    // fetch API로 POST 요청
    fetch('/item/item', { // 백엔드의 POST 엔드포인트
      method: 'POST', // HTTP POST 방식
      headers: {
        'Content-Type': 'application/json' // JSON 데이터임을 명시
      },
      body: JSON.stringify(jsonData) // 객체 → JSON 문자열로 변환
    })
    .then(response => {
      if (!response.ok) throw new Error("서버 오류 발생"); // 오류 처리
      return response.json(); // JSON 응답 파싱
    })
    .then(data => {
      alert('저장 완료!'); // 성공 알림
      location.reload(); // 페이지 새로고침
    })
    .catch(err => {
      alert('저장 실패!'); // 사용자에게 실패 알림
      console.error(err); // 콘솔에 에러 로그 출력
    });
  }
  function openEditModal(button) {
    document.getElementById("editItemId").value = button.getAttribute("data-id");
    document.getElementById("editItemName").value = button.getAttribute("data-name");
    document.getElementById("editItemType").value = button.getAttribute("data-type");
    document.getElementById("editItemSpec").value = button.getAttribute("data-spec");
    document.getElementById("editItemUnit").value = button.getAttribute("data-unit");
    document.getElementById("editItemUse").value = button.getAttribute("data-use");

    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById("itemEditModal"));
    modal.show();
  }


  // ✅ 수정 요청 처리 함수
  async function submitItemUpdate() {
    const id = document.getElementById("editItemId").value;
    const formData = {
      name: document.getElementById("editItemName").value,
      type: document.getElementById("editItemType").value,
      spec: document.getElementById("editItemSpec").value,
      unit: document.getElementById("editItemUnit").value,
      use: document.getElementById("editItemUse").value,
    };

    try {
      const response = await fetch(`/item/${id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(formData)
      });

      if (response.ok) {
        alert("수정이 완료되었습니다.");
        location.reload();
      } else {
        const errorText = await response.text();
        alert("수정 실패: " + errorText);
      }
    } catch (error) {
      alert("통신 오류: " + error.message);
      console.error("통신 오류:", error);
    }
  }

  function submitDelete() {
    const checkedBoxes = document.querySelectorAll('input[type="checkbox"][name="ids"]:checked');
    if (checkedBoxes.length === 0) {
      alert("삭제할 품목을 선택해주세요.");
      return;
    }

    const ids = Array.from(checkedBoxes).map(cb => cb.value);

    if (!confirm("정말 삭제하시겠습니까?")) return;

	fetch('/item/delete', {
	  method: 'DELETE',
	  headers: {
	    'Content-Type': 'application/json'
	  },
	  body: JSON.stringify(ids)
	})
	.then(async (response) => {
	  if (response.ok) {
	    alert("삭제가 완료되었습니다.");
	    location.reload();
	  } else {
	    const errorText = await response.text();  // 에러 메시지 읽기
	    console.error("서버 응답 오류:", response.status, errorText);
	    alert("삭제에 실패했습니다.\n" + errorText);  // 또는 개발단계에서는 이걸 alert로 출력해도 좋아
	  }
	})
	.catch(error => {
	  console.error("fetch 요청 실패:", error);
	  alert("통신 중 오류 발생: " + error.message);
	});

  }

  document.addEventListener("DOMContentLoaded", function () {
    const currentTab = sessionStorage.getItem("tab") || "all";

    // 현재 탭 버튼 활성화
    document.querySelector(`#${currentTab}-tab`)?.click();

    // 탭 클릭 시 현재 탭 상태 저장
    document.querySelectorAll('[data-bs-toggle="tab"]').forEach(tab => {
      tab.addEventListener('click', function () {
        const tabId = this.id.split("-")[0]; // all-tab → all
        sessionStorage.setItem("tab", tabId);
      });
    });

    // 페이징 링크 클릭 시 tab 값 추가
    document.querySelectorAll(".pagination a.page-link").forEach(link => {
      link.addEventListener("click", function (e) {
        e.preventDefault();
        const url = new URL(this.href);
        url.searchParams.set("tab", sessionStorage.getItem("tab") || "all");
        window.location.href = url.toString();
      });
    });
  });
  

  
