// File: src/main/java/com/team/berp/mrp/dto/ExtendedBomListViewResponse.java
package com.team.berp.mrp.dto;

import com.team.berp.bom.dto.BomListViewResponse;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExtendedBomListViewResponse extends BomListViewResponse {

    // 실제 내부에 담을 고급 컴포넌트 리스트
    private List<ExtendedComponent> extendedComponents;

    public ExtendedBomListViewResponse(
            String parentCode,
            String parentName,
            List<ExtendedComponent> extendedComponents
    ) {
        // super.components 은 쓰지 않으니 null 로 초기화
        super(parentCode, parentName, null);
        this.extendedComponents = extendedComponents;
    }

    /**
     * 오버라이드하면서 리턴 타입은 부모와 동일(List<Component>).
     * 내부에 담긴 List<ExtendedComponent> 를 ArrayList 복사해서 꺼내줍니다.
     */
    @Override
    public List<Component> getComponents() {
        return new ArrayList<>(extendedComponents);
    }

    /**
     * ExtendedComponent 는 기존 Component 를 상속하고,
     * 추가 필드를 모두 갖습니다.
     */
    @Data
    public static class ExtendedComponent extends BomListViewResponse.Component {
        private int    totalQty;           // 총 필요 수량 (부모 부족수량 * perParentQty)
        private int    stockQty;           // 현재고
        private int    shortageQty;        // 부족수량
        private int    safetyStock;        // 안전재고
        private int    purchaseQty;        // 발주필요수량
        private int    purchaseLeadTime;   // 구매 리드타임(일)
        private String expectedDate;       // 예상입고일 (YYYY-MM-DD)

        public ExtendedComponent(
                BomListViewResponse.Component base,
                int totalQty,
                int stockQty,
                int shortageQty,
                int safetyStock,
                int purchaseQty,
                int purchaseLeadTime,
                String expectedDate
        ) {
            super(
                base.getChildCode(),
                base.getChildName(),
                base.getQty(),
                base.getSpec(),
                base.getUnit(),
                base.getSeqNo(),
                base.getLossRate(),
                base.getUnitPrice(),
                base.getRemark()
            );
            this.totalQty          = totalQty;
            this.stockQty          = stockQty;
            this.shortageQty       = shortageQty;
            this.safetyStock       = safetyStock;
            this.purchaseQty       = purchaseQty;
            this.purchaseLeadTime  = purchaseLeadTime;
            this.expectedDate      = expectedDate;
        }

        // 👉 JSON으로 내려줄 때, 자바 필드명이 childCode / childName 인데
        // 프론트에서는 subItemCode, subItemName 으로 접근하기 때문에
        // 여기에 getter를 하나 더 만들어 줍니다.
        public String getSubItemCode() {
            return getChildCode();
        }
        public String getSubItemName() {
            return getChildName();
        }
    }
}
