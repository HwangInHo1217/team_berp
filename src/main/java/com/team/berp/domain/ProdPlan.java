// File: /Team_BERP/src/main/java/com/team/berp/domain/ProdPlan.java
package com.team.berp.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "prod_plan")
public class ProdPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private Integer planQty;

    // getters and setters
    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Integer getPlanQty() {
        return planQty;
    }

    public void setPlanQty(Integer planQty) {
        this.planQty = planQty;
    }
}