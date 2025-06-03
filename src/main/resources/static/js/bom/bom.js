// ✅ BOM.js 전체 수정본 (2025-05-30 기준)

let bomEditMode = false;
let selectedParentId = null;

// ✅ 사용여부 필터

document.getElementById("searchUseYn").addEventListener("change", function() {
	loadBomList(0);
});

document.addEventListener("DOMContentLoaded", () => {
	loadBomList();
});

document.getElementById("searchForm").addEventListener("submit", function(e) {
	e.preventDefault();
	loadBomList(0);
});

// 전역 (예: 파일 상단 혹은 script 하단)
function disableAddButtonIfNoOptions() {
	const selectedIds = Array.from(document.querySelectorAll("select[name='child_item_id[]']"))
		.map(s => s.value);

	const unusedOptions = window.allRawItemOptions.filter(opt => !selectedIds.includes(String(opt.value)));
	const addBtn = document.querySelector("#bomRegisterModal button.btn-outline-secondary");

	if (addBtn) {
		addBtn.disabled = unusedOptions.length === 0;
	}
}

// ✅ BOM 목록 로드
function loadBomList(page = 0) {
	const searchField = document.getElementById("searchType").value;
	const keyword = document.getElementById("searchKeyword").value;
	const useYn = document.getElementById("searchUseYn").value;

	const params = new URLSearchParams({ searchField, keyword, useYn, page });
	fetch(`/api/bom/list?${params}`)
		.then(res => res.json())
		.then(data => {
			renderBomTable(data.content);
			renderPagination(data.totalPages, data.number, searchField, keyword, useYn);

			window.allRawItemOptions = data.selectMaterialList.map(item => ({
				value: item.id,
				code: item.code,
				label: `${item.code} - ${item.name}`
			}));

			window.allProductItemOptions = data.selectProductList.map(item => ({
				value: item.id,
				code: item.code,
				label: `${item.code} - ${item.name}`
			}));

			// ─── 여기서 parentItemSelect 옵션을 세팅 ───
			const parentSelect = document.getElementById("parentItemSelect");
			if (parentSelect) {
				parentSelect.innerHTML =
					`<option value="">-- 선택하세요 --</option>` +
					window.allProductItemOptions
						.map(item => `<option value="${item.value}">${item.label}</option>`)
						.join('');
			}
			// ─── 이 직후에 “버전 자동 생성”을 위한 change 이벤트 리스너 등록 ───
			if (parentSelect) {
				parentSelect.addEventListener("change", function() {
					const parentId = this.value;
					const versionInput = document.getElementById("versionCodeInput");

					if (!parentId) {
						versionInput.value = "선택된 품목 없음";
						return;
					}
					fetch(`/api/bom/versions/${parentId}`)
						.then(res => res.json())
						.then(versions => {
							const maxSuffix = versions.reduce((max, v) => {
								const num = parseInt(v.versionCode.replace(/^V/, "")) || 0;
								return Math.max(max, num);
							}, 0);
							versionInput.value = `V${maxSuffix + 1}`;
						})
						.catch(err => {
							console.error("버전 목록 조회 중 오류:", err);
							versionInput.value = "자동 생성 실패";
						});
				});
			}
		})
		.catch(err => {
			console.error("❌ BOM 목록 조회 실패:", err);
			alert("BOM 목록 조회 실패");
		});
}

function renderBomTable(items) {
	const tbody = document.getElementById("bomTableBody");
	tbody.innerHTML = '';
	if (!items || items.length === 0) {
		tbody.innerHTML = '<tr><td colspan="10">데이터가 없습니다.</td></tr>';
		return;
	}
	items.forEach(item => {
		const row = document.createElement("tr");
		row.innerHTML = `
      <td><input type="checkbox" value="${item.id}" /></td>
      <td>${item.code}</td>
      <td>${item.name}</td>
      <td>${item.type}</td>
      <td>${item.spec}</td>
      <td>${item.unit}</td>
      <td>${item.use}</td>
      <td><button class="btn btn-sm btn-info" onclick="openBomVersionSelectModal(${item.id})">보기</button></td>
      <td><button class="btn btn-sm btn-outline-info" onclick="openItemDetailModal('${item.code}')">보기</button></td>
      <td><button class="btn btn-sm btn-outline-secondary" onclick="openBomEditVersionSelectModal(${item.id})">수정</button></td>`;
		tbody.appendChild(row);
	});
}

