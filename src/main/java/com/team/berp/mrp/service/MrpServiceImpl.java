package com.team.berp.mrp.service;

import com.team.berp.mrp.domain.DomainItem;
import com.team.berp.mrp.domain.DomainMrp;
import com.team.berp.mrp.domain.DomainStock;
import com.team.berp.mrp.dto.MrpViewDto;
import com.team.berp.mrp.repository.EntityMrpRepository;
import com.team.berp.mrp.repository.EntityItemRepository;
import com.team.berp.mrp.repository.EntityStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MrpServiceImpl implements MrpService {
    private final EntityMrpRepository mrpRepository;
    private final EntityItemRepository itemRepository;
    private final EntityStockRepository stockRepository;

    @Override
    public List<MrpViewDto> getMrpViewList() {
        List<DomainMrp> mrpList = mrpRepository.findAll();
        System.out.println("mrpList size: " + mrpList.size());

        return mrpList.stream().map(mrp -> {
            DomainItem item = itemRepository.findById(mrp.getItemId()).orElse(null);
            DomainStock stock = stockRepository.findAll().stream()
                    .filter(s -> s.getItemId().equals(mrp.getItemId()))
                    .findFirst()
                    .orElse(null);

            // 실제 필드에 맞게 수정
            return new MrpViewDto(
                (item != null) ? "" : "",                     // itemCode (추가된 경우 넣기)
                (item != null) ? item.getItemName() : "",
                (item != null) ? item.getItemType() : "",
                (item != null) ? item.getUnit() : "",
                "",                                           // 기준일자: 필요시 prodPlan 등 연동
                mrp.getRequiredQty(),
                (stock != null) ? stock.getQuantity() : 0,
                0,                                            // 확정수량: 추후 구현
                mrp.getRequiredQty() - ((stock != null) ? stock.getQuantity() : 0), // 부족수량 계산
                "",                                           // 소요처: 필요시 prodPlan 등 연동
                0,                                            // 리드타임: 필요시 item 테이블 확장
                ""                                            // 비고
            );
        }).collect(Collectors.toList());
    }
}
