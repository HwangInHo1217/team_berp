package com.team.berp.client.service;

import com.team.berp.client.dto.ClientViewDto;
import com.team.berp.client.repository.ClientRepository;
import com.team.berp.domain.Company;
import com.team.berp.domain.Company.CompanyType;
import com.team.berp.domain.Employee;
import com.team.berp.employee.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository repo;
    private final EmployeeRepository employeeRepository;

    @Override
    public Long register(ClientViewDto dto, Employee employee) {
        // -------------------------------
        // 1) 필수 정보 유효성 검사
        // -------------------------------
        ClientViewDto.validateMandatoryAddress(dto);
        ClientViewDto.validateMandatoryEmployee(dto);

        // ---------------------------------------------------
        // 2) “useYn 상관없이” 중복 검사 (정지(N) 포함, 모두 걸러야 함)
        // ---------------------------------------------------
        if (existsBizNumDuplicate(dto.getCompanyNo(), null)) {
            throw new IllegalArgumentException("이미 등록된 사업자번호입니다. (정지된 거래처 포함)");
        }
        if (existsNameDuplicate(dto.getCompanyName(), null)) {
            throw new IllegalArgumentException("이미 동일한 회사명이 존재합니다. (정지된 거래처 포함)");
        }
        if (existsDuplicate(dto.getCompanyName(), dto.getCompanyNo(), null)) {
            throw new IllegalArgumentException("이미 회사명/사업자번호 조합이 존재합니다. (정지된 거래처 포함)");
        }

        // -------------------------------
        // 3) 신규 거래처 저장 (기본 useYn = "Y")
        // -------------------------------
        Company company = dto.toEntity(employee);
        company.setUseYn("Y");
        return repo.save(company).getCompanyId();
    }

    @Override
    public void update(Long companyId, ClientViewDto dto, Employee employee) {
        // -------------------------------
        // 1) 필수 정보 유효성 검사
        // -------------------------------
        ClientViewDto.validateMandatoryAddress(dto);
        ClientViewDto.validateMandatoryEmployee(dto);

        // ------------------------------------------------------
        // 2) “useYn 상관없이” 중복 검사 (정기 수정 시 자기 자신 제외)
        // ------------------------------------------------------
        if (existsBizNumDuplicate(dto.getCompanyNo(), companyId)) {
            throw new IllegalArgumentException("이미 등록된 사업자번호입니다. (정지된 거래처 포함)");
        }
        if (existsNameDuplicate(dto.getCompanyName(), companyId)) {
            throw new IllegalArgumentException("이미 동일한 회사명이 존재합니다. (정지된 거래처 포함)");
        }
        if (existsDuplicate(dto.getCompanyName(), dto.getCompanyNo(), companyId)) {
            throw new IllegalArgumentException("이미 회사명/사업자번호 조합이 존재합니다. (정지된 거래처 포함)");
        }

        // -------------------------------
        // 3) 실제 업데이트
        // -------------------------------
        repo.findById(companyId).ifPresent(company -> {
            dto.updateEntity(company, employee);
            repo.save(company);
        });
    }

    @Override
    public ClientViewDto getById(Long companyId) {
        return repo.findById(companyId)
                   .map(ClientViewDto::fromEntity)
                   .orElseThrow(() -> new RuntimeException("거래처 없음"));
    }

    @Override
    public Page<ClientViewDto> getList(CompanyType type,
                                       String keyword,
                                       String searchType,
                                       Pageable pageable) {
        boolean hasKeyword = (keyword != null && !keyword.isBlank());
        Page<Company> page;

        if (type == null) {
            // 1) 전체 유형: useYn 필터 없이 조회 → 정지(N) 포함
            if (!hasKeyword) {
                page = repo.findAll(pageable);  
            } else {
                switch (searchType) {
                    case "biznum":
                        page = repo.findByCompanyNoContaining(keyword, pageable);
                        break;
                    case "ceo":
                        page = repo.findByPresidentNmContaining(keyword, pageable);
                        break;
                    case "employee":
                    case "empName":
                        page = repo.findByEmployee_EmpNameContaining(keyword, pageable);
                        break;
                    default:  // name
                        page = repo.findByCompanyNameContaining(keyword, pageable);
                }
            }
        } else {
            // 2) 특정 유형: useYn 필터 없이 조회 → 정지(N) 포함
            if (!hasKeyword) {
                page = repo.findByCompanyType(type, pageable);
            } else {
                switch (searchType) {
                    case "biznum":
                        page = repo.findByCompanyTypeAndCompanyNoContaining(type, keyword, pageable);
                        break;
                    case "ceo":
                        page = repo.findByCompanyTypeAndPresidentNmContaining(type, keyword, pageable);
                        break;
                    case "employee":
                    case "empName":
                        page = repo.findByCompanyTypeAndEmployee_EmpNameContaining(type, keyword, pageable);
                        break;
                    default:  // name
                        page = repo.findByCompanyTypeAndCompanyNameContaining(type, keyword, pageable);
                }
            }
        }

        return page.map(ClientViewDto::fromEntity);
    }

    @Override
    public void delete(Long companyId) {
        // 논리 삭제 → useYn = "N"
        repo.findById(companyId).ifPresent(c -> {
            c.setUseYn("N");
            repo.save(c);
        });
    }

    @Override
    public void changeUseYn(Long companyId, String useYn) {
        // 상태 변경 → useYn 값을 "Y" 또는 "N"으로 설정
        repo.findById(companyId).ifPresent(c -> {
            c.setUseYn(useYn);
            repo.save(c);
        });
    }

    @Override
    public boolean existsDuplicate(String companyName, String companyNo, Long excludeId) {
        // “useYn 상관없이” 중복 검사
        if (excludeId == null) {
            return repo.existsByCompanyNameAndCompanyNo(companyName, companyNo);
        }
        return repo.existsByCompanyNameAndCompanyNoAndCompanyIdNot(companyName, companyNo, excludeId);
    }

    @Override
    public boolean existsBizNumDuplicate(String companyNo, Long excludeId) {
        // “useYn 상관없이” 사업자번호 중복 검사
        if (excludeId == null) {
            return repo.existsByCompanyNo(companyNo);
        }
        return repo.existsByCompanyNoAndCompanyIdNot(companyNo, excludeId);
    }

    @Override
    public boolean existsNameDuplicate(String companyName, Long excludeId) {
        // “useYn 상관없이” 회사명 중복 검사
        if (excludeId == null) {
            return repo.existsByCompanyName(companyName);
        }
        return repo.existsByCompanyNameAndCompanyIdNot(companyName, excludeId);
    }

    @Override
    public List<Company> getAllcompanies() {
        // 전체 목록 조회 (정지(N) 포함)
        return repo.findAll();
    }
}
