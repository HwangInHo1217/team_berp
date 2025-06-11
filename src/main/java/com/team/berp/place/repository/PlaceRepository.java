package com.team.berp.place.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.team.berp.domain.CompanyOrder;

public interface PlaceRepository extends JpaRepository<CompanyOrder, Long> {

    /*거래처명과 발주일자 범위로 발주 내역과 관련된 모든 연관 데이터를 한 번에 조회
     * - CompanyOrder + Company + OrderLineItem + Item을 JOIN FETCH 해서 N+1 문제 방지
     * - LIKE 연산으로 거래처명 부분 검색 가능
     * - 날짜 범위 조건 포함
    일반 Join: SQL조인, 지연로딩 그대로, 조건 조회, 기준 엔티티만 가져옴
    Join FETCH: 연관된 엔티티를 즉시 함께 조회, 즉시 로딩처럼 연관 객체도 함께 가져옴, 
    조회 + N + 1 문제 해결, 기준 + 연관된 엔티티도 함께 가져옴*/

	@Query("SELECT DISTINCT o FROM CompanyOrder o " +
		       "LEFT JOIN FETCH o.company c " +
		       "LEFT JOIN FETCH c.employee e " +
		       "JOIN FETCH o.lineItems li " +
		       "JOIN FETCH li.item " +
		       "WHERE o.orderType = com.team.berp.domain.CompanyOrder.OrderType.SUPPLIER " +
		       "ORDER BY o.orderDate DESC, o.id DESC")
		List<CompanyOrder> findAllWithItems();
	
	// ✅ 품목명으로 발주 검색하는 메서드 추가
	@Query("SELECT DISTINCT co FROM CompanyOrder co " +
	           "JOIN FETCH co.lineItems li " +
	           "JOIN FETCH li.item i " +
	           "JOIN FETCH co.company comp " +
	           "LEFT JOIN FETCH comp.employee " +
	           "WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :itemName, '%')) " + 
	           "AND co.orderType = com.team.berp.domain.CompanyOrder.OrderType.SUPPLIER " +
	           "ORDER BY co.orderDate DESC, co.id DESC")
	    List<CompanyOrder> findOrdersByItemName(@Param("itemName") String itemName);

	 // 🆕 발주 번호로 조회
    Optional<CompanyOrder> findByOrderNum(String orderNum);
    
    // 🆕 발주 상태별 조회
    List<CompanyOrder> findByOrderStatus(CompanyOrder.OrderStatus orderStatus);
    
    // 🆕 발주 유형별 조회
    List<CompanyOrder> findByOrderType(CompanyOrder.OrderType orderType);
    
    // 🆕 공급업체 발주 중 CONFIRMED 상태인 것들만 조회 (입고 처리 가능한 발주)
    @Query("SELECT DISTINCT co FROM CompanyOrder co " +
           "LEFT JOIN FETCH co.company c " +
           "LEFT JOIN FETCH c.employee e " +
           "JOIN FETCH co.lineItems li " +
           "JOIN FETCH li.item " +
           "WHERE co.orderType = com.team.berp.domain.CompanyOrder.OrderType.SUPPLIER " +
           "AND co.orderStatus = com.team.berp.domain.CompanyOrder.OrderStatus.CONFIRMED")
    List<CompanyOrder> findConfirmedSupplierOrders();
    
    // 🆕 회사별 발주 조회
    @Query("SELECT co FROM CompanyOrder co WHERE co.company.companyId = :companyId")
    List<CompanyOrder> findByCompanyId(@Param("companyId") Long companyId);
    
    // 🆕 발주 상태별 + 발주 유형별 조회 (입고 관리에서 사용)
    @Query("SELECT DISTINCT co FROM CompanyOrder co " +
           "LEFT JOIN FETCH co.company c " +
           "LEFT JOIN FETCH c.employee e " +
           "JOIN FETCH co.lineItems li " +
           "JOIN FETCH li.item " +
           "WHERE co.orderType = :orderType AND co.orderStatus = :orderStatus")
    List<CompanyOrder> findByOrderTypeAndOrderStatus(
        @Param("orderType") CompanyOrder.OrderType orderType,
        @Param("orderStatus") CompanyOrder.OrderStatus orderStatus);
}

