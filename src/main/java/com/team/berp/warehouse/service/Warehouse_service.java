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
import com.team.berp.domain.WarehouseType;
import com.team.berp.stock.repository.StockRepository; // ✅ StockRepository 추가
import com.team.berp.warehouse.dto.WarehouseCreateRequestDTO;
import com.team.berp.warehouse.dto.WarehouseResponseDTO;
import com.team.berp.warehouse.repository.Warehouse_repository;

@Service
public class Warehouse_service {
    
    private static final Logger log = LoggerFactory.getLogger(Warehouse_service.class);
    private final Warehouse_repository repo;
    private final StockRepository stockRepo; // ✅ StockRepository 의존성 주입 추가
    private static final int PAGE_SIZE = 10;
    
    @Autowired
    public Warehouse_service(Warehouse_repository repo, StockRepository stockRepo) {
        this.repo = repo;
        this.stockRepo = stockRepo; // ✅ 생성자에 stockRepo 추가
    }
    
    // ========== ✅ 창고 코드 생성 및 중복 확인 ==========

    /**
     * 창고 유형에 따라 자동 코드 생성 (RWWH0001 / PDWH0001 등)
     * 컨트롤러에서 generateWhsCode 호출됨
     */
    @Transactional(readOnly = true)
    public String generateWhsCode(WarehouseType type) {
    	String prefix;
    	if (type == WarehouseType.RAW) {
    	    prefix = "RWWH";
    	} else {
    	    prefix = "PDWH";
    	}
        List<Integer> existingNumbers = repo.findAllCodeNumbersByType(type, prefix + "%");
        int nextNumber = findFirstAvailableNumber(existingNumbers);
        return String.format("%s%04d", prefix, nextNumber);
    }
    
    /**
     * 창고 코드 중복 확인
     * 컨트롤러에서 checkWhsCodeDuplicate 호출됨
     */
    @Transactional(readOnly = true)
    public Map<String, String> checkWhsCodeDuplicate(String code, Long excludeId) {
        Optional<Warehouse> existing = repo.findByWarehouseCode(code);
        
        if (existing.isPresent() && (excludeId == null || !existing.get().getId().equals(excludeId))) {
            return Map.of("status", "duplicate", "message", "이미 사용 중인 창고 코드입니다.");
        }
        return Map.of("status", "ok", "message", "사용 가능한 창고 코드입니다.");
    }
   
    // ========== ✅ CRUD 기능 ==========

    /**
     * 창고 등록 - 컨트롤러에서 createWhs 호출됨
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
     * ✅ 창고 수정 - 재고 검증 로직 추가
     * 컨트롤러에서 updateWhs 호출됨
     */    
    @Transactional
    public WarehouseResponseDTO updateWhs(Long id, WarehouseCreateRequestDTO dto) {
        log.debug("창고 수정 시작, ID: {}", id);
        
        Warehouse whs = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("수정할 창고를 찾을 수 없습니다. ID: " + id));

        // ✅ 사용여부를 Y -> N으로 변경하려는 경우 재고 검증
        if ("Y".equals(whs.getUseYn()) && "N".equals(dto.getUseYn())) {
            validateWarehouseHasNoStock(id, whs.getWarehouseName());
        }

