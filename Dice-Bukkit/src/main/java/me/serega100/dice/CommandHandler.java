package me.serega100.dice;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.Locales;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import me.serega100.dice.game.GameManager;
import org.bukkit.entity.Player;

public class CommandHandler extends BaseCommand { // todo multilang acf and syntax
    private final GameManager manager;

    CommandHandler(BukkitCommandManager cmdManager, GameManager gameManager) {
        this.manager = gameManager;
        cmdManager.getLocales().setDefaultLocale(Locales.RUSSIAN);
    }

    @CommandAlias("dice")
    @CommandPermission("dice.play")
    @Syntax("<противник> <ставка> - предложить сыграть в кости")
    @CommandCompletion("@players:30 @nothing")
    public void dice(Player p, OnlinePlayer enemy, int bet) {
        try {
            manager.createGame(p, enemy.getPlayer(), bet);
        } catch (DiceException e) {
            e.notifyPlayer();
        }
    }

    @CommandAlias("diceyes|dyes")
    @CommandPermission("dice.play")
    @Syntax(" - принять запрос на игру в кости")
    public void diceYes(Player p) {
        try {
            manager.gameAgree(p);
        } catch (DiceException e) {
            e.notifyPlayer();
        }
    }

    @CommandAlias("diceno|dno")
    @CommandPermission("dice.play")
    @Syntax(" - отклонить запрос на игру в кости")
    public void diceNo(Player p) {
        try {
            manager.gameDisagree(p);
        } catch (DiceException e) {
            e.notifyPlayer();
        }
    }

    @CommandAlias("diceignore|dignore")
    @CommandPermission("dice.ignore")
    @Syntax(" - вкл/выкл игнорирование запросов на игру в кости")
    public void diceIgnore(Player p) {
        manager.switchIgnore(p);
    }
}
