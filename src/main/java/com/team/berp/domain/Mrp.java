// File: /Team_BERP/src/main/java/com/team/berp/domain/Mrp.java
package com.team.berp.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "mrp")
public class Mrp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer mrpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProdPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private Integer requiredQty;

    // getters and setters
    public Integer getMrpId() {
        return mrpId;
    }

    public void setMrpId(Integer mrpId) {
        this.mrpId = mrpId;
    }

    public ProdPlan getPlan() {
        return plan;
    }

    public void setPlan(ProdPlan plan) {
        this.plan = plan;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Integer getRequiredQty() {
        return requiredQty;
    }

    public void setRequiredQty(Integer requiredQty) {
        this.requiredQty = requiredQty;
    }
}
