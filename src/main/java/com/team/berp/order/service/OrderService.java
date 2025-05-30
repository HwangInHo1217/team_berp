package com.team.berp.order.service;
import org.springframework.data.domain.Page;
import com.team.berp.order.dto.*;
public interface OrderService {
  Page<OrderPageDto> getPage(Long companyId, Long itemId, java.time.LocalDate fromDate, java.time.LocalDate toDate, int page);
  void registerOrder(OrderRegisterFormDto form);
  OrderDetailDto getOrderDetail(Long orderId);
  void updateOrder(OrderRegisterFormDto form);
  void deleteOrders(java.util.List<Long> ids);
}