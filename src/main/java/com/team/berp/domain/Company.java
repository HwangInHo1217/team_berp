// File: /Team_BERP/src/main/java/com/team/berp/domain/Company.java
package com.team.berp.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "company")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer companyId;

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

    @Column(length = 100)
    private String companyAddr;

    @Column(length = 20)
    private String companyTel;

    @Column(length = 20)
    private String companyFax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false, length = 1)
    private String useYn = "Y";

    // getters and setters
    public Integer getCompanyId() {
        return companyId;
    }
    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public CompanyType getCompanyType() {
        return companyType;
    }
    public void setCompanyType(CompanyType companyType) {
        this.companyType = companyType;
    }

    public String getCustCd() {
        return custCd;
    }
    public void setCustCd(String custCd) {
        this.custCd = custCd;
    }

    public String getPresidentNm() {
        return presidentNm;
    }
    public void setPresidentNm(String presidentNm) {
        this.presidentNm = presidentNm;
    }

    public String getCompanyNo() {
        return companyNo;
    }
    public void setCompanyNo(String companyNo) {
        this.companyNo = companyNo;
    }

    public String getCompanyCond() {
        return companyCond;
    }
    public void setCompanyCond(String companyCond) {
        this.companyCond = companyCond;
    }

    public String getCompanyItem() {
        return companyItem;
    }
    public void setCompanyItem(String companyItem) {
        this.companyItem = companyItem;
    }

    public String getCompanyAddr() {
        return companyAddr;
    }
    public void setCompanyAddr(String companyAddr) {
        this.companyAddr = companyAddr;
    }

    public String getCompanyTel() {
        return companyTel;
    }
    public void setCompanyTel(String companyTel) {
        this.companyTel = companyTel;
    }

    public String getCompanyFax() {
        return companyFax;
    }
    public void setCompanyFax(String companyFax) {
        this.companyFax = companyFax;
    }

    public Employee getEmployee() {
        return employee;
    }
    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getUseYn() {
        return useYn;
    }
    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }
}
