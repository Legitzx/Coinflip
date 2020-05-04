package org.legitzxdevelopment.coinflip.coinflip;

// POJO

public class CoinflipGame {
    private String player1;
    private String player2;

    private long prize;
    private long timeCreated;

    public CoinflipGame(String player1, String player2, long prize, long timeCreated) {
        this.player1 = player1;
        this.player2 = player2;
        this.prize = prize;
        this.timeCreated = timeCreated;
    }

    public String getPlayer1() {
        return player1;
    }

    public void setPlayer1(String player1) {
        this.player1 = player1;
    }

    public String getPlayer2() {
        return player2;
    }

    public void setPlayer2(String player2) {
        this.player2 = player2;
    }

    public long getPrize() {
        return prize;
    }

    public void setPrize(long prize) {
        this.prize = prize;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }
}
