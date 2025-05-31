package com.team.berp.domain;


import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bom_version")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BomVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bom_version_id")
    private Long id;

    // 완제품 연결 (ManyToOne)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id", nullable = false)
    private Item parentItem;

    @Column(name = "version_code", nullable = false, unique = true, length = 20)
    private String versionCode;

    @Column(name = "description")
    private String description;

    @Column(name = "use_yn", length = 1)
    private String useYn = "Y";

    // 역방향 - 구성 목록
    @OneToMany(mappedBy = "bomVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bom> bomList;
    
 
}
