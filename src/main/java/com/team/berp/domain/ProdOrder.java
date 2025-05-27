// File: /Team_BERP/src/main/java/com/team/berp/domain/ProdOrder.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Entity
@Table(name = "prod_order")
public class ProdOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long prodOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProdPlan plan;

    @Column(nullable = false)
    private Integer issuedQty;

 
}