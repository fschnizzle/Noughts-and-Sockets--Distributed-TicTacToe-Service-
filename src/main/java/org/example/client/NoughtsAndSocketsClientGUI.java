package org.example.client;

import org.example.common.Move;
import org.example.common.Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class NoughtsAndSocketsClientGUI {
    private Client client;
    public char sign;
    public boolean isMyTurn;
    private Set<JButton> disabledButtons = new HashSet<>();


    public JButton[] grid = new JButton[9];
    public Color bgColor = Color.GRAY;

    public JTextArea chatArea;
    public JTextField chatInput;
    private JLabel timerLabel;
    private JLabel statusHeadLabel;
    private JLabel chatHeadLabel;
    private JLabel titleLabel;


    public NoughtsAndSocketsClientGUI(Client client) {
        this.client = client;
        init_components();
    }

    public void play(int id) {
        // Your game logic goes here
    }



    public void setMyTurn(boolean myTurn) {
        isMyTurn = myTurn;
        if(isMyTurn) {
            enableBoard();
        } else {
            disableBoard();
        }
        updatePlayerStatus();
    }

    private char getCurrentPlayerSign() {
        return isMyTurn ? 'X' : 'O';
    }

    public void updateChatBoard(String message) {
        chatArea.append(message + "\n");
    }

    public void receiveChatMessage(String message) {
        chatArea.append("Opponent: " + message + "\n");
    }


    public void enableChat() {
        chatInput.setEnabled(true);
        chatInput.setBackground(Color.WHITE);
    }

    public void disableChat() {
        chatInput.setEnabled(false);
        chatInput.setBackground(Color.GRAY);
        chatArea.setBackground(Color.GRAY);
    }

    public void disableGameComponents() {
        for (JButton button : grid) {
            button.setEnabled(false);
            button.setBackground(Color.DARK_GRAY);
        }
        chatInput.setEnabled(false);
        statusHeadLabel.setText("Finding Opponent...");
    }

    public void enableBoard() {
        for (JButton button : grid) {
            if (!disabledButtons.contains(button)) {
                button.setEnabled(true);
                button.setBackground(Color.GRAY);
            }
        }
        updatePlayerStatus();
    }
    public void disableBoard() {
        for (JButton button : grid) {
            button.setEnabled(false);
        }
        updatePlayerStatus();
    }

    public void startGame(char sign) {
        this.sign = sign;
        disabledButtons.clear();
        for (JButton button : grid) {
            button.setText("");
        }
        updatePlayerStatus();
        enableChat();
        if (sign == 'X'){
            setMyTurn(true);
        } else{
            setMyTurn(false);
        }
    }

    public void endGame() {
        disableGameComponents();
        enableChat();
        System.out.println("YOU WIN");
        statusHeadLabel.setText("Game Over!");
    }

    // Method to check for a winning condition
    public boolean isWinning() {
        // Check rows, columns, and diagonals
        for (int i = 0; i < 3; i++) {
            // Checks For Horizontal Win
            if (!grid[i*3].getText().isEmpty() &&
                    grid[i*3].getText().equals(grid[i*3+1].getText()) &&
                    grid[i*3+1].getText().equals(grid[i*3+2].getText())) {
                return true;
            }
            // Checks For Vertical Win
            if (!grid[i].getText().isEmpty() &&
                    grid[i].getText().equals(grid[i+3].getText()) &&
                    grid[i+3].getText().equals(grid[i+6].getText())) {
                return true;
            }
        }
        // Checks For Diagonal Win
        if (!grid[0].getText().isEmpty() &&
                grid[0].getText().equals(grid[4].getText()) &&
                grid[4].getText().equals(grid[8].getText())) {
            return true;
        }
        if (!grid[2].getText().isEmpty() &&
                grid[2].getText().equals(grid[4].getText()) &&
                grid[4].getText().equals(grid[6].getText())) {
            return true;
        }
        return false;
    }

    // Method to check if the board is full (a draw)
    public boolean isFull() {
        for (int i = 0; i < 9; i++) {
            if (Objects.equals(grid[i].getText(), "")) {
                return false;
            }
        }
        return true;
    }

    private void updatePlayerStatus() {
        String currentPlayerStatus = isMyTurn ? "Your turn (" + sign + ")" : "Opponent's turn (" + (sign == 'X' ? 'O' : 'X') + ")";
        statusHeadLabel.setText(currentPlayerStatus);
    }


    public void updateBoard(Move move) {
        int id = move.getX() * 3 + move.getY();
        JButton clickedButton = grid[id];

        clickedButton.setText(String.valueOf(move.getSign()));
        clickedButton.setEnabled(false);
        disabledButtons.add(clickedButton);

        SwingUtilities.invokeLater(() -> {
            // Check for end-game conditions after updating the board
            if (isWinning()) {
                endGame();
                if (move.getSign() == sign) { // local player win
                    statusHeadLabel.setText("You Win!");
                } else { // opponent win
                    statusHeadLabel.setText("Opponent Wins!");
                }
            } else if (isFull()) {
                endGame();
                statusHeadLabel.setText("It's a draw!");
            }
        });

    }

    private void initBoard(JPanel Board) {
        Board.setLayout(new GridLayout(3, 3));
        for (int i = 0; i < 9; i++) {
            final int position = i;
            grid[i] = new JButton();
            grid[i].addActionListener(new ActionListener() {
                int id = position;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isMyTurn) {
                        int x = id / 3;
                        int y = id % 3;
                        char playerSign = sign;
                        Move move = new Move(x, y, playerSign);
                        updateBoard(move);
                        setMyTurn(false);
                        client.sendMoveToServer(move);
                        System.out.println("Sent move to server: " + move);  // Add this
                    } else {
                        // TODO: Show a message or play a sound to indicate it's not the player's turn.
                        System.out.println("NOT YOUR TURN"); //placeholder functionality
                    }
                }
            });

            Board.add(grid[i]);
        }
    }


    public void init_components() {
        JFrame frame = new JFrame("Noughts and Sockets");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;

        // Timer Panel
        timerLabel = new JLabel("Timer: 17", SwingConstants.CENTER); // TODO: Use actual timer value here
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.2;
        gbc.weighty = 0.2;
        frame.add(timerLabel, gbc);

        // Status / Turn Info Panel
        statusHeadLabel = new JLabel("Waiting for an opponent to join", SwingConstants.CENTER);
        gbc.gridx = 1;
        gbc.gridy = 0;
        frame.add(statusHeadLabel, gbc);

        // Chat Header Panel
        chatHeadLabel = new JLabel("Player Chat", SwingConstants.CENTER);
        chatHeadLabel.setBackground(bgColor);
        gbc.gridx = 2;
        gbc.gridy = 0;
        frame.add(chatHeadLabel, gbc);

        // Game Title
        titleLabel = new JLabel("Distributed Tic-Tac-Toe", SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridheight = 1;
        frame.add(titleLabel, gbc);

        // Board Panel
        JPanel Board = new JPanel();
        initBoard(Board);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridheight = 2;
        gbc.weighty = 0.8;
        frame.add(Board, gbc);

        // Chat Body Panel
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridheight = 1;
        gbc.weighty = 0.6;
        frame.add(chatScrollPane, gbc);

        // Quit Button
        JButton Quit = new JButton("Quit");
        Quit.addActionListener(e -> System.exit(0));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridheight = 1;
        gbc.weighty = 0.2;
        frame.add(Quit, gbc);

        // Chat Input
        chatInput = new JTextField();
        chatInput.addActionListener(e -> {
            String messageToSend = chatInput.getText();
            chatArea.append("You: " + messageToSend + "\n");
            client.sendChatMessageToServer(messageToSend);
            chatInput.setText("");
        });
        gbc.gridx = 2;
        gbc.gridy = 2;
        frame.add(chatInput, gbc);

        // Disable features until matched
        disableGameComponents();

        // Set Frame Size
        frame.setSize(800, 600);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
    }
}