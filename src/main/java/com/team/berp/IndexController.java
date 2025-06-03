package com.team.berp;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.team.berp.domain.InventoryLog;
import com.team.berp.index.Index_DashboardService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class IndexController {

    private final Index_DashboardService indexDashboardService;

    @GetMapping("/")
    public String getIndex(Model model) {
        // 최신 IN 로그 5개
        List<InventoryLog> inLogs = indexDashboardService.getLatestInLogs(5);
        // 최신 OUT 로그 5개
        List<InventoryLog> outLogs = indexDashboardService.getLatestOutLogs(5);

        model.addAttribute("inLogs", inLogs);
        model.addAttribute("outLogs", outLogs);
        return "index";  // resources/templates/index.html 을 렌더
    }
}
