package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "warehouse")
@Getter
@Setter
@NoArgsConstructor
public class Warehouse {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id")
    private Long Id;
    
    @Column(name = "warehouse_code", unique = true, length = 20)
    private String warehouseCode;
    
    @Column(name = "warehouse_name", nullable = false, length = 100)
    private String warehouseName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "warehouse_type", nullable = false)
    private WarehouseType warehouseType;
    
    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn = "Y";
    
    @Column(columnDefinition = "TEXT")
    private String description;
}