package lobby.minecraft_minigames.model;

import lobby.minecraft_minigames.game.MinigameType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameResult {

    private final MinigameType gameType;
    private final List<UUID> placements;
    private final Map<UUID, Integer> scores;

    public GameResult(MinigameType gameType, List<UUID> placements, Map<UUID, Integer> scores) {
        this.gameType = gameType;
        this.placements = placements;
        this.scores = scores;
    }

    public MinigameType getGameType() { return gameType; }

    public List<UUID> getPlacements() { return placements; }

    public Map<UUID, Integer> getScores() { return scores; }

    public UUID getWinner() {
        return placements.isEmpty() ? null : placements.get(0);
    }

    public int getPlacement(UUID uuid) {
        int index = placements.indexOf(uuid);
        return index >= 0 ? index + 1 : -1;
    }
}
