package com.team.berp.domain;

public enum LogType {
    IN,         // 입고
    OUT,        // 출고
    DISPOSE,    // 폐기
    RETURN_IN;  // 반품입고

    public String getLabel() {
        return switch (this) {
            case IN -> "입고";
            case OUT -> "출고";
            case DISPOSE -> "폐기";
            case RETURN_IN -> "반품입고";
        };
    }
}
