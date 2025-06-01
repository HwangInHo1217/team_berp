const { loadBomList } = require("./bom");

document.getElementById("bomForm").addEventListener("submit", function(e) {
    e.preventDefault(); // 기본 제출 막기

    const form = e.target;

    // ✅ 기본 정보
    const parentItemId = form.parent_item_id.value;
    const versionCode = form.version_code.value;
    const description = form.description.value;
    const useYn = form.use_yn.value;

    if (!parentItemId || !versionCode) {
        alert("완제품과 버전 코드는 필수입니다.");
        return;
    }

    // ✅ 자재 구성 정보 수집
    const childItemIds = Array.from(form.querySelectorAll("select[name='child_item_id[]']")).map(e => e.value);
    const seqNos = Array.from(form.querySelectorAll("input[name='seq_no[]']")).map(e => e.value);
    const qtys = Array.from(form.querySelectorAll("input[name='qty[]']")).map(e => e.value);
    const lossRates = Array.from(form.querySelectorAll("input[name='loss_rt[]']")).map(e => e.value);
    const itemPrices = Array.from(form.querySelectorAll("input[name='item_price[]']")).map(e => e.value);
    const remarks = Array.from(form.querySelectorAll("input[name='remark[]']")).map(e => e.value);

    // ✅ 데이터 구조 정리
    const components = childItemIds.map((id, i) => ({
        childItemId: parseInt(id),
        seqNo: seqNos[i] ? parseInt(seqNos[i]) : null,
        qty: parseFloat(qtys[i]),
        lossRt: parseFloat(lossRates[i]) || 0,
        itemPrice: parseFloat(itemPrices[i]) || 0,
        remark: remarks[i] || ''
    }));

    // ✅ 최종 전송 데이터
    const payload = {
        parentItemId: parseInt(parentItemId),
        versionCode,
        description,
        useYn,
        components
    };

    // ✅ 서버로 POST 전송
    fetch("/api/bom", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
    })
        .then(async (res) => {
            if (!res.ok) {
                // ✅ 응답 타입이 JSON인지 먼저 확인
                const contentType = res.headers.get("content-type");
                if (contentType && contentType.includes("application/json")) {
                    const data = await res.json();
                    throw new Error(data.error || "알 수 없는 오류 발생");
                } else {
                    const text = await res.text();
                    throw new Error(text || "등록 실패");
                }
                return res.json(); // 성공 시만 JSON 파싱
            }
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
