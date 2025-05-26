package com.team.berp.client.controller;

import com.team.berp.client.dto.ClientViewDto;
import com.team.berp.client.service.ClientService;
import com.team.berp.domain.Company.CompanyType;
import com.team.berp.domain.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientApiController {

    private final ClientService clientService;

    // 상태 변경
    @PatchMapping("/{companyId}/status")
    public void changeStatus(
        @PathVariable("companyId") Integer companyId,
        @RequestParam("useYn") String useYn
    ) {
        clientService.changeUseYn(companyId, useYn);
    }

    // 거래처 리스트 (페이징, 검색)
    @GetMapping
    public Page<ClientViewDto> list(
        @RequestParam(value = "type", required = false) CompanyType type,
        @RequestParam(value = "searchType", required = false) String searchType,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        // service에서 type이 null이면 전체, 아니면 type별 조회
        return clientService.getList(type, keyword, searchType, PageRequest.of(page, size));
    }

    // 거래처 단건 조회
    @GetMapping("/{companyId}")
    public ClientViewDto get(@PathVariable("companyId") Integer companyId) {
        return clientService.getById(companyId);
    }

    // 거래처 등록
    @PostMapping
    public Integer register(@RequestBody ClientViewDto dto) {
        Employee emp = new Employee();
        emp.setEmployeeId(dto.getEmployeeId() != null ? dto.getEmployeeId() : 1L); // 임시
        return clientService.register(dto, emp);
    }

    // 거래처 수정
    @PutMapping("/{companyId}")
    public void update(
        @PathVariable("companyId") Integer companyId,
        @RequestBody ClientViewDto dto
    ) {
        Employee emp = new Employee();
        emp.setEmployeeId(dto.getEmployeeId() != null ? dto.getEmployeeId() : 1L); // 임시
        clientService.update(companyId, dto, emp);
    }

    // 거래처 삭제
    @DeleteMapping("/{companyId}")
    public void delete(@PathVariable("companyId") Integer companyId) {
        clientService.delete(companyId);
    }
}
