package com.team.berp.warehouse.service;


import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.team.berp.warehouse.domain.WarehouseTypeEnum;
import com.team.berp.warehouse.domain.Warehouse_entity;
import com.team.berp.warehouse.dto.WarehouseCreateRequestDTO;
import com.team.berp.warehouse.dto.WarehouseResponseDTO;
import com.team.berp.warehouse.repository.Warehouse_repository;


//실제 생성 로직 담당
@Service
public class Warehouse_service {
	
	private final Warehouse_repository warehouse_repo; 
	
	@Autowired
	public Warehouse_service(Warehouse_repository warehouse_repo) {
	    this.warehouse_repo = warehouse_repo;
	}
	
	@Transactional //데이터 변경 작업이기 때문에 트랜잭션 처리
	public WarehouseResponseDTO create_warehouse(WarehouseCreateRequestDTO reqDTO) {
		Warehouse_entity warehouse = new Warehouse_entity();
		warehouse.setWarehouseName(reqDTO.getWarehouseName());
		warehouse.setDescription(reqDTO.getDescription());
		
		//DTO에서 받은 문자열 타입을 Enum 타입으로 변환 (대소문자 구분 없이)
		if(StringUtils.hasText(reqDTO.getWarehouseType())) {
			try {
				warehouse.setWarehouseType(WarehouseTypeEnum.valueOf(reqDTO.getWarehouseType().toUpperCase()));
			} catch (IllegalArgumentException e) {
				throw new IllegalAccessError("유효하지 않은 창고 유형입니다." + reqDTO.getWarehouseType());
			}
		}else {
			throw new IllegalArgumentException("창고 유형은 필수입니다.");
		}
		
		 // use_Yn은 DTO 값이 있으면 사용, 없거나 비어있으면 엔티티의 기본값('Y') 사용
		if(StringUtils.hasText(reqDTO.getUseYn())) {
			warehouse.setUseYn(reqDTO.getUseYn());
		}else {
			warehouse.setUseYn("Y"); 	//기본값 설정
		}
		// 엔티티 클래스에서 use_Yn 필드에 기본값 "Y"가 설정되어 있으므로
        // DTO에서 null이나 빈 문자열이 넘어오면 엔티티의 기본값이 사용됨
        // 명시적으로 'N'이 아니면 'Y'로 설정하는 로직을 추가할 수도 있음
        // 예: warehouse.setUse_Yn("N".equalsIgnoreCase(requestDto.getUse_Yn()) ? "N" : "Y");
		
		Warehouse_entity saveWarehouse = warehouse_repo.save(warehouse);
		return new WarehouseResponseDTO(saveWarehouse);
	}

/*
 * 모든 활성(사용 중인) 창고 목록을 조회
 * @return 활성 창고 DTO목록
 */
	//활성화된 창고만 조회 (useYn = 'Y')
	@Transactional(readOnly = true) //조회 전용 트랜잭션, 성능에 약간 유리
	public List<WarehouseResponseDTO> getAllActiveWarehouses(){
		//Warehouse_repository 사용해서 use_Yn이 "Y"인 창고 목록을 가져오기
		List<Warehouse_entity> activeWarehouses = warehouse_repo.findAllByUseYn("Y");
		
		//가져온 엔티티 목록을 DTO목록으로 변환함
		return activeWarehouses.stream() // Stream API 사용
        //.map(wh_entity -> new WarehouseResponseDTO(wh_entity)) // 각 엔티티를 DTO로 매핑
		.map(WarehouseResponseDTO::new) // 생성자 참조 방식으로 간결하게 작성			
        .collect(Collectors.toList()); // 스트림의 결과를 List로 수집! 이 부분이 수정됨.
	}
	
