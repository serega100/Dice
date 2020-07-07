package me.serega100.dice.listener;

import me.serega100.dice.DicePlayer;
import me.serega100.dice.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final GameManager manager;

    public PlayerListener(GameManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        DicePlayer.loadDicePlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DicePlayer dPlayer = DicePlayer.getDicePlayer(player);
        if (dPlayer.hasDiceGame()) {
            manager.onPlayerQuit(player);
        }
        DicePlayer.unloadDicePlayer(player);
    }
}
