package com.team.berp.place.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

import com.team.berp.item.dto.ItemListViewResponse;

@Data
@Setter
@Getter
public class PlaceDTO {
    private Long companyId; // 거래처 ID
    private Long orderId; //주문서 ID
    private String orderType; // 주문 유형 (CUSTOMER / SUPPLIER)
    private String orderDate; // 발주일자
    private String note; // 비고 (선택사항)
    private Integer orderQty;  // 총 품목 개수
    private List<OrderLineItemDTO> lineItems;
    private String orderNum;


    @Data
    public static class OrderLineItemDTO {
        private Long orderLineItemId;
        private Long itemId;
        private String itemCode;
        private String itemName;
        private Long unitQty;        // 개별 수량
        private String unit;
        private Long unitPrice;

        private Long orderQty;       // 총 주문 수량 (필요시)
        private Long amount;         // 해당 품목 총 금액 (unitPrice * unitQty)
        private Long unitPriceAll;   // 단가 * 수량 (중복으로 필요없다면 생략 가능)

        private ItemListViewResponse item;
    }

    
    // ↓ 출력용으로 쓸 수 있는 추가 필드들 (선택)
    private Long employeeId;
    private String employeeName;    // 담당자 이름
    private String employeeTel;     // 담당자 번호
    private String employeeEmail;   // 담당자 이메일
    
}
 