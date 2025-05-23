package com.team.berp.warehouse.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;

import com.team.berp.domain.Warehouse;
import com.team.berp.domain.Warehouse.WarehouseType;
import com.team.berp.warehouse.dto.WarehouseCreateRequestDTO;
import com.team.berp.warehouse.dto.WarehouseResponseDTO;
import com.team.berp.warehouse.repository.Warehouse_repository;

/**
 * 창고 관리 비즈니스 로직 서비스
 * 창고의 CRUD 작업, 검색, 필터링 등의 핵심 비즈니스 로직을 처리합니다.
 */
@Service
public class Warehouse_service {
    
    private static final Logger log = LoggerFactory.getLogger(Warehouse_service.class);
    private final Warehouse_repository repo;
    
    @Autowired
    public Warehouse_service(Warehouse_repository repo) {
        this.repo = repo;
    }
    
    /**
     * 창고 유형에 따른 고유 코드 생성
     * 자재창고: RWWH0001, 완제품창고: PDWH0001 형식
     */
    @Transactional(readOnly = true)
    public String generateWhsCode(WarehouseType type) {
        String prefix = type == WarehouseType.RAW ? "RWWH" : "PDWH";
        long count = repo.countByWarehouseType(type);
        return String.format("%s%04d", prefix, count + 1);
    }
    
    /**
     * 창고 코드 중복 검사
     * 수정 시에는 자기 자신은 제외하고 검사
     */
    @Transactional(readOnly = true)
    public Map<String, String> checkWhsCodeDuplicate(String code, Integer excludeId) {
        Optional<Warehouse> existing = repo.findByWarehouseCode(code);
        
        if (existing.isPresent()) {
            // 수정 중인데 자기 자신의 코드인 경우는 OK
            if (excludeId != null && existing.get().getWarehouseId().equals(excludeId)) {
                return Map.of("status", "ok", "message", "사용 가능한 창고 코드입니다.");
            }
            return Map.of("status", "duplicate", "message", "이미 사용 중인 창고 코드입니다.");
        }
        return Map.of("status", "ok", "message", "사용 가능한 창고 코드입니다.");
    }
    
    /**
     * 새 창고 등록
     * 코드가 제공되지 않으면 자동 생성
     */
    @Transactional
    public WarehouseResponseDTO createWhs(WarehouseCreateRequestDTO dto) {
        log.debug("창고 등록 시작: {}", dto.getWarehouseName());
        
        Warehouse whs = buildWhs(dto);
        
        // 창고 코드 처리
        if (StringUtils.hasText(dto.getWarehouseCode())) {
            validateWhsCodeNotDuplicate(dto.getWarehouseCode(), null);
            whs.setWarehouseCode(dto.getWarehouseCode());
        } else {
            whs.setWarehouseCode(generateWhsCode(whs.getWarehouseType()));
        }
        
        Warehouse saved = repo.save(whs);
        log.info("창고 등록 완료: {}", saved.getWarehouseCode());
        return new WarehouseResponseDTO(saved);
    }
    
    /**
     * 창고 정보 수정
     * 코드 변경 시에만 중복 검사 수행
     */
    @Transactional
    public WarehouseResponseDTO updateWhs(Integer id, WarehouseCreateRequestDTO dto) {
        log.debug("창고 수정 시작, ID: {}", id);
        
        Warehouse whs = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("수정할 창고를 찾을 수 없습니다. ID: " + id));
        
        updateWhsFields(whs, dto, id);
        
