package org.example.client;

import org.example.common.Move;
import org.example.common.Player;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.swing.*;

public class Client {
    private String hostname;
    private int serverPort;
    private String username;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private NoughtsAndSocketsClientGUI gui;

    private static final int MAX_RECONNECTION_ATTEMPTS = 3; // Change later
    private int reconnectionAttempts = 0;


    public String getUsername(){
        return this.username;
    }

    public Client(String hostname, int serverPort, String username) {
        this.hostname = hostname;
        this.serverPort = serverPort;
        this.username = username;
        this.gui = new NoughtsAndSocketsClientGUI(this);
    }

    public void connectToServer() {
        try {
            Socket socket = new Socket(hostname, serverPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Player player = new Player(username);
            out.writeObject(player);
            out.flush();

            reconnectionAttempts = 0; // Reset reconnection attempts when connection is successful

            // Rest of the code...
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Unable to connect to server. Please try again later.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void listenToServer() {
        new Thread(() -> {
            try {
                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof String && obj.equals("MATCHED")) {
//                        SwingUtilities.invokeLater(() -> {
//                            gui.enableChat();
//                        });
                    } else if (obj instanceof String) {
                        String message = (String) obj;
                        gui.receiveChatMessage(message);
                    } else if (obj instanceof Move) {
                        // Handle move logic
                        // E.g., update the GUI based on the move received
                        System.out.println("GOT A MOVE!");
                        Move receivedMove = (Move) obj;
                        SwingUtilities.invokeLater(() -> {
                            gui.updateBoard(receivedMove); // Update the opponent's move on the board
                            gui.setMyTurn(true);          // Since the opponent has made a move, now it's this client's turn
                        });
                    } else if (obj instanceof Player) {
                        // Player object passed at start of new game
                        Player updatedPlayer = (Player) obj;
                        String username = updatedPlayer.getUsername();
                        char sign = updatedPlayer.getSign();
                        System.out.println("You are: " + username);
                        System.out.println("Sign: " + sign);

                        // Set turn appropriately
                        SwingUtilities.invokeLater(() -> {
                            gui.startGame(sign);
                        });
                    }
                    // handle the object
                }
            } catch (EOFException e) {
                System.out.println("Lost connection to server.");
                attemptReconnection();
                e.printStackTrace();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error communicating with server.");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("An unexpected error occurred.");
                e.printStackTrace();
            }
        }).start();
    }

    private void attemptReconnection() {
        if (reconnectionAttempts < MAX_RECONNECTION_ATTEMPTS) {
            reconnectionAttempts++;
            System.out.println("Attempting to reconnect... (Attempt " + reconnectionAttempts + ")");

            try {
                Thread.sleep(5000); // Wait 5 seconds before attempting to reconnect
                connectToServer();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            // Notify user that maximum reconnection attempts have been reached
            JOptionPane.showMessageDialog(null, "Unable to reconnect after " + MAX_RECONNECTION_ATTEMPTS + " attempts. Please check your connection and try again later.", "Reconnection Failed", JOptionPane.ERROR_MESSAGE);
        }
    }


    public void sendMoveToServer(Move move) {
        try {
            out.writeObject(move);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendChatMessageToServer(String message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

        try {
            // Check for correct CL usage and arguments
            if (args.length != 3) {
                System.out.println("Usage: java -jar Client.jar <username> <server-ip-address> <server-port>");
                return;
            }
            // Update username, port, and ip based on arguments
            String username = args[0];
            String hostname = args[1];
            int serverPort = Integer.parseInt(args[2]);

            // Get added to player lobby queue
            Client client = new Client(hostname, serverPort, username);
            client.connectToServer();
            client.listenToServer();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}