function renderPagination(totalPages, currentPage, searchField, keyword, useYn) {
	const pagination = document.getElementById("bomPagination");
	pagination.innerHTML = '';
	if (totalPages <= 1) return;

	const createItem = (page, label, active = false, disabled = false) => `
    <li class="page-item ${active ? 'active' : ''} ${disabled ? 'disabled' : ''}">
      <a class="page-link" href="#" onclick="loadBomList(${page}); event.preventDefault();">${label}</a>
    </li>`;

	pagination.innerHTML += createItem(currentPage - 1, '이전', false, currentPage === 0);
	for (let i = 0; i < totalPages; i++) {
		pagination.innerHTML += createItem(i, i + 1, currentPage === i);
	}
	pagination.innerHTML += createItem(currentPage + 1, '다음', false, currentPage === totalPages - 1);
}

function openBomEditVersionSelectModal(parentId) {
	selectedParentId = parentId;
	bomEditMode = true;
	openBomVersionSelectModal(parentId);
}

function openBomVersionSelectModal(parentId) {
	fetch(`/api/bom/versions/${parentId}`)
		.then(res => res.json())
		.then(versions => {
			const select = document.getElementById("bomVersionSelect");
			select.innerHTML = versions.map(v => `<option value="${v.id}">${v.versionCode} (${v.useYn})</option>`).join('');
			bootstrap.Modal.getOrCreateInstance(document.getElementById("bomVersionSelectModal")).show();
		})
		.catch(err => {
			console.error("❌ BOM 버전 목록 조회 실패:", err);
			alert("버전 목록 조회 실패");
		});
}

function loadBomByVersion() {
	const versionId = document.getElementById("bomVersionSelect").value;
	if (!versionId) {
		alert("버전을 선택해주세요.");
		return;
	}
	bomEditMode ? loadBomVersionForEdit(versionId) : loadBomVersionForView(versionId);
}