        Warehouse updated = repo.save(whs);
        log.info("창고 수정 완료: {}", updated.getWarehouseCode());
        return new WarehouseResponseDTO(updated);
    }
    
    /**
     * 사용여부 필터에 따른 창고 목록 조회
     * ALL 또는 null이면 전체, Y/N이면 해당 상태만 조회
     */
    @Transactional(readOnly = true)
    public List<WarehouseResponseDTO> getWhsByFilter(String useYnFilter) {
        log.debug("필터로 창고 목록 조회: useYnFilter=[{}]", useYnFilter);
        
        List<Warehouse> whsList;
        
        // ALL이거나 null/빈값이면 전체 데이터 조회
        if (useYnFilter == null || useYnFilter.isEmpty() || "ALL".equals(useYnFilter)) {
            log.debug("전체 데이터 조회");
            whsList = repo.findAllByOrderByWarehouseIdDesc();
        } else {
            log.debug("사용여부 필터 적용: {}", useYnFilter);
            whsList = repo.findByUseYnOrderByWarehouseIdDesc(useYnFilter);
        }
        
        log.debug("조회된 창고 수: {}", whsList.size());
        
        return whsList.stream()
                .map(WarehouseResponseDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * 전체 데이터 대상 키워드 검색
     * 사용여부에 관계없이 모든 데이터에서 검색
     */
    @Transactional(readOnly = true)
    public List<WarehouseResponseDTO> searchWhsFromAll(String keyword, String searchType) {
        log.debug("전체 데이터 대상 검색: keyword={}, searchType={}", keyword, searchType);
        
        List<Warehouse> whsList = switch (searchType) {
            case "code" -> repo.findByWarehouseCodeContainingOrderByWarehouseIdDesc(keyword);
            case "name" -> repo.findByWarehouseNameContainingOrderByWarehouseIdDesc(keyword);
            default -> repo.findByWarehouseNameContainingOrderByWarehouseIdDesc(keyword);
        };
        
        return whsList.stream()
                .map(WarehouseResponseDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * 기존 searchWhs 메소드 (호환성 유지)
     */
    @Transactional(readOnly = true)
    public List<WarehouseResponseDTO> searchWhs(String keyword, String searchType, String useYnFilter) {
        log.debug("검색 실행: keyword={}, searchType={}, useYnFilter={}", keyword, searchType, useYnFilter);
        
        List<Warehouse> whsList = findWhsList(keyword, searchType, useYnFilter);
        return whsList.stream()
                .map(WarehouseResponseDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * ID로 특정 창고 조회
     */
    @Transactional(readOnly = true)
    public WarehouseResponseDTO getWhsById(Integer id) {
        return repo.findById(id)
            .map(WarehouseResponseDTO::new)
            .orElseThrow(() -> new RuntimeException("창고를 찾을 수 없습니다. ID: " + id));
    }
    
    /**
     * 창고 삭제
     */
    @Transactional
    public void deleteWhs(Integer id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("창고를 찾을 수 없습니다. ID: " + id);
        }
        repo.deleteById(id);
    }
    
    // ========== 화면 렌더링 지원 메소드 ==========
    
    /**
     * 화면 렌더링용 데이터 준비
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWhsPageData(String useYn, String keyword, String searchType) {
        List<WarehouseResponseDTO> whsList;
        
        if (StringUtils.hasText(keyword)) {
            whsList = searchWhsFromAll(keyword, searchType);
        } else {
            String filter = (useYn != null) ? useYn : "ALL";
            whsList = getWhsByFilter(filter);
        }
        
        return Map.of(
            "warehouses", whsList,
            "useYnFilter", useYn != null ? useYn : "ALL",
            "keyword", keyword != null ? keyword : "",
            "searchType", searchType != null ? searchType : ""
        );
    }
    
    /**
     * 페이지 에러 처리
     */
    public void handleWhsPageError(Model model, Exception e) {
        model.addAttribute("warehouses", List.of());
        model.addAttribute("error", "창고 목록을 불러오는데 실패했습니다: " + e.getMessage());
    }
    
    // ========== Private 헬퍼 메소드 ==========
    
    /**
     * DTO에서 엔티티 객체 생성
     */
    private Warehouse buildWhs(WarehouseCreateRequestDTO dto) {
        Warehouse whs = new Warehouse();
        whs.setWarehouseName(dto.getWarehouseName());
        whs.setDescription(dto.getDescription());
        whs.setWarehouseType(parseWhsType(dto.getWarehouseType()));
        whs.setUseYn(StringUtils.hasText(dto.getUseYn()) ? dto.getUseYn() : "Y");
        return whs;
    }
    
    /**
     * 문자열을 창고 유형 enum으로 변환
     */
    private WarehouseType parseWhsType(String typeStr) {
        if (!StringUtils.hasText(typeStr)) {
            throw new IllegalArgumentException("창고 유형은 필수입니다.");
        }
        try {
            return WarehouseType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 창고 유형입니다: " + typeStr);
        }
    }
    
    /**
     * 코드 중복 검사 후 예외 발생
     */
    private void validateWhsCodeNotDuplicate(String code, Integer excludeId) {
        Map<String, String> result = checkWhsCodeDuplicate(code, excludeId);
        if ("duplicate".equals(result.get("status"))) {
            throw new IllegalArgumentException(result.get("message"));
        }
    }
    
    /**
     * 기존 창고 엔티티의 필드들 업데이트
     */
    private void updateWhsFields(Warehouse whs, WarehouseCreateRequestDTO dto, Integer id) {
        // 창고 코드 변경 시에만 중복 검사
        if (StringUtils.hasText(dto.getWarehouseCode()) && 
            !dto.getWarehouseCode().equals(whs.getWarehouseCode())) {
            validateWhsCodeNotDuplicate(dto.getWarehouseCode(), id);
            whs.setWarehouseCode(dto.getWarehouseCode());
        }
        
        if (StringUtils.hasText(dto.getWarehouseName())) {
            whs.setWarehouseName(dto.getWarehouseName());
        }
        
        // 창고 유형 변경 시 코드 형식 검증
        if (StringUtils.hasText(dto.getWarehouseType())) {
            WarehouseType newType = parseWhsType(dto.getWarehouseType());
            
            // 창고 유형이 변경되는 경우 코드 형식 검증
            if (!newType.equals(whs.getWarehouseType())) {
                validateWhsCodeFormat(whs.getWarehouseCode(), newType);
            }
            
            whs.setWarehouseType(newType);
        }
        
        if (StringUtils.hasText(dto.getUseYn())) {
            whs.setUseYn(dto.getUseYn());
        }
        whs.setDescription(dto.getDescription());
    }
    
    /**
     * 창고 코드와 유형 일치성 검증
     * @param code 창고 코드
     * @param type 창고 유형
     */
    private void validateWhsCodeFormat(String code, WarehouseType type) {
        String expectedPrefix = type == WarehouseType.RAW ? "RWWH" : "PDWH";
        
        if (!code.startsWith(expectedPrefix)) {
            String typeName = type == WarehouseType.RAW ? "자재창고" : "완제품창고";
            throw new IllegalArgumentException(
                String.format("창고 유형을 %s로 변경하려면 창고 코드가 %s로 시작해야 합니다. 현재 코드: %s", 
                    typeName, expectedPrefix, code)
            );
        }
    }
    
    /**
     * 향상된 검색 로직 (사용여부 필터 포함) - 호환성 유지용
     */
    private List<Warehouse> findWhsList(String keyword, String searchType, String useYnFilter) {
        // 검색어가 있는 경우
        if (StringUtils.hasText(keyword)) {
            return switch (searchType) {
                case "code" -> repo.findByWarehouseCodeContainingOrderByWarehouseIdDesc(keyword);
                case "name" -> repo.findByWarehouseNameContainingOrderByWarehouseIdDesc(keyword);
                default -> repo.findByWarehouseNameContainingOrderByWarehouseIdDesc(keyword);
            };
        }
        
        // 사용여부 필터만 있는 경우
        if (StringUtils.hasText(useYnFilter)) {
            if ("ALL".equals(useYnFilter)) {
                return repo.findAllByOrderByWarehouseIdDesc();
            } else {
                return repo.findByUseYnOrderByWarehouseIdDesc(useYnFilter);
            }
        }
        
        // 기본값: 사용중인 것만
        return repo.findByUseYnOrderByWarehouseIdDesc("Y");
    }
}
