package com.team.berp.order.controller;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.team.berp.order.dto.OrderPageDto;
import com.team.berp.order.dto.OrderRegisterFormDto;
import com.team.berp.order.service.OrderService;
import java.time.LocalDate;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService orderService;

  @GetMapping
  public String orderPage(@RequestParam(required=false) Long companyId,
                          @RequestParam(required=false) Long itemId,
                          @RequestParam(required=false) String fromDate,
                          @RequestParam(required=false) String toDate,
                          @RequestParam(defaultValue="0") int page,
                          Model model) {
    Page<OrderPageDto> op = orderService.getPage(
      companyId, itemId,
      fromDate!=null?LocalDate.parse(fromDate):null,
      toDate!=null?LocalDate.parse(toDate):null,
      page);
    model.addAttribute("orderPage", op);
    model.addAttribute("customers", orderService.findAllCompanies());
    model.addAttribute("items", orderService.findAllItems());
    model.addAttribute("filter", new FilterDto(companyId,itemId,fromDate,toDate));
    return "order/order";
  }

  @PostMapping("/add")
  public String addOrder(OrderRegisterFormDto form) {
    orderService.registerOrder(form);
    return "redirect:/order";
  }

  @PostMapping("/delete")
  public String delete(@RequestBody java.util.List<Long> ids) {
    orderService.deleteOrders(ids);
    return "redirect:/order";
  }
}