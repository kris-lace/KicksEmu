package com.neikeq.kicksemu.game.inventory.table;

import com.neikeq.kicksemu.game.inventory.shop.Payment;
import com.neikeq.kicksemu.utils.table.Row;

public class SkillInfo {

    private final int id;
    private final short position;
    private final short level;
    private final Payment payment;
    private final Price price;

    public SkillInfo(Row row) {
        row.nextColumn();
        id = Integer.valueOf(row.nextColumn());
        position = Short.valueOf(row.nextColumn());
        row.nextColumn();
        level = Short.valueOf(row.nextColumn());
        payment = Payment.fromInt(Integer.valueOf(row.nextColumn()));
        price = new Price(row);
    }

    public int getId() {
        return id;
    }

    public short getPosition() {
        return position;
    }

    public short getLevel() {
        return level;
    }

    public Price getPrice() {
        return price;
    }

    public Payment getPayment() {
        return payment;
    }
}
