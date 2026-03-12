package lobby.minecraft_minigames.model;

import java.util.UUID;

public class PlayerData {

    private final UUID playerId;
    private int score;
    private int kills;
    private int deaths;
    private boolean alive;
    private boolean spectating;
    private int placement;

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.score = 0;
        this.kills = 0;
        this.deaths = 0;
        this.alive = true;
        this.spectating = false;
        this.placement = 0;
    }

    public UUID getPlayerId() { return playerId; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public void addScore(int amount) { this.score += amount; }
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    public void addKill() { this.kills++; }
    public int getDeaths() { return deaths; }
    public void addDeath() { this.deaths++; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public boolean isSpectating() { return spectating; }
    public void setSpectating(boolean spectating) { this.spectating = spectating; }
    public int getPlacement() { return placement; }
    public void setPlacement(int placement) { this.placement = placement; }

    public void reset() {
        this.score = 0;
        this.kills = 0;
        this.deaths = 0;
        this.alive = true;
        this.spectating = false;
        this.placement = 0;
    }
}
