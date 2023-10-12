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

            // Creates a new Player object and writes it to the server
            sendInitialPlayerData();

            // Continuously listen out for new data packets
            listenToServer();

        } catch (IOException e) {
            System.out.println("Unable to connect to server. Please try again later. Connection Error");
            e.printStackTrace();
        }
    }

    private void sendInitialPlayerData() throws IOException {
        Player player = new Player(username);
        out.writeObject(player);
        out.flush();
        reconnectionAttempts = 0;  // Reset reconnection attempts when connection is successful
    }

    private void listenToServer() {
        new Thread(() -> {
            try {
                processServerMessages();
            } catch (EOFException e) {
                handleConnectionLoss();
            } catch (Exception e) {
                displayGenericError();
            }
        }).start();
    }


    private void displayGenericError() {
        System.out.println("UNKNOWN GENERIC ISSUE OCCURRED");
    }

    private void processServerMessages() throws IOException, ClassNotFoundException {
        Player updatedPlayer = null;
        Player opponentPlayer = null;
        while (true) {
            Object obj = in.readObject();

            if (obj instanceof String) {
                switch ((String) obj) {
                    case "OPPONENT_QUIT":
                        gui.setGameActive(false);
                        gui.handleGameEndReceive('L');
                        break;
                    default:
                        gui.receiveChatMessage((String) obj);
                }
            } else if (obj instanceof Character && obj.equals('0')) {
                gui.handleOpponentQuit();
//                sendMessageToServer((Character) obj);
            } else if (obj instanceof Move) {
                processMove((Move) obj);
            } else if (obj instanceof Player) {
                if (updatedPlayer == null) {
                    updatedPlayer = (Player) obj;
                } else {
                    opponentPlayer = (Player) obj;
                    gui.startGame(updatedPlayer, opponentPlayer);
                }
            }
        }
    }

    private void processMove(Move move) {
        SwingUtilities.invokeLater(() -> {
            gui.updateBoard(move);
            gui.setMyTurn(true);
        });
    }

    private void handleConnectionLoss() {
        gui.lostOpponentConnection(true);
        attemptReconnection();
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
        sendToServer(move);
    }

    public void sendChatMessageToServer(String message) {
        sendToServer(message);
    }

    public void sendMessageToServer(char message) {
        sendToServer(message);
    }

    private void sendToServer(Object message) {
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}