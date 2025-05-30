package com.team.berp.client.repository;

import com.team.berp.domain.Company;
import com.team.berp.domain.Company.CompanyType;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Company, Long> {

    // 회사명+사업자번호 중복(등록/수정 시 사용)
    boolean existsByCompanyNameAndCompanyNoAndUseYn(String companyName, String companyNo, String useYn);
    boolean existsByCompanyNameAndCompanyNoAndCompanyIdNotAndUseYn(String companyName, String companyNo, Long companyId, String useYn);

    // 논리삭제 제외(전체)
    Page<Company> findByUseYn(String useYn, Pageable pageable);

    // 회사명 포함(논리삭제 제외)
    Page<Company> findByCompanyNameContainingAndUseYn(String name, String useYn, Pageable pageable);

    // 대표자명 포함(논리삭제 제외)
    Page<Company> findByPresidentNmContainingAndUseYn(String ceo, String useYn, Pageable pageable);

    // 유형별(논리삭제 제외)
    Page<Company> findByCompanyTypeAndUseYn(CompanyType companyType, String useYn, Pageable pageable);

    // 유형+회사명(논리삭제 제외)
    Page<Company> findByCompanyTypeAndCompanyNameContainingAndUseYn(CompanyType companyType, String name, String useYn, Pageable pageable);

    // 유형+대표자명(논리삭제 제외)
    Page<Company> findByCompanyTypeAndPresidentNmContainingAndUseYn(CompanyType companyType, String ceo, String useYn, Pageable pageable);

    //사업장 유형 선택, supplier or customer
    List<Company> findByCompanyType(CompanyType Type);
}
