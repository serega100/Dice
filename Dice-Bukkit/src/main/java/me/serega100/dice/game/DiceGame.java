package me.serega100.dice.game;

import me.serega100.dice.DicePlayer;
import org.bukkit.block.Block;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DiceGame {
    public enum Status {REQUESTING, STARTED, FINISHED}

    private final DicePlayer player;
    private final DicePlayer enemy;
    private final int bet;
    private int result;
    private Status status;
    private Block block;

    private final Lock locker = new ReentrantLock();

    DiceGame(DicePlayer player, DicePlayer enemy, int bet) {
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

    public DicePlayer getEnemy(DicePlayer player) {
        if (this.player.equals(player)) {
            return enemy;
        } else {
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
