// File: /Team_BERP/src/main/java/com/team/berp/mrp/repository/CompanyOrderRepository.java
package com.team.berp.mrp.repository;

import com.team.berp.domain.CompanyOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Mrp_CompanyOrderRepository extends JpaRepository<CompanyOrder, Long> {
    // CompanyOrder 엔티티는 OrderLineItem에서 companyOrder를 통해 가져올 예정이므로,
    // 특별히 커스텀 메소드는 여기서 정의할 필요는 없습니다.
}