function loadBomVersionForView(versionId) {
	// 1) 부모/상세 테이블 부분은 그대로 두고
	  fetch(`/api/bom/version/${versionId}`)
	    .then(res => {
	      if (!res.ok) throw new Error("버전 상세 조회 실패");
	      return res.json();
	    })
	    .then(data => {
	      // (a) 최상위 부모 이름
	      document.getElementById("bomParentName").innerText = data.parentName;

	      // (b) BOM 구성 상세 테이블 채우기
	      const tbody = document.getElementById("bomComponentTableBody");
	      tbody.innerHTML = "";
	      data.components.forEach((c, i) => {
	        const row = `
	        <tr>
	          <td>${i + 1}</td>
	          <td>${data.parentName}</td>
	          <td>${c.childName}</td>
	          <td>${c.seqNo ?? "-"}</td>
	          <td>${c.qty}</td>
	          <td>${c.lossRate}</td>
	          <td>${c.unitPrice}</td>
	          <td>${c.remark}</td>
	        </tr>`;
	        tbody.insertAdjacentHTML("beforeend", row);
	      });

	      // (c) ===== 트리 전체 조회 함수 호출 =====
	      // versionId가 아니라, data.parentId(또는 data.parentItemId)로 변경하세요.
	      // 만약 version API에서 parentItemId를 주지 않는다면,
	      // loadBomTree에는 “data.parentId”가 아니라 “부모 품목 ID”를 사용해야 합니다.
	      loadBomTree(data.parentId || data.parentItemId);

	      // (d) 모달 숨기고 → modal.show()
	      bootstrap.Modal.getInstance(
	        document.getElementById("bomVersionSelectModal")
	      ).hide();
	      bootstrap.Modal.getOrCreateInstance(
	        document.getElementById("bomViewModal")
	      ).show();
	    })
	    .catch(err => {
	      console.error("상세 보기용 데이터 불러오기 실패:", err);
	      alert("BOM 상세 정보를 가져오는 데 실패했습니다.");
	    });
}
// ✅ 수정 구성 정보 로드 함수
function loadBomVersionForEdit(versionId) {
	function extractNumber(str) {
		if (!str) return "";
		return parseFloat(str.toString().replace(/[^\d.-]/g, "")) || "";
	}

	fetch(`/api/bom/version/${versionId}`)
		.then(res => {
			if (!res.ok) throw new Error("서버 오류");
			return res.json();
		})
		.then(raw => {
			const data = Array.isArray(raw) ? raw[0] : raw;

			// 🔧 버전 상단 정보 채우기
			document.getElementById("editVersionId").value = data.versionId ?? data.id;
			document.getElementById("editVersionCode").value = data.versionCode ?? '';
			document.getElementById("editDescription").value = data.description ?? '';
			document.getElementById("editUseYn").value = data.useYn ?? '';
			document.getElementById("editParentItemDisplay").value = `[${data.parentCode}] ${data.parentName}`;

			// 🔧 자재 구성 영역 렌더링
			const area = document.getElementById("bomEditTableArea");
			area.innerHTML = "";

			data.components.forEach((c, i) => {
				const row = document.createElement("div");
				row.className = "row g-2 align-items-end mb-2";
				row.innerHTML = `
          <div class="col-md-2">
            <label class="form-label">자재</label>
            <select class="form-select" name="child_item_id[]">
              ${window.allRawItemOptions.map(opt =>
					`<option value="${opt.value}" ${opt.value === c.childItemId ? 'selected' : ''}>${opt.label}</option>`
				).join('')}
            </select>
          </div>
          <div class="col-md-1">
            <label class="form-label">순번</label>
            <input type="number" class="form-control" name="seq_no[]" value="${c.seqNo ?? ''}" />
          </div>
          <div class="col-md-1">
            <label class="form-label">소요량</label>
            <input type="number" class="form-control" name="qty[]" value="${c.qty}" required />
          </div>
          <div class="col-md-1">
            <label class="form-label">로스율</label>
            <input type="number" step="0.01" class="form-control" name="loss_rt[]" value="${extractNumber(c.lossRate)}" />
          </div>
          <div class="col-md-2">
            <label class="form-label">재료비</label>
            <input type="number" class="form-control" name="item_price[]" value="${extractNumber(c.unitPrice)}" />
          </div>
          <div class="col-md-3">
            <label class="form-label">비고</label>
            <input type="text" class="form-control" name="remark[]" value="${c.remark ?? ''}" />
          </div>
          <div class="col-md-2">
            <label class="form-label d-block">&nbsp;</label>
            <button type="button" class="btn btn-outline-danger w-100" onclick="this.closest('.row').remove()">삭제</button>
          </div>
        `;
				area.appendChild(row);
			});

			// ✅ 자재 추가 버튼 추가
			const addBtnWrapper = document.createElement("div");
			addBtnWrapper.className = "text-end mt-2";
			addBtnWrapper.innerHTML = `<button type="button" class="btn btn-outline-secondary" onclick="addEditRow()">+ 자재 추가</button>`;
			area.appendChild(addBtnWrapper);

			// 모달 표시
			bootstrap.Modal.getInstance(document.getElementById("bomVersionSelectModal")).hide();
			bootstrap.Modal.getOrCreateInstance(document.getElementById("bomEditModal")).show();
		})
		.catch(err => {
			console.error("❌ 수정 정보 불러오기 실패:", err);
			alert("수정 정보 불러오기 실패");
		});
}

function addEditRow() {
	const area = document.getElementById("bomEditTableArea");

	// ✅ 현재 이미 선택된 자재 ID들 수집
	const selectedIds = Array.from(document.querySelectorAll("select[name='child_item_id[]']"))
		.map(select => select.value);

	// ✅ 선택되지 않은 자재 목록만 추림
	const availableOptions = window.allRawItemOptions
		.filter(opt => !selectedIds.includes(String(opt.value)));

	// ✅ 더 이상 추가할 수 있는 자재가 없다면 종료
	if (availableOptions.length === 0) {
		alert("선택 가능한 자재가 더 이상 없습니다.");
		return;
	}

	// ✅ 행 DOM 생성
	const row = document.createElement("div");
	row.className = "row g-2 align-items-end mb-2";

	// ✅ innerHTML에 availableOptions만 포함
	row.innerHTML = `
    <div class="col-md-2">
      <label class="form-label">자재</label>
      <select class="form-select" name="child_item_id[]">
        ${availableOptions.map(opt => `<option value="${opt.value}">${opt.label}</option>`).join('')}
      </select>
    </div>
    <div class="col-md-1">
      <label class="form-label">순번</label>
      <input type="number" class="form-control" name="seq_no[]" />
    </div>
    <div class="col-md-1">
      <label class="form-label">소요량</label>
      <input type="number" class="form-control" name="qty[]" required />
    </div>
    <div class="col-md-1">
      <label class="form-label">로스율</label>
      <input type="number" class="form-control" step="0.01" name="loss_rt[]" />
    </div>
    <div class="col-md-2">
      <label class="form-label">재료비</label>
      <input type="number" class="form-control" name="item_price[]" />
    </div>
    <div class="col-md-3">
      <label class="form-label">비고</label>
      <input type="text" class="form-control" name="remark[]" />
    </div>
    <div class="col-md-2">
      <label class="form-label d-block">&nbsp;</label>
      <button type="button" class="btn btn-outline-danger w-100" onclick="this.closest('.row').remove()">삭제</button>
    </div>
  `;

	// ✅ 추가
	area.appendChild(row);
	disableAddButtonIfNoOptions();
}



