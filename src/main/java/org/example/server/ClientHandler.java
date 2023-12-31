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
            System.out.println("Client disconnected ..");
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
            Thread.currentThread().interrupt(); //  re-interrupt the thread
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
        }
        else if (lobby.getPlayerRanking(player.getUsername()) != 1500) {
            player.setRank(lobby.getPlayerRanking(player.getUsername()));
//            System.out.println("Client " + player.getUsername() + " (" + lobby.getPlayerRanking(player.getUsername()) + ") joins lobby");
            PlayerSession playerSession = new PlayerSession(player, out);
            lobby.addToLobbyQueue(playerSession);
        }
        else {
//            System.out.println("Client " + player.getUsername() + " (" + lobby.getPlayerRanking(player.getUsername()) + ") joins lobby");
            PlayerSession playerSession = new PlayerSession(player, out);
            lobby.addToLobbyQueue(playerSession);
        }

        // Connected, handle game requests
        Game gameRoom = null;
        Object obj = null;
        // Lobby stage
        while (isPlayerConnected && gameRoom == null) {
            obj = in.readObject();

            // Check for the 'X' character signaling the player wants to exit
            if (obj instanceof Character && obj.equals('X')) {
                lobby.removePlayerSessionByPlayer(player);
//                System.out.println(player.getUsername() + " removed from lobby.");
                isPlayerConnected = false; // Exit the loop as the player wants to leave.
            } else {
                // Check if a game is available.
                gameRoom = lobby.findGameByPlayer(player);
            }
        }

        // Transition stage
        if (obj != null){
            // Handle game-related messages.
            handleReceivedObject(obj, gameRoom, player);
        }

        // Game stage
        while (isPlayerConnected && gameRoom != null) {
            obj = in.readObject();
            // Handle game-related messages.
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
        // Before handling any object, check if  player still connected
        if (!isPlayerConnected) {
//            System.out.println("Player disconnected. Stopping current operations.");
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
    private void handleCharacterMessage(Character status, Game game, Player player) {

        Player p1, p2;

        // Set p1 and p2 players accordingly
        // p1 is always PLAYER, p2 is always OPPONENT
        if (game.getPlayer1().equals(player)) {
            p1 = game.getPlayer1();
            p2 = game.getPlayer2();
        }
        else if (game.getPlayer2().equals(player)) {
            p1 = game.getPlayer2();
            p2 = game.getPlayer1();
        }
        else {
            return; // If the player isn't Player1 or Player2 in the game room, return.
        }

        if (status.equals('R')){
            ObjectOutputStream stream = game.getPlayer1Stream();
            try {
                isPlayerConnected = true;
                stream.writeObject('R');
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Handle Game End processes
        else if (!game.isProcessed) {
            handleGameEnd(p1, p2, game, status, true);
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

    private void notifyOtherPlayerOfStatus(Player opponent, Game gameRoom, char status) {
        try {
            if (opponent != null) {
                ObjectOutputStream oppStream = (opponent.equals(gameRoom.getPlayer1())) ? gameRoom.getPlayer1Stream() : gameRoom.getPlayer2Stream();
                ObjectOutputStream myStream = (opponent.equals(gameRoom.getPlayer1())) ? gameRoom.getPlayer2Stream() : gameRoom.getPlayer1Stream();

                if (oppStream != null) {
                    // Send updated ranks for both players
                    oppStream.writeObject(lobby.getPlayerRanking(opponent.getUsername()));

                    Player opponentFromGame = getOpponentFromGame(opponent);
                    if (opponentFromGame != null && opponentFromGame.getUsername() != null) {
                        oppStream.writeObject(lobby.getPlayerRanking(opponent.getUsername()));

                        // Send the second rank object to the other player as well
                        if (myStream != null) {
                            myStream.writeObject(lobby.getPlayerRanking(opponentFromGame.getUsername()));
                            myStream.flush(); // Added flush for the other player stream
                        }

                    } else {
                        // later: Handle the situation when either opponentFromGame or getUsername() is null.
                    }
                    oppStream.writeObject(status); // Sending specific message to the client
                    oppStream.flush();
                }
            }
        } catch (SocketException se) {
            // Not critical
            try {
                ObjectOutputStream loserStream = (opponent.equals(gameRoom.getPlayer1())) ? gameRoom.getPlayer2Stream() : gameRoom.getPlayer1Stream();
                loserStream.writeObject('0');
            } catch(Exception e){
                System.out.println("Failed to notify " + (opponent != null ? opponent.getUsername() : "the opponent") + ". They have already disconnected.");
            }
        } catch (IOException e) {
            System.out.println("Failed to notify the opponent due to some IO issue.");
            e.printStackTrace();
        }
    }

    private void notifyOtherPlayerOfQuit(Player opponent, Player quitter, Game gameRoom) {
        try {
            if (opponent != null) {
                ObjectOutputStream winnerStream = (opponent.equals(gameRoom.getPlayer1())) ? gameRoom.getPlayer1Stream() : gameRoom.getPlayer2Stream();

                if (winnerStream != null) {
                    winnerStream.writeObject("OPPONENT_QUIT"); // Sending specific message to the client
                    winnerStream.flush();
                }
            }
        } catch (SocketException se) {
            // Not critical
            try {
                ObjectOutputStream loserStream = (opponent.equals(gameRoom.getPlayer1())) ? gameRoom.getPlayer2Stream() : gameRoom.getPlayer1Stream();
                loserStream.writeObject('0');
            } catch(Exception e){
                System.out.println("Failed to notify " + (opponent != null ? opponent.getUsername() : "the opponent") + ". They have already disconnected.");
            }
        } catch (IOException e) {
            System.out.println("Failed to notify the opponent due to some IO issue.");
            e.printStackTrace();
        }
    }

    private void sendMoveToOtherPlayer(Move move, Game gameRoom, Player player) {
        try {
            if (gameRoom.getPlayer1().equals(player)) {
                gameRoom.getPlayer2Stream().writeObject(move);
                gameRoom.getPlayer2Stream().flush(); // Ensures the move is actually sent
            } else {
                gameRoom.getPlayer1Stream().writeObject(move);
                gameRoom.getPlayer1Stream().flush(); // Ensures the move is actually sent
            }
        } catch (IOException e) {
            System.err.println("Error sending move to the other player: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGameEnd(Player p1, Player p2, Game game, char status, boolean isQuit) {
        // Updates player rankings with: (winner, loser, isDraw)
        // p1 = player
        // p2 = opponent
        switch (status) {
            case 'W':
                // Player 1 wins, Player 2 loses
                updatePlayerRankings(p1, p2, false);
                break;
            case 'L':
            case 'Q':
                // Player 1 loses, Player 2 wins
                updatePlayerRankings(p2, p1, false);
                break;
            case 'D':
                // It's a draw, so there's no winner or loser
                updatePlayerRankings(p1, p2, true);
                break;
        }

        // Update isProcessed to avoid repeating
        game.isProcessed = true;

        // Alert the other player about the status change
        notifyOtherPlayerOfStatus(p2, game, status);

        // Remove the game from activeGames
        lobby.removeGame(p1, p2);
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
        Double oldRank = lobby.getPlayerRanking(username);
        lobby.updatePlayerRankings(username, rank);
        //TODO: System.out.println(username + " " + oldRank + " -> " + lobby.getPlayerRanking(username));
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