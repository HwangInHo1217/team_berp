// File: /Team_BERP/src/main/java/com/team/berp/domain/Mrp.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mrp")
@Getter
@Setter
public class Mrp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer mrp_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProdPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private Integer required_qty;
}
