package com.thyagoronald.domain.constants;

public enum FlagEnum {
    SUCCESS(1),
    FAILURE(-1),
    PENDING(0);

    private final int value;

    FlagEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
