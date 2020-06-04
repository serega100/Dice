package me.serega100.dice.game;

import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.serega100.dice.*;
import me.serega100.dice.nms.HeadCreator;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.locks.Lock;

public class GameManager {
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
        checkGaming(player);
        checkEnemyInIgnoreList(player, enemy);
        checkHand(player);
        checkPlayerLocation(player);
        checkEnemyLocation(player, enemy);
        checkMoney(player, bet);

        MetaUtil.setBlockMeta(player);
        DiceGame game = new DiceGame(player, enemy, bet);
        MetaUtil.setDiceGame(player, enemy, game);

        new MessageBuilder(Message.ON_REQUEST)
                .setPlayer(enemy)
                .setBet(bet)
                .send(player);
        new MessageBuilder(Message.REQUEST_TO_SEND)
                .setPlayer(player)
                .setBet(bet)
                .send(enemy);
        enemy.sendMessage(Message.MSG_TO_ACCEPT.toString());
        enemy.sendMessage(Message.MSG_TO_REFUSE.toString());
    }

    public void gameAgree(Player enemy) throws DiceException {
        checkGaming(enemy);
        checkGameRequest(enemy);
        checkHand(enemy);

        DiceGame game = MetaUtil.getDiceGame(enemy);
        Player player = game.getEnemy(enemy);
        int bet = game.getBet();

        EconomyResponse responsePlayer = plugin.getEconomy().withdrawPlayer(player, bet);
        if (!responsePlayer.transactionSuccess()) {
            MetaUtil.removeBlockMeta(player);
            MetaUtil.removeDiceGame(player, enemy);
            player.sendMessage(Message.YOU_HAVE_NO_MONEY.toString());
            player.sendMessage(Message.YOU_DISALLOW_REQUEST.toString());
            enemy.sendMessage(Message.YOUR_REQUEST_DISALLOWED.toString());
            return;
        }

        EconomyResponse responseEnemy = plugin.getEconomy().withdrawPlayer(enemy, bet);
        if (!responseEnemy.transactionSuccess()) {
            MetaUtil.removeBlockMeta(player);
            MetaUtil.removeDiceGame(player, enemy);
            plugin.getEconomy().depositPlayer(player, bet);
            enemy.sendMessage(Message.YOU_HAVE_NO_MONEY.toString());
            player.sendMessage(Message.YOUR_REQUEST_DISALLOWED.toString());
            return;
        }

        MetaUtil.setBlockMeta(enemy);
        game.start(); //todo check this to bag
        giveDiceItem(player);
        giveDiceItem(enemy);
        player.sendMessage(Message.YOUR_REQUEST_ACCEPTED.toString());
        enemy.sendMessage(Message.YOU_ACCEPT_REQUEST.toString());
        player.sendMessage(Message.GAME_HAS_STARTED.toString());
        enemy.sendMessage(Message.GAME_HAS_STARTED.toString());
    }

    public void gameDisagree(Player enemy) throws DiceException {
        checkGaming(enemy);
        checkGameRequest(enemy);

        DiceGame game = MetaUtil.getDiceGame(enemy);
        Player player = game.getEnemy(enemy);
        removeData(player, enemy);
        player.sendMessage(Message.YOUR_REQUEST_DISALLOWED.toString());
        enemy.sendMessage(Message.YOU_DISALLOW_REQUEST.toString());
    }

    public void onPlaceDiceItem(Player player, Block block) throws DiceException {
        checkPlayerLocation(player);

        DiceGame game = MetaUtil.getDiceGame(player);
        int result = randomDice();
        Lock locker = game.getLocker();

        plugin.getBoneManager().getBone(result).setAsBlock(block);
        new MessageBuilder(Message.YOU_HAVE_NUMBER)
                .setNumber(result)
                .send(player);
        player.getInventory().setItemInMainHand(null);

        locker.lock();
        if(game.getStatus() == DiceGame.Status.FINISHED) {
            int result2 = game.getResult();
            Player enemy = game.getEnemy(player);
            int bet = game.getBet();
            if(result > result2) {
                plugin.getEconomy().depositPlayer(player, bet * 2);
                new MessageBuilder(Message.YOU_WIN)
                        .setBet(bet)
                        .send(player);
                new MessageBuilder(Message.YOU_LOSE)
                        .setBet(bet)
                        .send(enemy);
            }else if(result < result2) {
                plugin.getEconomy().depositPlayer(enemy, bet * 2);
                new MessageBuilder(Message.YOU_WIN)
                        .setBet(bet)
                        .send(enemy);
                new MessageBuilder(Message.YOU_LOSE)
                        .setBet(bet)
                        .send(player);
            }else {
                plugin.getEconomy().depositPlayer(player, bet);
                plugin.getEconomy().depositPlayer(enemy, bet);
                new MessageBuilder(Message.DEAD_HEAD)
                        .setBet(bet)
                        .send(player)
                        .send(enemy);
            }
            removeData(player, enemy);
            scheduleBlockRemoving(game.getBlock(), block);
        }else{
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
        DiceGame game = MetaUtil.getDiceGame(player);
        Player enemy = game.getEnemy(player);
        int bet = game.getBet();
        MetaUtil.removeDiceGame(player, enemy);
        if (game.getStatus() == DiceGame.Status.REQUESTING) {
            enemy.sendMessage(Message.ON_REQUESTING_PLAYER_QUIT.toString());
        }
        if (game.getStatus() == DiceGame.Status.STARTED) {
            plugin.getEconomy().depositPlayer(enemy, bet * 2);
            enemy.sendMessage(Message.YOU_WIN.toString());
        }
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (MetaUtil.hasDiceGame(player)) {
                DiceGame game = MetaUtil.getDiceGame(player);
                Player enemy = game.getEnemy(player);
                int bet = game.getBet();
                if (game.getStatus() != DiceGame.Status.REQUESTING) {
                    plugin.getEconomy().depositPlayer(player, bet);
                    plugin.getEconomy().depositPlayer(player, bet);
                    new MessageBuilder(Message.ON_SERVER_SHUTDOWN)
                            .setBet(game.getBet())
                            .send(player)
                            .send(enemy);
                }
            }
        }
        saveIgnoreList();
    }

    private void saveIgnoreList() {
        plugin.getConfig().set("ignoreList", ignoreList);
        plugin.saveConfig();
    }


    private void checkHand(Player player) throws DiceException {
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
            throw new DiceException(player, Message.MAIN_HAND_SHOULD_BE_EMPTY);
        }
    }

    private void checkMoney(Player player, int bet) throws DiceException {
        if (!plugin.getEconomy().has(player, bet)) {
            throw new DiceException(player, Message.YOU_HAVE_NO_MONEY);
        }
    }

    private void checkEnemyInIgnoreList(Player player, Player enemy) throws DiceException {
        if (ignoreList.contains(enemy)) {
            throw new DiceException(player, Message.ENEMY_IS_IGNORING);
        }
    }

    private void checkGaming(Player player) throws DiceException {
        if (MetaUtil.isBlocked(player)) {
            throw new DiceException(player, Message.YOU_ARE_ALREADY_PLAYING);
        }
    }

    private void checkPlayerLocation(Player player) throws DiceException {
        if (isNotAvailableLocation(player.getLocation())) {
            throw new DiceException(player, Message.LOCATION_IS_NOT_AVAILABLE);
        }
    }

    private void checkEnemyLocation(Player player, Player enemy) throws DiceException {
        if (isNotAvailableLocation(enemy.getLocation())) {
            throw new DiceException(player, Message.ENEMY_LOCATION_IS_NOT_AVAILABLE);
        }
    }

    private void checkGameRequest(Player player) throws DiceException {
        if(!MetaUtil.hasDiceGame(player)) {
            throw new DiceException(player, Message.NO_REQUESTS);
        }
    }

    private void giveDiceItem(Player p) {
        p.getInventory().setItemInMainHand(plugin.getBoneManager().getBone(1).getAsItem());
    }

    private int randomDice() {
        Random rand = new Random();
        return rand.nextInt(5) + 1;
    }

    private void removeData(Player p1, Player p2) {
        MetaUtil.removeDiceGame(p1, p2);
        MetaUtil.removeBlockMeta(p1);
        MetaUtil.removeBlockMeta(p2);
    }

    private void scheduleBlockRemoving(Block b1, Block b2) {
        Bukkit.getScheduler().runTaskLater(DicePlugin.getInstance(), () -> {
            b1.setType(Material.AIR);
            b2.setType(Material.AIR);
        }, 40L);
    }

    private boolean isNotAvailableLocation(Location loc) {
        // todo null safety
        ApplicableRegionSet regions = container.get(loc.getWorld()).getApplicableRegions(loc);
        for (ProtectedRegion region : regions) {
            StateFlag.State flag = region.getFlag(DicePlugin.DICE_AVAILABLE_FLAG);
            if (flag == null) continue;
            if(flag.equals(StateFlag.State.ALLOW)) {
                return false;
            }
        }
        return true;
    }
}
