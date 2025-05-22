package com.team.berp.warehouse.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.team.berp.warehouse.domain.Warehouse_entity;
import com.team.berp.warehouse.dto.WarehouseResponseDTO;
import com.team.berp.warehouse.repository.Warehouse_repository;

@Controller
@RequestMapping("/warehouse")
public class WarehouseView_controller {

    private static final Logger log = LoggerFactory.getLogger(WarehouseView_controller.class);
    
    @Autowired
    private Warehouse_repository warehouseRepo;

    @GetMapping
    public String warehousePage(
            Model model,
            @RequestParam(name = "useYn",required = false) String useYn) {
        
        try {
            List<Warehouse_entity> warehouses;
            
            // 사용여부 파라미터에 따라 다른 데이터 조회
            if (useYn != null && !useYn.isEmpty()) {
                if (useYn.equals("ALL")) {
                    // 모든 창고 조회
                    warehouses = warehouseRepo.findAll();
                } else {
                    // Y 또는 N으로 필터링
                    warehouses = warehouseRepo.findByUseYn(useYn);
                }
            } else {
                // 기본적으로는 사용 중인 창고만 표시
                warehouses = warehouseRepo.findByUseYn("Y");
            }
            
            // Entity 목록을 DTO 목록으로 변환
            List<WarehouseResponseDTO> warehouseList = warehouses.stream()
                .map(WarehouseResponseDTO::new)
                .collect(java.util.stream.Collectors.toList());
            
            // 모델에 데이터 추가
            model.addAttribute("warehouses", warehouseList);
            model.addAttribute("useYnFilter", useYn != null ? useYn : "Y");
            
            log.info("창고 목록 페이지 로드 완료. 조회된 창고 수: {}", warehouseList.size());
            
        } catch (Exception e) {
            log.error("창고 목록 페이지 로드 중 오류 발생", e);
            model.addAttribute("warehouses", List.of());
            model.addAttribute("error", "창고 목록을 불러오는데 실패했습니다: " + e.getMessage());
        }
        
        return "warehouse/warehouse";
    }
}