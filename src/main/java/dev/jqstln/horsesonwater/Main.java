package dev.jqstln.horsesonwater;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Main extends JavaPlugin implements Listener {

    private final Map<Location, Long> iceMap = new HashMap<>();
    private int durationMillis; // Duration in milliseconds (1000 milliseconds = 1 second)
    private boolean debug;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Creates config.yml with default values if it doesn't exist
        loadConfiguration();

        Bukkit.getPluginManager().registerEvents(this, this);

        // Start the repeating task to manage ice circles
        new BukkitRunnable() {
            @Override
            public void run() {
                removeExpiredIce();
            }
        }.runTaskTimer(this, 20, 20); // Check every 1 second (20 ticks)
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Entity vehicle = player.getVehicle();

        if (vehicle instanceof Horse || vehicle instanceof Mule || vehicle instanceof Donkey) {
            if (player.getInventory().getBoots() != null &&
                    player.getInventory().getBoots().containsEnchantment(Enchantment.FROST_WALKER)) {

                Location playerLocation = player.getLocation();
                Block blockBelowPlayer = playerLocation.subtract(0, 1, 0).getBlock();

                if (blockBelowPlayer.getType() == Material.WATER) {
                    if (isDebug()) {
                        getLogger().info("Player " + player.getName() + " is riding a mount and walking on water at " + playerLocation);
                    }
                    createIceCircle(playerLocation);
                }
            }
        }
    }

    private void createIceCircle(Location center) {
        int radius = 3;

        if (isDebug()) {
            getLogger().info("Creating ice circle at " + center.toString());
        }

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Location location = center.clone().add(x, 0, z);
                if (center.distance(location) <= radius && location.getBlock().getType() == Material.WATER) {
                    location.getBlock().setType(Material.ICE);
                    iceMap.put(location, System.currentTimeMillis() + durationMillis);
                }
            }
        }
    }

    private void removeExpiredIce() {
        long currentTime = System.currentTimeMillis();

        // Use an iterator to avoid ConcurrentModificationException when removing elements from the map
        Iterator<Map.Entry<Location, Long>> iterator = iceMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Location, Long> entry = iterator.next();
            Location location = entry.getKey();
            long expirationTime = entry.getValue();

            if (currentTime > expirationTime) {
                location.getBlock().setType(Material.WATER);
                iterator.remove();
            }
        }
    }

    private void loadConfiguration() {
        // Load values from config.yml
        getConfig().options().copyDefaults(true);
        saveConfig();
        debug = getConfig().getBoolean("debug");
        durationMillis = getConfig().getInt("durationMillis", 2000); // Read the durationMillis value with a default of 2000 if not specified
    }

    public boolean isDebug() {
        return debug;
    }
}