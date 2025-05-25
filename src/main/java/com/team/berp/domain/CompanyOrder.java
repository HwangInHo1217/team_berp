// File: /Team_BERP/src/main/java/com/team/berp/domain/CompanyOrder.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_order")
@Getter
@Setter
public class CompanyOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer order_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    public enum OrderType { CUSTOMER, SUPPLIER }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType order_type;
    
    @Column
    private LocalDateTime order_date;
}
