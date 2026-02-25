package lobby.minecraft_minigames;

import lobby.minecraft_minigames.command.MinigameCommand;
import lobby.minecraft_minigames.command.PartyCommand;
import lobby.minecraft_minigames.listener.GameProtectionListener;
import lobby.minecraft_minigames.listener.GlobalListener;
import lobby.minecraft_minigames.listener.LobbyListener;
import lobby.minecraft_minigames.manager.*;

public final class Minecraft_minigames extends org.bukkit.plugin.java.JavaPlugin {

    private static Minecraft_minigames instance;
    private GameManager gameManager;
    private PartyManager partyManager;
    private ArenaManager arenaManager;
    private LobbyManager lobbyManager;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default configs
        saveDefaultConfig();
        if (!new java.io.File(getDataFolder(), "arenas.yml").exists()) {
            saveResource("arenas.yml", false);
        }

        // Initialize managers
        arenaManager = new ArenaManager(this);
        arenaManager.loadArenas();

        scoreboardManager = new ScoreboardManager(this);
        partyManager = new PartyManager();
        gameManager = new GameManager(this, arenaManager, scoreboardManager);
        lobbyManager = new LobbyManager(this, gameManager, scoreboardManager);
        gameManager.setLobbyManager(lobbyManager);
        gameManager.initialize();

        // Register listeners
        getServer().getPluginManager().registerEvents(new GlobalListener(this, gameManager), this);
        getServer().getPluginManager().registerEvents(new LobbyListener(this, lobbyManager, gameManager), this);
        getServer().getPluginManager().registerEvents(new GameProtectionListener(this, gameManager), this);

        // Register commands
        var mgCmd = getCommand("minigame");
        if (mgCmd != null) {
            MinigameCommand mgExecutor = new MinigameCommand(this, gameManager, lobbyManager);
            mgCmd.setExecutor(mgExecutor);
            mgCmd.setTabCompleter(mgExecutor);
        }

        var partyCmd = getCommand("party");
        if (partyCmd != null) {
            PartyCommand partyExecutor = new PartyCommand(this, partyManager);
            partyCmd.setExecutor(partyExecutor);
            partyCmd.setTabCompleter(partyExecutor);
        }

        getLogger().info("MiniGames plugin enabled! Loaded " + arenaManager.getAllArenas().size() + " arenas.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
        getLogger().info("MiniGames plugin disabled!");
    }

    public static Minecraft_minigames getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public LobbyManager getLobbyManager() { return lobbyManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
}
