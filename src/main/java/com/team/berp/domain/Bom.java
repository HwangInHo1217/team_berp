package com.team.berp.domain;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bom")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id", nullable = false)
    private Item parentItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_item_id", nullable = false)
    private Item childItem;

    /** 소요 수량 */
    @Column(nullable = false)
    private Integer qty;

    /** BOM 버전 (연관관계 주인) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_version_id")
    private BomVersion bomVersion;

    @Column(name = "seq_no")
    private Integer seqNo;

    @Column(name = "loss_rt", precision = 5, scale = 2)
    private BigDecimal lossRt;

    @Column(name = "item_price")
    private Integer itemPrice;

    @Column(length = 100)
    private String remark;

    /**
     * 부모·자식 품목만 지정하고 qty 세팅 시 사용
     */
    public Bom(Item parent, Item child, int qty) {
        this.parentItem = parent;
        this.childItem  = child;
        this.qty        = qty;
    }

}
