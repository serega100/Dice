package me.serega100.dice;

import me.serega100.dice.game.DiceGame;
import me.serega100.dice.game.GameManager;
import me.serega100.dice.game.MetaUtil;
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
public class EventManager implements Listener {
    private final GameManager manager;
    private final ItemStack firstBoneItem = DicePlugin.getInstance().getBoneManager().getBone(1).getAsItem();;

    EventManager(GameManager manager) {
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
        Player p = (Player) event.getWhoClicked();
        if(MetaUtil.isBlocked(p)) {
            ItemStack item = p.getInventory().getItemInMainHand();
            event.setCancelled(true);
            p.getInventory().setItemInMainHand(item);
            p.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        Player p = (Player) event.getWhoClicked();
        if(MetaUtil.isBlocked(p)) {
            ItemStack item = p.getInventory().getItemInMainHand();
            event.setCancelled(true);
            p.getInventory().setItemInMainHand(item);
            p.closeInventory();
        }
    }

    @EventHandler
    public void onBuildDiceItem(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if(MetaUtil.isBlocked(player)) {
            if(event.getItemInHand().equals(firstBoneItem)) {
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
        if (MetaUtil.hasDiceGame(player)) {
            manager.onPlayerQuit(player);
        }
        DicePlugin.getInstance().getLogger().info(String.format("%s left from the server", player.getName()));
        DicePlugin.getInstance().getLogger().info("Plugin is enabled: " + DicePlugin.getInstance().isEnabled());
    }

    private <T extends PlayerEvent & Cancellable> void cancelIfPlayerDicing(T event) {
        if(MetaUtil.isBlocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
