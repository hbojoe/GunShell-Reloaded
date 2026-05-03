package com.jazzkuh.gunshell.common.listeners;

import com.jazzkuh.gunshell.GunshellPlugin;
import com.jazzkuh.gunshell.api.objects.GunshellAmmunition;
import com.jazzkuh.gunshell.api.objects.GunshellFireable;
import com.jazzkuh.gunshell.common.configuration.PlaceHolder;
import com.jazzkuh.gunshell.common.configuration.lang.MessagesConfig;
import com.jazzkuh.gunshell.utils.PluginUtils;
import com.jazzkuh.gunshell.utils.NBTEditor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PlayerSwapHandListener implements Listener {
    private final String GUN_AMMO_KEY = "gunshell_weapon_ammo";
    private final String GUN_AMMOTYPE_KEY = "gunshell_weapon_ammotype";

    private final String AMMUNITION_KEY = "gunshell_ammunition_key";

    @EventHandler
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        ItemStack itemStack = player.getInventory().getItemInMainHand();

        if (!NBTEditor.contains(itemStack, "gunshell_weapon_key")) return;
        event.setCancelled(true);

        if (GunshellPlugin.getInstance().getReloadingSet().contains(player.getUniqueId())) return;

        String weaponKey = NBTEditor.getString(itemStack, "gunshell_weapon_key");
        GunshellFireable fireable = GunshellPlugin.getInstance().getWeaponRegistry().getWeapons().get(weaponKey);

        int ammo = NBTEditor.getInt(itemStack, GUN_AMMO_KEY);

        List<String> ammunitionKeys = fireable.getAmmunitionKeys();
        if (ammo <= 0 && PluginUtils.getInstance().getItemWithNBTTags(player, AMMUNITION_KEY, ammunitionKeys).isEmpty()) {
            MessagesConfig.ERROR_OUT_OF_AMMO.get(player);
            player.playSound(player.getLocation(), fireable.getEmptySound(), fireable.getSoundVolume(), 1F);
            return;
        }

        if (ammo <= 0 && PluginUtils.getInstance().getItemWithNBTTags(player, AMMUNITION_KEY, ammunitionKeys).isPresent()) {
            fireable.reload(player, itemStack);
        } else {
            GunshellAmmunition newAmmunition = GunshellPlugin.getInstance().getWeaponRegistry().getAmmunition()
                    .get(NBTEditor.getString(itemStack, GUN_AMMOTYPE_KEY));
            int ammoAmount = NBTEditor.getInt(itemStack, GUN_AMMO_KEY);
            ItemStack ammoItem = newAmmunition.getItemStack(ammoAmount);

            // Weapon has been unloaded so set the ammo to 0
            PluginUtils.getInstance().applyNBTTag(itemStack, GUN_AMMO_KEY, 0);
            fireable.updateItemMeta(itemStack, 0);

            // Add the ammo to the player's inventory
            player.getInventory().addItem(ammoItem);
            MessagesConfig.UNLOADING_FINISHED.get(player,
                    new PlaceHolder("Ammo", String.valueOf(ammoAmount > fireable.getMaxAmmo() ? fireable.getMaxAmmo() : ammoAmount)),
                    new PlaceHolder("MaxAmmo", String.valueOf(fireable.getMaxAmmo())));
        }
    }
}
