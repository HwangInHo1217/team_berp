package com.team.berp.stock.controller;

import com.team.berp.stock.service.StockBusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 거래처 API 컨트롤러 (긴급출고용)
 * - 재고 관리에서 긴급출고 시 사용할 거래처 목록 제공
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerApiController {
    
    private final StockBusinessService stockBiz;
    
    /**
     * 긴급출고용 거래처 목록 조회 - GET /api/customers
     * 
     * @param useYn 사용여부 (기본값: Y)
     * @return 활성 거래처 목록
     */
    @GetMapping("/customers")
    public ResponseEntity<List<Map<String, Object>>> getCustomers(
            @RequestParam(name = "useYn", defaultValue = "Y") String useYn) {
        
        try {
            System.out.println("🏢 긴급출고용 거래처 목록 API 요청 - useYn: " + useYn);
            
            // StockBusinessService의 getActiveCustomers 메서드 호출
            List<Map<String, Object>> customers = stockBiz.getActiveCustomers();
            
            System.out.println("✅ 거래처 목록 조회 성공: " + customers.size() + "개");
            
            // 디버그용 로그 - 첫 번째 거래처 정보 출력
            if (!customers.isEmpty()) {
                System.out.println("첫 번째 거래처 예시: " + customers.get(0));
            }
            
            return ResponseEntity.ok(customers);
            
        } catch (Exception e) {
            System.err.println("❌ 거래처 목록 조회 실패: " + e.getMessage());
            e.printStackTrace();
            
            // 실패해도 기본 거래처는 제공하여 시스템이 동작하도록 함
            List<Map<String, Object>> defaultCustomers = List.of(
                Map.of(
                    "id", 0L, 
                    "customerCode", "DEFAULT", 
                    "customerName", "기본 거래처",
                    "companyType", "CUSTOMER"
                )
            );
            
            System.out.println("🔄 기본 거래처로 응답: " + defaultCustomers);
            return ResponseEntity.ok(defaultCustomers);
        }
    }
}