package yourpackage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

public class TabManager implements TabCompleter {
    private final FileConfiguration config;
    private final JavaPlugin plugin;
    private final Map<String, List<String>> tabPrefixes = new HashMap<>();

    // Список приоритетов групп (чем меньше индекс — тем выше приоритет)
    private static final List<String> GROUP_PRIORITY = java.util.Arrays.asList("Owner", "Moderator", "VIP", "Default");

    public TabManager(FileConfiguration config, JavaPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
        loadTabPrefixes();
    }

    public void loadTabPrefixes() {
        tabPrefixes.clear();
        if (plugin.getConfig().isConfigurationSection("tab-prefixes")) {
            for (String group : plugin.getConfig().getConfigurationSection("tab-prefixes").getKeys(false)) {
                tabPrefixes.put(group, plugin.getConfig().getStringList("tab-prefixes." + group));
            }
        }
    }

    public boolean addGroup(CommandSender sender, String group) {
        if (plugin.getConfig().isSet("tab-prefixes." + group)) {
            sender.sendMessage("§eGroup already exists.");
            return false;
        }
        plugin.getConfig().set("tab-prefixes." + group, new java.util.ArrayList<String>());
        plugin.saveConfig();
        loadTabPrefixes();
        sender.sendMessage("§aGroup '" + group + "' created.");
        return true;
    }

    // Получить текущую группу игрока
    public String getPlayerGroup(String playerName) {
        for (String group : GROUP_PRIORITY) {
            List<String> players = tabPrefixes.get(group);
            if (players != null && players.contains(playerName)) {
                return group;
            }
        }
        return "Default";
    }

    // Проверка приоритета группы
    private boolean isLowerPriority(String newGroup, String currentGroup) {
        return GROUP_PRIORITY.indexOf(newGroup) > GROUP_PRIORITY.indexOf(currentGroup);
    }

    // Изменённый setPrefix
    public boolean setPrefix(CommandSender sender, String group, String playerName) {
        if (!tabPrefixes.containsKey(group)) {
            sender.sendMessage("§cGroup not found!");
            return false;
        }
        String currentGroup = getPlayerGroup(playerName);
        if (!currentGroup.equals("Default") && isLowerPriority(group, currentGroup)) {
            sender.sendMessage("§cPlayer already has a higher group (" + currentGroup + "). Use /removeprefix first!");
            return true;
        }
        // Удаляем из всех групп
        for (List<String> players : tabPrefixes.values()) {
            players.remove(playerName);
        }
        // Добавляем в новую группу
        tabPrefixes.get(group).add(playerName);
        for (String g : tabPrefixes.keySet()) {
            plugin.getConfig().set("tab-prefixes." + g, tabPrefixes.get(g));
        }
        plugin.saveConfig();
        loadTabPrefixes();
        sender.sendMessage("§aAdded " + playerName + " to group " + group + ".");
        return true;
    }

    // Изменённый removePrefix
    public boolean removePrefix(CommandSender sender, String playerName) {
        boolean removed = false;
        for (List<String> players : tabPrefixes.values()) {
            if (players.remove(playerName)) {
                removed = true;
            }
        }
        // Добавляем в Default
        if (!tabPrefixes.get("Default").contains(playerName)) {
            tabPrefixes.get("Default").add(playerName);
        }
        for (String g : tabPrefixes.keySet()) {
            plugin.getConfig().set("tab-prefixes." + g, tabPrefixes.get(g));
        }
        plugin.saveConfig();
        loadTabPrefixes();
        if (removed) {
            sender.sendMessage("§aRemoved " + playerName + " from all groups and set to Default.");
        } else {
            sender.sendMessage("§ePlayer not found in any group. Set to Default.");
        }
        return true;
    }

    public void setPlayerPrefix(Player player) {
        String group = getPlayerGroup(player.getName());
        String prefix = getPrefixColor(group) + "[" + group + "] §f";
        player.setPlayerListName(prefix + player.getName());
    }

    private String getPrefixColor(String group) {
        switch (group.toLowerCase()) {
            case "owner": return "§c";
            case "moderator": return "§9";
            case "vip": return "§a";
            default: return "§7";
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("addprefix")) {
            if (args.length == 1) {
                return addGroup(sender, args[0]);
            } else {
                sender.sendMessage("§cUsage: /addprefix <Group>");
                return true;
            }
        }
        if (command.getName().equalsIgnoreCase("setprefix")) {
            if (args.length == 2) {
                return setPrefix(sender, args[0], args[1]);
            } else {
                sender.sendMessage("§cUsage: /setprefix <Group> <Player>");
                return true;
            }
        }
        if (command.getName().equalsIgnoreCase("removeprefix")) {
            if (args.length == 1) {
                return removePrefix(sender, args[0]);
            } else {
                sender.sendMessage("§cUsage: /removeprefix <Player>");
                return true;
            }
        }
        return false;
    }

    public void updateTabForAll() {
        File tabConfigFile = new File(plugin.getDataFolder(), "tabconfig.yml");
        YamlConfiguration tabConfig = YamlConfiguration.loadConfiguration(tabConfigFile);

        String header = tabConfig.getString("tablist.header", "");
        String footer = tabConfig.getString("tablist.footer", "");
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        String tps = String.format("%.2f", Bukkit.getServer().getTPS()[0]);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        String timeKyiv = LocalDateTime.now(ZoneId.of("Europe/Kiev")).format(fmt);
        String timeCST = LocalDateTime.now(ZoneId.of("America/Chicago")).format(fmt);

        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerHeader = header
                .replace("{online}", String.valueOf(online))
                .replace("{max}", String.valueOf(max))
                .replace("{tps}", tps)
                .replace("{ping}", String.valueOf(player.getPing()))
                .replace("{time_kyiv}", timeKyiv)
                .replace("{time_cst}", timeCST);
            String playerFooter = footer
                .replace("{online}", String.valueOf(online))
                .replace("{max}", String.valueOf(max))
                .replace("{tps}", tps)
                .replace("{ping}", String.valueOf(player.getPing()))
                .replace("{time_kyiv}", timeKyiv)
                .replace("{time_cst}", timeCST);
            player.setPlayerListHeaderFooter(playerHeader, playerFooter);
        }
    }

    public boolean isInGroup(String playerName, String group) {
        List<String> players = tabPrefixes.get(group);
        return players != null && players.contains(playerName);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("setprefix")) {
            if (args.length == 1) {
                // Подсказка групп для первого аргумента
                return new ArrayList<>(tabPrefixes.keySet());
            }
        }
        if (command.getName().equalsIgnoreCase("addprefix")) {
            // Нет подсказок, можно вернуть пустой список
            return Collections.emptyList();
        }
        if (command.getName().equalsIgnoreCase("removeprefix")) {
            if (args.length == 1) {
                // Подсказка всех игроков онлайн
                List<String> names = new ArrayList<>();
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    names.add(p.getName());
                }
                return names;
            }
        }
        return Collections.emptyList();
    }
}