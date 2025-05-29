
package com.team.berp.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "company")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long companyId;

    @Column(nullable = false, length = 100)
    private String companyName;

    public enum CompanyType { CUSTOMER, SUPPLIER, BOTH }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyType companyType;

    @Column(length = 10)
    private String custCd;

    @Column(length = 20)
    private String presidentNm;

    @Column(length = 20)
    private String companyNo;

    @Column(length = 20)
    private String companyCond;

    @Column(length = 20)
    private String companyItem;
    
    @Column(length = 20)
    private String companyEmpName;

    // ========== 주소 관련 필드 추가 ==========
    @Column(length = 10)
    private String postcode;

    @Column(name = "main_address", length = 100)
    private String mainAddress;

    @Column(name = "detail_address", length = 100)
    private String detailAddress;
    // =======================================

    @Column(length = 20)
    private String companyTel;

    @Column(length = 20)
    private String companyFax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false, length = 1)
    private String useYn = "Y";
}
