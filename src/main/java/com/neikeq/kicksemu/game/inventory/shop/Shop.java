package com.neikeq.kicksemu.game.inventory.shop;

import com.neikeq.kicksemu.game.characters.CharacterManager;
import com.neikeq.kicksemu.game.characters.CharacterUtils;
import com.neikeq.kicksemu.game.characters.PlayerInfo;
import com.neikeq.kicksemu.game.characters.Position;
import com.neikeq.kicksemu.game.inventory.Celebration;
import com.neikeq.kicksemu.game.inventory.Expiration;
import com.neikeq.kicksemu.game.inventory.InventoryUtils;
import com.neikeq.kicksemu.game.inventory.Item;
import com.neikeq.kicksemu.game.inventory.Product;
import com.neikeq.kicksemu.game.inventory.Skill;
import com.neikeq.kicksemu.game.inventory.Training;
import com.neikeq.kicksemu.game.inventory.table.CeleInfo;
import com.neikeq.kicksemu.game.inventory.table.InventoryTable;
import com.neikeq.kicksemu.game.inventory.table.ItemInfo;
import com.neikeq.kicksemu.game.inventory.table.LearnInfo;
import com.neikeq.kicksemu.game.inventory.table.OptionInfo;
import com.neikeq.kicksemu.game.inventory.table.SkillInfo;
import com.neikeq.kicksemu.game.sessions.Session;
import com.neikeq.kicksemu.game.users.UserInfo;
import com.neikeq.kicksemu.network.packets.in.ClientMessage;
import com.neikeq.kicksemu.network.packets.out.MessageBuilder;
import com.neikeq.kicksemu.network.packets.out.ServerMessage;
import com.neikeq.kicksemu.storage.MySqlManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

public class Shop {

    public static void purchaseSkill(Session session, ClientMessage msg) {
        Payment payment = Payment.fromInt(msg.readByte());
        int price = msg.readInt();
        int skillId = msg.readInt();
        Expiration expiration = Expiration.fromInt(msg.readInt());

        // If the payment mode is invalid, ignore the request
        if (payment == null || payment == Payment.BOTH) return;

        int playerId = session.getPlayerId();
        int money = getMoneyFromPaymentMode(payment, playerId);
        short position = PlayerInfo.getPosition(playerId);
        short level = PlayerInfo.getLevel(playerId);

        // Get the information about the skill with the requested id which
        // is available for the player's position or its base position
        SkillInfo skillInfo = InventoryTable.getSkillInfo(s -> s.getId() == skillId &&
                (s.getPosition() == position || s.getPosition() == Position.trunk(position)));

        Skill skill = null;
        byte result = 0;

        // If there is a skill with this id and the player position is valid for this skill
        if (skillInfo != null && expiration != null) {
            // If the player meets the level requirements for this skill
            if (level >= skillInfo.getLevel()) {
                int skillPrice = skillInfo.getPrice().getPriceFor(expiration, payment);

                // If the price sent by the client is valid
                if (skillPrice != -1 && skillPrice == price &&
                        skillInfo.getPayment().accepts(payment)) {
                    // If the player has enough money
                    if (price <= money) {
                        Map<Integer, Skill> skills = PlayerInfo.getInventorySkills(playerId);

                        // If the item is not already purchased
                        if (!alreadyPurchased(skillId, skills.values())) {
                            // Initialize skill with the requested data
                            int id = InventoryUtils.getSmallestMissingId(skills.values());
                            byte index = InventoryUtils.getSmallestMissingIndex(skills.values());

                            skill = new Skill(skillId, id, expiration.toInt(), index,
                                    InventoryUtils.expirationToTimestamp(expiration), true);

                            // Add it to the player's inventory
                            skills.put(id, skill);
                            PlayerInfo.addInventorySkill(skill, playerId);
                            // Deduct the price from the player's money
                            sumMoneyToPaymentMode(payment, playerId, -price);
                        } else {
                            // Already purchased
                            result = -10;
                        }
                    } else {
                        // Not enough money
                        result = (byte) (payment == Payment.KASH ? -8 : -5);
                    }
                } else {
                    // The payment mode or price sent by the client is invalid
                    result = (byte) (payment == Payment.KASH ? -2 : -3);
                }
            } else {
                // Invalid level
                result = -9;
            }
        } else {
            // System detected a problem
            // May be due to an invalid skill id or an invalid position for this skill
            result = -1;
        }

        try (Connection con = MySqlManager.getConnection()){
            ServerMessage response = MessageBuilder.purchaseSkill(playerId, skill, result, con);
            session.send(response);
        } catch (SQLException ignored) {}
    }

