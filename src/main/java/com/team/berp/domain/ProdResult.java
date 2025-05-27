// File: /Team_BERP/src/main/java/com/team/berp/domain/ProdResult.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Entity
@Table(name = "prod_result")
public class ProdResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resultId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prod_order_id", nullable = false)
    private ProdOrder prodOrder;

    @Column(nullable = false)
    private Integer resultQty;


}
