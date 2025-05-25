package com.team.berp.mrp.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.team.berp.domain.Item;     // 품목 엔티티 (공통 도메인)
import com.team.berp.domain.Mrp;     // MRP 엔티티 (공통 도메인)
import com.team.berp.domain.Stock;   // 재고 엔티티 (공통 도메인)
import com.team.berp.mrp.dto.MrpViewDto;
import com.team.berp.mrp.repository.EntityItemRepository;
import com.team.berp.mrp.repository.EntityMrpRepository;
import com.team.berp.mrp.repository.EntityStockRepository;

import lombok.RequiredArgsConstructor;

@Service // 비즈니스 로직 계층임을 명시 (스프링 컴포넌트 스캔 대상)
@RequiredArgsConstructor // 생성자 주입을 Lombok으로 자동화 (final 필드 자동 주입)
public class MrpServiceImpl implements MrpService {
    // ===== 레포지토리 DI(의존성 주입) =====
    private final EntityMrpRepository mrpRepository;
    private final EntityItemRepository itemRepository;
    private final EntityStockRepository stockRepository;

    @Override
    public List<MrpViewDto> getMrpViewList() {
        // 1. 전체 MRP 레코드 조회 (DB에서 다 가져옴)
        List<Mrp> mrpList = mrpRepository.findAll();
        System.out.println("mrpList size: " + mrpList.size());

        // 2. 각 MRP별로 Item/Stock 정보를 join해서 DTO로 변환 (Stream 사용)
        return mrpList.stream().map(mrp -> {
            // 품목 정보 조회 (PK 매칭: mrp의 mrpId가 아니라, itemId로 조회해야 일반적임 - 여기 매핑 주의)
            Item item = itemRepository.findById(mrp.getMrpId()).orElse(null);

            // 재고 정보 조회 (Stock 리스트에서 item이 mrp의 mrpId와 같은 것 찾음 - 실제 매핑 로직 점검 필요)
            Stock stock = stockRepository.findAll().stream()
                    .filter(s -> s.getItem().equals(mrp.getMrpId()))
                    .findFirst()
                    .orElse(null);

            // DTO 생성 (각 필드에 맞게 데이터 세팅)
            return new MrpViewDto(
                (item != null) ? "" : "",                      // itemCode (Item 엔티티에 있으면 입력)
                (item != null) ? item.getItemName() : "",      // 품목명
                (item != null) ? item.getItemType().toString() : "", // 품목유형(ENUM일 경우 toString 필요)
                (item != null) ? item.getUnit() : "",          // 단위
                "",                                            // 기준일자(추후 필요시 세팅)
                mrp.getRequiredQty(),                          // 필요수량(MRP)
                (stock != null) ? stock.getQuantity() : 0,     // 현재고(Stock)
                0,                                             // 확정수량(추후 구현)
                mrp.getRequiredQty() - ((stock != null) ? stock.getQuantity() : 0), // 부족수량(계산)
                "",                                            // 소요처(필요시 연동)
                0,                                             // 리드타임(필요시 연동)
                ""                                             // 비고
            );
        }).collect(Collectors.toList());
    }
}
