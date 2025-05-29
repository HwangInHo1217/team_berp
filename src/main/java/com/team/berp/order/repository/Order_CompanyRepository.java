// File: src/main/java/com/team/berp/order/repository/Order_CompanyRepository.java
package com.team.berp.order.repository;

import com.team.berp.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

/** 고객사(Company) 단일 조회  */
public interface Order_CompanyRepository extends JpaRepository<Company, Long> {
    Company findByCompanyName(String companyName);
}