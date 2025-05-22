package com.team.berp.mrp.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity                     // 이 클래스가 JPA 엔티티(테이블과 매핑됨)임을 나타냄
@Table(name = "item")       // 실제 DB의 "item" 테이블과 매핑됨을 지정
@Data                       // Lombok: Getter/Setter 등 자동 생성
public class DomainItem {
    @Id                     // PK(Primary Key) 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto_increment(DB에서 자동 증가)
    private Integer itemId; // 품목ID(PK, auto_increment)

    private String itemName;  // 품목명
    private String itemType;  // 품목유형 (raw/semi/product 등)
    private String unit;      // 단위(kg, EA 등)
    private String spec;      // 규격/스펙
    private String useYn;     // 사용여부(Y/N)
}
