package com.team.berp.order.dto;

/**
 * 고객사 선택 시 반환할 우리사 담당자명(empName)과
 * 고객사 담당자명(companyEmpName)을 담는 DTO
 */
public class CompanyContactDto {
    private String empName;         // 우리사 내부 담당자명
    private String companyEmpName;  // 고객사 담당자명

    // 기본 생성자(for Jackson)
    public CompanyContactDto() {}

    // 필드 모두 초기화하는 생성자
    public CompanyContactDto(String empName, String companyEmpName) {
        this.empName = empName;
        this.companyEmpName = companyEmpName;
    }

    // Getter/Setter
    public String getEmpName() {
        return empName;
    }
    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public String getCompanyEmpName() {
        return companyEmpName;
    }
    public void setCompanyEmpName(String companyEmpName) {
        this.companyEmpName = companyEmpName;
    }
}