document.addEventListener("change", function(e) {
	if (e.target.matches("select[name='child_item_id[]']")) {
		disableAddButtonIfNoOptions();
	}
});


function addChildRow() {
	const container = document.getElementById('child-items-area');
	const template = document.getElementById('child-item-template');
	const clone = template.content.cloneNode(true);
	const select = clone.querySelector("select[name='child_item_id[]']");

	// ✅ 현재 선택된 자재 목록 추출
	const selectedIds = Array.from(document.querySelectorAll("select[name='child_item_id[]']"))
		.map(s => s.value);

	// ✅ 선택되지 않은 자재만 렌더링
	select.innerHTML = window.allRawItemOptions
		.filter(opt => !selectedIds.includes(String(opt.value)))
		.map(opt => `<option value="${opt.value}">${opt.label}</option>`)
		.join('');

	container.appendChild(clone);
	disableAddButtonIfNoOptions();
}
document.getElementById("bomForm").addEventListener("submit", function(e) {
	e.preventDefault();

	const form = e.target;

	const parentItemId = form.parent_item_id.value;
	const versionCode = form.version_code.value;
	const description = form.description.value;
	const useYn = form.use_yn.value;

	if (!parentItemId || !versionCode) {
		alert("완제품과 버전 코드는 필수입니다.");
		return;
	}

	const childItemIds = Array.from(form.querySelectorAll("select[name='child_item_id[]']")).map(e => e.value);
	const seqNos = Array.from(form.querySelectorAll("input[name='seq_no[]']")).map(e => e.value);
	const qtys = Array.from(form.querySelectorAll("input[name='qty[]']")).map(e => e.value);
	const lossRates = Array.from(form.querySelectorAll("input[name='loss_rt[]']")).map(e => e.value);
	const itemPrices = Array.from(form.querySelectorAll("input[name='item_price[]']")).map(e => e.value);
	const remarks = Array.from(form.querySelectorAll("input[name='remark[]']")).map(e => e.value);

	const components = childItemIds.map((id, i) => ({
		childItemId: parseInt(id),
		seqNo: seqNos[i] ? parseInt(seqNos[i]) : null,
		qty: parseFloat(qtys[i]),
		lossRt: parseFloat(lossRates[i]) || 0,
		itemPrice: parseFloat(itemPrices[i]) || 0,
		remark: remarks[i] || ''
	}));

	const payload = {
		parentItemId: parseInt(parentItemId),
		versionCode,
		description,
		useYn,
		components
	};

	fetch("/api/bom", {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(payload)
	})
		.then(async res => {
			if (!res.ok) {
				const contentType = res.headers.get("content-type");
				if (contentType && contentType.includes("application/json")) {
					const data = await res.json();
					throw new Error(data.error || "알 수 없는 오류 발생");
				} else {
					const text = await res.text();
					throw new Error(text || "등록 실패");
				}
			}

			// ✅ 이 return은 정상 응답일 때만 호출
			return res.json();
		})
		.then(() => {
			alert("등록 완료");
			bootstrap.Modal.getInstance(document.getElementById("bomRegisterModal")).hide();
			form.reset();
			document.getElementById("child-items-area").innerHTML = "";
			loadBomList();
		})
		.catch(err => {
			alert(err.message || "등록 실패");
			console.error("❌ 등록 오류:", err);
		});
});

