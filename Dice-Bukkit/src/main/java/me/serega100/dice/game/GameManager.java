package me.serega100.dice.game;

import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import me.serega100.dice.DiceException;
import me.serega100.dice.DicePlayer;
import me.serega100.dice.DicePlugin;
import me.serega100.dice.message.Message;
import me.serega100.dice.message.MessageBuilder;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

public class GameManager {
    // todo remove heads and bone from blocked players before server shutdown
    private final DicePlugin plugin;
    private final RegionContainer container = WorldGuardPlugin.inst().getRegionContainer();
    private final List<OfflinePlayer> ignoreList = new ArrayList<>();

    public GameManager(DicePlugin plugin) {
        this.plugin = plugin;
        for (String uuid : plugin.getConfig().getStringList("ignoreList")) {
            ignoreList.add(Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
        }
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::saveIgnoreList, 6000L, 6000L);
    }

    public void createGame(Player player, Player enemy, int bet) throws DiceException {
        DicePlayer dPlayer = DicePlayer.getDicePlayer(player);
        if (dPlayer.isBlocked()) {
            throw new DiceException(player, Message.YOU_ARE_ALREADY_PLAYING);
        }
        if (ignoreList.contains(enemy)) {
            throw new DiceException(player, Message.ENEMY_IS_IGNORING);
        }
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            throw new DiceException(player, Message.MAIN_HAND_MUST_BE_EMPTY);
        }
        if (!plugin.getEconomy().has(player, bet)) {
            throw new DiceException(player, Message.YOU_HAVE_NO_MONEY);
        }

        ApplicableRegionSet regions = container.get(player.getWorld()).getApplicableRegions(player.getLocation());
        if (regions.queryState(null, DicePlugin.DICE_AVAILABLE_FLAG) != StateFlag.State.ALLOW) {
            throw new DiceException(player, Message.LOCATION_IS_NOT_AVAILABLE);
        }

        DicePlayer dEnemy = DicePlayer.getDicePlayer(enemy);
        DiceGame game = new DiceGame(regions, dPlayer, dEnemy, bet);
        if (!game.isAvailableLocation(enemy.getLocation())) {
            throw new DiceException(player, Message.ENEMY_LOCATION_IS_NOT_AVAILABLE);
        }

        dPlayer.setBlocked(true);
        dPlayer.setDiceGame(game);
        dEnemy.setDiceGame(game);

