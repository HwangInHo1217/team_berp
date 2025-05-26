// File: /Team_BERP/src/main/java/com/team/berp/domain/ProdOrder.java
package com.team.berp.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "prod_order")
public class ProdOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer prodOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProdPlan plan;

    @Column(nullable = false)
    private Integer issuedQty;

    // getters and setters
    public Integer getProdOrderId() {
        return prodOrderId;
    }

    public void setProdOrderId(Integer prodOrderId) {
        this.prodOrderId = prodOrderId;
    }

    public ProdPlan getPlan() {
        return plan;
    }

    public void setPlan(ProdPlan plan) {
        this.plan = plan;
    }

    public Integer getIssuedQty() {
        return issuedQty;
    }

    public void setIssuedQty(Integer issuedQty) {
        this.issuedQty = issuedQty;
    }
}