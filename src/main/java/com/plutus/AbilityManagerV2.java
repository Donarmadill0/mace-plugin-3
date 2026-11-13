package com.plutus;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AbilityManagerV2 {

    private final PlutusMacePluginV2 plugin;
    private final Map<UUID, Long> dashCooldowns = new HashMap<>();
    private final Map<UUID, Long> pactCooldowns = new HashMap<>();
    private final Map<UUID, Set<UUID>> activePacts = new HashMap<>();
    private final Map<UUID, Integer> hitCounters = new HashMap<>();

    private byte[] packBytes;
    private String packHash;
    private String hostedPackUrl;

    public AbilityManagerV2(PlutusMacePluginV2 plugin) {
        this.plugin = plugin;
        startVisualTask();
        generateAndHostPack();
    }

    private void generateAndHostPack() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                        addFileToZip(zos, "pack.mcmeta", "{\"pack\":{\"description\":\"Plutus Mace V2 Assets\",\"pack_format\":37}}");
                        addResourceToZip(zos, "assets/minecraft/textures/item/plutus_mace.png");
                        addResourceToZip(zos, "assets/minecraft/models/item/plutus_mace.json");
                        addResourceToZip(zos, "assets/minecraft/models/item/mace.json");
                    }

                    packBytes = baos.toByteArray();
                    MessageDigest digest = MessageDigest.getInstance("SHA-1");
                    byte[] hash = digest.digest(packBytes);
                    StringBuilder sb = new StringBuilder();
                    for (byte b : hash) sb.append(String.format("%02x", b));
                    packHash = sb.toString();

                    hostedPackUrl = uploadToCatbox(packBytes);
                    if (hostedPackUrl != null && !hostedPackUrl.isEmpty()) {
                        plugin.getLogger().info("PlutusMaceV2 resource pack auto-hosted at: " + hostedPackUrl);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error in pack generation: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void addFileToZip(ZipOutputStream zos, String path, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private void addResourceToZip(ZipOutputStream zos, String path) throws IOException {
        InputStream is = plugin.getResource(path);
        if (is == null) return;
        zos.putNextEntry(new ZipEntry(path));
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) > 0) zos.write(buffer, 0, len);
        zos.closeEntry();
        is.close();
    }

    private String uploadToCatbox(byte[] data) {
        try {
            String boundary = "*****" + System.currentTimeMillis() + "*****";
            URL url = new URL("https://catbox.moe/user/api.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"reqtype\"\r\n\r\n");
                dos.writeBytes("fileupload\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"fileToUpload\"; filename=\"plutus_v2.zip\"\r\n");
                dos.writeBytes("Content-Type: application/zip\r\n\r\n");
                dos.write(data);
                dos.writeBytes("\r\n");

                dos.writeBytes("--" + boundary + "--\r\n");
            }

            if (conn.getResponseCode() == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) response.append(line);
                    return response.toString().trim();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public void sendPack(Player player) {
        if (hostedPackUrl == null || packHash == null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (hostedPackUrl != null && packHash != null) {
                        player.setResourcePack(hostedPackUrl, packHash, true, MiniMessage.miniMessage().deserialize("<red>Please accept the Plutus Mace textures!"));
                    }
                }
            }.runTaskLater(plugin, 40L);
            return;
        }
        player.setResourcePack(hostedPackUrl, packHash, true, MiniMessage.miniMessage().deserialize("<red>Please accept the Plutus Mace textures!"));
    }

    public void handleAbility(Player player, boolean isSneaking) {
        if (isSneaking) {
            triggerPact(player);
        } else {
            triggerDash(player);
        }
    }

    private void triggerDash(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        int cd = plugin.getConfig().getInt("dash.cooldown-seconds", 10) * 1000;

        if (dashCooldowns.getOrDefault(id, 0L) > now) {
            long remaining = (dashCooldowns.get(id) - now) / 1000;
            player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>Dash Cooldown: " + remaining + "s"));
            return;
        }

        dashCooldowns.put(id, now + cd);
        Vector dir = player.getLocation().getDirection();
        player.setVelocity(dir.multiply(1.8).setY(0.25));

        try {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 0.8f);
        } catch (Exception ignored) {}

        spawnRedBurst(player.getLocation());

        for (Entity e : player.getNearbyEntities(4.0, 4.0, 4.0)) {
            if (e instanceof Player && !e.equals(player)) {
                ((Player) e).damage(3.0, player);
            }
        }
    }

    private void triggerPact(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        int cd = plugin.getConfig().getInt("pact.cooldown-seconds", 25) * 1000;

        if (pactCooldowns.getOrDefault(id, 0L) > now) {
            long remaining = (pactCooldowns.get(id) - now) / 1000;
            player.sendActionBar(MiniMessage.miniMessage().deserialize("<dark_red>Pact Cooldown: " + remaining + "s"));
            return;
        }

        Set<UUID> targets = new HashSet<>();
        for (Entity e : player.getNearbyEntities(15.0, 15.0, 15.0)) {
            if (e instanceof Player && !e.equals(player) && targets.size() < 3) {
                targets.add(e.getUniqueId());
                try {
                    e.getWorld().playSound(e.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1f, 1f);
                } catch (Exception ignored) {}
            }
        }

        if (targets.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>No targets nearby to link!"));
            return;
        }

        pactCooldowns.put(id, now + cd);
        activePacts.put(id, targets);

        new BukkitRunnable() {
            @Override
            public void run() {
                activePacts.remove(id);
            }
        }.runTaskLater(plugin, 200L);
    }

    public void handleDamageSharing(Player victim, double damage) {
        UUID victimId = victim.getUniqueId();
        for (Map.Entry<UUID, Set<UUID>> entry : activePacts.entrySet()) {
            if (entry.getValue().contains(victimId)) {
                for (UUID otherId : entry.getValue()) {
                    if (otherId.equals(victimId)) continue;
                    Player other = plugin.getServer().getPlayer(otherId);
                    if (other != null && other.isOnline()) {
                        other.damage(damage);
                    }
                }
                break;
            }
        }
    }

    public void handleBloodToll(Player attacker) {
        attacker.damage(1.0);
        try {
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, 0.8f, 0.8f);
        } catch (Exception ignored) {}
    }

    private void startVisualTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, Set<UUID>> entry : activePacts.entrySet()) {
                    Player owner = plugin.getServer().getPlayer(entry.getKey());
                    if (owner == null || !owner.isOnline()) continue;

                    List<Player> targets = new ArrayList<>();
                    for (UUID tid : entry.getValue()) {
                        Player t = plugin.getServer().getPlayer(tid);
                        if (t != null && t.isOnline()) targets.add(t);
                    }

                    for (int i = 0; i < targets.size(); i++) {
                        for (int j = i + 1; j < targets.size(); j++) {
                            drawLink(targets.get(i), targets.get(j));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void drawLink(Player a, Player b) {
        Location start = a.getLocation().add(0, 1, 0);
        Location end = b.getLocation().add(0, 1, 0);
        Vector vec = end.toVector().subtract(start.toVector());
        double dist = vec.length();
        vec.normalize().multiply(0.5);

        for (double d = 0; d < dist; d += 0.5) {
            Location point = start.clone().add(vec.clone().multiply(d));
            if (point.getWorld() != null) {
                point.getWorld().spawnParticle(Particle.CRIMSON_SPORE, point, 1, 0, 0, 0, 0);
            }
        }
    }

    private void spawnRedBurst(Location loc) {
        if (loc.getWorld() != null) {
            loc.getWorld().spawnParticle(Particle.CRIMSON_SPORE, loc.clone().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        }
    }
}
