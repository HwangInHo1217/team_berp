package com.team.berp.bom.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.team.berp.bom.dto.BomTreeDTO;
import com.team.berp.bom.repository.BomRepository;
import com.team.berp.bom.repository.BomVersionRepository;
import com.team.berp.domain.Bom;
import com.team.berp.domain.BomVersion;
import com.team.berp.domain.Item;
import com.team.berp.item.repository.ItemRepository;

import lombok.RequiredArgsConstructor;

//BomVersionServiceImpl.java
@Service
@RequiredArgsConstructor
public class BomVersionService {

	private final BomVersionRepository bomVersionRepository;
	private final BomRepository bomRepository; // Bom 엔티티(자재 구성) 조회용
	private final ItemRepository itemRepository;

	
	public BomTreeDTO getBomTreeByVersion(Long versionId) {
		BomVersion version = bomVersionRepository.findById(versionId)
				.orElseThrow(() -> new RuntimeException("해당 BOM 버전이 없습니다. ID=" + versionId));

		// (1) 최상위 노드: 부모 품목 정보
		Item parentItem = version.getParentItem();
		BomTreeDTO root = new BomTreeDTO();
		root.setItemId(parentItem.getId());
		root.setItemName(parentItem.getName());
		root.setChildren(new ArrayList<>());

		// (2) 해당 버전의 모든 Bom(자재 구성) 목록 조회
		// Bom 엔티티에는 버전과 연결되는 필드(bomVersion)가 있으므로
		List<Bom> bomComponents = bomRepository.findByBomVersion_Id(versionId);

		// (3) 재귀적으로 트리 구조를 만들어 주는 헬퍼
		for (Bom b : bomComponents) {
			if (b.getParentItem().getId().equals(parentItem.getId())) {
				// parentItem 바로 아래 자식 노드
				root.getChildren().add(buildSubTree(bomComponents, b));
			}
		}
		return root;
	}

	/**
	 * 재귀: 주어진 Bom(부모→child 링크) 리스트 전체에서, 해당 bom 엔트리가 부모면 자식 Bom을 찾아 subTree로 추가
	 */
	private BomTreeDTO buildSubTree(List<Bom> allBoms, Bom currentBom) {
		BomTreeDTO node = new BomTreeDTO();
		node.setItemId(currentBom.getChildItem().getId());
		node.setItemName(currentBom.getChildItem().getName());
		node.setChildren(new ArrayList<>());

		for (Bom b : allBoms) {
			if (b.getParentItem().getId().equals(currentBom.getChildItem().getId())) {
				node.getChildren().add(buildSubTree(allBoms, b));
			}
		}
		return node;
	}
}