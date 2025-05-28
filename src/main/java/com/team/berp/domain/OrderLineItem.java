package com.team.berp.domain;

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
    @JoinColumn(name = "order_id", nullable = false)
    private CompanyOrder order;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;
}
