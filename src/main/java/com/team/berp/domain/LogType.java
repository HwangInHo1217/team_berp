package com.team.berp.domain;

public enum LogType {
    IN,         // 입고
    OUT,        // 출고
    TRANSFER;  // 내부 창고 간 이동


    public String getLabel() {
        return switch (this) {
            case IN -> "입고";
            case OUT -> "출고";
            case TRANSFER -> "창고이동";

        };
    }
}
