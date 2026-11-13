package com.plutus;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class MaceListenerV2 implements Listener {

    private final PlutusMacePluginV2 plugin;
    private final AbilityManagerV2 abilityManager;
    private boolean isInternalDamage = false;

    public MaceListenerV2(PlutusMacePluginV2 plugin, AbilityManagerV2 abilityManager) {
        this.plugin = plugin;
        this.abilityManager = abilityManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                abilityManager.sendPack(event.getPlayer());
            }
        }.runTaskLater(plugin, 40L);
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (plugin.isPlutusMace(item)) {
            event.setCancelled(true);
            abilityManager.handleAbility(player, player.isSneaking());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;

        ItemStack item = attacker.getInventory().getItemInMainHand();
        if (plugin.isPlutusMace(item)) {
            abilityManager.handleBloodToll(attacker);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVictimDamage(EntityDamageEvent event) {
        if (isInternalDamage) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        isInternalDamage = true;
        try {
            abilityManager.handleDamageSharing(victim, event.getFinalDamage());
        } finally {
            isInternalDamage = false;
        }
    }
}
