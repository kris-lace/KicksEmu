package com.neikeq.kicksemu.game.inventory;

public enum Expiration {
    DAYS_7,
    DAYS_30,
    DAYS_PERM,
    USAGE_10,
    USAGE_50,
    USAGE_100;

    public boolean isPermanent() {
        return this == DAYS_PERM;
    }

    public static Expiration fromInt(int value) {
        switch (value) {
            case 9200007:
            case 9201007:
                return DAYS_7;
            case 9200030:
            case 9201030:
                return  DAYS_30;
            case 9200999:
            case 9201999:
                return DAYS_PERM;
            case 9100010:
            case 9101010:
                return USAGE_10;
            case 9100050:
            case 9101050:
                return USAGE_50;
            case 9100100:
            case 9101100:
                return USAGE_100;
            default:
                return null;
        }
    }

    public int toInt() {
        switch (this) {
            case DAYS_7:
                return 9201007;
            case DAYS_30:
                return 9201030;
            case DAYS_PERM:
                return 9201999;
            case USAGE_10:
                return 9101010;
            case USAGE_50:
                return 9101050;
            case USAGE_100:
                return 9101100;
            default:
                return -1;
        }
    }
}
