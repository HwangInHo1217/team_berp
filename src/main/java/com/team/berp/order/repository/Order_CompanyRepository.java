package com.example.order.repository;

import com.example.order.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    /**
     * 회사명으로 회사 엔티티 조회
     */
    Company findByCompanyName(String companyName);
}