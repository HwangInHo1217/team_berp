package com.team.berp.order.controller;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.team.berp.domain.Company;
import com.team.berp.domain.Item;
import com.team.berp.order.dto.OrderPageDto;
import com.team.berp.order.dto.OrderRegisterFormDto;
import com.team.berp.order.service.OrderService;
import java.util.List;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    public String orderPage(Model model) {
        List<Company> customers = orderService.findAllCompanies();
        model.addAttribute("customers", customers);
        List<Item> items = orderService.findAllItems();
        model.addAttribute("items", items);
        model.addAttribute("orders", orderService.findAllOrders());
//        Page<OrderPageDto> paging = orderService.getOrderPage(page, size);
//        model.addAttribute("paging", paging);
        return "order/order";
    }
    
    /*
    @GetMapping("/{page}/{size}")
    public Page<OrderPageDto> page(
    		@PathVariable("page") int page,
    	    @PathVariable("size") int size){
    	return 
    }
    */

    @PostMapping("/add")
    public String addOrder(@ModelAttribute OrderRegisterFormDto form) {
        orderService.registerOrder(form);
        return "redirect:/order";
    }

    @PostMapping("/detail")
    public String detail(@RequestParam Long lineItemId, Model model) {
        model.addAttribute("detailDto", orderService.getOrderDetail(lineItemId));
        return "order/order-detail :: detailModal";
    }
}