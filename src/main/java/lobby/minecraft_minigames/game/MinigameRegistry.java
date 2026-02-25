package lobby.minecraft_minigames.game;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.game.impl.*;
import lobby.minecraft_minigames.manager.GameManager;
import lobby.minecraft_minigames.model.Arena;

import java.util.EnumMap;
import java.util.Map;

@FunctionalInterface
interface MinigameFactory {
    Minigame create(Minecraft_minigames plugin, GameManager manager, Arena arena);
}

public class MinigameRegistry {

    private final Map<MinigameType, MinigameFactory> factories;

    public MinigameRegistry() {
        factories = new EnumMap<>(MinigameType.class);
        registerDefaults();
    }

    private void registerDefaults() {
        factories.put(MinigameType.TNT_RUN, TntRunGame::new);
        factories.put(MinigameType.SPLEEF, SpleefGame::new);
        factories.put(MinigameType.SUMO, SumoGame::new);
        factories.put(MinigameType.ONE_IN_THE_QUIVER, OneInTheQuiverGame::new);
        factories.put(MinigameType.HOT_POTATO, HotPotatoGame::new);
        factories.put(MinigameType.COLOR_SWAP, ColorSwapGame::new);
        factories.put(MinigameType.KING_OF_THE_HILL, KingOfTheHillGame::new);
        factories.put(MinigameType.PARKOUR_RACE, ParkourRaceGame::new);
        factories.put(MinigameType.MUSICAL_MINECARTS, MusicalMinecartsGame::new);
        factories.put(MinigameType.BLOCK_SHUFFLE, BlockShuffleGame::new);
        factories.put(MinigameType.ANVIL_RAIN, AnvilRainGame::new);
        factories.put(MinigameType.SNOWBALL_FIGHT, SnowballFightGame::new);
    }

    public Minigame createGame(MinigameType type, Minecraft_minigames plugin,
                                GameManager manager, Arena arena) {
        MinigameFactory factory = factories.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("No factory registered for: " + type);
        }
        return factory.create(plugin, manager, arena);
    }

    public boolean isRegistered(MinigameType type) {
        return factories.containsKey(type);
    }
}
