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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class NoughtsAndSocketsClientGUI {
    private Client client;
    public char sign;
    public boolean isMyTurn;
    public Player player;

    public Player opponent;

    public boolean gameActive = false;
    private Set<JButton> disabledButtons = new HashSet<>();
    public JButton[] grid = new JButton[9];
    public Color bgColor = Color.GRAY;

    public JTextArea chatArea;
    public JButton Quit;
    public JTextField chatInput;
    private JLabel timerLabel;
    private JLabel statusHeadLabel;
    private JLabel chatHeadLabel;
    private JLabel titleLabel;


    public NoughtsAndSocketsClientGUI(Client client) {
        this.client = client;
        init_components();
    }

    public void setGameActive(boolean gameActive) {
        this.gameActive = gameActive;
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

    public void receiveChatMessage(String message) {
        chatArea.append("Opponent: " + message + "\n");
    }


    public void enableChat() {
        chatInput.setEnabled(true);
        chatInput.setBackground(Color.WHITE);
    }

    public void disableGameComponents() {
        for (JButton button : grid) {
            button.setEnabled(false);
            button.setBackground(Color.DARK_GRAY);
        }
        chatInput.setEnabled(false);
//        statusHeadLabel.setText("Finding Opponent...");
    }

    public void lostOpponentConnection(boolean lost) {
        if (lost) {
            for (JButton button : grid) {
                button.setEnabled(false);
                button.setBackground(Color.DARK_GRAY);
            }
            chatInput.setEnabled(false);
            statusHeadLabel.setText("Lost connection to opponent...");
        } else {
            for (JButton button : grid) {
                button.setEnabled(true);
                button.setBackground(Color.GRAY);
            }
            chatInput.setEnabled(true);
            statusHeadLabel.setText("Connection to opponent returned");
        }
    }

    public void enableBoard() {
        for (JButton button : grid) {
            if (!disabledButtons.contains(button)) {
                button.setEnabled(true);
                button.setBackground(bgColor);
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

    public void startGame(Player currentPlayer, Player opponentPlayer) {
        // Set player and opponent player objects
        this.player = currentPlayer;
        this.opponent = opponentPlayer;
        this.sign = currentPlayer.getSign();

        // Reset perma-disabled (selected) grid cell buttons
        disabledButtons.clear();
        for (JButton button : grid) {
            button.setText("");
        }

        // Enable chat
        enableChat();

        // Give turn permission to 'X' player and update status accordingly
        if (sign == 'X'){
            setMyTurn(true);
        } else{
            setMyTurn(false);
        }
        updatePlayerStatus();
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
//        String currentPlayerStatus = isMyTurn ? "Your turn (" + sign + ")" : "Opponent's turn (" + (sign == 'X' ? 'O' : 'X') + ")";
        String currentPlayerStatus = isMyTurn ? "Your Turn (" + player.getSign() + ") - Rank: " + player.getRank()  : opponent.getUsername() + "'s turn  (" + opponent.getSign() + ") - Rank: " + opponent.getRank();
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
//                endGame();
                if (move.getSign() == sign) { // local player win
                    statusHeadLabel.setText("You Win!");
                    // Update ELO status with local player as winner
                    //client.sendMessageToServer('W');
                    handleGameEndSend('W');

                } else { // opponent win
                    statusHeadLabel.setText("Opponent Wins!");
                }
            } else if (isFull()) {
//                endGame();
                statusHeadLabel.setText("It's a draw!");
                // Update ELO status with draw outcome
//                client.sendMessageToServer('D');
                handleGameEndSend('D');

            }
        });

    }

    public void showQuitDialog() {
        String[] options = {"QUIT2", "PLAY AGAIN"};
        int response = JOptionPane.showOptionDialog(null, statusHeadLabel.getText() + "\nNew Rank" + player.getRank(), "Game Over", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if(response == 0) { // QUIT
            System.exit(0);
        } else if(response == 1) { // PLAY AGAIN
//            System.exit(0);
            // Handle the logic to restart the game or go back to lobby
        }
    }

    public void handleGameEndReceive(char oppStatus){
        // Here you have received notification from the server that the game has ended
//        handleGameEndSend();

        // Handle Dialog case by case (switch) according to opponent game case (ie: opponent wins)
        String message = "";
        System.out.println("RECEIVE ");
        switch(oppStatus) {
            case 'W':
                message = "Opponent Wins!";
                break;
            case 'L':
                message = "You Win!";
                break;
            case 'D':
                message = "It's a draw!";
                break;
            case 'Q':
                message = "Opponent has quit. You win!";
                break;
        }
        statusHeadLabel.setText(message);
        setGameActive(false);

        // Await response (updates etc)
        showQuitDialog();
    }

    public void handleGameEndSend(char status){
        System.out.println("SEND");
        // Disable all features
        disableGameComponents();
        Quit.setEnabled(false);

        // Update server
        client.sendMessageToServer(status);

        // Wait for 0.5 seconds
        try {
            Thread.sleep(500);  // Sleep for 500 milliseconds = 0.5 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Show quit popup
        showQuitDialog();
//        setGameActive(false);
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
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        // Window close
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Window closing event triggered!"); // Debugging statement

//                statusHeadLabel.setText("You quit. Opponent wins");
                client.sendMessageToServer('Q');
                System.exit(0);  // Close the application
            }
        });
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
        statusHeadLabel = new JLabel("Finding opponent...", SwingConstants.CENTER);
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
        Quit = new JButton("Quit");
//        Quit.addActionListener(e -> System.exit(0));
        Quit.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                statusHeadLabel.setText("You forfeit. Opponent wins");
                handleGameEndSend('Q');
            });
        });
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


