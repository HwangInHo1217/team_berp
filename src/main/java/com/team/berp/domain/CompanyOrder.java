// File: /Team_BERP/src/main/java/com/team/berp/domain/CompanyOrder.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


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
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    public enum OrderType { CUSTOMER, SUPPLIER }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    @Column
    private LocalDateTime orderDate;


}
