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

    public static EmployeeDto fromEntity(Employee e) {
        return EmployeeDto.builder()
                .employeeId(e.getEmployeeId())
                .empName(e.getEmpName())
                .build();
    }
}
