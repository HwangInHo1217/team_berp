package com.team.berp.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.team.berp.domain.OrderLineItem;
import com.team.berp.order.dto.OrderAllListDto;

//공동 도메인의 OrderLineItem 엔티티를 참조함
public interface OrderOrderLineItemRepository extends JpaRepository<OrderLineItem, Integer> {
    
	
	// OrderLineItem과 연관된 CompanyOrder, Company, Employee 엔티티 모두 JOIN FETCH로 가져오는 쿼리
	// JOIN FETCH를 사용하면 하나의 SQL쿼리로 연결된 모든 엔티티의 정보를 조회할 수 있음
	@Query("SELECT DISTINCT oli FROM OrderLineItem oli " +	// 기준을 OrderLineItem으로 선택, oli라는 별칭 부여
			"JOIN FETCH oli.item it " +						// OrderLineItem 엔티티에서 item이라는 엔티티를 참조
			"JOIN FETCH oli.order co " +					// OrderLineItem 엔티티에서 company_order라는 엔티티 참조
			"JOIN FETCH co.company cp " + 					// company_order 엔티티에서 company라는 엔티티 참조
			"JOIN FETCH cp.employee ey")					// company라는 엔티티에서 employee라는 엔티티 참조
	List<OrderLineItem> findAllWithAllRelatedEntitiesFetchJoin();	// 연관된 모든 엔티티를 하나의 list에 담음
	
	
	// 입력한 고객사(company_name)와 같은 정보만 조회하는 기능 추가
	@Query("SELECT DISTINCT oli FROM OrderLineItem oli " + 
			"JOIN FETCH oli.item it " + 
			"JOIN FETCH oli.order co " +	
			"JOIN FETCH co.company cp " + 
			"JOIN FETCH cp.employee ey " + 
			"WHERE cp.company_name = :company_name")		// :company_name  =>  매개변수 활용  => 매개변수와 같은 데이터들만 조회
	List<OrderLineItem> findBycompany_nameWithAllRelatedEntitiesFetchJoin(String company_name);	// 매개변수값 받음, 리스트에 담음
	
	// order_line_item_id로 조회하는 즉 주문 순서에 따라 조회, 상세페이지에 정보 출력에 사용
	@Query("SELECT DISTINCT oli FROM OrderLineItem oli " + 
			"JOIN FETCH oli.item it " + 
			"JOIN FETCH oli.order co " +	
			"JOIN FETCH co.company cp " + 
			"JOIN FETCH cp.employee ey " + 
			"WHERE oli.order_line_item_id = :order_line_item_id")
	List<OrderLineItem> findByorder_line_item_idWithAllRelatedEntitiesFetchJoin(Integer order_line_item_id);

}
