package com.team.berp.order.dto;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
@Data
public class OrderRegisterFormDto {
    private Long companyId;
    private String empName;
    private String companyEmpName;
    private LocalDate orderDate;
    private String note;
    private List<Long> itemId;
    private List<Long> unitQty;
    private List<Long> unitPrice;
    private String orderType;
}