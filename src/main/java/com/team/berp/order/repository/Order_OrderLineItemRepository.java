package com.team.berp.order.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.team.berp.domain.OrderLineItem;
import java.util.List;
@Repository
public interface Order_OrderLineItemRepository extends JpaRepository<OrderLineItem, Long> {
  @Query("SELECT oli FROM OrderLineItem oli JOIN FETCH oli.item WHERE oli.order.orderId = :orderId")
  List<OrderLineItem> findByOrderId(@Param("orderId") Long orderId);
}