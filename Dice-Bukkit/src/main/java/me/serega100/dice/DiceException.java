package me.serega100.dice;

import me.serega100.dice.message.Message;
import me.serega100.dice.message.MessageBuilder;
import org.bukkit.entity.Player;

public class DiceException extends Exception {
    private final DicePlayer player;
    private final Message message;

    public DiceException(Player player, Message message) {
        this(DicePlayer.getDicePlayer(player), message);
    }

    public DiceException(DicePlayer player, Message message) {
        super(String.format("The exception was occurred by %s with message %s", player.getPlayer().getName(), message.name()));
        this.player = player;
        this.message = message;
    }

    public void notifyPlayer() {
        new MessageBuilder(message).send(player);
    }

    public Message getDiceMessage() {
        return message;
    }

    public DicePlayer getPlayer() {
        return player;
    }
}