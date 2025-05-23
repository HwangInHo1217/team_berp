package com.team.berp.warehouse.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.team.berp.warehouse.service.Warehouse_service;

/**
 * 창고 관리 화면 컨트롤러
 * 창고 목록 페이지 렌더링을 담당합니다.
 */
@Controller
@RequestMapping("/warehouse")
public class WarehouseView_controller {

    private static final Logger log = LoggerFactory.getLogger(WarehouseView_controller.class);
    private final Warehouse_service service;
    
    @Autowired
    public WarehouseView_controller(Warehouse_service service) {
        this.service = service;
    }

    /**
     * 창고 관리 페이지 표시
     */
    @GetMapping
    public String whsPage(
            Model model,
            @RequestParam(name = "useYn", required = false) String useYn,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "searchType", required = false) String searchType) {
        
        try {
            model.addAllAttributes(service.getWhsPageData(useYn, keyword, searchType));
            log.info("창고 목록 페이지 로드 완료");
        } catch (Exception e) {
            log.error("창고 목록 페이지 로드 중 오류 발생", e);
            service.handleWhsPageError(model, e);
        }
        
        return "warehouse/warehouse";
    }
}