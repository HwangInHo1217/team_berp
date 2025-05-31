package com.team.berp.employee.dto;

import com.team.berp.domain.Employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDto {
    private Long employeeId;
    private String empName;
    
    //builder + static
    public static EmployeeDto fromEntity(Employee e) {      // Employee 엔티티를 받아서
        return EmployeeDto.builder()                        // 빌더 패턴으로 EmployeeDto 생성
                .employeeId(e.getEmployeeId())              // 엔티티의 employeeId를 dto에 복사
                .empName(e.getEmpName())                    // 엔티티의 empName을 dto에 복사
                .build();                                   // 완성된 EmployeeDto 객체 반환
    }
}

/*
 
전통적인 방식
// 사용법
public EmployeeDto(Employee e) {
    this.employeeId = e.getEmployeeId();
    this.empName = e.getEmpName();
}
EmployeeDto dto = new EmployeeDto(employee);

//builder 없이
public static EmployeeDto fromEntity(Employee e) {
    EmployeeDto dto = new EmployeeDto();
    dto.setEmployeeId(e.getEmployeeId());
    dto.setEmpName(e.getEmpName());
    return dto;
}

*/