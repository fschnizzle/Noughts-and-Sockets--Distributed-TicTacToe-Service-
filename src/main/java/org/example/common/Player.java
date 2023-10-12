package org.example.common;

import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Player implements Serializable {
    private String username;
    private char sign; // 'x' or 'o'
    private boolean myTurn;
    private double rank;

    // getters

    public String getUsername() {
        return username;
    }

    public char getSign() {
        return sign;
    }

    public double getRank() {
        return rank;
    }

    public boolean isMyTurn() {
        return myTurn;
    }


    public void setSign(char sign) {
        this.sign = sign;
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
    }

    public void setRank() {
        // Initial
        this.rank = 1500;
    }

    public void setRank(double newRank) {
        this.rank = newRank;
    }

    public void endMyTurn() {
        this.myTurn = false;
    }


    private void setUsername(String username){
        this.username = username;
    }

    public Player(String username) {
        this.username = username;
        setRank();
    }

    @Override
    public String toString() {
        return "Player " + username + "(" + rank + ")";
    }
}
