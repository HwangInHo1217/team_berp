//package com.team.berp.place.controller;
//
//import com.team.berp.domain.Employee;
//import com.team.berp.employee.repository.EmployeeRepository;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/employees") // ⭐ 꼭 /employees 붙이자
//public class PlaceApiController {
//
//    private final EmployeeRepository employeeRepository;
//
//    @GetMapping("/{employeeId}")
//    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long employeeId) {
//        return employeeRepository.findById(employeeId)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//}
