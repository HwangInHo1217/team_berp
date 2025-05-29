// File: src/main/java/com/team/berp/order/dto/OrderDto.java
package com.team.berp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** 주문 상세ㆍ요약 공용 DTO */
@Data
@NoArgsConstructor
public class OrderDto {

    private Long orderNum;
    private String companyName;
    private String empName;
    private String companyEmpName;
    private LocalDate orderDate;
    private Long orderQty;
    private Long amount;
    private String note;
    private List<OrderLineItemDto> items;

    // 요약 조회용 생성자
    public OrderDto(Long orderNum,
                    String companyName,
                    String empName,
                    String companyEmpName,
                    LocalDate orderDate,
                    Long orderQty,
                    Long amount,
                    String note) {
        this.orderNum = orderNum;
        this.companyName = companyName;
        this.empName = empName;
        this.companyEmpName = companyEmpName;
        this.orderDate = orderDate;
        this.orderQty = orderQty;
        this.amount = amount;
        this.note = note;
    }

    // 상세 조회용 생성자 (items 포함)
    public OrderDto(Long orderNum,
                    String companyName,
                    String empName,
                    String companyEmpName,
                    LocalDate orderDate,
                    Long orderQty,
                    Long amount,
                    String note,
                    List<OrderLineItemDto> items) {
        this(orderNum, companyName, empName, companyEmpName, orderDate, orderQty, amount, note);
        this.items = items;
    }
}


/*   훨씬 간단한 방법
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    private Long orderNum;
    private String companyName;
    private String empName;
    private String companyEmpName;
    private LocalDate orderDate;
    private Long orderQty;
    private Long amount;
    private String note;
    private List<OrderLineItemDto> items;
}
*/