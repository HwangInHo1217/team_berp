package com.team.berp.client.dto;

import com.team.berp.domain.Company;
import com.team.berp.domain.Employee;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientViewDto {
    private Long companyId;
    private String companyName;
    private Company.CompanyType companyType; // Enum
    private String custCd;
    private String presidentNm;
    private String companyNo;
    private String companyCond;
    private String companyItem;
    private String companyAddr;
    private String companyTel;
    private String companyFax;
    private Long employeeId;
    private String employeeName;
    private String useYn;

    // DTO → Entity 변환
    public Company toEntity(Employee employee) {
        Company company = new Company();
        company.setCompanyId(this.companyId);
        company.setCompanyName(this.companyName);
        company.setCompanyType(this.companyType);
        company.setCustCd(this.custCd);
        company.setPresidentNm(this.presidentNm);
        company.setCompanyNo(this.companyNo);
        company.setCompanyCond(this.companyCond);
        company.setCompanyItem(this.companyItem);
        company.setCompanyAddr(this.companyAddr);
        company.setCompanyTel(this.companyTel);
        company.setCompanyFax(this.companyFax);
        company.setEmployee(employee);
        company.setUseYn(this.useYn != null ? this.useYn : "Y");
        return company;
    }

    // Entity → DTO 변환 (static factory)
    public static ClientViewDto fromEntity(Company company) {
        return ClientViewDto.builder()
            .companyId(company.getCompanyId())
            .companyName(company.getCompanyName())
            .companyType(company.getCompanyType())
            .custCd(company.getCustCd())
            .presidentNm(company.getPresidentNm())
            .companyNo(company.getCompanyNo())
            .companyCond(company.getCompanyCond())
            .companyItem(company.getCompanyItem())
            .companyAddr(company.getCompanyAddr())
            .companyTel(company.getCompanyTel())
            .companyFax(company.getCompanyFax())
            .employeeId(company.getEmployee() != null ? company.getEmployee().getEmployeeId() : null)
            .employeeName(company.getEmployee() != null ? company.getEmployee().getEmpName() : null)
            .useYn(company.getUseYn())
            .build();
    }
}
