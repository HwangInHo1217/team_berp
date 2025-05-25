// File: /Team_BERP/src/main/java/com/team/berp/domain/Bom.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bom")
@Getter
@Setter
public class Bom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer bom_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id", nullable = false)
    private Item parent_item_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_item_id", nullable = false)
    private Item child_item_id;

    @Column(nullable = false)
    private Integer qty;
}

