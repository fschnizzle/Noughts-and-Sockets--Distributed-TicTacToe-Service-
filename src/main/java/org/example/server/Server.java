package org.example.server;

import org.example.common.GameLobby;
import org.example.common.Player;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private GameLobby lobby;


    public Server() {
        this.lobby = new GameLobby();
    }

    public void startServer(String IP, int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Notify server started
            System.out.println("Server is running...");
            String curUsername;

            // Continuously looks for new clients to make new thread for
            while (true) {
                // Blocks until new client found
                Socket clientSocket = serverSocket.accept();

                // Create and start a new ClientHandler thread for each connected client
                ClientHandler clientHandler = new ClientHandler(clientSocket, lobby);
                new Thread(clientHandler).start();
            }
        } catch (BindException e) {
            System.out.println("Port " + port + " is already in use. Please choose a different port.");
        } catch (IOException e) {
            System.out.println("Error initializing the server: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Check for correct CL usage and arguments
        if (args.length != 2) {
            System.out.println("Usage: java -jar Server.jar <ip-address> <port>");
        }
        try {
            String IP = args[0];
            int port = Integer.parseInt(args[1]);

            // Start server and initialise a Game Lobby in the process
            Server server = new Server();
            server.startServer(IP, port);
        } catch (Exception e){
            System.out.println("Command Line arguments error");
        }
    }



}