package me.serega100.dice;

import me.serega100.dice.game.DiceGame;
import me.serega100.dice.message.Message;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Objects;

public class DicePlayer {
    private static final HashMap<Player, DicePlayer> players = new HashMap<>();
    private final Player player;
    private boolean isBlocked;
    private DiceGame game;

    private DicePlayer(Player player) {
        this.player = player;
        this.isBlocked = false;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public void setDiceGame(@NotNull DiceGame game) {
        this.game = game;
    }

    public @NotNull DiceGame getDiceGame() {
        return game;
    }

    public boolean hasDiceGame() {
        return game != null;
    }

    public void removeDiceGame() {
        game = null;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void sendMessage(@NotNull Message msg) {
        player.sendMessage(msg.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DicePlayer that = (DicePlayer) o;
        return player.getUniqueId().equals(that.player.getUniqueId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(player.getUniqueId());
    }

    public static void loadDicePlayer(@NotNull Player player) {
        players.put(player, new DicePlayer(player));
    }

    public static DicePlayer getDicePlayer(@NotNull Player player) {
        return players.get(player);
    }

    public static void unloadDicePlayer(@NotNull Player player) {
        players.remove(player);
    }
}
