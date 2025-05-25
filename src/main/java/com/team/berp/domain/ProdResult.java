// File: /Team_BERP/src/main/java/com/team/berp/domain/ProdResult.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "prod_result")
@Getter
@Setter
public class ProdResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer result_id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prod_order_id", nullable = false)
    private ProdOrder prod_order;

    @Column(nullable = false)
    private Integer result_qty;
}
