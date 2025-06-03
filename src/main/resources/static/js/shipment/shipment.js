// 파일: src/main/resources/static/js/shipment.js
// (혹은 프로젝트 설정에 맞게 정적 리소스 경로에 배치)

document.addEventListener("DOMContentLoaded", function() {
  // 페이지 로드 직후에 출고 목록을 불러와 테이블에 표시
  loadShipments();

  // 검색 버튼 클릭 시에도 재조회
  const btnSearch = document.getElementById("btnSearch");
  btnSearch.addEventListener("click", function() {
    loadShipments();
  });
});

/**
 * 출고 데이터를 API에서 받아와 테이블에 렌더링하는 함수
 */
async function loadShipments() {
  try {
    const response = await fetch("/api/shipments");
    if (!response.ok) {
      throw new Error("출고 목록을 불러오는 중 오류가 발생했습니다.");
    }
    const shipments = await response.json(); // Array of ShipmentInfoDTO

    // 테이블 바디 가져오기
    const tbody = document.querySelector("#shipmentTable tbody");
    tbody.innerHTML = ""; // 초기화

    // 데이터가 없을 때
    if (shipments.length === 0) {
      const tr = document.createElement("tr");
      const td = document.createElement("td");
      td.setAttribute("colspan", "13");
      td.textContent = "등록된 출고 내역이 없습니다.";
      td.classList.add("text-center");
      tr.appendChild(td);
      tbody.appendChild(tr);
      return;
    }

    // 하나씩 행(row) 생성
    shipments.forEach((s, index) => {
      const tr = document.createElement("tr");

      // 1) 체크박스
      const tdCheckbox = document.createElement("td");
      const chk = document.createElement("input");
      chk.type = "checkbox";
      chk.classList.add("shipment-checkbox");
      chk.value = s.logId;
      tdCheckbox.appendChild(chk);
      tr.appendChild(tdCheckbox);

      // 2) 순번 (index + 1)
      const tdNo = document.createElement("td");
      tdNo.textContent = index + 1;
      tr.appendChild(tdNo);

      // 3) 출고일자
      const tdDate = document.createElement("td");
      // LocalDateTime → 문자열 YYYY-MM-DD 형식으로 포맷팅
      const date = new Date(s.logDatetime);
      tdDate.textContent = date.toISOString().slice(0, 10);
      tr.appendChild(tdDate);

      // 4) 고객사명
      const tdCompany = document.createElement("td");
      tdCompany.textContent = s.companyName || "";
      tr.appendChild(tdCompany);

      // 5) 품목코드
      const tdItemCode = document.createElement("td");
      tdItemCode.textContent = s.itemCode || "";
      tr.appendChild(tdItemCode);

      // 6) 품목명
      const tdItemName = document.createElement("td");
      tdItemName.textContent = s.itemName || "";
      tr.appendChild(tdItemName);

      // 7) 수량
      const tdQty = document.createElement("td");
      tdQty.textContent = s.quantity;
      tr.appendChild(tdQty);

      // 8) 단위
      const tdUnit = document.createElement("td");
      tdUnit.textContent = s.unit || "";
      tr.appendChild(tdUnit);

      // 9) 창고
      const tdWarehouse = document.createElement("td");
      tdWarehouse.textContent = s.warehouseName || "";
      tr.appendChild(tdWarehouse);

      // 10) 담당자
      const tdEmp = document.createElement("td");
      tdEmp.textContent = s.companyEmpName || "";
      tr.appendChild(tdEmp);

      // 11) 비고 (comment)
      const tdComment = document.createElement("td");
      tdComment.textContent = s.comment || "";
      tr.appendChild(tdComment);

      // 12) 상세 버튼 (모달 또는 상세 페이지로 연결)
      const tdDetail = document.createElement("td");
      const btnDetail = document.createElement("button");
      btnDetail.classList.add("btn", "btn-sm", "btn-info");
      btnDetail.textContent = "상세";
      // 예: 상세 모달을 띄우려면 버튼에 data-log-id 같은 속성을 둔다
      btnDetail.setAttribute("data-log-id", s.logId);
      btnDetail.addEventListener("click", function() {
        // TODO: 상세 모달을 띄우는 로직을 여기에 추가
        openShipmentDetailModal(s.logId);
      });
      tdDetail.appendChild(btnDetail);
      tr.appendChild(tdDetail);

      // 13) 수정 버튼
      const tdEdit = document.createElement("td");
      const btnEdit = document.createElement("button");
      btnEdit.classList.add("btn", "btn-sm", "btn-warning");
      btnEdit.textContent = "수정";
      btnEdit.setAttribute("data-log-id", s.logId);
      btnEdit.addEventListener("click", function() {
        // TODO: 수정 모달을 띄우는 로직을 여기에 추가
        openShipmentEditModal(s.logId);
      });
      tdEdit.appendChild(btnEdit);
      tr.appendChild(tdEdit);

      // 테이블 바디에 붙이기
      tbody.appendChild(tr);
    });

  } catch (error) {
    console.error(error);
    alert(error.message);
  }
}

/**
 * 출고 상세 모달 띄우기 (임시 함수 뼈대)
 */
function openShipmentDetailModal(logId) {
  // logId를 기반으로 /api/shipments/{logId} 같은 엔드포인트를 호출하여
  // 상세 정보를 fetch 한 뒤, 모달 내용을 채워 화면에 보여줍니다.
  console.log("상세 모달 열기: logId =", logId);
  // 예: fetch(`/api/shipments/${logId}`) …
}

/**
 * 출고 수정 모달 띄우기 (임시 함수 뼈대)
 */
function openShipmentEditModal(logId) {
  console.log("수정 모달 열기: logId =", logId);
  // 예: fetch(`/api/shipments/${logId}`) …
}
