package me.JackHaynie.Karismic;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.JackHaynie.Karismic.listeners.EggCatcherEntityListener;
import net.milkbowl.vault.economy.Economy;


public class EggCatcher extends JavaPlugin {
    public static Economy economy;

    public void onEnable() {
        CheckConfigurationFile();

        PluginManager pm = getServer().getPluginManager();

        EggCatcherEntityListener entityListener = new EggCatcherEntityListener(this);
        pm.registerEvents(entityListener, this);

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
            if (economyProvider != null) {
                economy = economyProvider.getProvider();
            }
        }

    }

    public void onDisable() {
        getLogger().info("KarismicEggCatcher has been disabled!");
    }

    public void CheckConfigurationFile() {
        double configVersion = getConfig().getDouble("ConfigVersion", 0.0D);
        if (configVersion == 4.0D) {
            saveConfig();
        } else {
            saveResource("config.yml", true);
            reloadConfig();
        }
    }
}
