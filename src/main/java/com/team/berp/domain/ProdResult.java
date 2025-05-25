// File: /Team_BERP/src/main/java/com/team/berp/domain/ProdResult.java
package com.team.berp.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "prod_result")
public class ProdResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer resultId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prod_order_id", nullable = false)
    private ProdOrder prodOrder;

    @Column(nullable = false)
    private Integer resultQty;

    // getters and setters
    public Integer getResultId() {
        return resultId;
    }

    public void setResultId(Integer resultId) {
        this.resultId = resultId;
    }

    public ProdOrder getProdOrder() {
        return prodOrder;
    }

    public void setProdOrder(ProdOrder prodOrder) {
        this.prodOrder = prodOrder;
    }

    public Integer getResultQty() {
        return resultQty;
    }

    public void setResultQty(Integer resultQty) {
        this.resultQty = resultQty;
    }
}
