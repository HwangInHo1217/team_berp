package com.team.berp.mrp.controller;

import com.team.berp.mrp.service.MrpService;
import com.team.berp.mrp.dto.MrpViewDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller // 스프링 MVC 컨트롤러임을 명시(HTML 뷰 반환)
@RequiredArgsConstructor // 생성자 주입 자동화
public class MrpController {

    private final MrpService mrpService; // 서비스 계층 의존성 주입

    @GetMapping("/mrp/mrp") // GET /mrp/mrp 요청 시 아래 메서드 실행
    public String showMrpList(Model m) {
        List<MrpViewDto> mrpList = mrpService.getMrpViewList(); // 서비스 계층에서 MRP 리스트 가져오기
        m.addAttribute("mrpList", mrpList); // 모델에 리스트 저장(뷰에서 사용 가능)
        return "/mrp/mrp"; // 뷰 이름 반환 (src/main/resources/templates/mrp/mrp.html 렌더링)
    }
    
    @GetMapping("/order/order")
    public String orerList() {
    	
    	return "/order/order";
    }
    
}
