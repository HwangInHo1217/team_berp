// File: /Team_BERP/src/main/java/com/team/berp/order/domain/OrderLineItem.java
package com.team.berp.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "order_line_item")
public class OrderLineItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderLineItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CompanyOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer unitPrice;

    // getters/setters
    public Integer getOrderLineItemId() { return orderLineItemId; }
    public void setOrderLineItemId(Integer id) { this.orderLineItemId=id; }
    public CompanyOrder getOrder() { return order; }
    public void setOrder(CompanyOrder o) { this.order=o; }
    public Item getItem() { return item; }
    public void setItem(Item i) { this.item=i; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer q) { this.quantity=q; }
    public Integer getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Integer p) { this.unitPrice=p; }
}