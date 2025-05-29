/*
 * DTO: ShipmentDto.java
 * Purpose: Represents summary information for a shipment.
 */
package com.team.berp.shipment.dto;

import java.time.LocalDateTime;

/**
 * Shipment summary data transfer object
 */
public class ShipmentDto {
    private Long id;
    private String useYn;
    private LocalDateTime lastStokedDate;
    private String companyName;
    private Long orderQty;
    private Long amount;
    private String empName;
    private String companyEmpName;

    // Constructor
    public ShipmentDto(Long id, String useYn, LocalDateTime lastStokedDate,
                       String companyName, Long orderQty, Long amount,
                       String empName, String companyEmpName) {
        this.id = id;
        this.useYn = useYn;
        this.lastStokedDate = lastStokedDate;
        this.companyName = companyName;
        this.orderQty = orderQty;
        this.amount = amount;
        this.empName = empName;
        this.companyEmpName = companyEmpName;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }

    public LocalDateTime getLastStokedDate() { return lastStokedDate; }
    public void setLastStokedDate(LocalDateTime lastStokedDate) { this.lastStokedDate = lastStokedDate; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public Long getOrderQty() { return orderQty; }
    public void setOrderQty(Long orderQty) { this.orderQty = orderQty; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getEmpName() { return empName; }
    public void setEmpName(String empName) { this.empName = empName; }

    public String getCompanyEmpName() { return companyEmpName; }
    public void setCompanyEmpName(String companyEmpName) { this.companyEmpName = companyEmpName; }
}