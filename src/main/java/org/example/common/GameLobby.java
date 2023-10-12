package org.example.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.UUID;

public class GameLobby {
    private final ConcurrentLinkedQueue<PlayerSession> playerLobbyQueue;
    private final ConcurrentHashMap<UUID, Game> activeGames;
    private final ConcurrentHashMap<String, Double> playerRankings ;


    public GameLobby() {
        this.playerLobbyQueue = new ConcurrentLinkedQueue<PlayerSession>();
        this.activeGames = new ConcurrentHashMap<>();
        this.playerRankings = new ConcurrentHashMap<>();
    }

    public double getPlayerRanking(String username) {
        return playerRankings.getOrDefault(username, 1500.0);
    }

    public void updatePlayerRankings(String username, double newRank) {
        this.playerRankings.put(username, newRank);
    }

    public synchronized void addToLobbyQueue(PlayerSession player) {
        playerLobbyQueue.add(player);
    }

    public synchronized PlayerSession popFromLobbyQueue() {
        return playerLobbyQueue.poll();
    }

    public void removeFromLobbyQueue(PlayerSession playerSession) {
        playerLobbyQueue.remove(playerSession);
    }

    public void removeGame(Player player1, Player player2) {
        UUID gameIdToRemove = activeGames.keySet().stream()
                .filter(gameId -> activeGames.get(gameId).containsPlayers(Arrays.asList(player1, player2)))
                .findFirst().orElse(null);

        if (gameIdToRemove != null) {
            activeGames.remove(gameIdToRemove);
        }
    }

    public synchronized void matchPlayers() {
        while (playerLobbyQueue.size() >= 2) {
            PlayerSession player1 = popFromLobbyQueue();
            PlayerSession player2 = popFromLobbyQueue();

            Game game = createNewGame(player1, player2);
            activeGames.put(UUID.randomUUID(), game);
            System.out.println("Game created with " + player1.getPlayer().getUsername() + " and " + player2.getPlayer().getUsername());
            notifyPlayersMatched(player1, player2);
        }
    }

    private Game createNewGame(PlayerSession player1, PlayerSession player2) {
        assignRoles(player1, player2);
        Game game = initializeGame(player1, player2);
        try {
            sendPlayerDetails(game, player1, player2);
        } catch (IOException e) {
            e.printStackTrace();
            // Consider adding more error handling here
        }
        return game;
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
        // Send each player their own details first
        game.getPlayer1Stream().writeObject(player1.getPlayer());
        game.getPlayer1Stream().flush();
        game.getPlayer2Stream().writeObject(player2.getPlayer());
        game.getPlayer2Stream().flush();

        // Then send their opponent's details
        game.getPlayer1Stream().writeObject(player2.getPlayer());
        game.getPlayer1Stream().flush();
        game.getPlayer2Stream().writeObject(player1.getPlayer());
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
        for (Game game : activeGames.values()) {
            if (game.containsPlayer(player)) {
                return game;
            }
        }
        return null;
    }

    public synchronized void reconnectPlayerToGame(PlayerSession reconnectingPlayer, Player opponent) {
        Game existingGame = findGameByPlayer(opponent);
        if (existingGame != null) {
            existingGame.updatePlayerStream(reconnectingPlayer.getPlayer(), reconnectingPlayer.getStream());
            notifyPlayersOfReconnection(reconnectingPlayer, existingGame);
        }
    }


    private void notifyPlayersOfReconnection(PlayerSession reconnectingPlayer, Game existingGame) {
        try {
            reconnectingPlayer.getStream().writeObject("RECONNECTED");
            if (!existingGame.getPlayer1().equals(reconnectingPlayer.getPlayer())) {
                existingGame.getPlayer1Stream().writeObject("OPPONENT_RECONNECTED");
            } else {
                existingGame.getPlayer2Stream().writeObject("OPPONENT_RECONNECTED");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}