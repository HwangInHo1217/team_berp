package com.team.berp.client.service;

import com.team.berp.client.dto.ClientViewDto;
import com.team.berp.client.repository.ClientRepository;
import com.team.berp.domain.Company;
import com.team.berp.domain.Company.CompanyType;
import com.team.berp.domain.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository companyRepository;

    // 등록
    @Override
    public Integer register(ClientViewDto dto, Employee employee) {
        Company company = dto.toEntity(employee);
        Company saved = companyRepository.save(company);
        return saved.getCompanyId();
    }

    // 수정
    @Override
    public void update(Integer companyId, ClientViewDto dto, Employee employee) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("거래처 없음"));
        company.setCompanyName(dto.getCompanyName());
        company.setCompanyType(dto.getCompanyType());
        company.setCustCd(dto.getCustCd());
        company.setPresidentNm(dto.getPresidentNm());
        company.setCompanyNo(dto.getCompanyNo());
        company.setCompanyCond(dto.getCompanyCond());
        company.setCompanyItem(dto.getCompanyItem());
        company.setCompanyAddr(dto.getCompanyAddr());
        company.setCompanyTel(dto.getCompanyTel());
        company.setCompanyFax(dto.getCompanyFax());
        company.setEmployee(employee);
        company.setUseYn(dto.getUseYn());
        companyRepository.save(company);
    }

    // 상세조회
    @Override
    public ClientViewDto getById(Integer companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("거래처 없음"));
        return ClientViewDto.fromEntity(company);
    }

    // 리스트조회(검색, 페이징)
    @Override
    public Page<ClientViewDto> getList(CompanyType type, String keyword, String searchType, Pageable pageable) {
        Page<Company> page;

        // 검색 + 유형 분기
        if (type == null) {
            // 전체
            if (keyword != null && !keyword.isBlank()) {
                if ("ceo".equals(searchType)) {
                    page = companyRepository.findByPresidentNmContaining(keyword, pageable);
                } else {
                    page = companyRepository.findByCompanyNameContaining(keyword, pageable);
                }
            } else {
                page = companyRepository.findAll(pageable);
            }
        } else {
            // 유형별
            if (keyword != null && !keyword.isBlank()) {
                if ("ceo".equals(searchType)) {
                    page = companyRepository.findByCompanyTypeAndPresidentNmContaining(type, keyword, pageable);
                } else {
                    page = companyRepository.findByCompanyTypeAndCompanyNameContaining(type, keyword, pageable);
                }
            } else {
                page = companyRepository.findByCompanyType(type, pageable);
            }
        }
        return page.map(ClientViewDto::fromEntity);
    }

    // 삭제
    @Override
    public void delete(Integer companyId) {
        companyRepository.deleteById(companyId);
    }

    // 상태 변경 (useYn)
    @Override
    public void changeUseYn(Integer companyId, String useYn) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("거래처 없음"));
        company.setUseYn(useYn);
        companyRepository.save(company);
    }
}
