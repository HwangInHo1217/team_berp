package com.team.berp.employee.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.team.berp.employee.dto.EmployeeDto;
import com.team.berp.employee.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
public class EmployeeApiController {
    private final EmployeeRepository employeeRepository;

    @GetMapping("/list")
    public List<EmployeeDto> list() {
        return employeeRepository.findAll().stream()
                .map(EmployeeDto::fromEntity)
                .toList();
    }
}
