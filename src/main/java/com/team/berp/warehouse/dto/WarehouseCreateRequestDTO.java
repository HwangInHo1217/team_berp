package com.team.berp.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor //매개변수 없는 생성자
@AllArgsConstructor //모든 필드를 인자로 받는 생성자 (테스트나 특정 상황에서 유용)
public class WarehouseCreateRequestDTO {
	private String warehouseCode;
	private String warehouseName; // 변경: warehouse_name → warehouseName
    private String warehouseType; // 변경: warehouse_type → warehouseType
    private String useYn; // 변경: use_yn → useYn
    private String description;

    
    
    // Lombok @Data 어노테이션이 다음을 자동으로 생성해줌:
    // - 모든 필드에 대한 Getter (getWarehouse_name(), ...)
    // - 모든 non-final 필드에 대한 Setter (setWarehouse_name(...), ...)
    // - toString() 메소드
    // - equals() 및 hashCode() 메소드
    // - 모든 final 또는 @NonNull 필드에 대한 생성자 (@RequiredArgsConstructor)
    //   (여기서는 final 이나 @NonNull 필드가 없으므로, @NoArgsConstructor와 함께 기본 생성자를 보장)
	
}
