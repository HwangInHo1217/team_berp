package com.team.berp.warehouse.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import com.team.berp.domain.Warehouse.WarehouseType;
import com.team.berp.warehouse.dto.WarehouseCreateRequestDTO;
import com.team.berp.warehouse.dto.WarehouseResponseDTO;
import com.team.berp.warehouse.service.Warehouse_service;
import com.team.berp.warehouse.util.ApiUtils;

/**
 * WarehouseAPI_controller
 * 
 * 창고 관리 REST API 컨트롤러 클래스.
 * 등록, 수정, 삭제, 단건 조회, 검색, 페이징 조회, 코드 생성, 중복 확인 등
 * 창고와 관련된 모든 API 요청을 처리함.
 */
@RestController
@RequestMapping("/api/warehouses")
public class WarehouseAPI_controller {

    private static final Logger log = LoggerFactory.getLogger(WarehouseAPI_controller.class);
    private final Warehouse_service whsService;

    @Autowired
    public WarehouseAPI_controller(Warehouse_service whsService) {
        this.whsService = whsService;
    }

    /**
     * [GET] /generate-code
     * 
     * 창고 유형(RAW, PRODUCT 등)에 따라 자동으로 창고 코드를 생성하여 반환
     * 예: RAW → RWWH0001, PRODUCT → PDWH0001
     */
    @GetMapping("/generate-code")
    public ResponseEntity<String> generateWhsCode(@RequestParam("type") String type) {
        return ApiUtils.handle(() -> whsService.generateWhsCode(WarehouseType.valueOf(type.toUpperCase())));
    }

    /**
     * [GET] /check-duplicate
     * 
     * 창고 코드의 중복 여부 확인
     * - 등록 시 중복된 코드가 있는지 검사
     * - 수정 시 본인의 ID는 제외하고 중복 확인 가능 (excludeId 사용)
     */
    @GetMapping("/check-duplicate")
    public ResponseEntity<Map<String, String>> checkWhsCodeDuplicate(
            @RequestParam("warehouseCode") String code,
            @RequestParam(value = "excludeId", required = false) Integer excludeId) {

        return ResponseEntity.ok(whsService.checkWhsCodeDuplicate(code, excludeId));
    }

    /**
     * [POST] /
     * 
     * 새 창고 등록
     * - 필수 값: 창고명, 창고코드, 창고유형, 사용여부
     * - 코드 중복 및 형식 검증 포함
     */
    @PostMapping
    public ResponseEntity<?> createWhs(@RequestBody WarehouseCreateRequestDTO dto) {
        log.info("창고 등록 요청: {}", dto.getWarehouseName());
        return ApiUtils.handleCreate(() -> {
            var result = whsService.createWhs(dto);
            log.info("창고 등록 성공: {}", result.getWarehouseCode());
            return result;
        });
    }

    /**
     * [PUT] /{id}
     * 
     * 기존 창고 정보 수정
     * - ID로 대상 창고 조회 후 수정
     * - 수정 시 코드 중복 검증 및 유형-코드 형식 일치 여부 확인
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWhs(@PathVariable("id") Integer id,
                                       @RequestBody WarehouseCreateRequestDTO dto) {
        log.info("창고 수정 요청, ID: {}", id);
        return ApiUtils.handle(() -> {
            var result = whsService.updateWhs(id, dto);
            log.info("창고 수정 성공: {}", result.getWarehouseCode());
            return result;
        });
    }

    /**
     * [GET] /
     * 
     * 창고 목록 조회 - 페이징 지원
     * - 검색어(keyword)와 검색유형(searchType)이 존재하면 전체 검색
     * - 아니면 사용여부 필터(useYnFilter)에 따라 조건부 조회
     * - page 파라미터로 페이징 처리 (1부터 시작)
     */
    @GetMapping
    public ResponseEntity<?> getAllWhs(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "searchType", required = false) String searchType,
            @RequestParam(name = "useYnFilter", required = false) String useYnFilter,
            @RequestParam(name = "page", required = false, defaultValue = "1") Integer page) {

        return ApiUtils.handle(() ->
            StringUtils.hasText(keyword)
                ? whsService.searchWhsFromAllWithPaging(keyword, searchType, page)
                : whsService.getWhsByFilterWithPaging(useYnFilter, page)
        );
    }

    /**
     * [GET] /all
     * 
     * 창고 목록 조회 - 페이징 없이 전체 조회 (기존 JS에서 사용하던 방식과의 호환용)
     * - 검색어가 있으면 검색, 없으면 필터 기준 전체 목록 반환
     */
    @GetMapping("/all")
    public ResponseEntity<List<WarehouseResponseDTO>> getAllWhsNoPaging(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "searchType", required = false) String searchType,
            @RequestParam(name = "useYnFilter", required = false) String useYnFilter) {

        return ApiUtils.handle(() -> {
            var result = StringUtils.hasText(keyword)
                ? whsService.searchWhsFromAll(keyword, searchType)
                : whsService.getWhsByFilter(useYnFilter);
            return result.isEmpty() ? null : result;
        });
    }

    /**
     * [GET] /{id}
     * 
     * 특정 ID를 가진 창고의 상세 정보 조회
     * - 수정 모달 진입 시 단건 조회에 사용 가능
     */
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponseDTO> getWhsById(@PathVariable("id") Integer id) {
        return ApiUtils.handleGetById(() -> whsService.getWhsById(id));
    }

    /**
     * [DELETE] /{id}
     * 
     * ID에 해당하는 창고 삭제
     * - 단일 삭제만 지원
     * - 다중 삭제는 프론트에서 여러 DELETE 요청을 보내 처리
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWhs(@PathVariable("id") Integer id) {
        return ApiUtils.handleDelete(() -> whsService.deleteWhs(id));
    }
}