package com.jazzkuh.gunshell.compatibility.extensions.griefprevention;

import com.jazzkuh.gunshell.GunshellPlugin;
import com.jazzkuh.gunshell.compatibility.framework.Extension;
import com.jazzkuh.gunshell.compatibility.framework.ExtensionInfo;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@ExtensionInfo(name = "GriefPreventionExtension", loadPlugin = "GriefPrevention")
public class GriefPreventionExtension implements Extension {
    private static final String GP_MAIN_CLASS = "me.ryanhamshire.GriefPrevention.GriefPrevention";
    private static final String GP_DATASTORE_CLASS = "me.ryanhamshire.GriefPrevention.DataStore";
    private static final String GP_CLAIM_CLASS = "me.ryanhamshire.GriefPrevention.Claim";
    private static final String GP_CLAIM_PERMISSION_CLASS = "me.ryanhamshire.GriefPrevention.ClaimPermission";

    private Object dataStore;
    private Class<?> claimClass;
    private Method getClaimAtMethod;

    private Method allowBuildMethod;

    private Method checkPermissionMethod;
    private Object claimBuildPermission;

    private boolean reflectionInitialized = false;
    private boolean reflectionFailed = false;

    @Override
    public void onEnable() {
        initializeReflection();
        GunshellPlugin.getInstance().getLogger().info("GriefPrevention compatibility layer enabled!");
    }

    @Override
    public void onDisable() {
        GunshellPlugin.getInstance().getLogger().info("GriefPrevention compatibility layer disabled!");
    }

    @Override
    public void onLoad() {
    }

    public boolean canUseWeapons(Player player, Location location) {
        if (player == null || location == null) return true;
        if (!ensureReady()) return true;

        try {
            Object claim = getClaimAt(location);
            if (claim == null) return true;

            if (allowBuildMethod != null) {
                return allowBuildMethod.invoke(claim, player, Material.STONE) == null;
            }

            if (checkPermissionMethod != null) {
                return checkPermissionMethod.invoke(claim, player, claimBuildPermission, (Event) null) == null;
            }
        } catch (Exception exception) {
            GunshellPlugin.getInstance().getLogger().warning("Failed to query GriefPrevention claim permissions.");
            return true;
        }

        return true;
    }

    private boolean ensureReady() {
        if (reflectionInitialized) return true;
        if (reflectionFailed) return false;

        initializeReflection();
        return reflectionInitialized;
    }

    private void initializeReflection() {
        if (reflectionInitialized || reflectionFailed) return;

        try {
            Class<?> griefPreventionClass = Class.forName(GP_MAIN_CLASS);
            Field instanceField = griefPreventionClass.getField("instance");
            Object griefPreventionInstance = instanceField.get(null);
            if (griefPreventionInstance == null) {
                reflectionFailed = true;
                return;
            }

            Field dataStoreField = griefPreventionClass.getField("dataStore");
            this.dataStore = dataStoreField.get(griefPreventionInstance);
            if (this.dataStore == null) {
                reflectionFailed = true;
                return;
            }

            this.claimClass = Class.forName(GP_CLAIM_CLASS);
            Class<?> dataStoreClass = Class.forName(GP_DATASTORE_CLASS);

            try {
                this.getClaimAtMethod = dataStoreClass.getMethod("getClaimAt", Location.class, boolean.class, boolean.class, claimClass);
            } catch (NoSuchMethodException ignored) {
                this.getClaimAtMethod = dataStoreClass.getMethod("getClaimAt", Location.class, boolean.class, claimClass);
            }

            try {
                this.allowBuildMethod = claimClass.getMethod("allowBuild", Player.class, Material.class);
            } catch (NoSuchMethodException ignored) {
                Class<?> claimPermissionClass = Class.forName(GP_CLAIM_PERMISSION_CLASS);
                this.checkPermissionMethod = claimClass.getMethod("checkPermission", Player.class, claimPermissionClass, Event.class);
                this.claimBuildPermission = Enum.valueOf((Class<Enum>) claimPermissionClass, "Build");
            }

            reflectionInitialized = true;
        } catch (Exception exception) {
            reflectionFailed = true;
            GunshellPlugin.getInstance().getLogger().warning("Failed to initialize GriefPrevention compatibility reflection.");
        }
    }

    private Object getClaimAt(Location location) throws Exception {
        if (getClaimAtMethod.getParameterCount() == 4) {
            return getClaimAtMethod.invoke(dataStore, location, true, false, null);
        }

        return getClaimAtMethod.invoke(dataStore, location, true, null);
    }
}
