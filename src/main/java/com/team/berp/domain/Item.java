// File: /Team_BERP/src/main/java/com/team/berp/domain/Item.java
package com.team.berp.domain;

import com.team.berp.item.dto.UpdateItemRequestDTO;
import jakarta.persistence.*;
import lombok.*;

@ToString
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="item_id")
    private Long id;              // PK

    @Column(name="item_code", unique=true, updatable=false, length=20)
    private String code;          // 코드

    @Column(name="item_name")
    private String name;          // 품목명

    @Enumerated(EnumType.STRING)
    @Column(name="item_type")
    private ItemType type;        // 자재/완제품

    @Column(name="unit")
    private String unit;          // 단위

    @Column(name="spec")
    private String spec;          // 규격

    @Column(name="use_yn")
    private String use;           // 사용 여부

    // ───────── NEW ─────────
    /** 안전재고 (item 테이블에 safety_stock 컬럼 추가) */
    @Column(name = "safety_stock")
    private Integer safetyStock;

    /** 구매 리드타임 (item 테이블에 purchase_lead_time 컬럼 추가) */
    @Column(name = "purchase_lead_time")
    private Integer purchaseLeadTime;
    // ───────────────────────

    @Builder
    public Item(String code,
                String name,
                ItemType type,
                String unit,
                String spec,
                String use,
                Integer safetyStock,
                Integer purchaseLeadTime) {
        this.code              = code;
        this.name              = name;
        this.type              = type;
        this.unit              = unit;
        this.spec              = spec;
        this.use               = use;
        this.safetyStock       = safetyStock;
        this.purchaseLeadTime  = purchaseLeadTime;
    }

    public void update(UpdateItemRequestDTO dto) {
        this.name             = dto.getName();
        this.type             = ItemType.valueOf(dto.getType());
        this.spec             = dto.getSpec();
        this.unit             = dto.getUnit();
        this.use              = dto.getUse();
        this.safetyStock      = dto.getSafetyStock();
        this.purchaseLeadTime = dto.getPurchaseLeadTime();
    }
}
