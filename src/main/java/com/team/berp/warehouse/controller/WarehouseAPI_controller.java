package com.team.berp.warehouse.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team.berp.warehouse.domain.WarehouseTypeEnum;
import com.team.berp.warehouse.domain.Warehouse_entity;
import com.team.berp.warehouse.dto.WarehouseCreateRequestDTO;
import com.team.berp.warehouse.dto.WarehouseResponseDTO;
import com.team.berp.warehouse.repository.Warehouse_repository;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseAPI_controller {

    private static final Logger log = LoggerFactory.getLogger(WarehouseAPI_controller.class);
    
    @Autowired
    private Warehouse_repository warehouseRepo;

    // 모든 창고 조회 (useYn 파라미터로 필터링)
    @GetMapping
    public ResponseEntity<List<WarehouseResponseDTO>> getAllWarehouses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String warehouseType,
            @RequestParam(required = false) String useYn) {
        
        try {
            List<Warehouse_entity> warehouses;
            
            // 키워드, 타입, 사용여부에 따른 검색 분기 처리
            if (keyword != null && !keyword.trim().isEmpty()) {
                // 키워드 검색 시
                if (useYn != null && !useYn.trim().isEmpty() && !useYn.equals("ALL")) {
                    // 키워드 + 사용여부 필터링
                    warehouses = warehouseRepo.findByWarehouseNameContainingAndUseYn(keyword, useYn);
                } else {
                    // 키워드만 검색
                    warehouses = warehouseRepo.findByWarehouseNameContaining(keyword);
                }
            } else if (warehouseType != null && !warehouseType.trim().isEmpty()) {
                // 타입으로 검색
                WarehouseTypeEnum type = WarehouseTypeEnum.valueOf(warehouseType.toUpperCase());
                
                if (useYn != null && !useYn.trim().isEmpty() && !useYn.equals("ALL")) {
                    // 타입 + 사용여부 필터링
                    warehouses = warehouseRepo.findByWarehouseTypeAndUseYn(type, useYn);
                } else {
                    // 타입만 검색
                    warehouses = warehouseRepo.findByWarehouseType(type);
                }
            } else if (useYn != null && !useYn.trim().isEmpty() && !useYn.equals("ALL")) {
                // 사용여부로만 필터링
                warehouses = warehouseRepo.findByUseYn(useYn);
            } else {
                // 필터 없음 - 모든 창고 조회
                warehouses = warehouseRepo.findAll();
            }
            
            // 엔티티 목록을 DTO 목록으로 변환
            List<WarehouseResponseDTO> responseList = warehouses.stream()
                .map(warehouse -> new WarehouseResponseDTO(warehouse))
                .collect(Collectors.toList());
            
            if (responseList.isEmpty()) {
                return ResponseEntity.noContent().build(); // 204 No Content
            }
            
            return ResponseEntity.ok(responseList);
            
        } catch (Exception e) {
            log.error("창고 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 창고 등록
    @PostMapping
    public ResponseEntity<WarehouseResponseDTO> createWarehouse(@RequestBody WarehouseCreateRequestDTO reqDTO) {
        try {
            log.info("창고 등록 요청: {}", reqDTO.getWarehouseName());
            
            // DTO -> Entity 변환
            Warehouse_entity warehouse = new Warehouse_entity();
            warehouse.setWarehouseName(reqDTO.getWarehouseName());
            
            // 타입 설정
            if (reqDTO.getWarehouseType() != null && !reqDTO.getWarehouseType().isEmpty()) {
                try {
                    warehouse.setWarehouseType(WarehouseTypeEnum.valueOf(reqDTO.getWarehouseType().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.error("잘못된 창고 타입: {}", reqDTO.getWarehouseType());
                    return ResponseEntity.badRequest().body(null);
                }
            } else {
                return ResponseEntity.badRequest().body(null);
            }
            
            // 사용 여부 설정
            warehouse.setUseYn(reqDTO.getUseYn() != null ? reqDTO.getUseYn() : "Y");
            
            // 설명 설정
            warehouse.setDescription(reqDTO.getDescription());
            
            // 저장
            Warehouse_entity savedWarehouse = warehouseRepo.save(warehouse);
            
            // Entity -> DTO 변환 후 응답
            WarehouseResponseDTO responseDTO = new WarehouseResponseDTO(savedWarehouse);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
            
        } catch (Exception e) {
            log.error("창고 등록 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 창고 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponseDTO> getWarehouseById(@PathVariable("id") Integer id) {
        try {
            return warehouseRepo.findById(id)
                .map(warehouse -> new WarehouseResponseDTO(warehouse))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("창고 조회 중 오류 발생, ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 창고 수정
    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponseDTO> updateWarehouse(
            @PathVariable("id") Integer id, 
            @RequestBody WarehouseCreateRequestDTO reqDTO) {
        try {
            // 먼저 창고가 존재하는지 확인
            if (!warehouseRepo.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            
            // 타입 변환
            WarehouseTypeEnum type = null;
            try {
                type = WarehouseTypeEnum.valueOf(reqDTO.getWarehouseType().toUpperCase());
            } catch (Exception e) {
                log.error("잘못된 창고 타입: {}", reqDTO.getWarehouseType());
                return ResponseEntity.badRequest().body(null);
            }
            
            // 직접 업데이트 쿼리 실행
            int updated = warehouseRepo.updateWarehouse(
                id,
                reqDTO.getWarehouseName(),
                type,
                reqDTO.getUseYn(),
                reqDTO.getDescription()
            );
            
            if (updated > 0) {
                // 업데이트 성공 후 최신 데이터 반환
                return warehouseRepo.findById(id)
                    .map(warehouse -> new WarehouseResponseDTO(warehouse))
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        } catch (Exception e) {
            log.error("창고 수정 중 오류 발생, ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 창고 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable("id") Integer id) {
        try {
            if (!warehouseRepo.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            
            warehouseRepo.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("창고 삭제 중 오류 발생, ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // 창고 상태 변경 (사용/미사용)
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> changeWarehouseStatus(
            @PathVariable("id") Integer id,
            @RequestParam("useYn") String useYn) {
        try {
            if (!warehouseRepo.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            
            // 값 검증
            if (!useYn.equals("Y") && !useYn.equals("N")) {
                return ResponseEntity.badRequest().build();
            }
            
            // 상태 변경 쿼리 실행
            int updated = warehouseRepo.changeWarehouseStatus(id, useYn);
            
            if (updated > 0) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            log.error("창고 상태 변경 중 오류 발생, ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}