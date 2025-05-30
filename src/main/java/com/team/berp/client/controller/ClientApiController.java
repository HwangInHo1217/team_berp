package com.team.berp.client.controller;

import com.team.berp.client.dto.ClientViewDto;
import com.team.berp.client.service.ClientService;
import com.team.berp.domain.Company.CompanyType;
import com.team.berp.domain.Employee;
import com.team.berp.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientApiController {

    private final ClientService clientService;
    private final EmployeeRepository employeeRepository;

    // 🔹 상태 변경 (정지/복구)
    @PatchMapping("/{companyId}/status")
    public void changeStatus(
        @PathVariable("companyId") Long companyId,
        @RequestParam("useYn") String useYn
    ) {
        clientService.changeUseYn(companyId, useYn);
    }

    // 🔹 거래처 리스트 (검색, 페이징, 정렬)
    @GetMapping
    public Page<ClientViewDto> list(
        @RequestParam(value = "type", required = false) CompanyType type,
        @RequestParam(value = "searchType", required = false, defaultValue = "name") String searchType,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "5") int size,
        @RequestParam(value = "sort", required = false, defaultValue = "companyId,desc") String sort // 정렬 옵션 추가
    ) {
        String[] sortArr = sort.split(",");
        Sort sorting = Sort.by(Sort.Direction.fromString(sortArr.length > 1 ? sortArr[1] : "desc"), sortArr[0]);
        return clientService.getList(type, keyword, searchType, PageRequest.of(page, size, sorting));
    }

    // 🔹 거래처 상세 조회
    @GetMapping("/{companyId}")
    public ClientViewDto get(@PathVariable("companyId") Long companyId) {
        return clientService.getById(companyId);
    }

    // 🔹 거래처 등록 (중복검사 필요)
    @PostMapping
    public Long register(@RequestBody ClientViewDto dto) {


        if (dto.getEmployeeId() == null) {
            throw new IllegalArgumentException("담당자(사원)는 필수입니다.");
        }
        Employee emp = employeeRepository.findById(dto.getEmployeeId())
            .orElseThrow(() -> new RuntimeException("해당 담당자(사원)가 존재하지 않습니다."));
        // 중복 등록 방지(회사명+사업자번호)
        if (clientService.existsDuplicate(dto.getCompanyName(), dto.getCompanyNo(), null)) {
            throw new IllegalArgumentException("이미 동일한 회사명/사업자번호로 등록된 거래처가 있습니다.");
        }


        return clientService.register(dto, emp);
    }

    // 🔹 거래처 수정 (중복검사 필요)
    @PutMapping("/{companyId}")
    public void update(
        @PathVariable("companyId") Long companyId,
        @RequestBody ClientViewDto dto
    ) {
        if (dto.getEmployeeId() == null) {
            throw new IllegalArgumentException("담당자(사원)는 필수입니다.");
        }
        Employee emp = employeeRepository.findById(dto.getEmployeeId())
            .orElseThrow(() -> new RuntimeException("해당 담당자(사원)가 존재하지 않습니다."));
        // 중복 등록 방지(회사명+사업자번호, 수정시 본인 제외)
        if (clientService.existsDuplicate(dto.getCompanyName(), dto.getCompanyNo(), companyId)) {
            throw new IllegalArgumentException("이미 동일한 회사명/사업자번호로 등록된 거래처가 있습니다.");
        }
        clientService.update(companyId, dto, emp);
    }

    // 🔹 거래처 논리삭제 (실제 삭제X)
    @DeleteMapping("/{companyId}")
    public void delete(@PathVariable("companyId") Long companyId) {
        clientService.delete(companyId);
    }

    // 🔹 [프론트 검증용] 중복 등록 체크 API
    @GetMapping("/duplicate")
    public Map<String, Boolean> checkDuplicate(
        @RequestParam("companyName") String companyName,
        @RequestParam("companyNo") String companyNo,
        @RequestParam(value = "excludeId", required = false) Long excludeId
    ) {
        boolean exists = clientService.existsDuplicate(companyName, companyNo, excludeId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("exists", exists);
        return result;
    }
    
    @GetMapping("/duplicate/biznum")
    public Map<String, Boolean> checkBizNumDuplicate(
        @RequestParam("companyNo") String companyNo,
        @RequestParam(value="excludeId", required=false) Long excludeId
    ) {
        boolean exists = clientService.existsBizNumDuplicate(companyNo, excludeId);
        return Map.of("exists", exists);
    }
    
    @GetMapping("/duplicateName")
    public Map<String, Boolean> checkNameDuplicate(@RequestParam String companyName,
                                                   @RequestParam(required=false) Long excludeId) {
        boolean exists = clientService.existsNameDuplicate(companyName, excludeId);
        return Map.of("exists", exists);
    }
    
}
