package com.jazzkuh.gunshell.common.listeners;

import com.jazzkuh.gunshell.GunshellPlugin;
import com.jazzkuh.gunshell.api.events.MeleeDamageEvent;
import com.jazzkuh.gunshell.api.objects.GunshellMelee;
import com.jazzkuh.gunshell.common.MeleeActionRegistry;
import com.jazzkuh.gunshell.common.actions.melee.abstraction.MeleeActionImpl;
import com.jazzkuh.gunshell.common.configuration.DefaultConfig;
import com.jazzkuh.gunshell.common.configuration.lang.MessagesConfig;
import com.jazzkuh.gunshell.compatibility.CompatibilityManager;
import com.jazzkuh.gunshell.compatibility.extensions.griefprevention.GriefPreventionExtension;
import com.jazzkuh.gunshell.compatibility.extensions.worldguard.WorldGuardExtension;
import com.jazzkuh.gunshell.utils.ChatUtils;
import com.jazzkuh.gunshell.utils.NBTEditor;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class EntityDamageByEntityListener implements Listener {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        ItemStack itemStack = player.getInventory().getItemInMainHand();

        if (!NBTEditor.contains(itemStack, "gunshell_melee_key")) return;

        CompatibilityManager compatibilityManager = GunshellPlugin.getInstance().getCompatibilityManager();
        if (compatibilityManager.isExtensionEnabled(GriefPreventionExtension.class)
                && !((GriefPreventionExtension) compatibilityManager.getExtension(GriefPreventionExtension.class))
                .canUseWeapons(player, player.getLocation())) {
            MessagesConfig.ERROR_CANNOT_USE_GUNSHELL_WEAPONS_HERE.get(player);
            return;
        }

        if (compatibilityManager.isExtensionEnabled(WorldGuardExtension.class)
                && ((WorldGuardExtension) compatibilityManager.getExtension(WorldGuardExtension.class)).isFlagState(player.getLocation(),
                WorldGuardExtension.GunshellFlag.GUNSHELL_USE_WEAPONS, false)) {
            MessagesConfig.ERROR_CANNOT_USE_GUNSHELL_WEAPONS_HERE.get(player);
            return;
        }

        String cooldownKey = DefaultConfig.PER_WEAPON_COOLDOWN.asBoolean() ?
                player.getUniqueId() + "_" + player.getInventory().getHeldItemSlot() :
                player.getUniqueId() + "_global";

        String meleeKey = NBTEditor.getString(itemStack, "gunshell_melee_key");
        GunshellMelee melee = GunshellPlugin.getInstance().getWeaponRegistry().getMelees().get(meleeKey);

        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (event.getEntity() instanceof ArmorStand) return;
        LivingEntity entity = (LivingEntity) event.getEntity();

        if (compatibilityManager.isExtensionEnabled(GriefPreventionExtension.class)
                && !((GriefPreventionExtension) compatibilityManager.getExtension(GriefPreventionExtension.class))
                .canUseWeapons(player, entity.getLocation())) return;

        event.setCancelled(true);
        event.setDamage(0);

        // Deny if the attacker is outside a region but the entity is.
        if (compatibilityManager.isExtensionEnabled(WorldGuardExtension.class)
                && ((WorldGuardExtension) compatibilityManager.getExtension(WorldGuardExtension.class)).isFlagState(player.getLocation(),
                WorldGuardExtension.GunshellFlag.GUNSHELL_USE_WEAPONS, false)) return;

        if (hasCooldown(cooldownKey, melee) || hasGrabCooldown(player.getUniqueId(), melee)) return;

        MeleeDamageEvent meleeDamageEvent = new MeleeDamageEvent(player, entity, melee);
        Bukkit.getPluginManager().callEvent(meleeDamageEvent);
        if (meleeDamageEvent.isCancelled()) return;

        GunshellPlugin.getInstance().getMeleeCooldownMap().put(cooldownKey, System.currentTimeMillis());

        MeleeActionImpl meleeAction = MeleeActionRegistry.getAction(melee, melee.getActionType());
        if (meleeAction == null) {
            ChatUtils.sendMessage(player, "&cError: &4Melee action not found!");
            return;
        }

        meleeAction.fireAction(entity, player, melee.getConfiguration());
    }

    private boolean hasCooldown(String cooldownKey, GunshellMelee melee) {
        Long lastUsed = GunshellPlugin.getInstance().getMeleeCooldownMap().getOrDefault(cooldownKey, 0L);
        return System.currentTimeMillis() <= (lastUsed + melee.getCooldown());
    }

    private boolean hasGrabCooldown(UUID uniqueId, GunshellMelee melee) {
        Long lastUsed = GunshellPlugin.getInstance().getMeleeGrabCooldownMap().getOrDefault(uniqueId, 0L);
        return System.currentTimeMillis() <= (lastUsed + (melee.getGrabCooldown() * 1000));
    }
}
