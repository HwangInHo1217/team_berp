// File: /Team_BERP/src/main/java/com/team/berp/order/domain/OrderLineItem.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Entity
@Table(name = "order_line_item")
@Builder
public class OrderLineItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderLineItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private CompanyOrder companyOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;
    @Column(length = 10)
    private String unit;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column
    private Long unitQty;

    @Column
    private Long unitPrice;

    @Column(name = "unit_price_all")
    private Long unitPriceall;
    
    

}