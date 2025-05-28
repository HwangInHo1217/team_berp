package com.team.berp.place.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.team.berp.domain.CompanyOrder;

public interface PlaceRepository extends JpaRepository<CompanyOrder, Long> {

	
	
    /**
     * 거래처명과 발주일자 범위로 발주 내역과 관련된 모든 연관 데이터를 한 번에 조회
     * - CompanyOrder + Company + OrderLineItem + Item을 JOIN FETCH 해서 N+1 문제 방지
     * - LIKE 연산으로 거래처명 부분 검색 가능
     * - 날짜 범위 조건 포함
    일반 Join: SQL조인, 지연로딩 그대로, 조건 조회, 기준 엔티티만 가져옴
    Join FETCH: 연관된 엔티티를 즉시 함께 조회, 즉시 로딩처럼 연관 객체도 함께 가져옴, 
    조회 + N + 1 문제 해결, 기준 + 연관된 엔티티도 함께 가져옴
     */
	
//    @Query("SELECT DISTINCT o FROM CompanyOrder o " + //CompanyOrder 별칭 o, CompanyOrder 엔티티 저회
//           "JOIN FETCH o.company c " + //CompanyOrder가 참조하는 Company 테이블과 즉시 조인
//           "JOIN FETCH o.lineItems li " + //CompanyOrder가 참조하는 OrderLineItem 목록을 즉시 조인
//           "JOIN FETCH li.item i " + //CompanyOrder가 참조하는 Item 테이블과 조인(품목명, 코드, 단위)
//           "WHERE c.companyName LIKE %:companyName% " + //거래처명 검색 조건, %로 문자열 일부만 포함해도 검색되도록 함
//           "AND o.orderDate BETWEEN :startDate AND :endDate") //발주일자 범위 조건
//   
//    List<CompanyOrder> findOrdersWithDetails(
//            @Param("companyName") String companyName,
//            @Param("startDate") LocalDateTime startDate,
//            @Param("endDate") LocalDateTime endDate);

	
//	@Query("SELECT o FROM CompanyOrder o " +
//		       "JOIN FETCH o.company c " +
//		       "JOIN FETCH o.orderLineItems li " + // ✅ 소문자로 수정
//		       "JOIN FETCH li.item i " +
//		       "WHERE c.companyName LIKE CONCAT('%', :companyName, '%') " +
//		       "AND o.orderDate BETWEEN :startDate AND :endDate")
//		List<CompanyOrder> findOrdersWithDetails(
//		    @Param("companyName") String companyName,
//		    @Param("startDate") LocalDateTime startDate,
//		    @Param("endDate") LocalDateTime endDate
//		);

//	@Query("SELECT DISTINCT o FROM CompanyOrder o " +
//		       "JOIN FETCH o.company c " +
//		       "JOIN FETCH o.lineItems li " +
//		       "JOIN FETCH li.item i " +
//		       "LEFT JOIN FETCH c.employee e " +
//		       "ORDER BY o.orderDate DESC, o.id DESC")
//		List<CompanyOrder> findOrdersWithDetails();

	// PlaceRepository.java
	@Query("SELECT DISTINCT o FROM CompanyOrder o " +
	       "JOIN FETCH o.lineItems li " +
	       "JOIN FETCH li.item")
	List<CompanyOrder> findAllWithItems();


}
