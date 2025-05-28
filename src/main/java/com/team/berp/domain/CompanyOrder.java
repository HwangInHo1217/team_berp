// File: /Team_BERP/src/main/java/com/team/berp/domain/CompanyOrder.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Entity
@Table(name = "company_order")
public class CompanyOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    public enum OrderType { customer, supllier }

    @Enumerated(EnumType.STRING)
    @Column
    private OrderType orderType;

    @Column
    private LocalDate orderDate;
    
    @Column(name = "order_qty")
    private Long orderQty;
    
    @Column(name = "amount")
    private Long amount;
    
    @Column(name = "note")
    private String note;
    
    @Column
    private LocalDate receiveDate;

}
