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
    
    //현재 방식
    @GetMapping("/list")
    public List<EmployeeDto> list() {
        return employeeRepository.findAll()  // 1. DB에서 모든 직원 엔티티 가져옴 (List<Employee>)
            .stream()                        // 2. 리스트를 스트림(흐름)으로 변환
            .map(EmployeeDto::fromEntity)    // 3. 각 Employee를 EmployeeDto로 변환 (map)
            .toList();                       // 4. 다시 List<EmployeeDto>로 모아서 반환
    }
}


/*
1-1단계 기본 for문 사용
@GetMapping("/list")
public List<EmployeeDto> list() {
    List<Employee> employeeList = employeeRepository.findAll();
    List<EmployeeDto> dtoList = new ArrayList<>();

    for (int i = 0; i < employeeList.size(); i++) {
        Employee e = employeeList.get(i);
        EmployeeDto dto = EmployeeDto.fromEntity(e);
        dtoList.add(dto);
    }

    return dtoList;
}

1-2단계 향상된 for문 (for-each) 사용
@GetMapping("/list")
public List<EmployeeDto> list() {
    List<Employee> employeeList = employeeRepository.findAll();
    List<EmployeeDto> dtoList = new ArrayList<>();

    for (Employee e : employeeList) {
        EmployeeDto dto = EmployeeDto.fromEntity(e);
        dtoList.add(dto);
    }

    return dtoList;
}

2-1단계 람다식(화살표 함수)
@GetMapping("/list")
public List<EmployeeDto> list() {
    return employeeRepository.findAll().stream()
            .map(e -> EmployeeDto.fromEntity(e))  // 람다식 (화살표 함수)
            .collect(Collectors.toList());        // 자바 16 이전 버전에서는 .toList() 대신 .collect 사용
}


*/