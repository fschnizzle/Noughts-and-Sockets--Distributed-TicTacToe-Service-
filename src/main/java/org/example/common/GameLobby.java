package org.example.common;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.UUID;

public class GameLobby {


    private final ConcurrentLinkedQueue<PlayerSession> playerLobbyQueue;
    private final ConcurrentHashMap<UUID, Game> activeGames;

    public GameLobby() {
        this.playerLobbyQueue = new ConcurrentLinkedQueue<PlayerSession>();
        this.activeGames = new ConcurrentHashMap<>();
    }

    public synchronized void addToLobbyQueue(PlayerSession player) {
        playerLobbyQueue.add(player);
    }

    public synchronized PlayerSession popFromLobbyQueue() {
        return playerLobbyQueue.poll();
    }

    public synchronized void matchPlayers() {
        while (playerLobbyQueue.size() >= 2) {
            PlayerSession player1 = popFromLobbyQueue();
            PlayerSession player2 = popFromLobbyQueue();
            assignRoles(player1, player2);
            Game game = initializeGame(player1, player2);

            try {
                sendPlayerDetails(game, player1, player2);
            } catch (IOException e) {
                e.printStackTrace();
                // Consider adding more error handling here
                return;
            }

            activeGames.put(UUID.randomUUID(), game);
            System.out.println("Game created with " + player1.getPlayer().getUsername() + " and " + player2.getPlayer().getUsername());
            System.out.println();

            // Notify the players
            notifyPlayersMatched(player1, player2);

        }
    }

    private void assignRoles(PlayerSession player1, PlayerSession player2) {
        player1.getPlayer().setSign('X');
        player2.getPlayer().setSign('O');
        player1.getPlayer().setMyTurn(true);
        player2.getPlayer().setMyTurn(false);
    }

    private Game initializeGame(PlayerSession player1, PlayerSession player2) {
        Game game = new Game(player1, player2);
        game.setPlayer1Stream(player1.getStream());
        game.setPlayer2Stream(player2.getStream());
        return game;
    }

    private void sendPlayerDetails(Game game, PlayerSession player1, PlayerSession player2) throws IOException {
        game.getPlayer1Stream().writeObject(player1.getPlayer());
        game.getPlayer1Stream().flush();
        game.getPlayer2Stream().writeObject(player2.getPlayer());
        game.getPlayer2Stream().flush();
    }

    private void notifyPlayersMatched(PlayerSession player1, PlayerSession player2) {
        try {
            player1.getStream().writeObject("MATCHED");
            player2.getStream().writeObject("MATCHED");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Game findGameByPlayer(Player player) {
        for (ConcurrentHashMap.Entry<UUID, Game> entry : activeGames.entrySet()) {
            Game game = entry.getValue();
            if (game.getPlayer1().getUsername().equals(player.getUsername()) ||
                    game.getPlayer2().getUsername().equals(player.getUsername())) {
                return game;
            }
        }
        return null;  // Return null if no game is found for the player
    }
}