        new MessageBuilder(Message.ON_REQUEST)
                .setPlayer(dEnemy)
                .setBet(bet)
                .send(dPlayer);
        new MessageBuilder(Message.REQUEST_TO_SEND)
                .setPlayer(dPlayer)
                .setBet(bet)
                .send(dEnemy);
        enemy.sendMessage(Message.MSG_TO_ACCEPT.toString());
        enemy.sendMessage(Message.MSG_TO_REFUSE.toString());
    }

    public void gameAgree(Player player) throws DiceException {
        DicePlayer dPlayer = DicePlayer.getDicePlayer(player);
        if (dPlayer.isBlocked()) {
            throw new DiceException(player, Message.YOU_ARE_ALREADY_PLAYING);
        }
        if (!dPlayer.hasDiceGame()) {
            throw new DiceException(dPlayer, Message.NO_REQUESTS);
        }
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            throw new DiceException(player, Message.MAIN_HAND_MUST_BE_EMPTY);
        }

        DiceGame game = dPlayer.getDiceGame();
        DicePlayer dEnemy = game.getEnemy(dPlayer);
        int bet = game.getBet();

        EconomyResponse responsePlayer = plugin.getEconomy().withdrawPlayer(dEnemy.getPlayer(), bet);
        if (!responsePlayer.transactionSuccess()) {
            dEnemy.setBlocked(false);
            dEnemy.removeDiceGame();
            dPlayer.removeDiceGame();
            dEnemy.sendMessage(Message.YOU_HAVE_NO_MONEY);
            dEnemy.sendMessage(Message.YOU_DISALLOW_REQUEST);
            dPlayer.sendMessage(Message.YOUR_REQUEST_DISALLOWED);
            return;
        }

        EconomyResponse responseEnemy = plugin.getEconomy().withdrawPlayer(player, bet);
        if (!responseEnemy.transactionSuccess()) {
            dEnemy.setBlocked(false);
            dEnemy.removeDiceGame();
            dPlayer.removeDiceGame();
            plugin.getEconomy().depositPlayer(dEnemy.getPlayer(), bet);
            dPlayer.sendMessage(Message.YOU_HAVE_NO_MONEY);
            dEnemy.sendMessage(Message.YOUR_REQUEST_DISALLOWED);
            return;
        }

        dPlayer.setBlocked(true);
        game.start();
        giveDiceItem(player);
        giveDiceItem(dEnemy.getPlayer());
        dPlayer.sendMessage(Message.YOU_ACCEPT_REQUEST);
        dEnemy.sendMessage(Message.YOUR_REQUEST_ACCEPTED);
        dPlayer.sendMessage(Message.GAME_HAS_STARTED);
        dEnemy.sendMessage(Message.GAME_HAS_STARTED);
    }

    public void gameDisagree(Player player) throws DiceException {
        DicePlayer dPlayer = DicePlayer.getDicePlayer(player);
        if (dPlayer.isBlocked()) {
            throw new DiceException(dPlayer, Message.YOU_ARE_ALREADY_PLAYING);
        }
        if (!dPlayer.hasDiceGame()) {
            throw new DiceException(dPlayer, Message.NO_REQUESTS);
        }

        DiceGame game = dPlayer.getDiceGame();
        DicePlayer dEnemy = game.getEnemy(dPlayer);

        dPlayer.removeDiceGame();
        dEnemy.removeDiceGame();
        dPlayer.setBlocked(false);
        dEnemy.setBlocked(false);

        dEnemy.sendMessage(Message.YOUR_REQUEST_DISALLOWED);
        dPlayer.sendMessage(Message.YOU_DISALLOW_REQUEST);
    }

    public void onPlaceDiceItem(Player player, Block block) throws DiceException {
        DicePlayer dPlayer = DicePlayer.getDicePlayer(player);
        DiceGame game = dPlayer.getDiceGame();

        if (!game.isAvailableLocation(block.getLocation())) {
            throw new DiceException(player, Message.LOCATION_IS_NOT_AVAILABLE);
        }

        int result = randomDice();
        Lock locker = game.getLocker();

        plugin.getBoneManager().getBone(result).setAsBlock(block);
        new MessageBuilder(Message.YOU_HAVE_NUMBER)
                .setNumber(result)
                .send(dPlayer);
        player.getInventory().setItemInMainHand(null);

        locker.lock();
        if (game.getStatus() == DiceGame.Status.FINISHED) {
            int result2 = game.getResult();
            DicePlayer dEnemy = game.getEnemy(dPlayer);
            int bet = game.getBet();
            if(result > result2) {
                plugin.getEconomy().depositPlayer(player, bet * 2);
                new MessageBuilder(Message.YOU_WIN)
                        .setBet(bet)
                        .send(dPlayer);
                new MessageBuilder(Message.YOU_LOSE)
                        .setBet(bet)
                        .send(dEnemy);
            } else if (result < result2) {
                plugin.getEconomy().depositPlayer(dEnemy.getPlayer(), bet * 2);
                new MessageBuilder(Message.YOU_WIN)
                        .setBet(bet)
                        .send(dEnemy);
                new MessageBuilder(Message.YOU_LOSE)
                        .setBet(bet)
                        .send(player);
            } else {
                plugin.getEconomy().depositPlayer(player, bet);
                plugin.getEconomy().depositPlayer(dEnemy.getPlayer(), bet);
                new MessageBuilder(Message.DEAD_HEAD)
                        .setBet(bet)
                        .send(player)
                        .send(dEnemy);
            }
            dPlayer.removeDiceGame();
            dPlayer.setBlocked(false);
            dEnemy.removeDiceGame();
            dEnemy.setBlocked(false);
            scheduleBlockRemoving(game.getBlock(), block);
        } else {
            game.finish(block, result);
        }
        locker.unlock();
    }
    
    public void setIgnore(Player p, boolean ignore) {
        if (ignore == isIgnore(p)) return;
        switchIgnore(p);
    }

    public void switchIgnore(Player p) {
        if (!isIgnore(p)) {
            ignoreList.add(p);
            p.sendMessage(Message.IGNORING_ENABLE.toString());
        } else {
            ignoreList.remove(p);
            p.sendMessage(Message.IGNORING_DISABLE.toString());
        }
    }
    
    public boolean isIgnore(Player p) {
        return ignoreList.contains(p);
    }
    
    public void onPlayerQuit(Player player) {
        DicePlayer dPlayer = DicePlayer.getDicePlayer(player);
        DiceGame game = DicePlayer.getDicePlayer(player).getDiceGame();
        DicePlayer dEnemy = game.getEnemy(dPlayer);
        int bet = game.getBet();
        dPlayer.removeDiceGame();
        dEnemy.removeDiceGame();
        if (game.getStatus() == DiceGame.Status.REQUESTING) {
            dEnemy.sendMessage(Message.ON_REQUESTING_PLAYER_QUIT);
        }
        if (game.getStatus() == DiceGame.Status.STARTED) {
            plugin.getEconomy().depositPlayer(dEnemy.getPlayer(), bet * 2);
            dEnemy.sendMessage(Message.YOU_WIN);
        }
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            DicePlayer dPlayer = DicePlayer.getDicePlayer(player);
            if (dPlayer.hasDiceGame()) {
                DiceGame game = dPlayer.getDiceGame();
                DicePlayer dEnemy = game.getEnemy(dPlayer);
                int bet = game.getBet();
                if (game.getStatus() != DiceGame.Status.REQUESTING) {
                    plugin.getEconomy().depositPlayer(player, bet);
                    plugin.getEconomy().depositPlayer(player, bet);
                    new MessageBuilder(Message.ON_SERVER_SHUTDOWN)
                            .setBet(game.getBet())
                            .send(dPlayer)
                            .send(dEnemy);
                }
            }
        }
        saveIgnoreList();
    }

    private void saveIgnoreList() {
        plugin.getConfig().set("ignoreList", ignoreList);
        plugin.saveConfig();
    }

    private void giveDiceItem(Player p) {
        p.getInventory().setItemInMainHand(plugin.getBoneManager().getBone(1).getAsItem());
    }

    private int randomDice() {
        Random rand = new Random();
        return rand.nextInt(5) + 1;
    }

    private void scheduleBlockRemoving(Block b1, Block b2) {
        Bukkit.getScheduler().runTaskLater(DicePlugin.getInstance(), () -> {
            b1.setType(Material.AIR);
            b2.setType(Material.AIR);
        }, 40L);
    }
}
