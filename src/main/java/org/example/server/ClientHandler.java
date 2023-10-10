package org.example.server;

import org.example.common.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private GameLobby lobby;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket clientSocket, GameLobby lobby) {
        this.clientSocket = clientSocket;
        this.lobby = lobby;
    }

    @Override
    public void run() {
        try {

            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            // Receive player from client
            Player player = (Player) in.readObject();
            // Print the client's username here, inside the ClientHandler
            System.out.println("New client connected: " + player.getUsername() + " (" +  clientSocket.getInetAddress() + ")");

            PlayerSession playerSession = new PlayerSession(player, out);
            lobby.addToLobbyQueue(playerSession);
            lobby.matchPlayers();

            Game gameRoom = null; // Check if player has been matched

            while (true) {
                // If gameRoom is null, keep checking until it's not
                while (gameRoom == null) {
                    gameRoom = lobby.findGameByPlayer(player);
                    if (gameRoom == null) {
                        Thread.sleep(1000); // Wait for a second before checking again
                    }
                }
                Object obj = in.readObject();  // Receive the object from the client

                if (obj instanceof Move) {
                    Move move = (Move) obj;
                    if (gameRoom == null) {
                        gameRoom = lobby.findGameByPlayer(player); // Check if player has been matched
                    }
                    boolean isValidMove = gameRoom.placeMove(move.getX(), move.getY(), move.getSign());
                    if (isValidMove) {
                        sendMoveToOtherPlayer(move, gameRoom, player);
                        gameRoom.switchCurrentPlayer();
                    }
                } else if (obj instanceof String) {
                    String message = (String) obj;
                    if ("QUIT".equalsIgnoreCase(message)) {
                        System.out.println("Player Quit");
                        // Handle the logic when a player quits
                        break;
                    } else {
                        sendChatMessageToBothPlayers(message, gameRoom, player);
                        System.out.println(message);
                    }
                    // Handle other string messages if needed
                }
            }


            // ... handle otherinteraction ...

        } catch (EOFException eof) {
            System.out.println("Client disconnected.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
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
                ioException.printStackTrace();
            }
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


    private void sendChatMessageToBothPlayers(String message, Game gameRoom, Player sender) throws IOException {
        if(!gameRoom.getPlayer1().equals(sender)) {
            gameRoom.getPlayer1Stream().writeObject(message);
        }

        if(!gameRoom.getPlayer2().equals(sender)) {
            gameRoom.getPlayer2Stream().writeObject(message);
        }
    }

}