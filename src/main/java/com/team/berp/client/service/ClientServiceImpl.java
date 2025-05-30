package com.team.berp.client.service;

import com.team.berp.client.dto.ClientViewDto;
import com.team.berp.client.repository.ClientRepository;
import com.team.berp.domain.Company;
import com.team.berp.domain.Company.CompanyType;
import com.team.berp.domain.Employee;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository companyRepository;

    // 등록
    @Override
    public Long register(ClientViewDto dto, Employee employee) {
        // 중복체크
        if (existsDuplicate(dto.getCompanyName(), dto.getCompanyNo(), null)) {
            throw new IllegalArgumentException("이미 등록된 거래처입니다 (회사명/사업자번호 중복)");
        }
        Company company = dto.toEntity(employee);
        company.setUseYn("Y"); // 신규는 항상 사용
        Company saved = companyRepository.save(company);
        return saved.getCompanyId();
    }

    // 수정
    @Override
    public void update(Long companyId, ClientViewDto dto, Employee employee) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("거래처 없음"));
        // 중복체크 (자기 자신 제외)
        if (existsDuplicate(dto.getCompanyName(), dto.getCompanyNo(), companyId)) {
            throw new IllegalArgumentException("이미 등록된 거래처입니다 (회사명/사업자번호 중복)");
        }
        company.setCompanyName(dto.getCompanyName());
        company.setCompanyType(dto.getCompanyType());
        company.setCustCd(dto.getCustCd());
        company.setPresidentNm(dto.getPresidentNm());
        company.setCompanyNo(dto.getCompanyNo());
        company.setCompanyCond(dto.getCompanyCond());
        company.setCompanyItem(dto.getCompanyItem());

        // 주소 필드 분리 적용
        company.setPostcode(dto.getPostcode());
        company.setMainAddress(dto.getMainAddress());
        company.setDetailAddress(dto.getDetailAddress());

        company.setCompanyTel(dto.getCompanyTel());
        company.setCompanyFax(dto.getCompanyFax());
        company.setEmployee(employee);

        // 논리삭제된 데이터 수정시 useYn 유지
        if (dto.getUseYn() != null) company.setUseYn(dto.getUseYn());

        companyRepository.save(company);
    }

    // 상세조회
    @Override
    public ClientViewDto getById(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("거래처 없음"));
        return ClientViewDto.fromEntity(company);
    }

    // 리스트조회 (고급 검색/정렬/논리삭제제외)
    @Override
    public Page<ClientViewDto> getList(CompanyType type, String keyword, String searchType, Pageable pageable) {
        Page<Company> page;

        // 논리삭제 제외 (useYn = 'Y'만)
        if (type == null) {
            // 전체
            if (keyword != null && !keyword.isBlank()) {
                if ("ceo".equals(searchType)) {
                    page = companyRepository.findByPresidentNmContainingAndUseYn(keyword, "Y", pageable);
                } else {
                    page = companyRepository.findByCompanyNameContainingAndUseYn(keyword, "Y", pageable);
                }
            } else {
                page = companyRepository.findByUseYn("Y", pageable);
            }
        } else {
            // 유형별
            if (keyword != null && !keyword.isBlank()) {
                if ("ceo".equals(searchType)) {
                    page = companyRepository.findByCompanyTypeAndPresidentNmContainingAndUseYn(type, keyword, "Y", pageable);
                } else {
                    page = companyRepository.findByCompanyTypeAndCompanyNameContainingAndUseYn(type, keyword, "Y", pageable);
                }
            } else {
                page = companyRepository.findByCompanyTypeAndUseYn(type, "Y", pageable);
            }
        }
        return page.map(ClientViewDto::fromEntity);
    }

    // 논리삭제 (실제 삭제X, useYn = 'N')
    @Override
    public void delete(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("거래처 없음"));
        company.setUseYn("N");
        companyRepository.save(company);
    }

    // 상태 변경 (useYn Y/N)
    @Override
    public void changeUseYn(Long companyId, String useYn) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("거래처 없음"));
        company.setUseYn(useYn);
        companyRepository.save(company);
    }

    // 회사명+사업자번호로 중복 등록 여부
    @Override
    public boolean existsDuplicate(String companyName, String companyNo, Long excludeId) {
        if (excludeId == null) {
            return companyRepository.existsByCompanyNameAndCompanyNoAndUseYn(companyName, companyNo, "Y");
        } else {
            return companyRepository.existsByCompanyNameAndCompanyNoAndCompanyIdNotAndUseYn(companyName, companyNo, excludeId, "Y");
        }
    }
    
    //repository에서 사업장 유형 선택, 모두 찾기
    @Override
    public List<Company> getAllcompanies() {
    	return companyRepository.findAll();
    }
}
