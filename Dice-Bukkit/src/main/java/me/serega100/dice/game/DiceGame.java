package me.serega100.dice.game;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DiceGame {
    public enum Status {REQUESTING, STARTED, FINISHED}

    private final Player player;
    private final Player enemy;
    private final int bet;
    private int result;
    private Status status;
    private Block block;

    private final Lock locker = new ReentrantLock();

    DiceGame(Player player, Player enemy, int bet) {
        this.player = player;
        this.enemy = enemy;
        this.bet = bet;
        this.status = Status.REQUESTING;
    }

    void start() {
        this.status = Status.STARTED;
    }

    void finish(Block block, int result) {
        this.status = Status.FINISHED;
        this.block = block;
        this.result = result;
    }

    public Player getEnemy(Player p) {
        if(player == p) {
            return enemy;
        }else{
            return player;
        }
    }

    public int getBet() {
        return bet;
    }

    public int getResult() {
        return result;
    }

    public Status getStatus() {
        return status;
    }

    public Lock getLocker() {
        return locker;
    }

    Block getBlock() {
        return block;
    }
}
