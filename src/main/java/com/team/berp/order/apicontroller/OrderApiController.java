package com.team.berp.order.apicontroller;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.team.berp.order.dto.*;
import com.team.berp.order.service.OrderService;
import com.team.berp.order.repository.Order_EmployeeRepository;
import com.team.berp.order.repository.Order_CompanyRepository;
import com.team.berp.order.repository.Order_ItemRepository;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class OrderApiController {
  private final OrderService orderService;
  private final Order_CompanyRepository compRepo;
  private final Order_EmployeeRepository empRepo;
  private final Order_ItemRepository itemRepo;

  @GetMapping("/api/order/{id}/detail")
  public OrderDetailDto detail(@PathVariable Long id) {
    return orderService.getOrderDetail(id);
  }

  @GetMapping("/api/order/company/{id}")
  public CompanyContactDto getCompanyInfo(@PathVariable Long id) {
    var c = compRepo.findById(id).orElseThrow();
    CompanyContactDto dto = new CompanyContactDto();
    dto.setEmpName(c.getEmployee().getEmpName());
    dto.setCompanyEmpName(c.getCompanyEmpName());
    return dto;
  }

  @GetMapping("/api/order/item/{id}")
  public OrderDetailDto getItemInfo(@PathVariable Long id) {
    var i = itemRepo.findById(id).orElseThrow();
    OrderDetailDto dto = new OrderDetailDto();
    dto.setUnit(i.getUnit());
    return dto;
  }
}