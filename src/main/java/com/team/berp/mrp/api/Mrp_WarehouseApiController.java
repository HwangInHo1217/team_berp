// File: /Team_BERP/src/main/java/com/team/berp/mrp/api/Mrp_WarehouseApiController.java
package com.team.berp.mrp.api;

import com.team.berp.domain.Item;
import com.team.berp.domain.Stock;
import com.team.berp.domain.Warehouse;
import com.team.berp.mrp.repository.EntityItemRepository;
import com.team.berp.mrp.repository.EntityStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * “품목 코드(itemCode)”를 PathVariable로 받아,
 * 해당 품목이 저장된 창고별 재고 합계를 JSON으로 반환하는 컨트롤러
 *
 *   GET /api/mrp/warehouses/{itemCode}
 *     - {itemCode}에 해당하는 Item 엔티티를 조회
 *     - Stock 테이블에서 (item_id = 해당 Item)의 레코드를 모두 꺼내옴
 *     - 각각의 Stock 엔티티에 연결된 Warehouse 정보를 꺼내, warehouseName별로 수량(quantity)을 합산
 *     - 최종적으로 [{"warehouseName":"물류센터 1호","quantity":25}, ...] 같이 JSON 배열을 반환
 */
@RestController
@RequestMapping("/api/mrp/warehouses")
@RequiredArgsConstructor
public class Mrp_WarehouseApiController {

    private final EntityItemRepository  itemRepository;
    private final EntityStockRepository stockRepository;

    /**
     * {itemCode}에 해당하는 Item의 창고별 재고 합계를 반환
     *
     * 예시 요청:
     *   GET http://localhost:8080/api/mrp/warehouses/PRD002
     *
     * 응답 예시 (200 OK):
     * [
     *   { "warehouseName": "물류센터 1호", "quantity": 25 },
     *   { "warehouseName": "완제품 3창고",   "quantity": 5  },
     *   ...
     * ]
     *
     * 만약 itemCode가 DB에 존재하지 않으면 404 Not Found를 반환합니다.
     */
    @GetMapping("/{itemCode}")
    public ResponseEntity<List<Map<String, Object>>> getWarehousesByItemCode(
            @PathVariable("itemCode") String itemCode
    ) {
        // 1) itemCode로 Item 엔티티 조회
        Item item = itemRepository.findByCode(itemCode);
        if (item == null) {
            // 해당 품목이 없으면 404 리턴
            return ResponseEntity.notFound().build();
        }

        // 2) 해당 Item의 Stock 목록 조회
        List<Stock> stockList = stockRepository.findByItem(item);

        // 3) 창고별 재고 수량 합산
        Map<String, Integer> qtyMap = new HashMap<>();
        for (Stock s : stockList) {
            Warehouse wh = s.getWarehouse();
            if (wh == null) {
                // 혹시 Warehouse 정보가 없는 재고 레코드라면 무시
                continue;
            }
            String whName = wh.getWarehouseName();
            Integer qty    = Optional.ofNullable(s.getQuantity()).orElse(0);
            qtyMap.put(whName, qtyMap.getOrDefault(whName, 0) + qty);
        }

        // 4) 응답용 JSON 배열 생성 (리스트 형태)
        List<Map<String, Object>> responseList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : qtyMap.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("warehouseName", entry.getKey());
            map.put("quantity", entry.getValue());
            responseList.add(map);
        }

        // (선택) 수량이 많은 순서대로 정렬
        responseList.sort((a, b) -> {
            Integer qa = (Integer) a.get("quantity");
            Integer qb = (Integer) b.get("quantity");
            return qb.compareTo(qa);
        });

        return ResponseEntity.ok(responseList);
    }
}
