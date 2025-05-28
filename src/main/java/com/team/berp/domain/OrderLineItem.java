package com.team.berp.domain;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_line_item")
@Getter
@Setter
public class OrderLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="order_line_item_id")
    private Long orderLineItemId;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private CompanyOrder order;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;
    
	@Column(name = "unit_price")
    private BigDecimal unitPrice;
	

    @Column(name = "unit_qty")
    private Long unitQty;
}
