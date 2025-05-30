// File: /Team_BERP/src/main/java/com/team/berp/domain/Mrp.java
package com.team.berp.domain;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Entity
@Table(name = "mrp")
public class Mrp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mrpId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProdPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private Integer requiredQty;

    // ────────────────────────────────────────────────────
    // NEW ▶ 납기요청일 컬럼 추가
    @Column(name = "due_date")
    private LocalDate dueDate;

    // NEW ▶ MRP 상태(Enum) 추가
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private MrpStatus status;
    
    @Column(name = "base_date")
    private LocalDate baseDate;

    @Column(name = "source")
    private String source;

    @Column(name = "lead_time")
    private Integer leadTime;

    @Column(name = "comment", length = 255)
    private String comment;
    
}
