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

    /**
     * “/” 또는 “/login” 으로 접근하면 로그인 화면을 보여준다.
     */
    @GetMapping({"/", "/login"})
    public String showLogin() {
        // src/main/resources/templates/login.html 을 렌더링
        return "login";
    }

    /**
     * 로그인 성공 후(클라이언트에서 redirect) 이동할 경로.
     * 실제로는 index.html(대시보드)을 model에 inLogs/outLogs를 담아 렌더한다.
     */
    @GetMapping("/index")
    public String showIndex(Model model) {
        // 최신 IN 로그 5개
        List<InventoryLog> inLogs  = indexDashboardService.getLatestInLogs(5);
        // 최신 OUT 로그 5개
        List<InventoryLog> outLogs = indexDashboardService.getLatestOutLogs(5);

        model.addAttribute("inLogs", inLogs);
        model.addAttribute("outLogs", outLogs);
        return "index";  // resources/templates/index.html 을 렌더
    }
}