	//사용 여부에 따라 창고 조회 (useYn 파라미터가 null이면 모든 창고 조회)
    @Transactional(readOnly = true)
    public List<WarehouseResponseDTO> getWarehousesByUseYn(String useYn) {
        List<Warehouse_entity> warehouses = warehouse_repo.findAllByUseYnOrAll(useYn);
        
        return warehouses.stream()
            .map(WarehouseResponseDTO::new)
            .collect(Collectors.toList());
    }
	
	
	//여러 조건으로 창고 검색 (useYn 포함)
	@Transactional(readOnly = true)
	public List<WarehouseResponseDTO> searchWarehouses(
			String keyword, String typeStr, String useYn, String sortBy, String sortDirection){
		WarehouseTypeEnum type = null;
		if(StringUtils.hasText(typeStr)) {
			try {
				type = WarehouseTypeEnum.valueOf(typeStr.toUpperCase());
			} catch (IllegalArgumentException  e) {
				// 유효하지 않은 타입은 무시
			}
		}
		
		Sort sort = Sort.by(
				Sort.Direction.fromString(sortDirection != null ? sortDirection : "asc"),
				StringUtils.hasText(sortBy) ? sortBy : "warehouseId"
		);
		
		// useYn 파라미터가 "ALL"이면 null로 설정하여 모든 창고 조회
        String useYnParam = "ALL".equalsIgnoreCase(useYn) ? null : useYn;
		
		List<Warehouse_entity> warehouses = warehouse_repo.searchWarehouses(keyword, type, "Y", sort);
	    
		
		return warehouses.stream()
				.map(WarehouseResponseDTO::new)
				.collect(Collectors.toList());
		
	}
	
	//창고 코드 자동으로 생성하는 로직 만들기
	private String generateWarehouseCode(WarehouseTypeEnum type) {
	    String prefix = type == WarehouseTypeEnum.RAW ? "RWWH" : "PDWH";
	    int count = (int) warehouse_repo.countByWarehouseType(type);
	    return String.format("%s%04d", prefix, count + 1);
	}

	
	
	
	// 단일 창고 조회 메소드 추가
	public WarehouseResponseDTO getWarehouseById(Integer id) {
	    return warehouse_repo.findById(id)
	        .map(WarehouseResponseDTO::new)
	        .orElseThrow(() -> new RuntimeException("창고를 찾을 수 없습니다. ID: " + id));
	}
	
	// 창고 수정 메소드 추가
	@Transactional
	public WarehouseResponseDTO updateWarehouse(Integer id, WarehouseCreateRequestDTO reqDTO) {
	    Warehouse_entity warehouse = warehouse_repo.findById(id)
	        .orElseThrow(() -> new RuntimeException("창고를 찾을 수 없습니다. ID: " + id));
	    
	    if (StringUtils.hasText(reqDTO.getWarehouseName())) {
	        warehouse.setWarehouseName(reqDTO.getWarehouseName());
	    }
	    
	    if (StringUtils.hasText(reqDTO.getWarehouseType())) {
	        try {
	            warehouse.setWarehouseType(WarehouseTypeEnum.valueOf(reqDTO.getWarehouseType().toUpperCase()));
	        } catch (IllegalArgumentException e) {
	            throw new IllegalArgumentException("유효하지 않은 창고 유형입니다: " + reqDTO.getWarehouseType());
	        }
	    }
	    
	    if (StringUtils.hasText(reqDTO.getUseYn())) {
	        warehouse.setUseYn(reqDTO.getUseYn());
	    }
	    
	    if (StringUtils.hasText(reqDTO.getDescription())) {
	        warehouse.setDescription(reqDTO.getDescription());
	    }
	    
	    Warehouse_entity updatedWarehouse = warehouse_repo.save(warehouse);
	    return new WarehouseResponseDTO(updatedWarehouse);
	}
	
	// 창고 삭제 메소드 추가
	@Transactional
	public void deleteWarehouse(Integer id) {
	    if (!warehouse_repo.existsById(id)) {
	        throw new RuntimeException("창고를 찾을 수 없습니다. ID: " + id);
	    }
	    warehouse_repo.deleteById(id);
	
	}
	
	
	
}
