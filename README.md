# DiscordStatusPlugin

A Minecraft Paper plugin for automatic Discord notifications and advanced tablist prefix management.

---

## Features

- **Discord Webhook Integration**  
  Sends beautiful embed messages to your Discord channel when the server starts, stops, or when SOS mode is toggled.

- **SOS Mode**  
  Temporarily blocks player logins unless they are an admin, owner, or moderator. Useful for maintenance or emergencies.

- **Custom Tab Prefixes**  
  Easily manage player groups and display colored prefixes in the tablist for Owners, Moderators, VIPs, and Default players.

- **Tablist Customization**  
  Dynamic tablist header and footer with placeholders for TPS, ping, online count, and time.

- **In-game Commands**  
  - `/sos on|off` — Enable or disable SOS mode (block join for regular players)
  - `/setprefix <Player> <Group>` — Assign a player to a tab prefix group
  - `/addprefix <Group>` — Create a new tab prefix group
  - `/removeprefix <Player>` — Remove a player's tab prefix group
  - `/setdiscord <link>` — Change the Discord invite link (requires permission)
  - `/helpplugin` — Show all plugin commands and their descriptions

---

## How it works

- **Server Online/Offline**  
  When the server starts or stops, an embed message is sent to your Discord channel.

- **SOS Mode**  
  When enabled, only staff (Owner/Moderator) can join. Others are shown a custom kick message with your Discord link.

- **Tab Prefixes**  
  Manage groups and assign players to them. Prefixes are shown in the tablist and can be customized in `tabconfig.yml`.

- **Discord Webhook**  
  All status messages are sent as Discord embeds with custom colors and formatting.

---

## Example Discord Embeds

- **Server Online:**  
  Blue border, bold title: `Server online now`
- **Server Offline:**  
  Blue border, bold title: `Server offline now`
- **SOS Enabled:**  
  Red border, title: `You can join the server`, description:  
  ```
  IP: OcrasMinecraft.aternos.me
  Version: 1.21.5
  ```
- **SOS Disabled:**  
  Red border, title: `You can't join the server`, description:  
  ```
  This means that the Administration or moderation is not online, or technical work is taking place.
  We apologize

  Sincerely, Server Administration/Moderation
  ```
- **Default:**  
  Gray border, title: `The server is working fine.`

---

## Configuration

- **config.yml**  
  Stores tab prefix groups and player assignments.

- **tabconfig.yml**  
  Customize tablist header/footer, group prefix colors, and more.

- **plugin.yml**  
  Command registration and permissions.

---

## Permissions

- `discordstatus.setdiscord` — Required to use `/setdiscord`

---

## Requirements

- PaperMC 1.21 or newer
- Java 17+

---

## Installation

1. Place the plugin JAR into your server's `plugins` folder.
2. Configure your Discord webhook in the source or config if needed.
3. Restart the server.
4. Use `/helpplugin` in-game for command help.

---

**DiscordStatusPlugin** helps you keep your community informed and your staff organized, all from within Minecraft!