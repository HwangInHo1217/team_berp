package com.team.berp.stock.controller;

import com.team.berp.stock.dto.StockResponseDTO;
import com.team.berp.stock.service.StockService;
import com.team.berp.warehouse.dto.WarehouseResponseDTO; // 만약 Warehouse DTO를 사용한다면
import com.team.berp.warehouse.service.Warehouse_service; // Warehouse_service 임포트

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List; // List 임포트
// import java.util.Collections; // Collections 임포트 (필요시)

@Controller
@RequiredArgsConstructor
public class StockViewController {

    private final StockService stockSvc;
    private final Warehouse_service warehouseSvc; // 창고 목록 가져오기 용

    @GetMapping("/stock")
    public String stockPage(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "whsCode", required = false) String whsCode,
            @RequestParam(name = "itemType", required = false) String itemType,     // itemType 파라미터 추가
            @RequestParam(name = "stockStatus", required = false) String stockStatus, // stockStatus 파라미터 추가
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        // 정렬 파라미터도 받을 수 있게 처리 (예: &sort=id,DESC)
        // 여기서는 기본 정렬을 id 내림차순으로 하되, 필요시 파라미터로 받을 수 있도록 확장 가능
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        // StockService의 getList 메소드 호출 시 모든 파라미터 전달
        Page<StockResponseDTO> stocks = stockSvc.getList(keyword, whsCode, itemType, stockStatus, pageable);
        model.addAttribute("stocks", stocks);

        // 창고 목록을 모델에 추가 (Warehouse_service에 getActiveWarehouses() 메소드가 있다고 가정)
        // getActiveWarehouses()가 List<WarehouseResponseDTO>를 반환한다고 가정
        // List<WarehouseResponseDTO> activeWarehouses = warehouseSvc.getActiveWarehouses(); 
        // model.addAttribute("warehouses", activeWarehouses);
        // getActiveWarehouses() 메소드가 Warehouse_service에 없다면 만들어야 함.
        // 또는 Warehouse_service의 getWhsByFilter("Y") 같은 메소드를 활용
        List<com.team.berp.warehouse.dto.WarehouseResponseDTO> activeWarehouses = warehouseSvc.getWhsByFilter("Y");
        model.addAttribute("warehouses", activeWarehouses);


        // 검색 조건 유지를 위해 모델에 추가
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedWhsCode", whsCode);
        model.addAttribute("selectedItemType", itemType);
        model.addAttribute("selectedStockStatus", stockStatus);

        return "stock/stock";
    }
}