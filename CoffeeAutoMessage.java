package org.exampleelo.coffeeAutoMessage;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CoffeeAutoMessage extends JavaPlugin {

    private final Map<String, BukkitTask> tasks = new HashMap<>();
    private final Map<String, Integer> indices = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadAllMessageTasks();
        getLogger().info("CoffeeMessage został włączony!");
    }

    @Override
    public void onDisable() {
        stopAllTasks();
        getLogger().info("CoffeeMessage został wyłączony!");
    }

    private void stopAllTasks() {
        for (BukkitTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        indices.clear();
    }

    private void loadAllMessageTasks() {
        stopAllTasks();
        FileConfiguration config = getConfig();

        ConfigurationSection zestawy = config.getConfigurationSection("zestawy");
        if (zestawy == null) {
            getLogger().warning("Brak sekcji 'zestawy' w config.yml!");
            return;
        }

        for (String nazwaZestawu : zestawy.getKeys(false)) {
            ConfigurationSection sekcja = zestawy.getConfigurationSection(nazwaZestawu);
            if (sekcja == null) continue;

            int czas = sekcja.getInt("czas", 60);
            List<String> wiadomosci = sekcja.getStringList("wiadomosci");

            if (wiadomosci == null || wiadomosci.isEmpty()) {
                getLogger().warning("Zestaw '" + nazwaZestawu + "' nie ma żadnych wiadomości!");
                continue;
            }

            indices.put(nazwaZestawu, 0);
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, () -> {
                int index = indices.get(nazwaZestawu);
                String wiadomosc = wiadomosci.get(index).replace("&", "§");
                Bukkit.broadcastMessage(wiadomosc);
                indices.put(nazwaZestawu, (index + 1) % wiadomosci.size());
            }, 0L, czas * 20L);

            tasks.put(nazwaZestawu, task);
            getLogger().info("Uruchomiono zestaw '" + nazwaZestawu + "' co " + czas + " sek.");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("coffeemessage")) return false;
        if (!sender.hasPermission("coffee.admin")) {
            sender.sendMessage("§cNie masz uprawnień do tej komendy.");
            return true;
        }

        if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "reload":
                    reloadConfig();
                    loadAllMessageTasks();
                    sender.sendMessage("§a[CoffeeMessage] §fKonfiguracja przeładowana.");
                    return true;
                case "stop":
                    stopAllTasks();
                    sender.sendMessage("§c[CoffeeMessage] §fWszystkie wiadomości zostały wyłączone.");
                    return true;
                case "start":
                    loadAllMessageTasks();
                    sender.sendMessage("§a[CoffeeMessage] §fWszystkie wiadomości zostały włączone.");
                    return true;
            }
        }

        sender.sendMessage("§6Użycie: §f/coffeemessage <reload|start|stop>");
        return true;
    }
}
