package com.team.berp.warehouse.controller;

import java.util.Map;

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
 * WarehouseView_controller
 *
 * 창고 관리 화면을 렌더링하는 컨트롤러 (Thymeleaf 템플릿 반환용).
 * 창고 목록 조회 및 페이징 결과를 HTML로 응답함.
 */
@Controller
@RequestMapping("/warehouse")
public class WarehouseView_controller {

    private static final Logger log = LoggerFactory.getLogger(WarehouseView_controller.class);
    private final Warehouse_service whsService;

    /**
     * 생성자 기반 의존성 주입
     */
    @Autowired
    public WarehouseView_controller(Warehouse_service whsService) {
        this.whsService = whsService;
    }

    /**
     * [GET] /warehouse
     *
     * 창고 관리 페이지 요청을 처리함.
     * - 사용여부 필터, 검색 키워드, 검색유형, 페이지 번호를 파라미터로 받음
     * - Warehouse_service에서 페이징된 창고 목록을 받아와 Model에 저장
     * - Thymeleaf에서 이를 기반으로 화면 구성
     *
     * @param model       Thymeleaf 뷰에 전달할 데이터 저장소
     * @param useYn       사용여부 필터 (Y / N / ALL)
     * @param keyword     검색 키워드 (코드 or 명)
     * @param searchType  검색 유형 (code / name)
     * @param page        현재 페이지 번호 (기본값: 1)
     * @return            warehouse/warehouse.html 반환
     */
    @GetMapping
    public String whsPage(
            Model model,
            @RequestParam(name = "useYn", required = false) String useYn,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "searchType", required = false) String searchType,
            @RequestParam(name = "page", required = false, defaultValue = "1") Integer page) {

        try {
            // 👉 useYn 파라미터가 비어있거나 null이면 기본값 "ALL" 설정
            String filter = (useYn == null || useYn.isBlank()) ? "ALL" : useYn;

            // ✅ 서비스에서 조건에 맞는 페이징 데이터 가져오기
            Map<String, Object> pageData = whsService.getWhsPageData(filter, keyword, searchType, page);

            // ✅ 모델에 전체 데이터 바인딩 (Thymeleaf에서 사용)
            model.addAllAttributes(pageData);

            // 📝 로그 기록
            log.info("창고 목록 페이지 로드 완료 - 페이지: {}, 필터: {}, 데이터 수: {}",
                    page, filter, pageData.get("totalElements"));

        } catch (Exception e) {
            // ❗ 에러 발생 시 빈 데이터 처리 및 에러 메시지 출력
            log.error("창고 목록 페이지 로드 중 오류 발생", e);
            whsService.handleWhsPageError(model, e);
        }

        // ✅ 렌더링할 뷰 반환 (warehouse/warehouse.html)
        return "warehouse/warehouse";
    }
}