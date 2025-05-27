// 1. com/team/berp/order/repository/OrderCompanyRepository.java
package com.team.berp.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.team.berp.domain.Company;

/**
 * Repository for Company entities (주문 고객사 관리).
 */
@Repository
public interface Order_CompanyRepository extends JpaRepository<Company, Long> {
    // 추가적인 커스텀 메서드 예시:
    // List<Company> findByUseYn(String useYn);
}