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
        ClientViewDto.validateMandatoryAddress(dto);
        ClientViewDto.validateMandatoryEmployee(dto);

        if (existsBizNumDuplicate(dto.getCompanyNo(), null)) {
            throw new IllegalArgumentException("이미 등록된 사업자번호입니다.");
        }
        if (existsNameDuplicate(dto.getCompanyName(), null)) {
            throw new IllegalArgumentException("이미 동일한 회사명이 존재합니다.");
        }
        if (existsDuplicate(dto.getCompanyName(), dto.getCompanyNo(), null)) {
            throw new IllegalArgumentException("이미 회사명/사업자번호 조합이 존재합니다.");
        }

        Company company = dto.toEntity(employee);
        company.setUseYn("Y");
        return repo.save(company).getCompanyId();
    }

    @Override
    public void update(Long companyId, ClientViewDto dto, Employee employee) {
        ClientViewDto.validateMandatoryAddress(dto);
        ClientViewDto.validateMandatoryEmployee(dto);

        if (existsBizNumDuplicate(dto.getCompanyNo(), companyId)) {
            throw new IllegalArgumentException("이미 등록된 사업자번호입니다.");
        }
        if (existsNameDuplicate(dto.getCompanyName(), companyId)) {
            throw new IllegalArgumentException("이미 동일한 회사명이 존재합니다.");
        }
        if (existsDuplicate(dto.getCompanyName(), dto.getCompanyNo(), companyId)) {
            throw new IllegalArgumentException("이미 회사명/사업자번호 조합이 존재합니다.");
        }

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
        String useYn = "Y";
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        Page<Company> page;
        if (type == null) {
            // 1) 전체유형
            if (!hasKeyword) {
                page = repo.findByUseYn(useYn, pageable);
            } else {
                switch (searchType) {
                    case "biznum":
                        page = repo.findByCompanyNoContainingAndUseYn(keyword, useYn, pageable);
                        break;
                    case "ceo":
                        page = repo.findByPresidentNmContainingAndUseYn(keyword, useYn, pageable);
                        break;
                    case "employee":
                    case "empName":
                        page = repo.findByEmployee_EmpNameContainingAndUseYn(keyword, useYn, pageable);
                        break;
                    default:  // name
                        page = repo.findByCompanyNameContainingAndUseYn(keyword, useYn, pageable);
                }
            }
        } else {
            // 2) 특정 유형
            if (!hasKeyword) {
                page = repo.findByCompanyTypeAndUseYn(type, useYn, pageable);
            } else {
                switch (searchType) {
                    case "biznum":
                        page = repo.findByCompanyTypeAndCompanyNoContainingAndUseYn(type, keyword, useYn, pageable);
                        break;
                    case "ceo":
                        page = repo.findByCompanyTypeAndPresidentNmContainingAndUseYn(type, keyword, useYn, pageable);
                        break;
                    case "employee":
                    case "empName":
                        page = repo.findByCompanyTypeAndEmployee_EmpNameContainingAndUseYn(type, keyword, useYn, pageable);
                        break;
                    default:  // name
                        page = repo.findByCompanyTypeAndCompanyNameContainingAndUseYn(type, keyword, useYn, pageable);
                }
            }
        }

        return page.map(ClientViewDto::fromEntity);
    }

    @Override
    public void delete(Long companyId) {
        repo.findById(companyId).ifPresent(c -> {
            c.setUseYn("N");
            repo.save(c);
        });
    }

    @Override
    public void changeUseYn(Long companyId, String useYn) {
        repo.findById(companyId).ifPresent(c -> {
            c.setUseYn(useYn);
            repo.save(c);
        });
    }

    @Override
    public boolean existsDuplicate(String companyName, String companyNo, Long excludeId) {
        if (excludeId == null) {
            return repo.existsByCompanyNameAndCompanyNoAndUseYn(companyName, companyNo, "Y");
        }
        return repo.existsByCompanyNameAndCompanyNoAndCompanyIdNotAndUseYn(companyName, companyNo, excludeId, "Y");
    }

    @Override
    public boolean existsBizNumDuplicate(String companyNo, Long excludeId) {
        if (excludeId == null) {
            return repo.existsByCompanyNoAndUseYn(companyNo, "Y");
        }
        return repo.existsByCompanyNoAndCompanyIdNotAndUseYn(companyNo, excludeId, "Y");
    }

    @Override
    public boolean existsNameDuplicate(String companyName, Long excludeId) {
        if (excludeId == null) {
            return repo.existsByCompanyNameAndUseYn(companyName, "Y");
        }
        return repo.existsByCompanyNameAndCompanyIdNotAndUseYn(companyName, excludeId, "Y");
    }

    //repository에서 사업장 유형 선택, 모두 찾기
    @Override
    public List<Company> getAllcompanies() {
    	return repo.findAll();
    }
    
    @Override
    public Employee getEmployeeById(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("직원 정보가 없습니다. id: " + employeeId));
    }
}