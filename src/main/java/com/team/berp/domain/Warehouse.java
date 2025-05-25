// File: /Team_BERP/src/main/java/com/team/berp/domain/Warehouse.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "warehouse")
@Getter
@Setter
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer warehouse_id;

    @Column(nullable = false, length = 100)
    private String warehouse_name;

    public enum WarehouseType { RAW, PRODUCT }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarehouseType warehouse_type;

    @Column(nullable = false, length = 1)
    private String use_yn = "Y";
}