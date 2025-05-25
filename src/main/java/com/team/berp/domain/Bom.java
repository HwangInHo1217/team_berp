// File: /Team_BERP/src/main/java/com/team/berp/domain/Bom.java
package com.team.berp.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "bom")
public class Bom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer bomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id", nullable = false)
    private Item parentItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_item_id", nullable = false)
    private Item childItem;

    @Column(nullable = false)
    private Integer qty;

    // getters and setters
    public Integer getBomId() {
        return bomId;
    }

    public void setBomId(Integer bomId) {
        this.bomId = bomId;
    }

    public Item getParentItem() {
        return parentItem;
    }

    public void setParentItem(Item parentItem) {
        this.parentItem = parentItem;
    }

    public Item getChildItem() {
        return childItem;
    }

    public void setChildItem(Item childItem) {
        this.childItem = childItem;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
}

