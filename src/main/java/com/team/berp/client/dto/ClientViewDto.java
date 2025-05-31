package com.team.berp.client.dto;

import com.team.berp.domain.Company;
import com.team.berp.domain.Employee;
import lombok.*;

import org.springframework.util.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientViewDto {
    private Long companyId;
    private String companyName;
    private Company.CompanyType companyType;
    private String custCd;
    private String presidentNm;
    private String companyNo;
    private String companyCond;
    private String companyItem;

    // 주소 관련 필드
    private String postcode;
    private String companyAddr;
    private String detailAddress;

    private String companyTel;
    private String companyFax;
    private Long employeeId;
    private String employeeName;
    private String useYn;

    // ◆ 유효성 검사: 필수 입력 체크
    public static void validateMandatoryAddress(ClientViewDto dto) {
        if (!StringUtils.hasText(dto.getPostcode())
            || !StringUtils.hasText(dto.getCompanyAddr())
            || !StringUtils.hasText(dto.getDetailAddress())) {
            throw new IllegalArgumentException("우편번호, 주소, 상세주소는 반드시 입력해야 합니다.");
        }
    }

    public static void validateMandatoryEmployee(ClientViewDto dto) {
        if (dto.getEmployeeId() == null) {
            throw new IllegalArgumentException("담당자(사원)는 필수입니다.");
        }
    }

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

        // 주소 관련
        company.setPostcode(this.postcode);
        company.setMainAddress(this.companyAddr);
        company.setDetailAddress(this.detailAddress);

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
            // 주소 관련
            .postcode(company.getPostcode())
            .companyAddr(company.getMainAddress())
            .detailAddress(company.getDetailAddress())
            .companyTel(company.getCompanyTel())
            .companyFax(company.getCompanyFax())
            .employeeId(company.getEmployee() != null ? company.getEmployee().getEmployeeId() : null)
            .employeeName(company.getEmployee() != null ? company.getEmployee().getEmpName() : null)
            .useYn(company.getUseYn())
            .build();
    }

    // Entity 업데이트 지원
    public void updateEntity(Company company, Employee employee) {
        company.setCompanyName(this.companyName);
        company.setCompanyType(this.companyType);
        company.setCustCd(this.custCd);
        company.setPresidentNm(this.presidentNm);
        company.setCompanyNo(this.companyNo);
        company.setCompanyCond(this.companyCond);
        company.setCompanyItem(this.companyItem);
        company.setPostcode(this.postcode);
        company.setMainAddress(this.companyAddr);
        company.setDetailAddress(this.detailAddress);
        company.setCompanyTel(this.companyTel);
        company.setCompanyFax(this.companyFax);
        company.setEmployee(employee);
        if (this.useYn != null) company.setUseYn(this.useYn);
    }
}
