package com.plutus;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlutusMacePluginV2 extends JavaPlugin {

    private static PlutusMacePluginV2 instance;
    private NamespacedKey maceKey;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private AbilityManagerV2 abilityManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        maceKey = new NamespacedKey(this, "plutus_mace_v2");

        abilityManager = new AbilityManagerV2(this);

        Objects.requireNonNull(getCommand("giveplutus")).setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (!player.hasPermission("plutus.admin")) {
                player.sendMessage(mm.deserialize("<red>No permission."));
                return true;
            }

            player.getInventory().addItem(createMace());
            player.sendMessage(mm.deserialize(mm.serialize(mm.deserialize("<gradient:#8B0000:#FF0000><bold>Plutus Mace</bold></gradient>")) + " <gray>given."));
            return true;
        });

        getServer().getPluginManager().registerEvents(new MaceListenerV2(this, abilityManager), this);
        getLogger().info("PlutusMaceV2 enabled successfully!");
    }

    public ItemStack createMace() {
        FileConfiguration config = getConfig();
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();

        if (meta != null) {
            meta.displayName(mm.deserialize("<gradient:#8B0000:#FF0000><bold>Plutus Mace</bold></gradient>"));

            List<Component> loreLines = new ArrayList<>();
            loreLines.add(mm.deserialize("<gray>A weapon forged from blood and crimson flame."));
            loreLines.add(mm.deserialize("<dark_red>⚡ Passive: Blood Toll"));
            loreLines.add(mm.deserialize("<red>⚔ Ability 1: Blood Dash <gray>(Press F)"));
            loreLines.add(mm.deserialize("<dark_purple>⛓ Ability 2: Blood Pact <gray>(Shift + F)"));
            meta.lore(loreLines);

            meta.setCustomModelData(70001);
            meta.getPersistentDataContainer().set(maceKey, PersistentDataType.BYTE, (byte) 1);
            mace.setItemMeta(meta);
        }
        return mace;
    }

    public boolean isPlutusMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(maceKey, PersistentDataType.BYTE);
    }

    public static PlutusMacePluginV2 getInstance() {
        return instance;
    }
}
