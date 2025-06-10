package yourpackage;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordNotifier {
    private final JavaPlugin plugin;
    private final Map<String, String> webhooks;

    public DiscordNotifier(JavaPlugin plugin, Map<String, String> webhooks) {
        this.plugin = plugin;
        this.webhooks = webhooks;
    }

    public void sendDiscord(String content, String state) {
        String webhookUrl = webhooks.getOrDefault(state, webhooks.get("default"));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String jsonPayload = "{\"content\":\"" + content + "\"}";
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(jsonPayload.getBytes());
                }
                connection.getInputStream().close();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send message to Discord: " + e.getMessage());
            }
        });
    }

    public void notifyServerOnline() {
        sendDiscord("Server is now ONLINE!", "online");
    }

    public void notifyServerOffline() {
        sendDiscord("Server is now OFFLINE!", "offline");
    }

    public void notifySosModeEnabled() {
        sendDiscord("SOS mode enabled. Server join is now blocked!", "sos_enabled");
    }

    public void notifySosModeDisabled() {
        sendDiscord("SOS mode disabled. Server join is now allowed!", "sos_disabled");
    }
}