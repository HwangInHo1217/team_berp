package com.team.berp.client.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClientController {

    // 거래처 관리 페이지 (html 템플릿 렌더링)
    @GetMapping("/client")
    public String clientMainPage() {
        // src/main/resources/templates/client/client.html
        return "client/client";
    }
}
