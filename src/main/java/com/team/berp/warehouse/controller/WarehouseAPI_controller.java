package com.team.berp.warehouse.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import com.team.berp.domain.Warehouse.WarehouseType;
import com.team.berp.warehouse.dto.WarehouseCreateRequestDTO;
import com.team.berp.warehouse.dto.WarehouseResponseDTO;
import com.team.berp.warehouse.service.Warehouse_service;

/**
 * 창고 관리 REST API 컨트롤러
 * 창고의 등록, 수정, 삭제, 조회 및 검색 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/warehouses")
public class WarehouseAPI_controller {

    private static final Logger log = LoggerFactory.getLogger(WarehouseAPI_controller.class);
    private final Warehouse_service service;

    @Autowired
    public WarehouseAPI_controller(Warehouse_service service) {
        this.service = service;
    }
    
    /**
     * 창고 유형에 따른 자동 코드 생성
     */
    @GetMapping("/generate-code")
    public ResponseEntity<String> generateWhsCode(@RequestParam("type") String type) {
        try {
            String code = service.generateWhsCode(WarehouseType.valueOf(type.toUpperCase()));
            return ResponseEntity.ok(code);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("INVALID_TYPE");
        }
    }
    
    /**
     * 창고 코드 중복 확인
     */
    @GetMapping("/check-duplicate")
    public ResponseEntity<Map<String, String>> checkWhsCodeDuplicate(
            @RequestParam("warehouseCode") String code,
            @RequestParam(value = "excludeId", required = false) Integer excludeId) {
        
        return ResponseEntity.ok(service.checkWhsCodeDuplicate(code, excludeId));
    }
    
    /**
     * 새 창고 등록
     */
    @PostMapping
    public ResponseEntity<?> createWhs(@RequestBody WarehouseCreateRequestDTO dto) {
        log.info("창고 등록 요청: {}", dto.getWarehouseName());
        try {
            WarehouseResponseDTO result = service.createWhs(dto);
            log.info("창고 등록 성공: {}", result.getWarehouseCode());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("창고 등록 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("창고 등록 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 내부 오류로 창고 등록에 실패했습니다."));
        }
    }
    
    /**
     * 창고 정보 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWhs(@PathVariable("id") Integer id, 
                                  @RequestBody WarehouseCreateRequestDTO dto) {
        log.info("창고 수정 요청, ID: {}", id);
        try {
            WarehouseResponseDTO result = service.updateWhs(id, dto);
            log.info("창고 수정 성공: {}", result.getWarehouseCode());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("창고 수정 실패, ID: {}: {}", id, e.getMessage());
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("창고 수정 중 서버 오류, ID: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 내부 오류로 창고 수정에 실패했습니다."));
        }
    }
    
    /**
     * 창고 목록 조회 (검색 및 필터링 지원)
     */
    @GetMapping
    public ResponseEntity<List<WarehouseResponseDTO>> getAllWhs(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "searchType", required = false) String searchType,
            @RequestParam(name = "useYnFilter", required = false) String useYnFilter) {
        try {
            List<WarehouseResponseDTO> result;
            
            if (StringUtils.hasText(keyword)) {
                // 검색어가 있으면 전체 데이터 대상 검색
                result = service.searchWhsFromAll(keyword, searchType);
            } else {
                // 검색어가 없으면 사용여부 필터만 적용
                result = service.getWhsByFilter(useYnFilter);
            }
            
            return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("창고 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * 특정 창고 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponseDTO> getWhsById(@PathVariable("id") Integer id) {
        try {
            WarehouseResponseDTO dto = service.getWhsById(id);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            log.warn("창고 조회 실패 ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("창고 조회 중 서버 오류 ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 창고 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWhs(@PathVariable("id") Integer id) {
        try {
            service.deleteWhs(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.warn("삭제할 창고 없음 ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("창고 삭제 중 서버 오류 ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
