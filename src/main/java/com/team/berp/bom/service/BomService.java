package com.team.berp.bom.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team.berp.bom.dto.AddBomRequestDTO;
import com.team.berp.bom.dto.BomEditResponseDTO;
import com.team.berp.bom.dto.BomListViewResponse;
import com.team.berp.bom.dto.BomProductItemDTO;
import com.team.berp.bom.dto.BomVersionResponseDTO;
import com.team.berp.bom.dto.ItemSelectionDTO;
import com.team.berp.bom.dto.UpdateBomRequestDTO;
import com.team.berp.bom.repository.BomRepository;
import com.team.berp.bom.repository.BomVersionRepository;
import com.team.berp.domain.Bom;
import com.team.berp.domain.BomVersion;
import com.team.berp.domain.Item;
import com.team.berp.domain.ItemType;
import com.team.berp.item.repository.ItemRepository;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Service // 스프링 서비스 컴포넌트로 등록
@RequiredArgsConstructor // final 필드를 가진 생성자를 자동 생성
public class BomService {

	private final ItemRepository itemRepository; // 품목(완제품/자재) 관련 JPA Repository
	private final BomRepository bomRepository; // BOM(구성) 관련 Repository
	private final BomVersionRepository bomVersionRepository;

	// bom 사용 여부로 구분하여 조회
	public Page<BomProductItemDTO> getPagedParentProductList(String field, String keyword, String useYn,
			Pageable pageable) {
		String searchValue = (keyword != null) ? keyword : "";
		ItemType type = ItemType.product;

		Page<Item> items;

		// 사용여부는 BOM 테이블에는 없으므로 item의 필드로 따로 필터링하거나 제외해야 함
		if ("name".equals(field)) {
			items = itemRepository.findRegisteredParentItemsByNameContaining(searchValue, type, pageable);
		} else if ("code".equals(field)) {
			items = itemRepository.findRegisteredParentItemsByCodeContaining(searchValue, type, pageable);
		} else {
			items = itemRepository.findRegisteredParentItems(type, pageable);
		}

		// ✅ item → DTO 매핑
		return items.map(item -> new BomProductItemDTO(item.getId(), item.getCode(), item.getName(), item.getSpec(),
				item.getUnit(), item.getUse(), item.getType()));
	}

