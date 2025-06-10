package com.team.berp.receive.controller;

import com.team.berp.domain.*;
import com.team.berp.inventory_log.repository.InventoryLogRepository;
import com.team.berp.client.repository.ClientRepository;
import com.team.berp.item.repository.ItemRepository;
import com.team.berp.warehouse.repository.Warehouse_repository;
import com.team.berp.order.repository.Order_OrderLineItemRepository;
import com.team.berp.receive.dto.ReceiveRequestDTO;
import com.team.berp.receive.service.ReceiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 입고 관리 REST API 컨트롤러 - 발주 상태 관리 및 필터링 개선
 */
@RestController
@RequestMapping("/api/receive")
@RequiredArgsConstructor
public class ReceiveApiController {

    private final ReceiveService receiveService;
    private final ClientRepository clientRepo;
    private final ItemRepository itemRepo;
    private final Warehouse_repository warehouseRepo;
    private final Order_OrderLineItemRepository orderLineItemRepo;
    private final InventoryLogRepository inventoryLogRepo;

    /**
     * 품목 목록 조회 - GET /api/receive/items (최적화된 버전)
     */
    @GetMapping("/items")
    public ResponseEntity<List<Map<String, Object>>> getItemsForReceive() {
        try {
            System.out.println("📦 품목 목록 조회 시작");
            
            // 사용중인 품목만 빠르게 조회
            List<Map<String, Object>> result = itemRepo.findAll().stream()
                .filter(item -> "Y".equals(item.getUse()))
                .limit(100) // 성능을 위해 100개로 제한
                .map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("id", item.getId());
                    itemMap.put("code", item.getCode());
                    itemMap.put("name", item.getName());
                    itemMap.put("type", item.getType().name());
                    itemMap.put("unit", item.getUnit());
                    return itemMap;
                })
                .collect(Collectors.toList());
            
