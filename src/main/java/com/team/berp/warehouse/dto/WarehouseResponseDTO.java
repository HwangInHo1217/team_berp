package com.team.berp.warehouse.dto;

import com.team.berp.warehouse.domain.WarehouseTypeEnum;
import com.team.berp.warehouse.domain.Warehouse_entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor //기본생성자
@AllArgsConstructor //모든 필드 생성자(선택적)
public class WarehouseResponseDTO {
	
	private Integer warehouseId;
	private String warehouseName;
	private WarehouseTypeEnum warehouseType;
	private String useYn;
	private String description;
	
	// 엔티티 객체로부터 DTO를 생성하는 생성자는 유지하는 것이 편리함
	public WarehouseResponseDTO(Warehouse_entity warehouse) {
		this.warehouseId = warehouse.getWarehouseId();
		this.warehouseName = warehouse.getWarehouseName();
        this.warehouseType = warehouse.getWarehouseType();
        this.useYn = warehouse.getUseYn();
        this.description = warehouse.getDescription();
	}
	
	// Lombok @Getter 어노테이션이 다음을 자동으로 생성해줌:
    // - 모든 필드에 대한 Getter (getWarehouse_id(), ...)
    // Setter는 없으므로, 이 DTO는 생성 후에는 불변(immutable) 객체처럼 사용될 수 있다 (필드 자체가 final은 아니지만)
	
}
