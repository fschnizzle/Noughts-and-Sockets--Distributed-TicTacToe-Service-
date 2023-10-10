package org.example.common;

import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Player implements Serializable {
    private String username;
    private char sign; // 'x' or 'o'
    private boolean myTurn;

    // getters

    public String getUsername() {
        return username;
    }

    public char getSign() {
        return sign;
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

    public void endMyTurn() {
        this.myTurn = false;
    }


    private void setUsername(String username){
        this.username = username;
    }

    public Player(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "Player " + username;
    }
}