// ✅ 수정 모달 저장 버튼 이벤트 등록
document.getElementById("bomEditForm").addEventListener("submit", function(e) {
	e.preventDefault(); // 기본 form 전송 막기

	const form = e.target;

	// 🔍 버전 ID와 부모 품목 ID 추출
	const versionId = document.getElementById("editVersionId").value;
	const parentDisplay = document.getElementById("editParentItemDisplay").value;

	// 예: [PRD-001] 완제품X → 코드만 추출
	const parentCode = parentDisplay.match(/\[(.*?)\]/)?.[1];
	const parentItem = window.allProductItemOptions.find(opt => opt.code === parentCode);
	if (!parentItem) {
		alert("잘못된 부모 품목 정보입니다.");
		return;
	}

	// 🔍 기타 기본 정보
	const description = document.getElementById("editDescription").value;
	const useYn = document.getElementById("editUseYn").value;

	// 🔍 자재 구성 수집
	const container = document.getElementById("bomEditTableArea");
	const rows = container.querySelectorAll(".row.g-2");

	const components = Array.from(rows).map(row => {
		return {
			childItemId: parseInt(row.querySelector("select[name='child_item_id[]']").value),
			seqNo: parseInt(row.querySelector("input[name='seq_no[]']").value) || null,
			qty: parseFloat(row.querySelector("input[name='qty[]']").value),
			lossRt: parseFloat(row.querySelector("input[name='loss_rt[]']").value) || 0,
			itemPrice: parseFloat(row.querySelector("input[name='item_price[]']").value) || 0,
			remark: row.querySelector("input[name='remark[]']").value || ""
		};
	});

	// ✅ 최종 요청 객체 구성
	const payload = {
		versionId: parseInt(versionId),
		parentItemId: parseInt(parentItem.value),
		description,
		useYn,
		components
	};

	// ✅ PUT 요청 전송
	fetch("/api/bom", {
		method: "PUT",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(payload)
	})
		.then(res => {
			if (!res.ok) throw new Error("수정 실패");
			alert("수정이 완료되었습니다.");
			bootstrap.Modal.getInstance(document.getElementById("bomEditModal")).hide();
			loadBomList(); // 목록 갱신
		})
		.catch(err => {
			console.error("❌ 수정 실패:", err);
			alert("수정 중 오류 발생");
		});
});


// ✅ BOM 버전 삭제 함수
function deleteBomVersion() {
	const versionId = document.getElementById("bomVersionSelect").value;
	if (!versionId) {
		alert("삭제할 버전을 선택하세요.");
		return;
	}

	if (!confirm("정말 이 BOM 버전을 삭제하시겠습니까?")) return;

	// 🔥 DELETE 요청 전송
	fetch(`/api/bom/version/${versionId}`, {
		method: "DELETE"
	})
		.then(res => {
			if (!res.ok) throw new Error("삭제 실패");
			alert("BOM 버전이 삭제되었습니다.");
			bootstrap.Modal.getInstance(document.getElementById("bomVersionSelectModal")).hide();
			loadBomList(); // 목록 새로고침
		})
		.catch(err => {
			console.error("❌ 삭제 실패:", err);
			alert("BOM 버전 삭제 중 오류 발생");
		});
}
// ✅ 체크된 BOM 항목 삭제 요청
function deleteSelectedBoms() {
	const checkedBoxes = document.querySelectorAll("#bomTableBody input[type='checkbox']:checked");
	if (checkedBoxes.length === 0) {
		alert("삭제할 항목을 선택하세요.");
		return;
	}

	if (!confirm("선택한 BOM을 모두 삭제하시겠습니까?")) return;

	const ids = Array.from(checkedBoxes).map(cb => parseInt(cb.value));

	// 🔥 DELETE 요청 전송
	fetch("/api/bom", {
		method: "DELETE",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(ids)
	})
		.then(res => {
			if (!res.ok) throw new Error("삭제 실패");
			alert("선택한 BOM이 삭제되었습니다.");
			loadBomList(); // 목록 갱신
		})
		.catch(err => {
			console.error("❌ 삭제 실패:", err);
			alert("BOM 삭제 중 오류 발생");
		});
}


function loadBomTree(parentId) {
  fetch(`/api/bom/tree/${parentId}`)
    .then(res => res.json())
    .then(tree => {
      const parentNameEl = document.getElementById("bomParentName");
      parentNameEl.innerText = tree.itemName;

      // bomTreeList를 비우고, 재귀 함수로 트리 렌더링
      const bomTreeList = document.getElementById("bomTreeList");
      bomTreeList.innerHTML = "";
      renderTreeRecursive(tree, bomTreeList);
    })
    .catch(err => {
      console.error("전체 트리 조회 실패:", err);
    });
}

function renderTreeRecursive(node, parentUl) {
  const li = document.createElement("li");
  li.textContent = node.itemName;
  parentUl.appendChild(li);

  if (node.children && node.children.length > 0) {
    const childUl = document.createElement("ul");
    childUl.className = "list-unstyled ms-3";
    li.appendChild(childUl);
    node.children.forEach(childNode => {
      renderTreeRecursive(childNode, childUl);
    });
  }
}
