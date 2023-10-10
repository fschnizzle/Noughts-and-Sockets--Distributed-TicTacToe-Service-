package org.example.common;

import java.io.ObjectOutputStream;

public class PlayerSession {
    private Player player;
    private ObjectOutputStream stream;

    public PlayerSession(Player player, ObjectOutputStream stream) {
        this.player = player;
        this.stream = stream;
    }

    public Player getPlayer() {
        return player;
    }

    public ObjectOutputStream getStream() {
        return stream;
    }
}
