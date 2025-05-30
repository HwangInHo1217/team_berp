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

	// PlaceRepository.java
//	@Query("SELECT DISTINCT o FROM CompanyOrder o " +
//	       "JOIN FETCH o.lineItems li " +
//	       "JOIN FETCH li.item")
//	List<CompanyOrder> findAllWithItems();

	@Query("SELECT DISTINCT o FROM CompanyOrder o " +
		       "JOIN FETCH o.company c " + //companyorder가 연관된 company를 같이 가져옴
		       "JOIN FETCH c.employee e " +  // 회사 → 직원까지 fetch
		       "JOIN FETCH o.lineItems li " + //주문 상세 항목을 가져옴
		       "JOIN FETCH li.item") //품목 항목을 가져옴
		List<CompanyOrder> findAllWithItems();


}
