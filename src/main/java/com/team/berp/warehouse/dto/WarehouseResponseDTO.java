package com.team.berp.warehouse.dto;

import com.team.berp.domain.Warehouse;
import com.team.berp.domain.Warehouse.WarehouseType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * WarehouseResponseDTO
 * 
 * 창고 정보를 클라이언트(화면 또는 API 응답)로 전달할 때 사용하는 응답 DTO 클래스.
 * 주로 GET 요청 응답 바디에 사용됨.
 * 
 * - Entity(Warehouse) → DTO 로의 변환 책임을 가짐
 */
@Getter                      // 모든 필드에 대한 Getter 메서드 자동 생성
@NoArgsConstructor          // 기본 생성자 자동 생성
@AllArgsConstructor         // 모든 필드를 매개변수로 받는 생성자 자동 생성
public class WarehouseResponseDTO {

    /** 창고 ID (기본 키) */
	private Integer warehouseId;

    /** 창고 코드 (예: RWWH0001, PDWH0001 등) */
	private String warehouseCode;

    /** 창고 이름 (예: 자재창고 A) */
	private String warehouseName;

    /** 창고 유형 (RAW, PRODUCT 등) */
	private WarehouseType warehouseType;

    /** 사용 여부 ("Y" or "N") */
	private String useYn;

    /** 설명 (선택 입력 가능) */
	private String description;

    /**
     * Warehouse 엔티티로부터 DTO 객체를 생성하는 생성자
     * 
     * @param warehouse Entity 객체 (DB로부터 조회된 창고 데이터)
     */
	public WarehouseResponseDTO(Warehouse warehouse) {
		this.warehouseId = warehouse.getWarehouseId();
		this.warehouseCode = warehouse.getWarehouseCode();
		this.warehouseName = warehouse.getWarehouseName();
        this.warehouseType = warehouse.getWarehouseType();
        this.useYn = warehouse.getUseYn();
        this.description = warehouse.getDescription();
	}

    /**
     * 정적 팩토리 메서드
     * - 단건 또는 스트림(map)에서 자주 사용됨
     * 
     * @param warehouse Entity 객체
     * @return WarehouseResponseDTO 객체
     */
	public static WarehouseResponseDTO from(Warehouse warehouse) {
		return new WarehouseResponseDTO(warehouse);
	}
}
