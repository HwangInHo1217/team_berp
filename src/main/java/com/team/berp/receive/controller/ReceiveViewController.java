package com.team.berp.receive.controller;

import com.team.berp.receive.service.ReceiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * 입고 관리 뷰 컨트롤러
 * - 입고 관리 페이지 렌더링
 */
@Controller
@RequiredArgsConstructor
public class ReceiveViewController {

    private final ReceiveService receiveService;

    /**
     * 입고 관리 메인 페이지
     * 
     * @param model 뷰에 전달할 데이터
     * @return 입고 관리 페이지 템플릿
     */
    @GetMapping("/receive")
    public String receivePage(
            @RequestParam(name = "date", required = false) String date,
            Model model) {
        
        try {
            System.out.println("📦 입고 관리 페이지 진입 - date: " + date);
            
            // 기본 날짜 설정 (오늘)
            LocalDate searchDate = LocalDate.now();
            if (date != null && !date.isEmpty()) {
                try {
                    searchDate = LocalDate.parse(date);
                } catch (Exception e) {
                    System.err.println("날짜 파싱 오류, 오늘 날짜로 설정: " + e.getMessage());
                }
            }
            
            // 오늘 입고 요약 통계
            ReceiveService.ReceiveSummary todaySummary = receiveService.getTodayReceiveSummary();
            
            // 모델에 데이터 추가
            model.addAttribute("selectedDate", searchDate.toString());
            model.addAttribute("todayCount", todaySummary.totalCount());
            model.addAttribute("todayQuantity", todaySummary.totalQuantity());
            model.addAttribute("todaySummary", todaySummary.getFormattedSummary());
            
            System.out.println("✅ 입고 관리 페이지 데이터 준비 완료");
            return "receive/receive";
            
        } catch (Exception e) {
            System.err.println("❌ 입고 관리 페이지 오류: " + e.getMessage());
            e.printStackTrace();
            
            // 에러 시 기본값 설정
            model.addAttribute("selectedDate", LocalDate.now().toString());
            model.addAttribute("todayCount", 0);
            model.addAttribute("todayQuantity", 0);
            model.addAttribute("todaySummary", "데이터 로드 실패");
            model.addAttribute("error", "페이지 로드 중 오류가 발생했습니다.");
            
            return "receive/receive";
        }
    }
}