        updateWhsFields(whs, dto, id);
        Warehouse updated = repo.save(whs);
        log.info("창고 수정 완료: {}", updated.getWarehouseCode());
        return WarehouseResponseDTO.from(updated);
    }


    /**
     * ✅ 창고에 재고가 있는지 검증하는 메서드 (새로 추가)
     * @param warehouseId 검증할 창고 ID
     * @param warehouseName 오류 메시지용 창고명
     * @throws IllegalArgumentException 재고가 있으면 예외 발생
     */
    private void validateWarehouseHasNoStock(Long warehouseId, String warehouseName) {
        log.debug("창고 재고 검증 시작 - 창고 ID: {}, 창고명: {}", warehouseId, warehouseName);
        
        // 해당 창고의 총 재고 품목 수 조회
        long totalStockItems = stockRepo.countByWarehouse_Id(warehouseId);
        
        if (totalStockItems > 0) {
            // 재고가 있는 경우, 실제 재고량이 있는 품목 수도 확인
            Long totalStockQuantity = stockRepo.sumQuantityByWarehouse_Id(warehouseId);
            
            if (totalStockQuantity != null && totalStockQuantity > 0) {
                String errorMessage = String.format(
                    "창고 '%s'에 재고가 %,d개 남아있어 사용여부를 '미사용'으로 변경할 수 없습니다. " +
                    "재고를 모두 이동하거나 출고한 후 다시 시도해주세요.",
                    warehouseName, totalStockQuantity
                );
                
                log.warn("창고 사용 중지 실패 - 재고 존재: 창고={}, 재고량={}", warehouseName, totalStockQuantity);
                throw new IllegalArgumentException(errorMessage);
            }
        }
        
        log.debug("창고 재고 검증 통과 - 창고에 재고가 없음");
    }

    /**
     * 단건 조회 - 컨트롤러에서 getWhsById 호출됨
     */
    @Transactional(readOnly = true)
    public WarehouseResponseDTO getWhsById(Long id) {
        return repo.findById(id)
            .map(WarehouseResponseDTO::from)
            .orElseThrow(() -> new RuntimeException("창고를 찾을 수 없습니다. ID: " + id));
    }
    
    /**
     * 단건 삭제 - 컨트롤러에서 deleteWhs 호출됨
     */
    @Transactional
    public void deleteWhs(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("창고를 찾을 수 없습니다. ID: " + id);
        }
        
        // ✅ 삭제 시에도 재고 검증 추가
        Warehouse warehouse = repo.findById(id).orElse(null);
        if (warehouse != null) {
            validateWarehouseHasNoStock(id, warehouse.getWarehouseName());
        }
        
        repo.deleteById(id);
    }
    
    // ========== ✅ 페이징 조회 ==========

    /**
     * 사용 여부 필터 기반 페이징 조회
     * 컨트롤러에서 getWhsByFilterWithPaging 호출됨
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWhsByFilterWithPaging(String useYnFilter, int page) {
        log.debug("페이징으로 창고 목록 조회: useYnFilter=[{}], page={}", useYnFilter, page);
        
        Pageable pageable = PageRequest.of(page - 1, PAGE_SIZE);
        Page<Warehouse> whsPage = isAllFilter(useYnFilter) 
            ? repo.findAllByOrderByIdDesc(pageable)
            : repo.findByUseYnOrderByIdDesc(useYnFilter, pageable);
            
        return wrapPagedResult(whsPage, page);
    }
    
    /**
     * 키워드 검색 기반 페이징 조회
     * 컨트롤러에서 searchWhsFromAllWithPaging 호출됨
     */
    @Transactional(readOnly = true)
    public Map<String, Object> searchWhsFromAllWithPaging(String keyword, String searchType, int page) {
        log.debug("전체 데이터 대상 페이징 검색: keyword={}, searchType={}, page={}", keyword, searchType, page);
        
        Pageable pageable = PageRequest.of(page - 1, PAGE_SIZE);
        Page<Warehouse> whsPage = switch (searchType) {
            case "code" -> repo.findByWarehouseCodeContainingOrderByIdDesc(keyword, pageable);
            case "name" -> repo.findByWarehouseNameContainingOrderByIdDesc(keyword, pageable);
            default -> repo.findByWarehouseNameContainingOrderByIdDesc(keyword, pageable);
        };
        
        return wrapPagedResult(whsPage, page);
    }
    
    /**
     * 사용 중인("Y") 모든 창고 목록을 DTO 리스트로 반환
     * 다른 서비스에서 활용하기 위한 메서드 (예: 재고 관리에서 창고 드롭다운 용도)
     */
    @Transactional(readOnly = true)
    public List<WarehouseResponseDTO> getActiveWarehouses() {
        log.debug("사용 중인 활성 창고 목록 조회 요청");
        List<Warehouse> activeWarehouses = repo.findByUseYnOrderByIdDesc("Y");
        return activeWarehouses.stream()
                               .map(WarehouseResponseDTO::from)
                               .collect(Collectors.toList());
    }
    
    // ========== ✅ 비페이징 조회 (JS 호환 유지용) ==========
    
    /**
     * 필터 기반 비페이징 조회
     * 컨트롤러에서 getWhsByFilter 호출됨 (/all 엔드포인트)
     */
    @Transactional(readOnly = true)
    public List<WarehouseResponseDTO> getWhsByFilter(String useYnFilter) {
        log.debug("필터로 창고 목록 조회: useYnFilter=[{}]", useYnFilter);
        
        List<Warehouse> whsList = isAllFilter(useYnFilter)
            ? repo.findAllByOrderByIdDesc()
            : repo.findByUseYnOrderByIdDesc(useYnFilter);
            
        return whsList.stream().map(WarehouseResponseDTO::from).collect(Collectors.toList());
    }
    
    /**
     * 키워드 검색 비페이징 조회
     * 컨트롤러에서 searchWhsFromAll 호출됨 (/all 엔드포인트)
     */
    @Transactional(readOnly = true)
    public List<WarehouseResponseDTO> searchWhsFromAll(String keyword, String searchType) {
        log.debug("전체 데이터 대상 검색: keyword={}, searchType={}", keyword, searchType);
        
        List<Warehouse> whsList = switch (searchType) {
            case "code" -> repo.findByWarehouseCodeContainingOrderByIdDesc(keyword);
            case "name" -> repo.findByWarehouseNameContainingOrderByIdDesc(keyword);
            default -> repo.findByWarehouseNameContainingOrderByIdDesc(keyword);
        };
        
        return whsList.stream().map(WarehouseResponseDTO::from).collect(Collectors.toList());
    }
    
    /**
     * 검색 + 필터 조합 (기존 호환용)
     */
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
    	int currentPage;
    	if (page != null && page > 0) {
    	    currentPage = page;
    	} else {
    	    currentPage = 1;
    	}

    	String filter;
    	if (useYn != null) {
    	    filter = useYn;
    	} else {
    	    filter = "ALL";
    	}

		Map<String, Object> result = new HashMap<>(
				StringUtils.hasText(keyword) ? searchWhsFromAllWithPaging(keyword.trim(), searchType, currentPage)
						: getWhsByFilterWithPaging(filter, currentPage));

		result.put("useYnFilter", filter);
		if (keyword != null) {
		    result.put("keyword", keyword);
		} else {
		    result.put("keyword", "");
		}

		if (searchType != null) {
		    result.put("searchType", searchType);
		} else {
		    result.put("searchType", "");
		}

        return result;
    }

    /**
     * 비페이징 뷰 렌더링용 데이터 반환 (오버로드)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWhsPageData(String useYn, String keyword, String searchType) {
    	String filter;
    	if (useYn != null) {
    	    filter = useYn;
    	} else {
    	    filter = "ALL";
    	}
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
    
    /**
     * Spring Data JPA Page 객체를 프론트엔드용 Map으로 변환
     */
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
    
    /**
     * 사용여부 필터가 "전체"인지 판단
     */
    private boolean isAllFilter(String useYnFilter) {
        return useYnFilter == null || useYnFilter.isEmpty() || "ALL".equals(useYnFilter);
    }
    
    /**
     * 첫 번째 사용 가능한 번호 찾기 (코드 생성용)
     */
    private int findFirstAvailableNumber(List<Integer> existingNumbers) {
        if (existingNumbers == null || existingNumbers.isEmpty()) return 1;
        
        int expectedNumber = 1;
        for (Integer actualNumber : existingNumbers) {
            if (expectedNumber < actualNumber) return expectedNumber;
            expectedNumber = actualNumber + 1;
        }
        return expectedNumber;
    }
    
    /**
     * DTO에서 Warehouse 엔티티 생성
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
     * 문자열을 WarehouseType enum으로 변환
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
     * 창고 코드 중복 검증
     */
    private void validateWhsCodeNotDuplicate(String code, Long excludeId) {
        Map<String, String> result = checkWhsCodeDuplicate(code, excludeId);
        if ("duplicate".equals(result.get("status"))) {
            throw new IllegalArgumentException(result.get("message"));
        }
    }
    
    /**
     * 창고 수정 시 필드 매핑 및 유효성 검증
     */
    private void updateWhsFields(Warehouse whs, WarehouseCreateRequestDTO dto, Long id) {
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
    	String expectedPrefix;
    	if (type == WarehouseType.RAW) {
    	    expectedPrefix = "RWWH";
    	} else {
    	    expectedPrefix = "PDWH";
    	}
        if (!code.startsWith(expectedPrefix)) {
        	String typeName;
        	if (type == WarehouseType.RAW) {
        	    typeName = "자재창고";
        	} else {
        	    typeName = "완제품창고";
        	}
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
                case "code" -> repo.findByWarehouseCodeContainingOrderByIdDesc(keyword);
                case "name" -> repo.findByWarehouseNameContainingOrderByIdDesc(keyword);
                default -> repo.findByWarehouseNameContainingOrderByIdDesc(keyword);
            };
        }
        
        if (StringUtils.hasText(useYnFilter)) {
            return "ALL".equals(useYnFilter) 
                ? repo.findAllByOrderByIdDesc()
                : repo.findByUseYnOrderByIdDesc(useYnFilter);
        }
        
        return repo.findByUseYnOrderByIdDesc("Y");
    }
}