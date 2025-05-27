package com.team.berp.place.dto;

import lombok.Data;

import java.util.List;

@Data
public class PlaceDTO {
    private Long companyId; // 거래처 ID
    private String orderType; // 주문 유형 (CUSTOMER / SUPPLIER)
    private String orderDate; // 발주일자
    private String comment; // 비고 (선택사항)

    private List<OrderLineItemDTO> lineItems;

    @Data
    public static class OrderLineItemDTO {
        private Long itemId;       // 품목 ID
        private String itemCode;   // 품목 코드 (출력용, 저장은 X)
        private String itemName;   // 품목 이름 (출력용, 저장은 X)
        private String unit;       // 단위 (출력용, 저장은 X)
        private Integer quantity;  // 수량
        private Integer unitPrice; // 단가
    }

    // ↓ 출력용으로 쓸 수 있는 추가 필드들 (선택)
    private String employeeName;    // 담당자 이름
    private String employeeTel;     // 담당자 번호
    private String employeeEmail;   // 담당자 이메일
}
