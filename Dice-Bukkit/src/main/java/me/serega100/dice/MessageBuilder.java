package me.serega100.dice;

import org.bukkit.entity.Player;

public class MessageBuilder {
    private String text;

    public MessageBuilder(Message msg) {
        this.text = msg.toString();
    }

    public MessageBuilder setPlayer(Player p) {
        text = text.replace("%player%", p.getName());
        return this;
    }

    public MessageBuilder setBet(double bet) {
        text = text.replace("%bet%", String.valueOf(bet));
        return this;
    }

    public MessageBuilder setNumber(int number) {
        text = text.replace("%number%", String.valueOf(number));
        return this;
    }

    public MessageBuilder send(Player p) {
        p.sendMessage(text);
        return this;
    }
}
