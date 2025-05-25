// File: /Team_BERP/src/main/java/com/team/berp/domain/ProdPlan.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "prod_plan")
@Getter
@Setter
public class ProdPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer plan_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private Integer plan_qty;
}