// File: /Team_BERP/src/main/java/com/team/berp/domain/Item.java
package com.team.berp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
@ToString
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Item {
   @Id
   @GeneratedValue(strategy=GenerationType.IDENTITY)
   @Column(name="item_id")
   private Long id; //pk
   
   @Column(name="item_code", unique=true, updatable=false, length=20)
   private String code; //코드
   @Column(name="item_name")
   private String name; // 품목 이름
   
   @Enumerated(EnumType.STRING)
   @Column(name="item_type")
   private ItemType type;//자재인지 완제품인지

   @Column(name="unit") //단위
   private String unit; 
   
   @Column(name="spec") //규격
   private String spec;
   
   @Column(name="use_yn") //사용 여부
   private String use;

   @Builder
   public Item(String code, String name, ItemType type, String unit, String spec, String use) {   
      this.code = code;
      this.name = name;
      this.type = type;
      this.unit = unit;
      this.spec = spec;
      this.use = use;
   }
   
   
}
