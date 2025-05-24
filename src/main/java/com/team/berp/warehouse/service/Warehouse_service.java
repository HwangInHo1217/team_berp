package com.team.berp.warehouse.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;

import com.team.berp.domain.Warehouse;
import com.team.berp.domain.Warehouse.WarehouseType;
import com.team.berp.warehouse.dto.WarehouseCreateRequestDTO;
import com.team.berp.warehouse.dto.WarehouseResponseDTO;
import com.team.berp.warehouse.repository.Warehouse_repository;

@Service
public class Warehouse_service {
    
    private static final Logger log = LoggerFactory.getLogger(Warehouse_service.class);
    private final Warehouse_repository repo;
    private static final int PAGE_SIZE = 10;
    
    @Autowired
    public Warehouse_service(Warehouse_repository repo) {
        this.repo = repo;
    }
    
    // ========== ✅ 창고 코드 생성 및 중복 확인 ==========

    /**
     * 창고 유형에 따라 자동 코드 생성 (RWWH0001 / PDWH0001 등)
     */
    @Transactional(readOnly = true)
    public String generateWhsCode(WarehouseType type) {
        String prefix = type == WarehouseType.RAW ? "RWWH" : "PDWH";
        List<Integer> existingNumbers = repo.findAllCodeNumbersByType(type, prefix + "%");
        int nextNumber = findFirstAvailableNumber(existingNumbers);
        return String.format("%s%04d", prefix, nextNumber);
    }
    
    /**
     * 창고 코드 중복 확인
     * - 등록 시: 동일 코드 존재 여부 확인
     * - 수정 시: 본인의 ID 제외하고 중복 확인 가능
     */
    @Transactional(readOnly = true)
    public Map<String, String> checkWhsCodeDuplicate(String code, Integer excludeId) {
        Optional<Warehouse> existing = repo.findByWarehouseCode(code);
        
        if (existing.isPresent() && (excludeId == null || !existing.get().getWarehouseId().equals(excludeId))) {
            return Map.of("status", "duplicate", "message", "이미 사용 중인 창고 코드입니다.");
        }
        return Map.of("status", "ok", "message", "사용 가능한 창고 코드입니다.");
    }
    
    // ========== ✅ CRUD 기능 ==========

    /**
     * 창고 등록
     * - 코드 중복 검사
     * - 코드가 없으면 자동 생성
     */ 
    @Transactional
    public WarehouseResponseDTO createWhs(WarehouseCreateRequestDTO dto) {
        log.debug("창고 등록 시작: {}", dto.getWarehouseName());
        
        Warehouse whs = buildWhs(dto);
        if (StringUtils.hasText(dto.getWarehouseCode())) {
            validateWhsCodeNotDuplicate(dto.getWarehouseCode(), null);
            whs.setWarehouseCode(dto.getWarehouseCode());
        } else {
            whs.setWarehouseCode(generateWhsCode(whs.getWarehouseType()));
        }
        
        Warehouse saved = repo.save(whs);
        log.info("창고 등록 완료: {}", saved.getWarehouseCode());
        return WarehouseResponseDTO.from(saved);
    }
    
