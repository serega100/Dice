package me.serega100.dice.message;

import me.serega100.dice.DicePlayer;
import org.bukkit.entity.Player;

public class MessageBuilder {
    private String text;

    public MessageBuilder(Message msg) {
        this.text = msg.toString();
    }

    public MessageBuilder setPlayer(DicePlayer dPlayer) {
        text = text.replace("%player%", dPlayer.getPlayer().getName());
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

    @Deprecated
    public MessageBuilder send(Player p) {
        p.sendMessage(text);
        return this;
    }

    public MessageBuilder send(DicePlayer dPlayer) {
        dPlayer.getPlayer().sendMessage(text);
        return this;
    }
}
