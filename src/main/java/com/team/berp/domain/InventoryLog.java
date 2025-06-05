package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLog {

    

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false, length = 20)
    private LogType logType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "log_datetime")
    private LocalDateTime logDatetime;
    

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_status", length = 20)
    private LogStatus logStatus;

    // Optional: 출고 근거 (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_line_item_id")
    private OrderLineItem orderLineItem; // 이 클래스가 아직 없으면 주석처리해도 됨

    @PrePersist
    protected void onCreate() {
        if (logDatetime == null) {
            logDatetime = LocalDateTime.now();
        }
    }
}
