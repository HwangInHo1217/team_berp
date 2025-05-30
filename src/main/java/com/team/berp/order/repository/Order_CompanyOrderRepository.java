package com.team.berp.order.repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.team.berp.domain.CompanyOrder;
import java.time.LocalDate;
@Repository
public interface Order_CompanyOrderRepository extends JpaRepository<CompanyOrder, Long> {
  @Query(value = "SELECT co FROM CompanyOrder co " +
      "JOIN FETCH co.company c " +
      "LEFT JOIN FETCH c.employee e " +
      "LEFT JOIN FETCH co.orderLineItems oli " +
      "LEFT JOIN FETCH oli.item i " +
      "WHERE (:companyId IS NULL OR c.companyId = :companyId) " +
      "AND (:itemId IS NULL OR i.id = :itemId) " +
      "AND (:fromDate IS NULL OR co.orderDate >= :fromDate) " +
      "AND (:toDate IS NULL OR co.orderDate <= :toDate)",
    countQuery = "SELECT count(co) FROM CompanyOrder co " +
      "WHERE (:companyId IS NULL OR co.company.companyId = :companyId) " +
      "AND (:itemId IS NULL OR exists (SELECT 1 FROM OrderLineItem oli WHERE oli.order = co AND oli.item.id = :itemId)) " +
      "AND (:fromDate IS NULL OR co.orderDate >= :fromDate) " +
      "AND (:toDate IS NULL OR co.orderDate <= :toDate)"
  )
  Page<CompanyOrder> findByFilters(
    @Param("companyId") Long companyId,
    @Param("itemId") Long itemId,
    @Param("fromDate") LocalDate fromDate,
    @Param("toDate") LocalDate toDate,
    Pageable pageable);
}