    public static void purchaseCele(Session session, ClientMessage msg) {
        Payment payment = Payment.fromInt(msg.readByte());
        int price = msg.readInt();
        int celeId = msg.readShort();
        Expiration expiration = Expiration.fromInt(msg.readInt());

        // If the payment mode is invalid, ignore the request
        if (payment == null || payment == Payment.BOTH) return;

        int playerId = session.getPlayerId();
        int money = getMoneyFromPaymentMode(payment, playerId);
        short level = PlayerInfo.getLevel(playerId);

        // Get the information about the celebration with the requested id
        CeleInfo celeInfo = InventoryTable.getCeleInfo(c -> c.getId() == celeId);

        Celebration cele = null;
        byte result = 0;

        // If there is a cele with this id and the player position is valid for this cele
        if (celeInfo != null && expiration != null) {
            // If the player meets the level requirements for this cele
            if (level >= celeInfo.getLevel()) {
                int celePrice = celeInfo.getPrice().getPriceFor(expiration, payment);

                // If the price sent by the client is valid
                if (celePrice != -1 && celePrice == price &&
                        celeInfo.getPayment().accepts(payment)) {
                    // If the player has enough money
                    if (price <= money) {
                        Map<Integer, Celebration> celes =
                                PlayerInfo.getInventoryCelebration(playerId);

                        // If the item is not already purchased
                        if (!alreadyPurchased(celeId, celes.values())) {
                            // Initialize cele with the requested data
                            int id = InventoryUtils.getSmallestMissingId(celes.values());
                            byte index = InventoryUtils.getSmallestMissingIndex(celes.values());
                            index = index > 5 ? 0 : index;

                            cele = new Celebration(celeId, id, expiration.toInt(), index,
                                    InventoryUtils.expirationToTimestamp(expiration), true);

                            // Add it to the player's inventory
                            celes.put(id, cele);
                            PlayerInfo.addInventoryCele(cele, playerId);
                            // Deduct the price from the player's money
                            sumMoneyToPaymentMode(payment, playerId, -price);
                        } else {
                            // Already purchased
                            result = -10;
                        }
                    } else {
                        // Not enough money
                        result = (byte) (payment == Payment.KASH ? -8 : -5);
                    }
                } else {
                    // The payment mode or price sent by the client is invalid
                    result = (byte) (payment == Payment.KASH ? -2 : -3);
                }
            } else {
                // Invalid level
                result = -9;
            }
        } else {
            // System detected a problem
            // May be due to an invalid cele id
            result = -1;
        }

        try (Connection con = MySqlManager.getConnection()) {
            ServerMessage response = MessageBuilder.purchaseCele(playerId, cele, result, con);
            session.send(response);
        } catch (SQLException ignored) {}
    }

    public static void purchaseLearn(Session session, ClientMessage msg) {
        Payment payment = Payment.fromInt(msg.readByte());
        int price = msg.readInt();
        int learnId = msg.readInt();

        // If the payment mode is invalid, ignore the request
        if (payment == null || payment == Payment.BOTH) return;

        int playerId = session.getPlayerId();
        int money = getMoneyFromPaymentMode(payment, playerId);
        short level = PlayerInfo.getLevel(playerId);

        // Get the information about the learn with the requested id
        LearnInfo learnInfo = InventoryTable.getLearnInfo(c -> c.getId() == learnId);

        Training learn = null;
        byte result = 0;

        // If there is a learn with this id and the player position is valid for this learn
        if (learnInfo != null) {
            // If the player meets the level requirements for this learn
            if (level >= learnInfo.getLevel()) {
                int learnPrice = payment == Payment.POINTS ?
                        learnInfo.getPoints() : learnInfo.getKash();

                // If the price sent by the client is valid
                if (learnPrice != -1 && learnPrice == price &&
                        learnInfo.getPayment().accepts(payment)) {
                    // If the player has enough money
                    if (price <= money) {
                        Map<Integer, Training> learns = PlayerInfo.getInventoryTraining(playerId);

                        // If the item is not already purchased
                        if (!alreadyPurchased(learnId, learns.values())) {
                            // Initialize learn with the requested data
                            int id = InventoryUtils.getSmallestMissingId(learns.values());

                            learn = new Training(learnId, id, true);

                            // Add it to the player's inventory
                            learns.put(id, learn);
                            PlayerInfo.addInventoryTraining(learn, playerId);
                            // Deduct the price from the player's money
                            sumMoneyToPaymentMode(payment, playerId, -price);
                        } else {
                            // Already purchased
                            result = -10;
                        }
                    } else {
                        // Not enough money
                        result = (byte) (payment == Payment.KASH ? -8 : -5);
                    }
                } else {
                    // The payment mode or price sent by the client is invalid
                    result = (byte) (payment == Payment.KASH ? -2 : -3);
                }
            } else {
                // Invalid level
                result = -9;
            }
        } else {
            // System detected a problem
            // May be due to an invalid learn id
            result = -1;
        }

        try (Connection con = MySqlManager.getConnection()) {
            ServerMessage response = MessageBuilder.purchaseLearn(playerId, learn, result, con);
            session.send(response);
        } catch (SQLException ignored) {}
    }

