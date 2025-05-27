// File: /Team_BERP/src/main/java/com/team/berp/domain/Mrp.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Entity
@Table(name = "mrp")
public class Mrp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mrpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProdPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private Integer requiredQty;

   
}