    /**
     * 창고 수정
     * - 코드 변경 시 중복 및 형식 검증 포함
     */    
    @Transactional
    public WarehouseResponseDTO updateWhs(Integer id, WarehouseCreateRequestDTO dto) {
        log.debug("창고 수정 시작, ID: {}", id);
        
        Warehouse whs = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("수정할 창고를 찾을 수 없습니다. ID: " + id));

        
        updateWhsFields(whs, dto, id);
        Warehouse updated = repo.save(whs);
        log.info("창고 수정 완료: {}", updated.getWarehouseCode());
        return WarehouseResponseDTO.from(updated);
    }

    
    /**
     * 단건 조회
     */
    @Transactional(readOnly = true)
    public WarehouseResponseDTO getWhsById(Integer id) {
        return repo.findById(id)
            .map(WarehouseResponseDTO::from)
            .orElseThrow(() -> new RuntimeException("창고를 찾을 수 없습니다. ID: " + id));
    }
    
    /**
     * 단건 삭제
     */
    @Transactional
    public void deleteWhs(Integer id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("창고를 찾을 수 없습니다. ID: " + id);
        }
        repo.deleteById(id);
    }
    
    // ========== ✅ 페이징 조회 ==========

    /**
     * 사용 여부 필터 기반 페이징 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWhsByFilterWithPaging(String useYnFilter, int page) {
        log.debug("페이징으로 창고 목록 조회: useYnFilter=[{}], page={}", useYnFilter, page);
        
        Pageable pageable = PageRequest.of(page - 1, PAGE_SIZE);
        Page<Warehouse> whsPage = isAllFilter(useYnFilter) 
            ? repo.findAllByOrderByWarehouseIdDesc(pageable)
            : repo.findByUseYnOrderByWarehouseIdDesc(useYnFilter, pageable);
            
        return wrapPagedResult(whsPage, page);
    }
    
    /**
     * 키워드 검색 기반 페이징 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Object> searchWhsFromAllWithPaging(String keyword, String searchType, int page) {
        log.debug("전체 데이터 대상 페이징 검색: keyword={}, searchType={}, page={}", keyword, searchType, page);
        
        Pageable pageable = PageRequest.of(page - 1, PAGE_SIZE);
        Page<Warehouse> whsPage = switch (searchType) {
            case "code" -> repo.findByWarehouseCodeContainingOrderByWarehouseIdDesc(keyword, pageable);
            case "name" -> repo.findByWarehouseNameContainingOrderByWarehouseIdDesc(keyword, pageable);
            default -> repo.findByWarehouseNameContainingOrderByWarehouseIdDesc(keyword, pageable);
        };
        
        return wrapPagedResult(whsPage, page);
    }
    
    // ========== ✅ 비페이징 조회 (JS 호환 유지용) ==========
    
    @Transactional(readOnly = true)
    public List<WarehouseResponseDTO> getWhsByFilter(String useYnFilter) {
        log.debug("필터로 창고 목록 조회: useYnFilter=[{}]", useYnFilter);
        
        List<Warehouse> whsList = isAllFilter(useYnFilter)
            ? repo.findAllByOrderByWarehouseIdDesc()
            : repo.findByUseYnOrderByWarehouseIdDesc(useYnFilter);
            
        return whsList.stream().map(WarehouseResponseDTO::from).collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<WarehouseResponseDTO> searchWhsFromAll(String keyword, String searchType) {
        log.debug("전체 데이터 대상 검색: keyword={}, searchType={}", keyword, searchType);
        
        List<Warehouse> whsList = switch (searchType) {
            case "code" -> repo.findByWarehouseCodeContainingOrderByWarehouseIdDesc(keyword);
            case "name" -> repo.findByWarehouseNameContainingOrderByWarehouseIdDesc(keyword);
            default -> repo.findByWarehouseNameContainingOrderByWarehouseIdDesc(keyword);
        };
        
        return whsList.stream().map(WarehouseResponseDTO::from).collect(Collectors.toList());
    }
    
    
    @Transactional(readOnly = true)
    public List<WarehouseResponseDTO> searchWhs(String keyword, String searchType, String useYnFilter) {
        log.debug("검색 실행: keyword={}, searchType={}, useYnFilter={}", keyword, searchType, useYnFilter);
        return findWhsList(keyword, searchType, useYnFilter).stream()
                .map(WarehouseResponseDTO::from).collect(Collectors.toList());
    }
    
    // ==========  화면 렌더링용 데이터 제공 (Thymeleaf 전용) ==========

    /**
     * 페이징이 있는 뷰 렌더링용 데이터 반환
     */ 
    @Transactional(readOnly = true)
    public Map<String, Object> getWhsPageData(String useYn, String keyword, String searchType, Integer page) {
        int currentPage = (page != null && page > 0) ? page : 1;
        String filter = (useYn != null) ? useYn : "ALL";

		Map<String, Object> result = new HashMap<>(
				StringUtils.hasText(keyword) ? searchWhsFromAllWithPaging(keyword.trim(), searchType, currentPage)
						: getWhsByFilterWithPaging(filter, currentPage));

		result.put("useYnFilter", filter);
		result.put("keyword", keyword != null ? keyword : "");
		result.put("searchType", searchType != null ? searchType : "");

        return result;
    }


    @Transactional(readOnly = true)
    public Map<String, Object> getWhsPageData(String useYn, String keyword, String searchType) {
        String filter = (useYn != null) ? useYn : "ALL";
        List<WarehouseResponseDTO> whsList = StringUtils.hasText(keyword)
            ? searchWhsFromAll(keyword, searchType)
            : getWhsByFilter(filter);
            
        return Map.of(
            "warehouses", whsList,
            "useYnFilter", filter,
            "keyword", keyword != null ? keyword : "",
            "searchType", searchType != null ? searchType : ""
        );
    }


    /**
     * 에러 발생 시 기본 값 세팅
     */
    public void handleWhsPageError(Model model, Exception e) {
        model.addAttribute("whsList", List.of());
        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPages", 0);
        model.addAttribute("totalElements", 0L);
        model.addAttribute("hasNext", false);
        model.addAttribute("hasPrevious", false);
        model.addAttribute("error", "창고 목록을 불러오는데 실패했습니다: " + e.getMessage());
    }
    
    // ========== ✅ 내부 유틸리티 ==========
    
    private Map<String, Object> wrapPagedResult(Page<Warehouse> page, int pageNum) {
        List<WarehouseResponseDTO> whsList = page.getContent().stream()
                .map(WarehouseResponseDTO::from).collect(Collectors.toList());
                
        return Map.of(
            "whsList", whsList,
            "currentPage", pageNum,
            "totalPages", page.getTotalPages(),
            "totalElements", page.getTotalElements(),
            "hasNext", page.hasNext(),
            "hasPrevious", page.hasPrevious()
        );
    }
    
    private boolean isAllFilter(String useYnFilter) {
        return useYnFilter == null || useYnFilter.isEmpty() || "ALL".equals(useYnFilter);
    }
    
    private int findFirstAvailableNumber(List<Integer> existingNumbers) {
        if (existingNumbers == null || existingNumbers.isEmpty()) return 1;
        
        int expectedNumber = 1;
        for (Integer actualNumber : existingNumbers) {
            if (expectedNumber < actualNumber) return expectedNumber;
            expectedNumber = actualNumber + 1;
        }
        return expectedNumber;
    }
    
    private Warehouse buildWhs(WarehouseCreateRequestDTO dto) {
        Warehouse whs = new Warehouse();
        whs.setWarehouseName(dto.getWarehouseName());
        whs.setDescription(dto.getDescription());
        whs.setWarehouseType(parseWhsType(dto.getWarehouseType()));
        whs.setUseYn(StringUtils.hasText(dto.getUseYn()) ? dto.getUseYn() : "Y");
        return whs;
    }
    
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
    
    private void validateWhsCodeNotDuplicate(String code, Integer excludeId) {
        Map<String, String> result = checkWhsCodeDuplicate(code, excludeId);
        if ("duplicate".equals(result.get("status"))) {
            throw new IllegalArgumentException(result.get("message"));
        }
    }
    
    
    /**
     * 창고 수정 시 필드 매핑 및 유효성 검증
     */
    private void updateWhsFields(Warehouse whs, WarehouseCreateRequestDTO dto, Integer id) {
        if (StringUtils.hasText(dto.getWarehouseCode()) && 
            !dto.getWarehouseCode().equals(whs.getWarehouseCode())) {
            validateWhsCodeNotDuplicate(dto.getWarehouseCode(), id);
            whs.setWarehouseCode(dto.getWarehouseCode());
        }
        
        if (StringUtils.hasText(dto.getWarehouseName())) {
            whs.setWarehouseName(dto.getWarehouseName());
        }
        
        if (StringUtils.hasText(dto.getWarehouseType())) {
            WarehouseType newType = parseWhsType(dto.getWarehouseType());
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
     * 창고 코드가 유형에 맞는 prefix인지 확인
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
     * 검색 + 필터를 함께 적용하는 내부 헬퍼
     */
    private List<Warehouse> findWhsList(String keyword, String searchType, String useYnFilter) {
        if (StringUtils.hasText(keyword)) {
            return switch (searchType) {
                case "code" -> repo.findByWarehouseCodeContainingOrderByWarehouseIdDesc(keyword);
                case "name" -> repo.findByWarehouseNameContainingOrderByWarehouseIdDesc(keyword);
                default -> repo.findByWarehouseNameContainingOrderByWarehouseIdDesc(keyword);
            };
        }
        
        if (StringUtils.hasText(useYnFilter)) {
            return "ALL".equals(useYnFilter) 
                ? repo.findAllByOrderByWarehouseIdDesc()
                : repo.findByUseYnOrderByWarehouseIdDesc(useYnFilter);
        }
        
        return repo.findByUseYnOrderByWarehouseIdDesc("Y");
    }
}