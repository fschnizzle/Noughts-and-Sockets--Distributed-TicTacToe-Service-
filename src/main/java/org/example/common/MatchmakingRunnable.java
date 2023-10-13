package org.example.common;

public class MatchmakingRunnable implements Runnable {
    private final GameLobby lobby;

    public MatchmakingRunnable(GameLobby lobby) {
        this.lobby = lobby;
    }

    @Override
    public void run() {
        while (true) {  // You might want to use a more sophisticated termination condition
            lobby.matchPlayers();
            try {
                Thread.sleep(500);  // Pause for a brief moment before checking again
            } catch (InterruptedException e) {
                // Handle the exception
                Thread.currentThread().interrupt();  // Respect the interrupt
                return;
            }
        }
    }
}
