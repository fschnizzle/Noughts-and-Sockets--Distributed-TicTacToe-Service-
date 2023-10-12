package org.example.server;

import org.example.common.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private GameLobby lobby;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private boolean isPlayerConnected = true;


    public ClientHandler(Socket clientSocket, GameLobby lobby) {
        this.clientSocket = clientSocket;
        this.lobby = lobby;
    }

    @Override
    public void run() {
        Player player = null;
        try {
            setupStreams();
            handleClient();
        } catch (EOFException eof) {
            System.out.println("Client disconnected.");
            isPlayerConnected = false;
            // Notify the other player about the disconnection if they're still connected
            if (player != null) {
                Player opponent = getOpponentFromGame(player);
                notifyOtherPlayerOfQuit(opponent, player, lobby.findGameByPlayer(player));
            }
        } catch (SocketException se) {
            System.out.println("Client disconnected abruptly.");
            isPlayerConnected = false;
        } catch (IOException e) {
            if(clientSocket != null && clientSocket.isClosed() && player != null) {
                System.out.println("Client " + player.getUsername() + " disconnected gracefully.");
            } else {
                System.out.println("ClientHandler IOException: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            System.out.println("CH ClassNotFoundException: Unable to recognize received object.");
        } catch (InterruptedException e) {
            System.out.println("Interrupted Exception: " + e.getMessage());
            Thread.currentThread().interrupt(); // Best practice: re-interrupt the thread
        } finally {
            closeConnection();
        }
    }

    private void setupStreams() throws IOException {
        out = new ObjectOutputStream(clientSocket.getOutputStream());
        in = new ObjectInputStream(clientSocket.getInputStream());
    }

    private void handleClient() throws IOException, ClassNotFoundException, InterruptedException {
        Player player = (Player) in.readObject();

        // Reconnection (in an existing game)
        if (isReconnectingPlayer(player)) {
            Player opponent = getOpponentFromGame(player);
            if (opponent != null) {
                System.out.println("Client " + player.getUsername() + " (" + lobby.getPlayerRanking(player.getUsername()) + ") reconnected!");
                PlayerSession playerSession = new PlayerSession(player, out);
                lobby.reconnectPlayerToGame(playerSession, opponent);
                //lobby.removeFromLobbyQueue(playerSession);
            }
        } else if (lobby.getPlayerRanking(player.getUsername()) != 1500) { // Returning player (not in current game) (ie: new game)
            player.setRank(lobby.getPlayerRanking(player.getUsername()));
            System.out.println("Client " + player.getUsername() + " (" + lobby.getPlayerRanking(player.getUsername()) + ") joins lobby");
            PlayerSession playerSession = new PlayerSession(player, out);
            lobby.addToLobbyQueue(playerSession);
            lobby.matchPlayers();
        } else { // New player joins server
            System.out.println("Client " + player.getUsername() + " (" + lobby.getPlayerRanking(player.getUsername()) + ") joins lobby");
            PlayerSession playerSession = new PlayerSession(player, out);
            lobby.addToLobbyQueue(playerSession);
            lobby.matchPlayers();
        }

        // Connected, handle game requests
        Game gameRoom = null;
        while (isPlayerConnected) {
            gameRoom = getGameForPlayer(player, gameRoom);
            Object obj = in.readObject();
            handleReceivedObject(obj, gameRoom, player);
        }
    }

    private boolean isReconnectingPlayer(Player player) {
        return lobby.findGameByPlayer(player) != null;
    }

    private Player getOpponentFromGame(Player player) {
        Game existingGame = lobby.findGameByPlayer(player);
        if (existingGame.getPlayer1().getUsername().equals(player.getUsername())) {
            return existingGame.getPlayer2();
        } else if (existingGame.getPlayer2().getUsername().equals(player.getUsername())) {
            return existingGame.getPlayer1();
        }
        return null;
    }

    private Game getGameForPlayer(Player player, Game currentGame) throws InterruptedException {
        Game game = currentGame;
        while (game == null) {
            game = lobby.findGameByPlayer(player);
            if (game == null) {
                Thread.sleep(1000); // Wait a second before checking again
            }
        }
        return game;
    }

    private void handleReceivedObject(Object obj, Game gameRoom, Player player) throws IOException {
        // Before handling any object, check if the player is still connected.
        if (!isPlayerConnected) {
            System.out.println("Player disconnected. Stopping current operations.");
            return;
        }
        if (obj instanceof Move) {
            handleMove((Move) obj, gameRoom, player);
        } else if (obj instanceof String) {
            sendChatMessageToBothPlayers((String) obj, gameRoom, player);
        } else if (obj instanceof Character) {
            handleCharacterMessage((Character) obj, gameRoom, player);
        }
    }

    private void handleMove(Move move, Game gameRoom, Player player) throws IOException {
        boolean isValidMove = gameRoom.placeMove(move.getX(), move.getY(), move.getSign());
        if (isValidMove) {
            sendMoveToOtherPlayer(move, gameRoom, player);
            gameRoom.switchCurrentPlayer();
        }
    }

    private void handleCharacterMessage(Character message, Game gameRoom, Player player) {

        // Only P1 handles the rank updating etc, assumes the message applies to them
        if (gameRoom.getPlayer1().equals(player)){
            switch (message) {
                case 'Q':
                    System.out.println(player.getUsername() + " has quit the game.");
                    notifyOtherPlayerOfQuit(getOpponentFromGame(player), player, gameRoom);
                    handleGameEnd(getOpponentFromGame(player), player, gameRoom, false, true);
                    break;
                case 'W':
                    handleGameEnd(gameRoom.getPlayer1(), gameRoom.getPlayer2(), gameRoom, false, false);
                    break;
                case 'L':
                    handleGameEnd(gameRoom.getPlayer2(), gameRoom.getPlayer1(), gameRoom, false, false);
                    break;
                case 'D':
                    handleGameEnd(gameRoom.getPlayer2(), gameRoom.getPlayer1(), gameRoom, true, false);
                    break;
                default:
                    System.out.println("Not a valid instruction: " + message);
            }
        }
    }

    private void closeConnection() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (IOException ioException) {
            System.out.println("Error closing connection.");
            ioException.printStackTrace();
        }
    }
    private void notifyOtherPlayerOfQuit(Player winner, Player quitter, Game gameRoom) {
        try {
            if (winner != null) {
                ObjectOutputStream winnerStream = (winner.equals(gameRoom.getPlayer1())) ? gameRoom.getPlayer1Stream() : gameRoom.getPlayer2Stream();
                if (winnerStream != null) {
                    winnerStream.writeObject('0'); // Sending specific message to the client
                    winnerStream.flush();
                }
            }
        } catch (SocketException se) {
            // Not critical
            System.out.println("Failed to notify " + (winner != null ? winner.getUsername() : "the opponent") + ". They have already disconnected.");
        } catch (IOException e) {
            System.out.println("Failed to notify the opponent due to some IO issue.");
            e.printStackTrace();
        }
    }

    private void sendMoveToOtherPlayer(Move move, Game gameRoom, Player player) {
        try {
            if (gameRoom.getPlayer1().equals(player)) {
                System.out.println("Sending move to Player 2");
                gameRoom.getPlayer2Stream().writeObject(move);
                gameRoom.getPlayer2Stream().flush(); // Ensure the move is actually sent
            } else {
                System.out.println("Sending move to Player 1");
                gameRoom.getPlayer1Stream().writeObject(move);
                gameRoom.getPlayer1Stream().flush(); // Ensure the move is actually sent
            }
        } catch (IOException e) {
            System.err.println("Error sending move to the other player: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGameEnd(Player winner, Player loser, Game gameRoom, boolean isDraw, boolean isQuit) {
        // Update Rankings
        // updatePlayerRankings(winner, loser, false);
        if (isDraw) { // Draw
            updatePlayerRankings(winner, loser, true);
        } else { // Win or Lose
            updatePlayerRankings(winner, loser, false);
        }

        // Alert the winner
//        if (!isQuit) {
        notifyOtherPlayerOfQuit(winner, loser, gameRoom);
        // Remove game from activeGames
        lobby.removeGame(winner, loser);
//        }
    }

    // Ranking Methods
    private double calculateElo(double currentRank, double opponentRank, double result) {
        final int K = 32;
        double expectedOutcome = 1.0 / (1.0 + Math.pow(10, (opponentRank - currentRank) / 400.0));
        return currentRank + (double)(K * (result - expectedOutcome));
    }
    private void updatePlayerRankings(Player winner, Player loser, boolean isDraw) {
        double winnerRank = lobby.getPlayerRanking(winner.getUsername());
        double loserRank = lobby.getPlayerRanking(loser.getUsername());

        double newWinnerRank, newLoserRank;
        if (isDraw) {
            newWinnerRank = calculateElo(winnerRank, loserRank, 0.5);
            newLoserRank = calculateElo(loserRank, winnerRank, 0.5);
        } else {
            newWinnerRank = calculateElo(winnerRank, loserRank, 1);
            newLoserRank = calculateElo(loserRank, winnerRank, 0);
        }

        updatePlayerRank(winner, newWinnerRank);
        updatePlayerRank(loser, newLoserRank);
    }

    private void updatePlayerRank(Player player, double rank) {
        String username = player.getUsername();

        // Update Player Object
        player.setRank(rank);
//        System.out.println("Player object:");
//        System.out.println(username + " " + player.getRank());

        // Update in lobby
        Double oldRank = lobby.getPlayerRanking(username);
        lobby.updatePlayerRankings(username, rank);
//        System.out.println("Lobby structure:");
        System.out.println(username + " " + oldRank + " -> " + lobby.getPlayerRanking(username));
    }
    private void sendChatMessageToBothPlayers(String message, Game gameRoom, Player sender) throws IOException {
        if(!gameRoom.getPlayer1().equals(sender)) {
            gameRoom.getPlayer1Stream().writeObject(message);
        }
        if(!gameRoom.getPlayer2().equals(sender)) {
            gameRoom.getPlayer2Stream().writeObject(message);
        }
    }

}