
package com.team.berp.client.service;

import com.team.berp.client.dto.ClientViewDto;
import com.team.berp.domain.Company;
import com.team.berp.domain.Company.CompanyType;
import com.team.berp.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClientService {
    // 등록
    Long register(ClientViewDto dto, Employee employee);

    // 수정
    void update(Long companyId, ClientViewDto dto, Employee employee);

    // 상세조회
    ClientViewDto getById(Long companyId);

    // 리스트조회(검색, 페이징, 정렬, 논리삭제 제외)
    Page<ClientViewDto> getList(CompanyType type, String keyword, String searchType, Pageable pageable);

    // 논리삭제 (useYn = N)
    void delete(Long companyId);

    // 상태 변경
    void changeUseYn(Long companyId, String useYn);

    // 회사명+사업자번호로 중복 여부 (등록/수정 둘 다 사용)
    boolean existsDuplicate(String companyName, String companyNo, Long excludeId);

    // 단독 사업자번호 중복 체크
    boolean existsBizNumDuplicate(String companyNo, Long excludeId);

    // 단독 회사명 중복 체크
    boolean existsNameDuplicate(String companyName, Long excludeId);

    // 회사 전체 조회 (타입 포함 여부 없이)
    List<Company> getAllcompanies();

    
 // employee_id로 직원 조회
    Employee getEmployeeById(Long employeeId);

}
