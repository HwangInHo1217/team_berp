package com.team.berp.index;

import java.util.List;

import org.springframework.stereotype.Service;

import com.team.berp.domain.InventoryLog;
import com.team.berp.domain.LogType;           // ← 반드시 LogType enum을 import
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Index_DashboardServiceImpl implements Index_DashboardService {
    
    private final Index_InventoryLogRepository indexInventoryLogRepository;
    
    @Override
    public List<InventoryLog> getLatestInLogs(int limit) {
        // 올바르게: LogType.IN 열거형 객체를 넘겨줍니다.
        return indexInventoryLogRepository.findTop5ByLogTypeOrderByLogDatetimeDesc(LogType.IN);
    }
    
    @Override
    public List<InventoryLog> getLatestOutLogs(int limit) {
        return indexInventoryLogRepository.findTop5ByLogTypeOrderByLogDatetimeDesc(LogType.OUT);
    }
}