            System.out.println("✅ 품목 목록 조회 완료: " + result.size() + "개");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("❌ 품목 목록 조회 실패: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 창고 목록 조회 - GET /api/receive/warehouses (최적화된 버전)
     */
    @GetMapping("/warehouses")
    public ResponseEntity<List<Map<String, Object>>> getWarehousesForReceive() {
        try {
            System.out.println("🏢 창고 목록 조회 시작");
            
            // 사용중인 창고만 빠르게 조회
            List<Map<String, Object>> result = warehouseRepo.findAll().stream()
                .filter(wh -> "Y".equals(wh.getUseYn()))
                .map(wh -> {
                    Map<String, Object> whMap = new HashMap<>();
                    whMap.put("id", wh.getId());
                    whMap.put("code", wh.getWarehouseCode());
                    whMap.put("name", wh.getWarehouseName());
                    whMap.put("type", wh.getWarehouseType().name());
                    return whMap;
                })
                .collect(Collectors.toList());
            
            System.out.println("✅ 창고 목록 조회 완료: " + result.size() + "개");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("❌ 창고 목록 조회 실패: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 공급업체 목록 조회 - GET /api/receive/suppliers (수정된 버전 - 모든 공급업체 출력)
     */
    @GetMapping("/suppliers")
    public ResponseEntity<List<Map<String, Object>>> getSuppliers() {
        try {
            System.out.println("🏢 공급업체 목록 조회 시작");
            
            // 모든 공급업체 조회 (제한 없이)
            List<Map<String, Object>> result = clientRepo.findAll().stream()
                .filter(company -> {
                    boolean isActive = "Y".equals(company.getUseYn());
                    boolean isSupplier = Company.CompanyType.SUPPLIER.equals(company.getCompanyType()) || 
                                       Company.CompanyType.BOTH.equals(company.getCompanyType());
                    return isActive && isSupplier;
                })
                // .limit(50) 제거 - 모든 공급업체 표시
                .map(company -> {
                    Map<String, Object> supplierMap = new HashMap<>();
                    supplierMap.put("companyId", company.getCompanyId());
                    supplierMap.put("companyName", company.getCompanyName());
                    supplierMap.put("companyType", company.getCompanyType().name());
                    
                    // 담당자 정보
                    if (company.getEmployee() != null) {
                        supplierMap.put("managerName", company.getEmployee().getEmpName());
                        supplierMap.put("managerEmail", company.getEmployee().getEmpEmail());
                        supplierMap.put("managerPhone", company.getEmployee().getEmpHp());
                    } else {
                        supplierMap.put("managerName", "담당자 미지정");
                        supplierMap.put("managerEmail", "");
                        supplierMap.put("managerPhone", "");
                    }
                    
                    return supplierMap;
                })
                .collect(Collectors.toList());
            
            System.out.println("✅ 공급업체 목록 조회 완료: " + result.size() + "개 (전체 공급업체)");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("❌ 공급업체 목록 조회 실패: " + e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 🆕 미완료 발주 목록 조회 - 발주 상태 관리 개선
     * CONFIRMED 상태인 발주만 입고 가능하도록 필터링
     */
    @GetMapping("/pending-orders")
    public ResponseEntity<List<Map<String, Object>>> getPendingOrders() {
        try {
            System.out.println("📋 미완료 발주 목록 조회 시작 (CONFIRMED 상태만)");
            
            // 최근 60일 내 발주만 조회하여 성능 향상
            LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);
            
            List<Map<String, Object>> result = orderLineItemRepo.findAll().stream()
                .filter(oli -> {
                    // SUPPLIER 타입 발주만
                    if (oli.getCompanyOrder() == null || 
                        !CompanyOrder.OrderType.SUPPLIER.equals(oli.getCompanyOrder().getOrderType())) {
                        return false;
                    }
                    
                    // 🆕 CONFIRMED 상태인 발주만 입고 가능
                    if (oli.getCompanyOrder().getOrderStatus() != CompanyOrder.OrderStatus.CONFIRMED) {
                        System.out.println("❌ 발주 상태 필터링: " + oli.getCompanyOrder().getOrderNum() + 
                                         " - 상태: " + oli.getCompanyOrder().getOrderStatus());
                        return false;
                    }
                    
                    // 최근 60일 내 발주만
                    if (oli.getCompanyOrder().getOrderDate() != null) {
                        LocalDateTime orderDateTime = oli.getCompanyOrder().getOrderDate().atStartOfDay();
                        if (orderDateTime.isBefore(sixtyDaysAgo)) {
                            return false;
                        }
                    }
                    
                    // 입고 완료 여부 확인 (간단하게)
                    Integer orderQty = oli.getUnitQty() != null ? oli.getUnitQty() : 0;
                    if (orderQty <= 0) return false;
                    
                    // 입고된 수량 확인
                    Integer receivedQty = inventoryLogRepo.findAll().stream()
                        .filter(log -> LogType.IN.equals(log.getLogType()) && 
                                     oli.getOrderLineItemId().equals(log.getOrderLineItem() != null ? 
                                     log.getOrderLineItem().getOrderLineItemId() : null))
                        .mapToInt(log -> log.getQuantity() != null ? log.getQuantity() : 0)
                        .sum();
                    
                    boolean hasRemaining = orderQty > receivedQty;
                    if (hasRemaining) {
                        System.out.println("✅ 입고 가능한 발주: " + oli.getCompanyOrder().getOrderNum() + 
                                         " - 주문: " + orderQty + ", 입고: " + receivedQty);
                    }
                    
                    return hasRemaining;
                })
                .limit(30) // 성능을 위해 30개로 제한
                .map(oli -> {
                    Map<String, Object> orderMap = new HashMap<>();
                    
                    orderMap.put("orderLineItemId", oli.getOrderLineItemId());
                    orderMap.put("orderNum", oli.getCompanyOrder() != null ? 
                        oli.getCompanyOrder().getOrderNum() : "주문번호없음");
                    
                    // 품목 정보
                    if (oli.getItem() != null) {
                        orderMap.put("itemId", oli.getItem().getId());
                        orderMap.put("itemCode", oli.getItem().getCode());
                        orderMap.put("itemName", oli.getItem().getName());
                        orderMap.put("itemUnit", oli.getItem().getUnit());
                        orderMap.put("itemType", oli.getItem().getType().name()); // 🔥 이 부분이 핵심!
                    }
                    
                    // 수량 정보
                    Integer orderQty = oli.getUnitQty() != null ? oli.getUnitQty() : 0;
                    Integer receivedQty = inventoryLogRepo.findAll().stream()
                        .filter(log -> LogType.IN.equals(log.getLogType()) && 
                                     oli.getOrderLineItemId().equals(log.getOrderLineItem() != null ? 
                                     log.getOrderLineItem().getOrderLineItemId() : null))
                        .mapToInt(log -> log.getQuantity() != null ? log.getQuantity() : 0)
                        .sum();
                    
                    orderMap.put("unitQty", orderQty);
                    orderMap.put("receivedQty", receivedQty);
                    orderMap.put("remainingQty", orderQty - receivedQty);
                    
                    // 회사 및 담당자 정보
                    if (oli.getCompanyOrder() != null && oli.getCompanyOrder().getCompany() != null) {
                        Company company = oli.getCompanyOrder().getCompany();
                        orderMap.put("companyId", company.getCompanyId());
                        orderMap.put("companyName", company.getCompanyName());
                        
                        if (company.getEmployee() != null) {
                            orderMap.put("managerName", company.getEmployee().getEmpName());
                            orderMap.put("managerEmail", company.getEmployee().getEmpEmail());
                            orderMap.put("managerPhone", company.getEmployee().getEmpHp());
                        } else {
                            orderMap.put("managerName", "담당자 미지정");
                            orderMap.put("managerEmail", "");
                            orderMap.put("managerPhone", "");
                        }
                    }
                    
                    return orderMap;
                })
                .collect(Collectors.toList());
            
            System.out.println("✅ 미완료 발주 목록 조회 완료: " + result.size() + "개 (CONFIRMED 상태만)");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("❌ 미완료 발주 목록 조회 실패: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 🆕 입고 이력 조회 - 필터링 및 전체 조회 기능 개선
     */
    @GetMapping
    public ResponseEntity<?> getReceiveHistory(
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        
        try {
            System.out.println("📦 입고 이력 조회 API - 날짜: " + startDate + " ~ " + endDate + ", 타입: " + type);
            
            // 날짜 파싱 (전체 조회 지원)
            LocalDateTime searchStartDate = null;
            LocalDateTime searchEndDate = null;
            
            // 🆕 날짜가 지정된 경우에만 날짜 필터링 적용
            if (startDate != null && !startDate.trim().isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(startDate);
                    searchStartDate = date.atStartOfDay();
                    
                    // endDate가 없으면 같은 날의 끝으로 설정
                    if (endDate == null || endDate.trim().isEmpty()) {
                        searchEndDate = date.atTime(23, 59, 59);
                    }
                } catch (DateTimeParseException e) {
                    return ResponseEntity.badRequest().body("잘못된 날짜 형식입니다.");
                }
            }
            
            if (endDate != null && !endDate.trim().isEmpty()) {
                try {
                    searchEndDate = LocalDate.parse(endDate).atTime(23, 59, 59);
                } catch (DateTimeParseException e) {
                    return ResponseEntity.badRequest().body("잘못된 종료일 형식입니다.");
                }
            }
            
            // 페이징 설정
            Pageable pageable = PageRequest.of(page, size, 
                Sort.by(Sort.Direction.DESC, "logDatetime"));
            
            // 🆕 조건에 따른 쿼리 실행
            Page<InventoryLog> logPage;
            
            if (searchStartDate != null && searchEndDate != null) {
                // 날짜 범위가 지정된 경우
                if (type != null && !type.trim().isEmpty()) {
                    // 날짜 + 타입 필터링
                    System.out.println("📅 날짜 + 타입 필터링 조회");
                    logPage = inventoryLogRepo.findByLogTypeAndLogDatetimeBetweenOrderByLogDatetimeDesc(
                        LogType.IN, searchStartDate, searchEndDate, pageable);
                } else {
                    // 날짜만 필터링
                    System.out.println("📅 날짜만 필터링 조회");
                    logPage = inventoryLogRepo.findByLogTypeAndLogDatetimeBetweenOrderByLogDatetimeDesc(
                        LogType.IN, searchStartDate, searchEndDate, pageable);
                }
            } else {
                // 전체 조회
                System.out.println("📅 전체 기간 조회");
                logPage = inventoryLogRepo.findByLogTypeOrderByLogDatetimeDesc(LogType.IN, pageable);
            }
            
            // 🆕 타입 필터링은 메모리에서 처리 (DB 쿼리 복잡도 감소)
            List<InventoryLog> filteredLogs = logPage.getContent();
            if (type != null && !type.trim().isEmpty()) {
                filteredLogs = filteredLogs.stream()
                    .filter(log -> {
                        if ("ORDER_BASED".equals(type)) {
                            return log.getOrderLineItem() != null;
                        } else if ("INDEPENDENT".equals(type)) {
                            return log.getOrderLineItem() == null;
                        }
                        return true; // 전체 유형
                    })
                    .collect(Collectors.toList());
            }
            
            // 빠른 데이터 변환 (단위 표시 개선)
            List<Map<String, Object>> receiveList = filteredLogs.stream()
                .map(log -> {
                    Map<String, Object> receiveMap = new HashMap<>();
                    
                    receiveMap.put("id", log.getId());
                    receiveMap.put("receiveDate", log.getLogDatetime().toLocalDate().toString());
                    receiveMap.put("logDatetime", log.getLogDatetime());
                    
                    // 품목 정보
                    if (log.getItem() != null) {
                        receiveMap.put("itemCode", log.getItem().getCode());
                        receiveMap.put("itemName", log.getItem().getName());
                        receiveMap.put("unit", log.getItem().getUnit());
                    } else {
                        receiveMap.put("itemCode", "-");
                        receiveMap.put("itemName", "-");
                        receiveMap.put("unit", "EA");
                    }
                    
                    // 창고 정보
                    if (log.getWarehouse() != null) {
                        receiveMap.put("warehouse", log.getWarehouse().getWarehouseName());
                    } else {
                        receiveMap.put("warehouse", "-");
                    }
                    
                    receiveMap.put("quantity", log.getQuantity());
                    
                    // 입고 유형 및 회사 정보 (독립적 입고 개선)
                    if (log.getOrderLineItem() != null) {
                        receiveMap.put("type", "발주 기반");
                        receiveMap.put("receiveType", "ORDER_BASED");
                        
                        if (log.getOrderLineItem().getCompanyOrder() != null && 
                            log.getOrderLineItem().getCompanyOrder().getCompany() != null) {
                            receiveMap.put("company", log.getOrderLineItem().getCompanyOrder().getCompany().getCompanyName());
                        } else {
                            receiveMap.put("company", "-");
                        }
                    } else {
                        receiveMap.put("type", "독립적 입고");
                        receiveMap.put("receiveType", "INDEPENDENT");
                        
                        // 🆕 독립적 입고에서 comment에서 회사 정보 추출
                        String companyName = extractCompanyFromComment(log.getComment());
                        receiveMap.put("company", companyName);
                    }
                    
                    // 상태 (입고 대기/입고 완료만)
                    if (log.getLogStatus() != null) {
                        switch (log.getLogStatus()) {
                            case PENDING -> receiveMap.put("status", "입고대기");
                            case CONFIRMED -> receiveMap.put("status", "입고완료");
                            default -> receiveMap.put("status", "입고완료");
                        }
                    } else {
                        receiveMap.put("status", "입고완료");
                    }
                    
                    receiveMap.put("note", log.getComment() != null ? log.getComment() : "");
                    
                    return receiveMap;
                })
                .collect(Collectors.toList());
            
            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("content", receiveList);
            response.put("totalElements", logPage.getTotalElements());
            response.put("totalPages", logPage.getTotalPages());
            response.put("number", logPage.getNumber());
            response.put("size", logPage.getSize());
            response.put("first", logPage.isFirst());
            response.put("last", logPage.isLast());
            
            System.out.println("✅ 입고 이력 조회 완료 - " + logPage.getTotalElements() + "건");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ 입고 이력 조회 API 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("입고 이력 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 🆕 comment에서 회사명 추출 헬퍼 메서드
     */
    private String extractCompanyFromComment(String comment) {
        if (comment == null || comment.isEmpty()) {
            return "독립적 입고";
        }
        
        try {
            // "[독립적 입고] 공급업체: ABC회사" 형태에서 추출
            if (comment.contains("공급업체:")) {
                String[] parts = comment.split("공급업체:");
                if (parts.length > 1) {
                    String supplierPart = parts[1].trim();
                    if (supplierPart.contains(",")) {
                        return supplierPart.split(",")[0].trim();
                    } else {
                        return supplierPart.trim();
                    }
                }
            }
            
            return "독립적 입고";
            
        } catch (Exception e) {
            System.err.println("comment에서 회사명 추출 실패: " + e.getMessage());
            return "독립적 입고";
        }
    }

    /**
     * 입고 등록 - POST /api/receive
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> processReceive(@RequestBody ReceiveRequestDTO request) {
        
        Map<String, String> response = new HashMap<>();
        
        try {
            System.out.println("📦 입고 등록 요청: " + request);
            
            // ReceiveService 호출
            receiveService.processReceive(request);
            
            response.put("status", "success");
            response.put("message", "입고 등록이 완료되었습니다.");
            
            System.out.println("✅ 입고 등록 성공");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            System.err.println("❌ 입고 등록 유효성 오류: " + e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            System.err.println("❌ 입고 등록 시스템 오류: " + e.getMessage());
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "입고 등록 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 입고 상세 조회 - GET /api/receive/{logId} (독립적 입고 정보 개선)
     */
    @GetMapping("/{logId}")
    public ResponseEntity<Map<String, Object>> getReceiveDetail(@PathVariable("logId") Long logId) {
        
        try {
            System.out.println("🔍 입고 상세 조회 - logId: " + logId);
            
            InventoryLog log = inventoryLogRepo.findById(logId)
                .orElseThrow(() -> new RuntimeException("입고 정보를 찾을 수 없습니다."));
            
            if (!LogType.IN.equals(log.getLogType())) {
                throw new RuntimeException("입고 정보가 아닙니다.");
            }
            
            Map<String, Object> detail = new HashMap<>();
            detail.put("id", log.getId());
            detail.put("receiveDate", log.getLogDatetime().toLocalDate().toString());
            
            // 품목 정보
            if (log.getItem() != null) {
                detail.put("itemCode", log.getItem().getCode());
                detail.put("itemName", log.getItem().getName());
                detail.put("unit", log.getItem().getUnit());
            }
            
            // 창고 정보
            if (log.getWarehouse() != null) {
                detail.put("warehouse", log.getWarehouse().getWarehouseName());
            }
            
            detail.put("quantity", log.getQuantity());
            
            // 입고 유형 및 관련 정보 (독립적 입고 개선)
            if (log.getOrderLineItem() != null) {
                detail.put("receiveType", "발주 기반");
                
                if (log.getOrderLineItem().getCompanyOrder() != null) {
                    detail.put("orderNum", log.getOrderLineItem().getCompanyOrder().getOrderNum());
                    
                    if (log.getOrderLineItem().getCompanyOrder().getCompany() != null) {
                        Company company = log.getOrderLineItem().getCompanyOrder().getCompany();
                        detail.put("company", company.getCompanyName());
                        
                        if (company.getEmployee() != null) {
                            detail.put("manager", company.getEmployee().getEmpName());
                        } else {
                            detail.put("manager", "담당자 미지정");
                        }
                    }
                }
            } else {
                detail.put("receiveType", "독립적 입고");
                detail.put("orderNum", "-");
                
                // 🆕 독립적 입고에서 comment에서 회사/담당자 정보 추출
                String comment = log.getComment();
                Map<String, String> extractedInfo = extractSupplierInfoFromComment(comment);
                
                detail.put("company", extractedInfo.get("company"));
                detail.put("manager", extractedInfo.get("manager"));
            }
            
            detail.put("note", log.getComment() != null ? log.getComment() : "");
            
            System.out.println("✅ 입고 상세 조회 완료");
            return ResponseEntity.ok(detail);
            
        } catch (RuntimeException e) {
            System.err.println("❌ 입고 상세 조회 실패: " + e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            System.err.println("❌ 입고 상세 조회 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🆕 comment에서 공급업체와 담당자 정보 추출
     */
    private Map<String, String> extractSupplierInfoFromComment(String comment) {
        Map<String, String> result = new HashMap<>();
        result.put("company", "독립적 입고");
        result.put("manager", "담당자 미지정");
        
        if (comment == null || comment.isEmpty()) {
            return result;
        }
        
        try {
            // "[독립적 입고] 공급업체: ABC회사, 담당자: 홍길동" 형태에서 추출
            if (comment.contains("공급업체:")) {
                String[] parts = comment.split("공급업체:");
                if (parts.length > 1) {
                    String supplierPart = parts[1].trim();
                    
                    // 공급업체명 추출
                    if (supplierPart.contains(",")) {
                        String companyName = supplierPart.split(",")[0].trim();
                        result.put("company", companyName);
                        
                        // 담당자 정보도 있는지 확인
                        if (supplierPart.contains("담당자:")) {
                            String[] managerParts = supplierPart.split("담당자:");
                            if (managerParts.length > 1) {
                                String managerName = managerParts[1].trim();
                                // 추가 텍스트가 있으면 제거 (예: "홍길동 - 추가메모")
                                if (managerName.contains("-")) {
                                    managerName = managerName.split("-")[0].trim();
                                }
                                result.put("manager", managerName);
                            }
                        }
                    } else {
                        // 공급업체명만 있는 경우
                        result.put("company", supplierPart);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("comment에서 공급업체 정보 추출 실패: " + e.getMessage());
        }
        
        return result;
    }
}