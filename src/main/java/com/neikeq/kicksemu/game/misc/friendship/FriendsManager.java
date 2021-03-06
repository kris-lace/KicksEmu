package com.neikeq.kicksemu.game.misc.friendship;

import com.neikeq.kicksemu.game.characters.PlayerInfo;
import com.neikeq.kicksemu.game.sessions.Session;
import com.neikeq.kicksemu.network.packets.in.ClientMessage;
import com.neikeq.kicksemu.network.packets.out.MessageBuilder;
import com.neikeq.kicksemu.network.packets.out.ServerMessage;
import com.neikeq.kicksemu.network.server.ServerManager;

public class FriendsManager {

    private static final int FRIENDS_LIST_LIMIT = 30;

    public static void friendsList(Session session, ClientMessage msg) {
        byte page = msg.readByte();

        FriendsList friends = PlayerInfo.getFriendsList(session.getPlayerId());

        ServerMessage response = MessageBuilder.friendList(friends.fromPage(page), page);
        session.send(response);
    }

    public static void friendRequest(Session session, ClientMessage msg) {
        int targetId = msg.readInt();
        int playerId = session.getPlayerId();

        byte result = 0;

        if (ServerManager.isPlayerConnected(targetId)) {
            FriendsList friendsList = PlayerInfo.getFriendsList(playerId);

            if (friendsList.size() < FRIENDS_LIST_LIMIT) {
                if (!friendsList.containsFriend(targetId)) {
                    FriendsList targetFriendList = PlayerInfo.getFriendsList(targetId);

                    if (targetFriendList.size() < FRIENDS_LIST_LIMIT) {
                        // Send the friend request to the target
                        ServerMessage request = MessageBuilder.friendRequest(playerId, result);
                        ServerManager.getSessionById(targetId).sendAndFlush(request);
                    } else {
                        result = (byte)252; // Target friend list is full
                    }
                } else {
                    result = (byte)251; // Already in the friend list
                }
            } else {
                result = (byte)253; // Friend list is full
            }
        } else {
            result = (byte)254; // Player not found
        }

        if (result != 0) {
            ServerMessage response = MessageBuilder.friendRequest(playerId, result);
            session.sendAndFlush(response);
        }
    }

    public static void friendResponse(Session session, ClientMessage msg) {
        int targetId = msg.readInt();
        int playerId = session.getPlayerId();
        boolean accepted = msg.readBoolean();

        byte result = 0;

        if (ServerManager.isPlayerConnected(targetId)) {
            // TODO Check if the target actually sent a friend request
            if (accepted) {
                FriendsList friendsList = PlayerInfo.getFriendsList(playerId);

                if (friendsList.size() < FRIENDS_LIST_LIMIT) {
                    FriendsList targetFriendList = PlayerInfo.getFriendsList(targetId);

                    if (targetFriendList.size() < FRIENDS_LIST_LIMIT) {
                        // Add target to the player friend list
                        friendsList.addFriend(targetId);
                        PlayerInfo.setFriendsList(friendsList, playerId);

                        // Add player to the target friend list
                        targetFriendList.addFriend(playerId);
                        PlayerInfo.setFriendsList(targetFriendList, targetId);
                    } else {
                        accepted = false;
                    }
                } else {
                    accepted = false;
                }
            }

            if (!accepted) {
                result = (byte)253; // Rejected the request
            }

            ServerMessage friendResponse = MessageBuilder.friendResponse(result);
            ServerManager.getSessionById(targetId).sendAndFlush(friendResponse);
        } else {
            // Player not found. May occur if the player disconnected after sending the request.
            result = (byte)254;
        }

        if (result != (byte)253) {
            ServerMessage response = MessageBuilder.friendResponse(result);
            session.sendAndFlush(response);
        }
    }

    public static void deleteFriend(Session session, ClientMessage msg) {
        int friendId = msg.readInt();

        int playerId = session.getPlayerId();

        byte result = 0;

        FriendsList friendList = PlayerInfo.getFriendsList(playerId);

        if (friendList.containsFriend(friendId)) {
            // Remove friend from from player's friend list
            friendList.removeFriend(friendId);
            PlayerInfo.setFriendsList(friendList, playerId);

            // Remove player from friend's friend list
            FriendsList targetFriendList = PlayerInfo.getFriendsList(friendId);
            targetFriendList.removeFriend(playerId);
            PlayerInfo.setFriendsList(targetFriendList, friendId);
        } else {
            result = (byte) 254; // Player not found
        }

        ServerMessage response = MessageBuilder.deleteFriend(result);
        session.send(response);
    }
}