    public static void purchaseItem(Session session, ClientMessage msg) {

        // TODO lace

        Payment payment = Payment.fromInt(msg.readByte());
        int price = msg.readInt();
        int itemId = msg.readInt();
        Expiration expiration = Expiration.fromInt(msg.readInt());
        int statsBonusOne = msg.readInt();
        int statsBonusTwo = msg.readInt();

        // If the payment mode is invalid, ignore the request
        if (payment == null || payment == Payment.BOTH) return;
        if (payment == Payment.POINTS && expiration == Expiration.DAYS_PERM) return;

        int playerId = session.getPlayerId();
        int money = getMoneyFromPaymentMode(payment, playerId);
        short level = PlayerInfo.getLevel(playerId);

        // Get the information about the item with the requested id
        ItemInfo itemInfo = InventoryTable.getItemInfo(c -> c.getId() == itemId);

        if (itemInfo != null && (itemInfo.getType() < 101 || itemInfo.getType() > 200)) return;

        OptionInfo optionInfoOne = InventoryTable.getOptionInfo(c -> c.getId() == statsBonusOne);
        OptionInfo optionInfoTwo = InventoryTable.getOptionInfo(c -> c.getId() == statsBonusTwo);

        boolean isInvalidBonus = (optionInfoOne == null && statsBonusOne != 0) ||
                (optionInfoTwo == null && statsBonusTwo != 0);

        boolean isValidBonusLevel =
                (optionInfoOne == null || optionInfoOne.isValidLevel(level, payment)) &&
                (optionInfoTwo == null || optionInfoTwo.isValidLevel(level, payment));

        byte result = 0;

        // If there is a item with this id and the player position is valid for this item
        if (itemInfo != null && expiration != null && !isInvalidBonus) {
            // If the player meets the level requirements for this item
            if (level >= itemInfo.getLevel() && isValidBonusLevel) {
                int itemPrice = itemInfo.getPrice().getPriceFor(expiration, payment);
                itemPrice += optionInfoOne == null ? 0 :
                        optionInfoOne.getPrice().getPriceFor(expiration, payment);
                itemPrice += optionInfoTwo == null ? 0 :
                        optionInfoTwo.getPrice().getPriceFor(expiration, payment);

                // If the price sent by the client is valid
                if (itemPrice != -1 && itemPrice == price &&
                        itemInfo.getPayment().accepts(payment)) {
                    // If the player has enough money
                    if (price <= money) {
                        Map<Integer, Item> items = PlayerInfo.getInventoryItems(playerId);

                        // Initialize item with the requested data
                        int id = InventoryUtils.getSmallestMissingId(items.values());

                        Item item = new Item(itemId, id, expiration.toInt(),
                                statsBonusOne, statsBonusTwo, (short)0,
                                InventoryUtils.expirationToTimestamp(expiration),
                                false, true);

                        // Add it to the player's inventory
                        items.put(id, item);
                        // Activate item
                        CharacterUtils.updateItemsInUse(item, playerId);
                        // Update player's inventory
                        PlayerInfo.addInventoryItem(item, playerId);
                        // Deduct the price from the player's money
                        sumMoneyToPaymentMode(payment, playerId, -price);
                    } else {
                        // Not enough money
                        result = (byte) (payment == Payment.KASH ? -8 : -5);
                    }
                } else {
                    // The payment mode or price sent by the client is invalid
                    result = (byte) (payment == Payment.KASH ? -2 : -3);
                }
            } else {
                // Invalid level
                result = -9;
            }
        } else {
            // System detected a problem
            // May be due to an invalid item id
            result = -1;
        }

        try (Connection con = MySqlManager.getConnection()) {
            session.send(MessageBuilder.purchaseItem(playerId, result, con));

            if (result == 0) {
                CharacterManager.sendItemList(session);
                CharacterManager.sendItemsInUse(session);
            }
        } catch (SQLException ignored) {}
    }

    private static boolean alreadyPurchased(int id, Collection<? extends Product> product) {
        return product.stream().filter(p -> p.getId() == id).findFirst().isPresent();
    }

    private static int getMoneyFromPaymentMode(Payment payment, int playerId) {
        switch (payment) {
            case KASH:
                return UserInfo.getKash(PlayerInfo.getOwner(playerId));
            case POINTS:
                return PlayerInfo.getPoints(playerId);
            default:
                return 0;
        }
    }

    private static void sumMoneyToPaymentMode(Payment payment, int playerId, int value) {
        switch (payment) {
            case KASH:
                UserInfo.sumKash(value, PlayerInfo.getOwner(playerId));
                break;
            case POINTS:
                PlayerInfo.sumPoints(value, playerId);
                break;
            default:
        }
    }
}
