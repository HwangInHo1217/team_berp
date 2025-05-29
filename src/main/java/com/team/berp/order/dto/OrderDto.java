package com.team.berp.order.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

/**
 * 주문 DTO (dueDate 제거)
 */
@Data
public class OrderDto {
    private Long   orderNum;
    private String companyName;
    private String manager;
    private String compEmpName;
    private LocalDate orderDate;
    private Long   orderQty;
    private Long   amount;
    private String note;
    private List<OrderLineItemDto> items;

    public OrderDto() {}

    /** 목록용 생성자(라인 아이템 제외) */
    public OrderDto(Long orderNum,
                    String companyName,
                    String manager,
                    String compEmpName,
                    LocalDate orderDate,
                    Long orderQty,
                    Long amount,
                    String note) {
        this.orderNum    = orderNum;
        this.companyName = companyName;
        this.manager     = manager;
        this.compEmpName = compEmpName;
        this.orderDate   = orderDate;
        this.orderQty    = orderQty;
        this.amount      = amount;
        this.note        = note;
    }

    /** 상세용 생성자(라인 아이템 포함) */
    public OrderDto(Long orderNum,
                    String companyName,
                    String manager,
                    String compEmpName,
                    LocalDate orderDate,
                    Long orderQty,
                    Long amount,
                    String note,
                    List<OrderLineItemDto> items) {
        this(orderNum, companyName, manager, compEmpName, orderDate, orderQty, amount, note);
        this.items = items;
    }

    // getters / setters …
}
