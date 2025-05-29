// File: src/main/java/com/team/berp/order/dto/CompanyContactDto.java
package com.team.berp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 고객사 선택 시 담당자 리턴용 */
@Data
@AllArgsConstructor
public class CompanyContactDto {
    private String empName;         // 우리회사 담당자
    private String companyEmpName;  // 거래처 담당자
}