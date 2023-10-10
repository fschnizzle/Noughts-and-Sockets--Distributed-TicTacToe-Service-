package org.example.common;

import java.io.ObjectOutputStream;

public class Game {
    private final Player player1;
    private final Player player2;
    private ObjectOutputStream player1Stream;
    private ObjectOutputStream player2Stream;
    private char[][] board = new char[3][3]; // Represents the Tic Tac Toe board
    private char currentPlayer = 'X'; // X Starts

    public Game(PlayerSession session1, PlayerSession session2) {
        this.player1 = session1.getPlayer();
        this.player2 = session2.getPlayer();
        this.player1Stream = session1.getStream();
        this.player2Stream = session2.getStream();

        // Initialize the board with empty characters
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                board[i][j] = '-';
            }
        }
    }

    public void setPlayer1Stream(ObjectOutputStream stream) {
        this.player1Stream = stream;
    }

    public void setPlayer2Stream(ObjectOutputStream stream) {
        this.player2Stream = stream;
    }

    public ObjectOutputStream getPlayer1Stream() {
        return player1Stream;
    }

    public ObjectOutputStream getPlayer2Stream() {
        return player2Stream;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    // Method to place a move on the board
    public boolean placeMove(int x, int y, char sign) {
        if (board[x][y] == '-') {
            board[x][y] = sign;
            return true;
        }
        return false; // Spot already taken
    }

    public void switchCurrentPlayer() {
        currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
    }

}
