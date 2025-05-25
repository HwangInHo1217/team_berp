// File: /Team_BERP/src/main/java/com/team/berp/domain/ProdOrder.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "prod_order")
@Getter
@Setter
public class ProdOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer prod_order_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProdPlan plan;

    @Column(nullable = false)
    private Integer issued_qty;
}