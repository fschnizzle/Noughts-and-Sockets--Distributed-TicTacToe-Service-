package org.example.client;

import org.example.common.Move;
import org.example.common.Player;

import java.io.*;
import java.lang.management.ManagementFactory;
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
                System.out.println("EOFException");
//                handleConnectionLoss();
            } catch (Exception e) {
                System.out.println("Other Exception");
                return;
            }
        }).start();
    }


    private void processServerMessages() throws IOException, ClassNotFoundException {
        Player updatedPlayer = null;
        Player opponentPlayer = null;
        while (true) {
            Object obj = in.readObject();

            if (obj instanceof String) {
                switch ((String) obj) {
                    case "OPPONENT_QUIT":
                        gui.handleGameEndReceive('Q');
                        break;
                    case "MATCHED":
                        gui.setGameActive(true);
                        break;
                    case "CONFIRM_PLAYER_QUIT_LOBBY":
                        // Received confirmation that the player has been removed from the lobby.
                        System.exit(0);
                        break;
                    default:
                        gui.receiveChatMessage((String) obj);
                }
            }
            else if (obj instanceof Character) {
                char status = (Character) obj;
                switch (status) {
                    // Technically redundant
                    case 'Q':
                        gui.handleGameEndReceive('Q');
                        break;
                    case 'W':
                        gui.handleGameEndReceive('W');
                        break;
                    case 'L':
                        gui.handleGameEndReceive('L');
                        break;
                    case 'D':
                        gui.handleGameEndReceive('D');
                        break;
                    case 'R':
                        newGame();
                    default:
                        System.out.println("Unhandled character message: " + status);
                }
            }
            else if (obj instanceof Move) {
                processMove((Move) obj);
            } else if (obj instanceof Player) {
                if (updatedPlayer == null) {
                    updatedPlayer = (Player) obj;
                } else {
                    opponentPlayer = (Player) obj;
                    gui.startGame(updatedPlayer, opponentPlayer);
                }
            }
            else if (obj instanceof Double) {
                gui.rank = (Double) obj;
            }
            else {
                System.out.println("Received unhandled object type: " + obj.getClass());
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

    public void restartApplication() {
        StringBuilder cmd = new StringBuilder();
        cmd.append(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java "); // Java path
        for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) { // JVM arguments
            cmd.append(jvmArg + " ");
        }
        cmd.append("-cp ").append(ManagementFactory.getRuntimeMXBean().getClassPath()).append(" "); // Classpath
        cmd.append("org.example.client.Client"); // This should be replaced by your main class's full name.
        String[] args = {username, hostname, String.valueOf(serverPort)};
        for (String arg : args) { // Command line arguments
            cmd.append(" ").append(arg);
        }

        try {
            Runtime.getRuntime().exec(cmd.toString());
            System.exit(0); // Exit current program
        } catch (IOException e) {
            e.printStackTrace();
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


    private static boolean shouldRestart = true;

    public void newGame() {
        this.gui = new NoughtsAndSocketsClientGUI(this);
        connectToServer();
    }

    public static class RestartException extends RuntimeException {}


    public static void main(String[] args) {
        while (shouldRestart) {
            try {
                shouldRestart = false; // reset the flag
                runClient(args);
            } catch (RestartException e) {
                System.out.println("Restarting client...");
                shouldRestart = true; // reset the flag
                continue;
            }
        }
    }

    public static void runClient(String[] args) {
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

//    public static void main(String[] args) {
//
//        try {
//            // Check for correct CL usage and arguments
//            if (args.length != 3) {
//                System.out.println("Usage: java -jar Client.jar <username> <server-ip-address> <server-port>");
//                return;
//            }
//            // Update username, port, and ip based on arguments
//            String username = args[0];
//            String hostname = args[1];
//            int serverPort = Integer.parseInt(args[2]);
//
//            // Get added to player lobby queue
//            Client client = new Client(hostname, serverPort, username);
//            client.connectToServer();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}