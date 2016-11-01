package com.neikeq.kicksemu.game.misc.ignored;

import com.neikeq.kicksemu.game.characters.CharacterUtils;
import com.neikeq.kicksemu.game.characters.PlayerInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class IgnoredList {

    private final List<Integer> ignoredPlayers = new ArrayList<>();

    public boolean addPlayer(int playerId) {
        return ignoredPlayers.add(playerId);
    }

    public boolean addAllPlayers(Collection<Integer> players) {
        return ignoredPlayers.addAll(players);
    }

    public void removePlayer(int playerId) {
        int index = ignoredPlayers.indexOf(playerId);

        if (index != -1) {
            ignoredPlayers.remove(index);
        }
    }

    public boolean containsPlayer(int playerId) {
        return ignoredPlayers.contains(playerId);
    }

    public int size() {
        return ignoredPlayers.size();
    }

    public static IgnoredList fromString(String strPlayers, int id) {
        final String[] playerArray = strPlayers.split(",");

        IgnoredList ignoredList = new IgnoredList();

        ignoredList.addAllPlayers(Arrays.stream(playerArray)
                                        .filter(playerId -> !playerId.isEmpty())
                                        .map(Integer::valueOf)
                                        .filter(CharacterUtils::characterExist)
                                        .collect(Collectors.toList()));

        if (ignoredList.size() > playerArray.length) {
            PlayerInfo.setIgnoredList(ignoredList, id);
        }

        return ignoredList;
    }

    @Override
    public String toString() {
        String strPlayers = "";

        for (int i = 0; i < ignoredPlayers.size(); i++) {
            strPlayers += (i > 0) ? "," : "";
            strPlayers += String.valueOf(ignoredPlayers.get(i));
        }

        return strPlayers;
    }

    public List<Integer> getIgnoredPlayers() {
        return ignoredPlayers;
    }

    private IgnoredList() {}
}