	/*
	 * // ✅ BOM 등록된 완제품 페이징 + 검색 public Page<BomProductItemDTO>
	 * getPagedParentProductList(String keyword, Pageable pageable) { return
	 * bomRepository.findPagedParentItemsByKeyword(keyword, pageable); }
	 */
	/*
	 * // ✅ 특정 완제품의 BOM 구성 목록 조회 public BomListViewResponse
	 * getBomByParentItemId(Long parentId) { Item parent =
	 * itemRepository.findById(parentId) // parent item 조회 .orElseThrow(() -> new
	 * IllegalArgumentException("존재하지 않는 품목입니다."));
	 * 
	 * List<Bom> bomList = bomRepository.findByParentItemId(parentId); // 해당 품목의 BOM
	 * 구성 조회
	 * 
	 * List<BomListViewResponse.Component> components = new ArrayList<>(); for (Bom
	 * bom : bomList) { // BOM 엔티티 → DTO로 변환 components.add(new
	 * BomListViewResponse.Component( bom.getChildItem().getCode(),
	 * bom.getChildItem().getName(), bom.getQty(), bom.getChildItem().getSpec(), //
	 * 규격 bom.getChildItem().getUnit(), // 단위 bom.getSeqNo(),
	 * formatLossRate(bom.getLossRt()), // "5%" 포맷 bom.getItemPrice() != null ?
	 * String.valueOf(bom.getItemPrice()) : "-", bom.getRemark() != null ?
	 * bom.getRemark() : "-" )); }
	 * 
	 * return new BomListViewResponse( // 최종 응답 DTO 생성 parent.getCode(),
	 * parent.getName(), components ); }
	 */
	// ✅ 2. Service 수정
	public BomListViewResponse getBomByVersionId(Long versionId) {
		BomVersion version = bomVersionRepository.findById(versionId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 BOM 버전입니다."));

		List<Bom> bomList = bomRepository.findByBomVersion(version);

		List<BomListViewResponse.Component> components = new ArrayList<>();
		for (Bom bom : bomList) {
			components.add(new BomListViewResponse.Component(bom.getChildItem().getCode(), bom.getChildItem().getName(),
					bom.getQty(), bom.getChildItem().getSpec(), bom.getChildItem().getUnit(), bom.getSeqNo(),
					formatLossRate(bom.getLossRt()),
					bom.getItemPrice() != null ? String.valueOf(bom.getItemPrice()) : "-",
					bom.getRemark() != null ? bom.getRemark() : "-"));
		}

		return new BomListViewResponse(version.getParentItem().getCode(), version.getParentItem().getName(),
				components);
	}

	public List<BomVersionResponseDTO> getVersionsByParentId(Long parentId) {
		Item parent = itemRepository.findById(parentId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 완제품입니다."));
		List<BomVersion> versions = bomVersionRepository.findByParentItem(parent);

		return versions.stream().map(v -> new BomVersionResponseDTO(v.getId(), v.getVersionCode(), v.getUseYn()))
				.toList();
	}

	// ✅ 로스율을 "10%" 형태로 가공
	private String formatLossRate(BigDecimal lossRt) {
		if (lossRt == null)
			return "-";
		return lossRt.stripTrailingZeros().toPlainString() + "%";
	}

	// ✅ BOM 등록된 완제품 목록 조회
	public List<BomProductItemDTO> getParentProductDTOList() {
		List<Item> items = bomRepository.findDistinctParentItems(); // 중복 없이 parent item 조회
		List<BomProductItemDTO> dtoList = new ArrayList<>();

		for (Item item : items) {
			BomProductItemDTO dto = new BomProductItemDTO(item.getId(), item.getCode(), item.getName(), item.getSpec(),
					item.getUnit(), item.getUse(), item.getType());
			dtoList.add(dto);
		}

		return dtoList;
	}

	// ✅ BOM 등록 기능 (완제품 + 구성 자재들 저장)
	@Transactional
	public void registerBomWithVersion(AddBomRequestDTO dto) {

		// 1. 부모 품목 조회
		Item parentItem = itemRepository.findById(dto.getParentItemId())
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 완제품입니다."));

		// 2. BOM 버전 저장
		BomVersion bomVersion = BomVersion.builder().versionCode(dto.getVersionCode()).description(dto.getDescription())
				.useYn(dto.getUseYn()).parentItem(parentItem).build();

		try {
			bomVersionRepository.save(bomVersion);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException("중복된 BOM 버전 코드입니다.");
		}

		// 3. 자재 구성 저장
		List<Bom> bomList = new ArrayList<>();
		for (AddBomRequestDTO.BomComponent comp : dto.getComponents()) {

			Item childItem = itemRepository.findById(comp.getChildItemId())
					.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자재입니다."));

			Bom bom = Bom.builder().parentItem(parentItem) // ✅ 이거 추가
					.bomVersion(bomVersion).childItem(childItem).qty(comp.getQty()).seqNo(comp.getSeqNo())
					.lossRt(comp.getLossRt()).itemPrice(comp.getItemPrice()).remark(comp.getRemark()).build();

			bomList.add(bom);
		}

		 try {
		        bomRepository.saveAll(bomList);
		    } catch (DataIntegrityViolationException e) {
		        // ✅ 순번 중복 등 UNIQUE 제약 조건 위반 시 사용자에게 알림
		        throw new IllegalArgumentException("BOM 순번은 동일한 버전 내에서 중복될 수 없습니다.");
		    }
	}

	// ✅ BOM 등록용 완제품/자재 선택 리스트
	public ItemSelectionDTO getSelectableItems() {
		return new ItemSelectionDTO(itemRepository.findByType(ItemType.product), // 완제품
				itemRepository.findByType(ItemType.raw) // 자재
		);
	}

	// ✅ 완제품별로 BOM 구성 목록 그룹화
	public List<BomListViewResponse> getBomGroupedByParent() {
		List<Item> parents = itemRepository.findByType(ItemType.product); // 완제품 리스트
		List<BomListViewResponse> result = new ArrayList<>();

		for (Item parent : parents) {
			List<Bom> boms = bomRepository.findByParentItem(parent); // BOM 리스트

			List<BomListViewResponse.Component> components = new ArrayList<>();
			for (Bom b : boms) {
				components.add(new BomListViewResponse.Component(b.getChildItem().getCode(), b.getChildItem().getName(),
						b.getQty(), b.getChildItem().getSpec(), b.getChildItem().getUnit(), b.getSeqNo(),
						formatLossRate(b.getLossRt()),
						b.getItemPrice() != null ? String.valueOf(b.getItemPrice()) : "-",
						b.getRemark() != null ? b.getRemark() : "-"));
			}

			BomListViewResponse dto = new BomListViewResponse(parent.getCode(), parent.getName(), components);
			result.add(dto);
		}

		return result;
	}

	// ✅ BOM 수정 기능 - 기존 BOM 삭제 후 재등록
	// ✅ BOM 수정 기능 (버전 기준 수정)
	@Transactional
	public void updateBom(UpdateBomRequestDTO dto) {
	    // 🔍 BOM 버전 조회 (존재하지 않으면 예외 발생)
	    BomVersion version = bomVersionRepository.findById(dto.getVersionId())
	            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 BOM 버전입니다."));

	    // 🔍 부모 품목 정보 재확인
	    Item parent = itemRepository.findById(dto.getParentItemId())
	            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부모 품목입니다."));

	    // 🔧 버전 정보 업데이트 (설명, 사용여부만 변경)
	    version.setDescription(dto.getDescription());
	    version.setUseYn(dto.getUseYn());

	    // ✅ 기존 BOM 구성 제거 (연관된 BOM 구성들을 전부 제거)
	    List<Bom> existing = bomRepository.findByBomVersion(version);
	    bomRepository.deleteAll(existing);

	    // ✅ 새로운 BOM 구성 재등록
	    List<Bom> newBoms = new ArrayList<>();
	    for (UpdateBomRequestDTO.BomComponent c : dto.getComponents()) {

	        Item child = itemRepository.findById(c.getChildItemId())
	                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자재입니다."));

	        Bom bom = Bom.builder()
	                .parentItem(parent)
	                .bomVersion(version)
	                .childItem(child)
	                .qty(c.getQty())
	                .seqNo(c.getSeqNo())
	                .lossRt(c.getLossRt())
	                .itemPrice(c.getItemPrice())
	                .remark(c.getRemark())
	                .build();

	        newBoms.add(bom);
	    }

	    // ✅ BOM 저장
	    bomRepository.saveAll(newBoms);
	}


	// ✅ BOM 일괄 삭제 (완제품 기준)
	@Transactional
	public void deleteBomsByParentIds(List<Long> parentIds) {
		for (Long parentId : parentIds) {
			bomRepository.deleteByParentItemId(parentId);
		}
	}

	@Transactional
	public void deleteBomVersion(Long versionId) {
		BomVersion version = bomVersionRepository.findById(versionId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 BOM 버전입니다."));

		bomVersionRepository.delete(version); // Cascade 옵션 설정 시 자동으로 BOM도 함께 삭제됨
	}

	public BomEditResponseDTO getBomEditData(Long versionId) {
	    BomVersion version = bomVersionRepository.findById(versionId)
	        .orElseThrow(() -> new RuntimeException("버전 없음"));

	    Item parent = version.getParentItem();

	    List<BomListViewResponse.Component> componentList = version.getBomList().stream()
	        .map(c -> new BomListViewResponse.Component(
	            c.getChildItem().getCode(),
	            c.getChildItem().getName(),
	            c.getQty(),
	            c.getChildItem().getSpec(),
	            c.getChildItem().getUnit(),
	            c.getSeqNo(),
	            c.getLossRt() + "%",
	            c.getItemPrice() != null ? c.getItemPrice().toString() : "0",
	            c.getRemark()
	        ))
	        .toList();

	    return new BomEditResponseDTO(
	        version.getId(),
	        version.getVersionCode(),
	        version.getDescription(),
	        version.getUseYn(),
	        parent.getId(),
	        parent.getCode(),
	        parent.getName(),
	        componentList
	    );
	}

}
