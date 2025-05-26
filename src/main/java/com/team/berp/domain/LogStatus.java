package com.team.berp.domain;

/**
 * 인벤토리 로그 상태 Enum
 * - 입고(IN) 로그일 때만 사용됨
 * - 출고(OUT), 반품입고(RETURN_IN), 폐기(DISPOSE) 등은 상태값이 필요 없음
 */
public enum LogStatus {

    PENDING("입고 예정"),     // 물품이 입고되기로 예약된 상태 (아직 창고에 들어오지 않음)
    CONFIRMED("입고 확정");   // 실제로 입고가 완료되어 재고로 반영된 상태

    private final String description;

    LogStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
