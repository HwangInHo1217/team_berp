package com.team.berp.bom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.team.berp.domain.BomVersion;
import com.team.berp.domain.Item;

public interface BomVersionRepository extends JpaRepository<BomVersion, Long> {
	List<BomVersion> findByParentItem(Item parent);
}
