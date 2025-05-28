package com.team.berp.order.dto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.team.berp.domain.CompanyOrder.OrderType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 주문 등록 폼 데이터를 바인딩하는 DTO
 */
@Data
public class OrderRegisterFormDto {
	
	private Long orderLineItemId;
	
	private Long orderId;

    /** 고객사 ID */
    // @NotNull(message = "고객사를 선택하세요.")
    private Long companyId;

    /** 주문일 */
    @NotNull(message = "주문일을 입력하세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

    /** 납기일 */
    @NotNull(message = "납기일을 입력하세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    /** 담당자 이름 (선택 입력) */
    private String empName;

    /** 비고 (선택 입력) */
    private String note;

    /** 테이블에 렌더링된 품목 행 개수 (unit_qty) */
    @NotNull
    private Long unitQty;

    /** 선택된 품목 ID 리스트 */
    @NotNull(message = "최소 한 개 이상의 품목을 선택하세요.")
    private List<Long> itemId;

    /** 각 품목별 주문 수량 리스트 */
    @NotNull
    private List<Integer> orderQty;

    /** 각 품목별 단가 리스트 */
    @NotNull
    private List<Long> unitPrice;

    /** 각 품목별 단위 리스트 (readonly) */
    private List<String> unit;
    
    @NotNull(message="주문 구분을 선택하세요.")
    private OrderType orderType;
}