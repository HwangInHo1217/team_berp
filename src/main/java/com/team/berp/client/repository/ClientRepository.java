package com.team.berp.client.repository;

import com.team.berp.domain.Company;
import com.team.berp.domain.Company.CompanyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Company, Long> {

    // ■ 중복 체크
    boolean existsByCompanyNameAndCompanyNoAndUseYn(
        String companyName,
        String companyNo,
        String useYn
    );
    boolean existsByCompanyNameAndCompanyNoAndCompanyIdNotAndUseYn(
        String companyName,
        String companyNo,
        Long excludeCompanyId,
        String useYn
    );

    // ■ 논리삭제 제외 전체 조회
    Page<Company> findByUseYn(String useYn, Pageable pageable);

    // ■ 검색 (부분일치)
    Page<Company> findByCompanyNameContainingAndUseYn(
        String name,
        String useYn,
        Pageable pageable
    );
    Page<Company> findByPresidentNmContainingAndUseYn(
        String ceo,
        String useYn,
        Pageable pageable
    );
    Page<Company> findByCompanyNoContainingAndUseYn(
        String bizNum,
        String useYn,
        Pageable pageable
    );
    Page<Company> findByEmployee_EmpNameContainingAndUseYn(
        String empName,
        String useYn,
        Pageable pageable
    );

    // ■ 유형별 조회
    Page<Company> findByCompanyTypeAndUseYn(
        CompanyType companyType,
        String useYn,
        Pageable pageable
    );

    // ■ 유형별 + 검색
    Page<Company> findByCompanyTypeAndCompanyNameContainingAndUseYn(
        CompanyType companyType,
        String name,
        String useYn,
        Pageable pageable
    );
    Page<Company> findByCompanyTypeAndPresidentNmContainingAndUseYn(
        CompanyType companyType,
        String ceo,
        String useYn,
        Pageable pageable
    );
    Page<Company> findByCompanyTypeAndCompanyNoContainingAndUseYn(
        CompanyType companyType,
        String bizNum,
        String useYn,
        Pageable pageable
    );
    Page<Company> findByCompanyTypeAndEmployee_EmpNameContainingAndUseYn(
        CompanyType companyType,
        String empName,
        String useYn,
        Pageable pageable
    );
    
    boolean existsByCompanyNoAndUseYn(String companyNo, String useYn);
    boolean existsByCompanyNoAndCompanyIdNotAndUseYn(
        String companyNo, Long excludeCompanyId, String useYn
    );
    
    // ■ 회사명 단독 중복 체크용 JPA 메서드
    boolean existsByCompanyNameAndUseYn(String companyName, String useYn);
    boolean existsByCompanyNameAndCompanyIdNotAndUseYn(
        String companyName,
        Long companyId,
        String useYn
    );
}
