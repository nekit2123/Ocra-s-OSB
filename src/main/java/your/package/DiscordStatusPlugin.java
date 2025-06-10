package yourpackage;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class DiscordStatusPlugin extends JavaPlugin implements Listener {

    private boolean sosEnabled = false;
    private String discordLink;
    private TabManager tabManager;
    private DiscordNotifier discordNotifier;

    // ОСТАВИТЬ ТОЛЬКО ЭТО:
    private static final String DISCORD_WEBHOOK = "https://discord.com/api/webhooks/1381522946543321199/W4tAN14QkuEe4b2O978-4y84nodZCBi8NukrAY9QB2aFSafLfLiooaGTwjUgU_2rcfEV";
    private static final Map<String, String> WEBHOOKS = new HashMap<>();
    static {
        WEBHOOKS.put("online", DISCORD_WEBHOOK);
        WEBHOOKS.put("offline", DISCORD_WEBHOOK);
        WEBHOOKS.put("sos_enabled", DISCORD_WEBHOOK);
        WEBHOOKS.put("sos_disabled", DISCORD_WEBHOOK);
        WEBHOOKS.put("default", DISCORD_WEBHOOK);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("tabconfig.yml", false);
        tabManager = new TabManager(getConfig(), this);
        Bukkit.getPluginManager().registerEvents(this, this);

        new BukkitRunnable() {
            @Override
            public void run() {
                tabManager.updateTabForAll();
            }
        }.runTaskTimer(this, 20, 100);

        discordLink = getConfig().getString("discord-link", "https://discord.gg/YHSYShaM3A");
        sendDiscord("online");
        getLogger().info("DiscordStatusPlugin enabled.");

        discordNotifier = new DiscordNotifier(this, WEBHOOKS);

        this.getCommand("setprefix").setTabCompleter(tabManager);
        this.getCommand("addprefix").setTabCompleter(tabManager);
        this.getCommand("removeprefix").setTabCompleter(tabManager);
    }

    @Override
    public void onDisable() {
        // Используем отдельный поток, чтобы не было ошибки Bukkit Scheduler
        new Thread(() -> sendDiscordSync("offline")).start();
        getLogger().info("DiscordStatusPlugin disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // SOS command
        if (command.getName().equalsIgnoreCase("sos")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("on")) {
                    sosEnabled = true;
                    sendDiscord("sos_enabled");
                    sender.sendMessage("§cSOS mode enabled! Joining the server is now blocked.");
                } else if (args[0].equalsIgnoreCase("off")) {
                    sosEnabled = false;
                    sendDiscord("sos_disabled");
                    sender.sendMessage("§aSOS mode disabled! Joining the server is now allowed.");
                } else {
                    sender.sendMessage("Usage: /sos on|off");
                }
            } else {
                sender.sendMessage("Usage: /sos on|off");
            }
            return true;
        }
        // Tab-related commands
        if (command.getName().equalsIgnoreCase("addprefix") 
            || command.getName().equalsIgnoreCase("setprefix")
            || command.getName().equalsIgnoreCase("removeprefix")) {
            return tabManager.onCommand(sender, command, label, args);
        }
        if (command.getName().equalsIgnoreCase("helpplugin")) {
            sender.sendMessage("§6--- DiscordStatusPlugin Commands ---");
            sender.sendMessage("§e/sos on|off §7- Enable or disable SOS mode (block join)");
            sender.sendMessage("§e/setprefix <Player> <Group> §7- Set a player's tab prefix group");
            sender.sendMessage("§e/addprefix <Group> §7- Create a new tab prefix group");
            sender.sendMessage("§e/removeprefix <Player> §7- Remove a player's tab prefix group");
            sender.sendMessage("§e/setdiscord <link> §7- Change Discord invite link");
            sender.sendMessage("§6--- Команди DiscordStatusPlugin ---");
            sender.sendMessage("§e/sos on|off §7- Увімкнути або вимкнути SOS режим (блокування входу)");
            sender.sendMessage("§e/setprefix <Гравець> <Група> §7- Встановити групу таб-префіксу для гравця");
            sender.sendMessage("§e/addprefix <Група> §7- Створити нову групу таб-префіксу");
            sender.sendMessage("§e/removeprefix <Гравець> §7- Видалити групу таб-префіксу у гравця");
            sender.sendMessage("§e/setdiscord <посилання> §7- Змінити посилання на Discord");
            return true;
        }
        if (command.getName().equalsIgnoreCase("setdiscord")) {
            if (!sender.hasPermission("discordstatus.setdiscord")) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage("§eUsage: /setdiscord <link>");
                return true;
            }
            String newLink = args[0];
            getConfig().set("discord-link", newLink);
            saveConfig();
            discordLink = newLink;
            sender.sendMessage("§aDiscord link updated to: " + newLink);
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (sosEnabled) {
            Player player = event.getPlayer();
            boolean isOp = player.isOp();
            boolean isAllowed = isOp;

            // Проверка на Owner или Moderator через TabManager
            if (!isAllowed && tabManager != null) {
                String name = player.getName();
                isAllowed = tabManager.isInGroup(name, "Owner") || tabManager.isInGroup(name, "Moderator");
            }

            if (!isAllowed) {
                String message = "§cThere are no administrators or moderators online, or technical maintenance is in progress.\n"
                        + "§7Please wait and try again later.\n"
                        + "§bYou can contact us here: §9§n" + discordLink;
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, message);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        tabManager.setPlayerPrefix(event.getPlayer());
        tabManager.updateTabForAll();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        tabManager.updateTabForAll();
    }

    private void sendDiscord(String state) {
        String webhookUrl = WEBHOOKS.getOrDefault(state, WEBHOOKS.get("default"));
        String jsonPayload = buildEmbedPayload(state);

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(jsonPayload.getBytes());
                }
                connection.getInputStream().close();
            } catch (Exception e) {
                getLogger().warning("Failed to send message to Discord: " + e.getMessage());
            }
        });
    }

    private void sendDiscordSync(String state) {
        String webhookUrl = WEBHOOKS.getOrDefault(state, WEBHOOKS.get("default"));
        String jsonPayload = buildEmbedPayload(state);

        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes());
            }
            connection.getInputStream().close();
        } catch (Exception e) {
            getLogger().warning("Failed to send message to Discord: " + e.getMessage());
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String buildEmbedPayload(String state) {
        String title = "";
        String description = "";
        int color = 0x0002ff; // default blue

        switch (state) {
            case "online":
                title = "**Server online now**";
                color = 0x0002ff;
                break;
            case "offline":
                title = "**Server offline now**";
                color = 0x0002ff;
                break;
            case "sos_enabled":
                title = "**You can join the server**";
                description = "IP: OcrasMinecraft.aternos.me\nVersion: 1.21.5";
                color = 0xf30303;
                break;
            case "sos_disabled":
                title = "__**You can't join the server**__";
                description = "**This means that the Administration or moderation is not online, or technical work is taking place.\nWe apologize**\n\nSincerely, Server Administration/Moderation";
                color = 0xf30303;
                break;
            case "default":
            default:
                title = "The server is working fine.";
                color = 0xbababa;
                break;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"embeds\":[{\"title\":\"").append(escapeJson(title)).append("\"");
        if (!description.isEmpty()) {
            sb.append(",\"description\":\"").append(escapeJson(description)).append("\"");
        }
        sb.append(",\"color\":").append(color).append("}]}");
        return sb.toString();
    }
}