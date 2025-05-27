package com.team.berp.client.repository;

import com.team.berp.domain.Company;
import com.team.berp.domain.Company.CompanyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Company, Long> {

    // 거래처 유형별 페이징 조회 (고객사/매입처/겸용)
    Page<Company> findByCompanyType(CompanyType companyType, Pageable pageable);

    // 거래처명(포함검색) + 유형 필터
    Page<Company> findByCompanyTypeAndCompanyNameContaining(CompanyType companyType, String companyName, Pageable pageable);

    // 대표자명(포함검색) + 유형 필터
    Page<Company> findByCompanyTypeAndPresidentNmContaining(CompanyType companyType, String presidentNm, Pageable pageable);

    // 전체검색 (회사명, 대표자명 둘 다)
    List<Company> findByCompanyNameContainingOrPresidentNmContaining(String name, String ceo);

    Page<Company> findByCompanyNameContaining(String name, Pageable pageable);
    Page<Company> findByPresidentNmContaining(String ceo, Pageable pageable);
}
