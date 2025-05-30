package com.team.berp.order.repository;

import com.team.berp.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Order_CompanyRepository
    extends JpaRepository<Company, Long> {
    Company findByCompanyName(String companyName);
}
