package com.team.berp.warehouse.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "warehouse")
public class Warehouse_entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id")
    private Integer warehouseId;  
    
    @Column(name = "warehouse_code", length = 20, unique = true)
    private String warehouseCode;
    

    @Column(name = "warehouse_name", nullable = false, length = 100)
    private String warehouseName;  

    @Enumerated(EnumType.STRING)
    @Column(name = "warehouse_type", nullable = false)
    private WarehouseTypeEnum warehouseType;  

    @Column(name = "use_yn", nullable = false, length = 1, columnDefinition = "char(1) default 'Y'")
    private String useYn = "Y";  

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    
}