package com.team.berp.order.service;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.team.berp.domain.*;
import com.team.berp.order.dto.*;
import com.team.berp.order.repository.*;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
  private final Order_CompanyRepository      companyRepo;
  private final Order_EmployeeRepository     employeeRepo;
  private final Order_ItemRepository         itemRepo;
  private final Order_CompanyOrderRepository orderRepo;
  private final Order_OrderLineItemRepository oliRepo;

  @Override
  public Page<OrderPageDto> getPage(Long companyId, Long itemId, LocalDate fromDate, LocalDate toDate, int page) {
    PageRequest pr = PageRequest.of(page, 10, Sort.by("orderDate").descending());
    Page<CompanyOrder> po = orderRepo.findByFilters(companyId, itemId, fromDate, toDate, pr);
    return po.map(co -> {
      OrderPageDto dto = new OrderPageDto();
      dto.setOrderId(co.getOrderId());
      dto.setOrderNum(generateOrderNum(co));
      dto.setOrderDate(co.getOrderDate());
      dto.setCompanyName(co.getCompany().getCompanyName());
      dto.setOrderLineCount(co.getOrderLineItems().size());
      dto.setAmount(co.getAmount());
      dto.setEmpName(co.getCompany().getEmployee().getEmpName());
      dto.setCompanyEmpName(co.getCompany().getCompanyEmpName());
      return dto;
    });
  }

  @Override
  public void registerOrder(OrderRegisterFormDto f) {
    CompanyOrder co = new CompanyOrder();
    Company c = companyRepo.findById(f.getCompanyId()).orElseThrow();
    co.setCompany(c);
    co.setOrderDate(f.getOrderDate());
    co.setNote(f.getNote());
    co.setOrderType(CompanyOrder.OrderType.CUSTOMER);
    long totalQty = f.getUnitQty().stream().mapToLong(Long::longValue).sum();
    co.setOrderQty(totalQty);
    long totalAmt = 0;
    for(int i=0;i<f.getItemId().size();i++){
      totalAmt += f.getUnitQty().get(i)*f.getUnitPrice().get(i);
    }
    co.setAmount(totalAmt);
    orderRepo.save(co);
    for(int i=0;i<f.getItemId().size();i++){
      OrderLineItem li = new OrderLineItem();
      li.setOrder(co);
      li.setItem(itemRepo.findById(f.getItemId().get(i)).orElseThrow());
      li.setUnitQty(f.getUnitQty().get(i));
      li.setUnitPrice(new java.math.BigDecimal(f.getUnitPrice().get(i)));
      oliRepo.save(li);
    }
  }

  @Override
  public OrderDetailDto getOrderDetail(Long orderId) {
    CompanyOrder co = orderRepo.findById(orderId).orElseThrow();
    OrderDetailDto dto = new OrderDetailDto();
    dto.setOrderId(orderId);
    dto.setOrderNum(generateOrderNum(co));
    dto.setItems(oliRepo.findByOrderId(orderId).stream().map(li->{
      OrderLineItemDto ld = new OrderLineItemDto();
      ld.setItemName(li.getItem().getName());
      ld.setUnitQty(li.getUnitQty());
      ld.setUnit(li.getItem().getUnit());
      ld.setUnitPrice(li.getUnitPrice().longValue());
      ld.setUnitPriceAll(li.getUnitQty()*li.getUnitPrice().longValue());
      return ld;
    }).collect(Collectors.toList()));
    return dto;
  }

  @Override
  public void updateOrder(OrderRegisterFormDto f) {
    // 구현: registerOrder 와 유사, 기존 lineItems 삭제 후 추가
    deleteOrders(java.util.List.of(f.getCompanyId())); // 간단 예시
    registerOrder(f);
  }

  @Override
  public void deleteOrders(java.util.List<Long> ids) {
    ids.forEach(orderRepo::deleteById);
  }

  // 주문번호 생성 로직 (예: CUS-xxx)
  private String generateOrderNum(CompanyOrder co) {
    String prefix = co.getOrderType()==CompanyOrder.OrderType.CUSTOMER?"CUS":"SUP";
    long seq = co.getOrderId();
    return String.format("%s-%03d", prefix, seq);
  }
}