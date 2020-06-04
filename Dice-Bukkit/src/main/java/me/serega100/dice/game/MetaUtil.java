package me.serega100.dice.game;

import me.serega100.dice.DicePlugin;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public class MetaUtil {
    private static final Plugin plugin = DicePlugin.getInstance();
    private static final String GAME_META = "DiceGameId";
    private static final String BLOCK_META = "DiceBlocking";

    public static void setDiceGame(Player player, Player enemy, DiceGame game) {
        player.setMetadata(GAME_META, new FixedMetadataValue(plugin, game));
        enemy.setMetadata(GAME_META, new FixedMetadataValue(plugin, game));
    }

    public static DiceGame getDiceGame(Player player) {
        return (DiceGame) player.getMetadata(GAME_META).get(0).value();
    }

    public static void setBlockMeta(Player p) {
        p.setMetadata(BLOCK_META, new FixedMetadataValue(plugin, true));
    }

    public static boolean isBlocked(Player p) {
       return p.hasMetadata(BLOCK_META);
    }

//    public static String getId(Player p) {
//        return p.getMetadata(GAME_META).get(0).asString();
//    }

    public static void removeDiceGame(Player p1, Player p2) {
        p1.removeMetadata(GAME_META, plugin);
        p2.removeMetadata(GAME_META, plugin);
    }

    public static void removeBlockMeta(Player p) {
        p.removeMetadata(BLOCK_META, plugin);
    }

    public static boolean hasDiceGame(Player p) {
        return p.hasMetadata(GAME_META);
    }
}
