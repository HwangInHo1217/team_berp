package com.team.berp.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.Company;
import com.team.berp.domain.Company.CompanyType;

public interface Order_CompanyRepository extends JpaRepository<Company, Long> {
	  // CompanyType(CUSTOMER, SUPPLIER, BOTH)에 따른 회사 목록 조회
    List<Company> findByCompanyType(CompanyType companyType);
}
