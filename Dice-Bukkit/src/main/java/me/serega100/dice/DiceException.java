package me.serega100.dice;

import org.bukkit.entity.Player;

public class DiceException extends Exception {
    private final Player player;
    private final Message message;

    public DiceException(Player player, Message message) {
        super(String.format("The exception was occurred by %s with message %s", player.getName(), message.name()));
        this.player = player;
        this.message = message;
    }

    public void notifyPlayer() {
        new MessageBuilder(message).send(player);
    }

    public Message getDiceMessage() {
        return message;
    }

    public Player getPlayer() {
        return player;
    }
}