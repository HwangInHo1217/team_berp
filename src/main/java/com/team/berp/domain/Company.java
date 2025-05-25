// File: /Team_BERP/src/main/java/com/team/berp/domain/Company.java
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "company")
@Getter
@Setter
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer company_id;

    @Column(nullable = false, length = 100, unique = true)
    private String company_name;

    public enum CompanyType { CUSTOMER, SUPPLIER, BOTH }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyType company_type;

    @Column(length = 10)
    private String cust_cd;

    @Column(length = 20)
    private String president_nm;

    @Column(length = 20)
    private String company_no;

    @Column(length = 20)
    private String company_cond;

    @Column(length = 20)
    private String company_item;

    @Column(length = 100)
    private String company_addr;

    @Column(length = 20)
    private String company_tel;

    @Column(length = 20)
    private String company_fax;

    // 외래키로 참조하는 엔티티의 컬럼명과 같아도 상관은 없음, 그냥 이렇게 사용하는게 좋아서 이렇게 사용함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false, length = 1)
    private String use_yn = "Y";
}
