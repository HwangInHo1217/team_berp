package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
public class Stock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse; // whs → warehouse로 변경
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "lot_number", length = 50)
    private String lotNumber;
    
    @Column(name = "first_stocked_date")
    private LocalDateTime firstStockedDate;
    
    @Column(name = "last_stocked_date")
    private LocalDateTime lastStockedDate;
    
    @PrePersist
    protected void onCreate() {
        if (firstStockedDate == null) {
            firstStockedDate = LocalDateTime.now();
        }
        lastStockedDate = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastStockedDate = LocalDateTime.now();
    }
}