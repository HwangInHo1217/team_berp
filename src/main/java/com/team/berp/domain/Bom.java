// File: /Team_BERP/src/main/java/com/team/berp/domain/Bom.java
package com.team.berp.domain;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bom")
@Data
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

    @Column(nullable = false)
    private Integer qty;
    
    // 버전 정보 (연관관계 주인)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_version_id") // 연관관계 주인
    private BomVersion bomVersion;
    
    @Column(name = "seq_no")
    private Integer seqNo;

    @Column(name = "loss_rt", precision = 5, scale = 2)
    private BigDecimal lossRt;

    @Column(name = "item_price")
    private Integer itemPrice;

    @Column(length = 100)
    private String remark;
    
    
    public Bom(Item parent, Item child, int qty) {
    	this.parentItem=parent;
    	this.childItem=child;
    	this.qty=qty;
	}
   
   
}

