// File: /Team_BERP/src/main/java/com/team/berp/domain/Item.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "item")
@Getter
@Setter
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer item_id;

    @Column(nullable = false, length = 100, unique = true)
    private String item_name;
    
    public enum ItemType { RAW, SEMI, PRODUCT }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType item_type;

    @Column(length = 20)
    private String unit;
    
    @Column(length = 100)
    private String spec;

    @Column(length = 1)
    private String useYn = "Y";

}