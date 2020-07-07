package me.serega100.dice;

import me.serega100.dice.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

// todo equal play zone
public class BlockingListener implements Listener {
    private final GameManager manager;
    private final ItemStack firstBoneItem = DicePlugin.getInstance().getBoneManager().getBone(1).getAsItem();;

    BlockingListener(GameManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChangeHandItem(PlayerItemHeldEvent event) {
        cancelIfPlayerDicing(event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        cancelIfPlayerDicing(event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDropItem(PlayerDropItemEvent event) {
        cancelIfPlayerDicing(event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        DicePlayer dPlayer = DicePlayer.getDicePlayer(player);
        if (dPlayer.isBlocked()) {
            ItemStack item = player.getInventory().getItemInMainHand();
            event.setCancelled(true);
            player.getInventory().setItemInMainHand(item);
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        Player player = (Player) event.getWhoClicked();
        DicePlayer dPlayer = DicePlayer.getDicePlayer(player);
        if (dPlayer.isBlocked()) {
            ItemStack item = player.getInventory().getItemInMainHand();
            event.setCancelled(true);
            player.getInventory().setItemInMainHand(item);
            player.closeInventory();
        }
    }

    @EventHandler
    public void onBuildDiceItem(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        DicePlayer dPlayer = DicePlayer.getDicePlayer(player);
        if (dPlayer.isBlocked()) {
            if (event.getItemInHand().equals(firstBoneItem)) {
                try {
                    manager.onPlaceDiceItem(player, event.getBlockPlaced());
                } catch (DiceException e) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DicePlayer dPlayer = DicePlayer.getDicePlayer(player);
        if (dPlayer.hasDiceGame()) {
            manager.onPlayerQuit(player);
        }
    }

    private <T extends PlayerEvent & Cancellable> void cancelIfPlayerDicing(T event) {
        DicePlayer dPlayer = DicePlayer.getDicePlayer(event.getPlayer());
        if (dPlayer.isBlocked()) {
            event.setCancelled(true);
        }
    }
}
