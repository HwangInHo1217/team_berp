package com.team.berp.order.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.berp.domain.Company;
import com.team.berp.domain.CompanyOrder;
import com.team.berp.domain.Employee;
import com.team.berp.domain.Item;
import com.team.berp.domain.OrderLineItem;
import com.team.berp.order.dto.OrderAllListDto;
import com.team.berp.order.dto.OrderFormDto;
import com.team.berp.order.dto.OrderOrderLineItemDto;
import com.team.berp.order.dto.OrderOrderLineItemFormDto;
import com.team.berp.order.repository.OrderCompanyOrderRepository;
import com.team.berp.order.repository.OrderCompanyRepository;
import com.team.berp.order.repository.OrderEmployeeRepository;
import com.team.berp.order.repository.OrderItemRepository;
import com.team.berp.order.repository.OrderOrderLineItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderOrderLineItemService {
	private final OrderCompanyOrderRepository ocoRepo;
	private final OrderCompanyRepository ocRepo;
	private final OrderEmployeeRepository oeRepo;
	private final OrderItemRepository oiRepo;
	private final OrderOrderLineItemRepository ooliRepo;
	
	// @Transactional : DB에 읽기, 쓰기 등의 작업을 묶어줌  =>  하나라도 에러가 나면 전체가 다 롤백됨
	// 새로운 주문을 등록하여 그 값을 DB에 insert, update 하는 역할의 메소드
	@Transactional
	public OrderFormDto createOrderLineItem(OrderFormDto orderFormDto) {
		
		// 필요한 엔티티들 조회
		Employee employee = oeRepo.findById(orderFormDto.getEmployee_id()).orElseThrow();
		Company company = ocRepo.findById(orderFormDto.getCompany_id()).orElseThrow();
		CompanyOrder companyOrder = ocoRepo.findById(orderFormDto.getOrder_id()).orElseThrow();
		Item item = oiRepo.findById(orderFormDto.getItem_id()).orElseThrow();
		
		// OrderLineItem에 사용자가 입력한 값을 입력받기 위해, 사용자가 입력한 부분만 작성
		OrderLineItem orderLineItem = new OrderLineItem();
		orderLineItem.setOrder(companyOrder);
		orderLineItem.setItem(item);
		orderLineItem.setOrder_qty(orderFormDto.getOrder_qty());
		
		// DB에 만든 orderLineItem 엔티티 저장
		ooliRepo.save(orderLineItem);
		
		// OrderOrderLineItemDto에 있는 모든 칼럼을 가져와 하나의 dto에 넣음
		OrderFormDto dto = new OrderFormDto();
		dto.setOrder_line_item_id(orderLineItem.getOrder_line_item_id());
		dto.setOrder_id(orderLineItem.getOrder().getOrder_id());
		dto.setOrder_type(orderLineItem.getOrder().getOrder_type());
		dto.setOrder_date(orderLineItem.getOrder().getOrder_date());
		dto.setCompany_id(orderLineItem.getOrder().getCompany().getCompany_id());
		dto.setCompany_name(orderLineItem.getOrder().getCompany().getCompany_name());
		dto.setEmployee_id(orderLineItem.getOrder().getCompany().getEmployee().getEmployee_id());
		dto.setEmp_name(orderLineItem.getOrder().getCompany().getEmployee().getEmp_name());
		dto.setItem_id(orderLineItem.getItem().getItem_id());
		dto.setItem_name(orderLineItem.getItem().getItem_name());
		
		return dto;
	}
	
	// 지금까지의 모든 주문 정보를 가져올 수 있음
	@Transactional(readOnly = true)
	public List<OrderAllListDto> findAllOrder() {
	    // ① Repository에 정의된 모든 OrderLineItem 조회
	    List<OrderLineItem> allOrder = ooliRepo.findAllWithAllRelatedEntitiesFetchJoin();
	    
	    // ② DTO로 변환
	    return allOrder.stream()
	        .map(orderAllList -> {
	        	OrderAllListDto dto = new OrderAllListDto();
	            dto.setOrder_line_item_id(orderAllList.getOrder_line_item_id());
	            dto.setOrder_qty(orderAllList.getOrder_qty());
	            dto.setUnit_price(orderAllList.getUnit_price());
	            dto.setItem_code(orderAllList.getItem_code());
	            dto.setOrder_id(orderAllList.getOrder().getOrder_id());
	            dto.setOrder_type(orderAllList.getOrder().getOrder_type());
	            dto.setOrder_date(orderAllList.getOrder().getOrder_date());
	            dto.setCompany_id(orderAllList.getOrder().getCompany().getCompany_id());
	            dto.setCompany_name(orderAllList.getOrder().getCompany().getCompany_name());
	            dto.setEmployee_id(orderAllList.getOrder().getCompany().getEmployee().getEmployee_id());
	            dto.setEmp_name(orderAllList.getOrder().getCompany().getEmployee().getEmp_name());
	            dto.setItem_id(orderAllList.getItem().getItem_id());
	            dto.setItem_name(orderAllList.getItem().getItem_name());
	            dto.setUnit(orderAllList.getItem().getUnit());
	            return dto;
	        })
	        .collect(Collectors.toList());
	}
	
	
	// 고객사명으로 정보 가져옴
	@Transactional(readOnly = true)
	public List<OrderOrderLineItemDto> findByCompany_Name(String company_name){
		List<OrderLineItem> company_name_search = ooliRepo.findBycompany_nameWithAllRelatedEntitiesFetchJoin(company_name);
		return company_name_search.stream().map(orderInfo -> {
			OrderOrderLineItemDto dto = new OrderOrderLineItemDto();
			dto.setOrder_line_item_id(orderInfo.getOrder_line_item_id());
			dto.setOrder_id(orderInfo.getOrder().getOrder_id());			// 참조한 곳의 컬럼을 가져오는 방법
			dto.setItem_id(orderInfo.getItem().getItem_id());
			dto.setOrder_qty(orderInfo.getOrder_qty());
			dto.setUnit_price(orderInfo.getUnit_price());
			dto.setItem_code(orderInfo.getItem_code());
			
			return dto;
		}).collect(Collectors.toList());
	}
	
	
	// 하나의 주문에 대한 주문 정보를 dto에 담아 리스트로 반환
	@Transactional(readOnly = true)
	public List<OrderOrderLineItemDto> findByOrder_Line_Item_Id(Integer order_line_item_id){
		List<OrderLineItem> order_line_item_id_search = ooliRepo.findByorder_line_item_idWithAllRelatedEntitiesFetchJoin(order_line_item_id);
		return order_line_item_id_search.stream().map(orderInfo -> {
			OrderOrderLineItemDto dto = new OrderOrderLineItemDto();
			dto.setOrder_line_item_id(orderInfo.getOrder_line_item_id());
			dto.setOrder_id(orderInfo.getOrder().getOrder_id());			// 참조한 곳의 컬럼을 가져오는 방법
			dto.setItem_id(orderInfo.getItem().getItem_id());
			dto.setOrder_qty(orderInfo.getOrder_qty());
			dto.setUnit_price(orderInfo.getUnit_price());
			dto.setItem_code(orderInfo.getItem_code());
			
			return dto;
		}).collect(Collectors.toList());
	}
}
