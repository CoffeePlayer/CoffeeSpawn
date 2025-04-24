package org.exampleelo.coffeeSpawn;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class CoffeeSpawn extends JavaPlugin implements Listener {

    private final HashMap<UUID, Location> teleportingPlayers = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SpawnPlugin został włączony.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SpawnPlugin został wyłączony.");
        teleportingPlayers.clear();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Ta komenda jest dostępna tylko dla graczy.");
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration config = getConfig();

        if (command.getName().equalsIgnoreCase("spawn")) {
            int delaySeconds = config.getInt("ustawienia.opoznienie", 5);
            String titleTemplate = config.getString("wiadomosci.title", "&aTeleportacja za &f%time% &asekund...");
            String teleportedMsg = config.getString("wiadomosci.teleportacja", "&aTeleportowano na spawn!");

            Location spawnLocation = getSpawnLocation();
            if (spawnLocation == null) {
                player.sendMessage("§cSpawn nie jest ustawiony!");
                return true;
            }

            Location initialLocation = player.getLocation().clone();
            teleportingPlayers.put(player.getUniqueId(), initialLocation);

            new BukkitRunnable() {
                int secondsLeft = delaySeconds;

                @Override
                public void run() {
                    if (!teleportingPlayers.containsKey(player.getUniqueId())) {
                        cancel();
                        return;
                    }

                    Location currentLoc = player.getLocation();
                    Location startLoc = teleportingPlayers.get(player.getUniqueId());

                    if (currentLoc.getX() != startLoc.getX()
                            || currentLoc.getY() != startLoc.getY()
                            || currentLoc.getZ() != startLoc.getZ()) {
                        teleportingPlayers.remove(player.getUniqueId());
                        player.sendMessage("§cTeleportacja została przerwana, ponieważ się poruszyłeś.");
                        cancel();
                        return;
                    }

                    if (secondsLeft <= 0) {
                        teleportingPlayers.remove(player.getUniqueId());
                        player.teleport(spawnLocation);
                        player.sendMessage(koloruj(teleportedMsg));
                        player.sendTitle("§aTeleportacja!", "", 10, 40, 10);
                        cancel();
                        return;
                    }

                    String title = koloruj(titleTemplate.replace("%time%", String.valueOf(secondsLeft)));
                    player.sendTitle(title, "", 0, 25, 5);
                    secondsLeft--;
                }
            }.runTaskTimer(this, 0L, 20L); // Co sekundę

            return true;
        }

        if (command.getName().equalsIgnoreCase("setspawn")) {
            if (!player.hasPermission("Spawnustaw")) {
                player.sendMessage("§cNie masz permisji, aby ustawić spawn!");
                return true;
            }

            Location loc = player.getLocation();
            config.set("spawn.world", loc.getWorld().getName());
            config.set("spawn.x", loc.getX());
            config.set("spawn.y", loc.getY());
            config.set("spawn.z", loc.getZ());
            config.set("spawn.yaw", loc.getYaw());
            config.set("spawn.pitch", loc.getPitch());
            saveConfig();

            String msg = config.getString("wiadomosci.ustawiono", "&aSpawn został ustawiony!");
            player.sendMessage(koloruj(msg));
            return true;
        }

        return false;
    }

    private Location getSpawnLocation() {
        FileConfiguration config = getConfig();
        if (!config.contains("spawn.world")) return null;

        World world = Bukkit.getWorld(config.getString("spawn.world"));
        if (world == null) return null;

        double x = config.getDouble("spawn.x");
        double y = config.getDouble("spawn.y");
        double z = config.getDouble("spawn.z");
        float yaw = (float) config.getDouble("spawn.yaw");
        float pitch = (float) config.getDouble("spawn.pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    private String koloruj(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!teleportingPlayers.containsKey(uuid)) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            teleportingPlayers.remove(uuid);
            event.getPlayer().sendMessage("§cTeleportacja została przerwana, ponieważ się poruszyłeś.");
        }
    }
}
