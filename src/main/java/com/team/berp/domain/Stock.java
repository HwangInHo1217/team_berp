package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long id;  // 기본 키(PK)

    // 🔗 품목 정보 (ManyToOne: 재고는 하나의 품목에 속함)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // 🔗 창고 정보 (ManyToOne: 재고는 하나의 창고에 보관됨)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse whs;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "last_stocked_date", insertable = false, updatable = false)
    private LocalDateTime updatedAt; // 마지막 입출고일
    // MySQL에서 자동으로 CURRENT_TIMESTAMP ON UPDATE가 적용되므로 Java에서는 따로 업데이트 안 함

    @Column(name = "first_stocked_date", insertable = false, updatable = false)
    private LocalDateTime stockedAt; // 최초 입고일
    // 최초 입고일 (처음 INSERT 시 고정)

    @Column(name = "lot_number", length = 50)
    private String lotNum; // 로트번호

    // 생성 시점 → JPA에서 자동으로 설정하고 싶다면 @PrePersist 사용 가능
}
