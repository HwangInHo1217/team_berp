// File: /Team_BERP/src/main/java/com/team/berp/domain/InventoryLog.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_log")
@Getter
@Setter
public class InventoryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer log_id;

    public enum LogType { IN, OUT }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogType log_type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_line_item_id")
    private OrderLineItem order_line_item;

    @Column
    private LocalDateTime log_datetime;

    @Column(columnDefinition = "TEXT")
    private String comment;

    public enum LogStatus { PENDING, CONFIRMED }

    @Enumerated(EnumType.STRING)
    private LogStatus log_status;
}

