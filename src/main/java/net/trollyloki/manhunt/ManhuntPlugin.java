package net.trollyloki.manhunt;

import net.trollyloki.manhunt.compass.CompassListener;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ManhuntPlugin extends JavaPlugin {

    private static ManhuntPlugin instance;

    private File resetFile;

    private File dataFile;
    private YamlConfiguration data;

    private ManhuntListener listener;
    private ReviveGUI reviveGUI;

    private CompassListener compassListener;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        resetFile = new File(getDataFolder(), "reset");

        List<World> worlds = getServer().getWorlds();
        for (int i = 1; i < worlds.size(); i++) {
            worlds.get(i).setKeepSpawnInMemory(false);
        }

        dataFile = new File(getDataFolder(), "data.json");
        if (!dataFile.exists()) {
            try {
                dataFile.mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        this.listener = new ManhuntListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        this.reviveGUI = new ReviveGUI(this);
        getServer().getPluginManager().registerEvents(reviveGUI, this);

        this.compassListener = new CompassListener(this);
        getServer().getPluginManager().registerEvents(compassListener, this);

        getCommand("manhunt").setExecutor(new ManhuntCommand(this));

    }

    @Override
    public void onDisable() {
        instance = null;

        try {
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ManhuntPlugin getInstance() {
        return instance;
    }

    /**
     * Restarts the server and generates a new world
     *
     * @return {@code true} if the reset request was successful
     */
    public boolean resetWorld() {
        try {
            resetFile.createNewFile();
            getServer().getScheduler().runTaskLater(this, () -> getServer().spigot().restart(), 20);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets a string from the config, translating color codes
     *
     * @param path Path
     * @return String
     */
    public String getConfigString(String path) {
        String string = getConfig().getString(path);
        if (string != null)
            return ChatColor.translateAlternateColorCodes('&', string);
        return null;
    }

    /**
     * Gets the data configuration
     *
     * @return Data configuration
     */
    public Configuration getData() {
        return data;
    }

    /**
     * Gets the manhunt listener for this plugin
     *
     * @return Manhunt listener
     */
    public ManhuntListener getListener() {
        return listener;
    }

    /**
     * Gets the revive GUI listener for this plugin
     *
     * @return Revive GUI
     */
    public ReviveGUI getReviveGUI() {
        return reviveGUI;
    }

}
