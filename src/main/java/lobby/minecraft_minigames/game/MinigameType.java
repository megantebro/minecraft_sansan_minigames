package lobby.minecraft_minigames.game;

import org.bukkit.Material;

public enum MinigameType {
    TNT_RUN("TNT Run", "踏んだ床が消える！最後まで生き残れ！", Material.TNT),
    SPLEEF("Spleef", "雪ブロックを壊して相手を落とせ！", Material.DIAMOND_SHOVEL),
    SUMO("Sumo", "相手を押し出して場外に落とせ！", Material.STICK),
    ONE_IN_THE_QUIVER("One in the Quiver", "矢1本で一撃！近接キルで矢を補充！", Material.ARROW),
    HOT_POTATO("Hot Potato", "TNTを持っている人が爆発！パンチで渡せ！", Material.BAKED_POTATO),
    COLOR_SWAP("Color Swap", "指定された色の羊毛に乗れ！", Material.WHITE_WOOL),
    KING_OF_THE_HILL("King of the Hill", "中央エリアを制圧してポイントを獲得！", Material.GOLDEN_HELMET),
    PARKOUR_RACE("Parkour Race", "パルクールコースを最速でゴールしろ！", Material.LEATHER_BOOTS),
    MUSICAL_MINECARTS("Musical Minecarts", "音楽が止まったらトロッコに乗れ！", Material.MINECART),
    BLOCK_SHUFFLE("Block Shuffle", "指定されたブロックを探して乗れ！", Material.GRASS_BLOCK),
    ANVIL_RAIN("Anvil Rain", "降ってくる金床を避けろ！", Material.ANVIL),
    SNOWBALL_FIGHT("Snowball Fight", "雪合戦！最後まで立っていろ！", Material.SNOWBALL);

    private final String displayName;
    private final String description;
    private final Material icon;

    MinigameType(String displayName, String description, Material icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Material getIcon() { return icon; }

    public static MinigameType fromString(String name) {
        for (MinigameType type : values()) {
            if (type.name().equalsIgnoreCase(name) ||
                type.displayName.equalsIgnoreCase(name) ||
                type.name().replace("_", "").equalsIgnoreCase(name.replace("_", "").replace(" ", ""))) {
                return type;
            }
        }
        return null;
    }
}
