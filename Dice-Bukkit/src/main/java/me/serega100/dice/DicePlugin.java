package me.serega100.dice;

import co.aikar.commands.BukkitCommandManager;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import me.serega100.dice.game.GameManager;
import me.serega100.dice.listener.BlockingListener;
import me.serega100.dice.listener.PlayerListener;
import me.serega100.dice.nms.HeadCreator;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

// todo test => first commit => move to gradle
// todo record video for YouTube
// todo public build on Google Drive
public final class DicePlugin extends JavaPlugin implements Listener {
    public final static StateFlag DICE_AVAILABLE_FLAG = new StateFlag("dice", false);
    private static DicePlugin instance;

    private GameManager gameManager;
    private BoneManager boneManager;
    private FileConfiguration messageConfig;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadMessageConfig(getConfig().getString("language", "en"));

        HeadCreator headCreator;
        try {
            headCreator = new HeadCreator(getServer());
            boneManager = new BoneManager(getConfig(), headCreator);
        } catch (HeadCreator.UnsupportedVersion e) {
            getLogger().severe("Your version of CraftBukkit is unsupported.");
            getLogger().severe("The plugin may work on 1.12-1.15.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupEconomy() ) {
            getLogger().severe("Disabled due to no Vault plugin found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().severe("Disabled due to no WorldGuard plugin found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        gameManager = new GameManager(this);
        PlayerListener playerListener = new PlayerListener(gameManager);
        getServer().getPluginManager().registerEvents(playerListener, this);
        BlockingListener blockingListener = new BlockingListener(gameManager);
        getServer().getPluginManager().registerEvents(blockingListener, this);
        BukkitCommandManager cmdManager = new BukkitCommandManager(this);
        cmdManager.registerCommand(new CommandHandler(cmdManager, gameManager));
    }

    @Override
    public void onLoad() {
        // Register WG flag
        FlagRegistry flagRegistry = WorldGuardPlugin.inst().getFlagRegistry();
        flagRegistry.register(DICE_AVAILABLE_FLAG);
    }

    @Override
    public void onDisable() {
        gameManager.shutdown();
    }

    public static DicePlugin getInstance() {
        return instance;
    }

    public BoneManager getBoneManager() {
        return boneManager;
    }

    private void loadMessageConfig(String lang) {
        InputStream messageConfigStream = this.getResource("messages_" + lang + ".yml");
        messageConfig = new YamlConfiguration();
        try {
            messageConfig.load(new InputStreamReader(messageConfigStream, StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getMessageConfig() {
        return messageConfig;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }
}
