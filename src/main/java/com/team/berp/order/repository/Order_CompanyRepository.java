package com.team.berp.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.Company;

public interface Order_CompanyRepository extends JpaRepository<Company, Long> {

}
