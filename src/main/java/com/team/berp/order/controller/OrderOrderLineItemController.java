package com.team.berp.order.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.team.berp.order.dto.OrderAllListDto;
import com.team.berp.order.dto.OrderFormDto;
import com.team.berp.order.dto.OrderFormListDto;
import com.team.berp.order.dto.OrderOrderLineItemDto;
import com.team.berp.order.service.OrderOrderLineItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/order")	// 앞에 orders라는 경로가 붙음
public class OrderOrderLineItemController {
	private final OrderOrderLineItemService service;
	
	@ModelAttribute("orderFormDto")
    public OrderFormDto orderFormDto() {
        return new OrderFormDto();  
    }
	
	// 모든 주문 목록 정보를 가져오는 기능
	@GetMapping
	public String orderList(Model model) {
		// orders라는 thymeleaf 구문에 값을 넣어줌
		List<OrderAllListDto> orders = service.findAllOrder();
		model.addAttribute("orders", orders);
		/*
		OrderFormListDto orderFormListDto = new OrderFormListDto();
		orderFormListDto.setOrderFormList(Collections.singletonList(new OrderFormDto()));
		model.addAttribute("orderFormListDto", orderFormListDto);
		*/
		
		return "order/order";
	}
	
	// 주문 등록 기능
	@PostMapping
	public String orderForm(@Valid @ModelAttribute("orderFormDto")OrderFormDto orderFormDto, BindingResult bindingResult, Model model) {
		
		if (bindingResult.hasErrors()) {
            // 목록 데이터를 다시 담아 줘야 뷰에서 ${orders}가 비어 있지 않습니다.
			List<OrderAllListDto> orders = service.findAllOrder();
            model.addAttribute("orders", orders);
            return "order/order";
        }
		
		OrderFormDto formDto = service.createOrderLineItem(orderFormDto);
		
		
		return "redirect:/order" + formDto.getOrder_line_item_id();
	}
	
	/*
	@PostMapping
    public String create(
        @Valid 
        @ModelAttribute("orderLineItemFormDto") OrderLineItemFormDto form, // 폼과 DTO를 매핑
        BindingResult br,                                 // 폼 검증 결과
        Model model                                      // 뷰에 다시 데이터를 넘길 때 사용
    ) {
        // 검증 에러가 발생한 경우
        if (br.hasErrors()) {
            // 에러 상태에서 폼을 다시 보여주기 위해 주문 목록을 다시 조회
            model.addAttribute("orders", service.findAll());
            // 에러 메시지와 함께 동일 뷰로 돌아감
            return "order/order";
        }

        // 검증 통과 시 서비스 호출로 주문 생성(헤더 + 라인아이템 저장)
        service.createOrder(form);
        // 리다이렉트 방식으로 GET /orders 페이지로 이동해 목록 갱신
        return "redirect:/orders";
    }
    */
}
