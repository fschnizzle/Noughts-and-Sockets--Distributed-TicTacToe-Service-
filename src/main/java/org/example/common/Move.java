package org.example.common;

        import java.io.Serializable;

public class Move implements Serializable {
    private int x; // Row position on the board
    private int y; // Column position on the board
    private char sign; // Player's sign: 'X' or 'O'

    // Constructor
    public Move(int x, int y, char sign) {
        this.x = x;
        this.y = y;
        this.sign = sign;
    }

    // Getter methods
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public char getSign() {
        return sign;
    }

    // Setter methods (in case they're needed later)
    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setSign(char sign) {
        this.sign = sign;
    }

    @Override
    public String toString() {
        return "Move{" + "x=" + x + ", y=" + y + ", sign=" + sign + '}';
    }
}

