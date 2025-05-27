package com.team.berp.client.service;

import com.team.berp.client.dto.ClientViewDto;
import com.team.berp.domain.Company.CompanyType;
import com.team.berp.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientService {
    // 등록
    Long register(ClientViewDto dto, Employee employee);

    // 수정
    void update(Long companyId, ClientViewDto dto, Employee employee);

    // 상세조회
    ClientViewDto getById(Long companyId);

    // 리스트조회(검색, 페이징)
    Page<ClientViewDto> getList(CompanyType type, String keyword, String searchType, Pageable pageable);

    // 삭제
    void delete(Long companyId);

    // 상태 변경
    void changeUseYn(Long companyId, String useYn);
}
