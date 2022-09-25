package com.the123saurav.raftee.core.network.message;

public enum FollowerState {
    MATCHING((byte) 1),
    MATCHED((byte) 5),
    JOINING((byte) 11),
    ACTIVE((byte) 15);

    final byte val;

    FollowerState(byte b) {
        val = b;
    }

    public static FollowerState from(byte b) {
        return switch (b) {
            case 0 -> MATCHING;
            case 5 -> MATCHED;
            case 10 -> JOINING;
            case 15 -> ACTIVE;
            default -> throw new IllegalArgumentException("Unexpected byte value: " + b);
        };
    }

    public byte serialize() {
        return this.val;
    }
}
