package com.team.berp.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * WarehouseCreateRequestDTO
 *
 * 창고 등록 및 수정 시 클라이언트로부터 전달받는 요청 데이터 DTO.
 * 주로 POST/PUT 요청의 @RequestBody 파라미터로 사용됨.
 * 
 * - warehouseCode: 창고 코드 (예: RWWH0001)
 * - warehouseName: 창고 이름
 * - warehouseType: 창고 유형 (RAW, PRODUCT 등)
 * - useYn: 사용 여부 (Y/N)
 * - description: 설명
 *
 * Lombok 어노테이션으로 getter/setter 및 생성자 자동 생성됨.
 */
@Getter  // 모든 필드에 대해 Getter 메서드 자동 생성
@Setter  // 모든 필드에 대해 Setter 메서드 자동 생성
@NoArgsConstructor  // 파라미터 없는 기본 생성자 자동 생성
@AllArgsConstructor // 모든 필드를 파라미터로 받는 생성자 자동 생성
public class WarehouseCreateRequestDTO {

    /** 창고 코드 (예: RWWH0001, PDWH0002 등) */
	private String warehouseCode;

    /** 창고 이름 (예: 자재창고 A, 완제품창고 B) */
    private String warehouseName;

    /** 창고 유형 (RAW, PRODUCT 등 문자열) */
    private String warehouseType;

    /** 사용 여부 ("Y" or "N") */
    private String useYn;

    /** 창고에 대한 설명 (선택 입력) */
    private String description;

    // ⚠️ 주의: @Data 어노테이션은 포함되어 있지 않지만, @Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor 조합으로 대부분 커버됨
    // 만약 toString(), equals(), hashCode()가 필요하다면 @Data 추가 고려
}
