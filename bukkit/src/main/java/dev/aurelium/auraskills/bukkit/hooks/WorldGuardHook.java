package dev.aurelium.auraskills.bukkit.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.hooks.WorldGuardFlags.FlagKey;
import dev.aurelium.auraskills.common.hooks.Hook;
import dev.aurelium.auraskills.common.hooks.HookRegistrationException;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;

import java.util.*;

public class WorldGuardHook extends Hook {

    private final AuraSkills plugin;
    private RegionContainer container;
    private List<String> blockedRegions;
    private List<String> blockedCheckBlockReplaceRegions;
    private List<String> checkedWorlds;
    private final Cache<String, Boolean> isBlockedCache = CacheBuilder.newBuilder()
            .expireAfterWrite(50, TimeUnit.MILLISECONDS)
            .maximumSize(1000)
            .build();

    public WorldGuardHook(AuraSkills plugin, ConfigurationNode config) {
        super(plugin, config);
        this.plugin = plugin;
        try {
            loadRegions(config);
        } catch (SerializationException e) {
            throw new HookRegistrationException("Error serializing config list");
        }
    }

    public void loadRegions(ConfigurationNode config) throws SerializationException {
        container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        blockedRegions = new LinkedList<>();
        blockedRegions.addAll(config.node("blocked_regions").getList(String.class, new ArrayList<>()));
        blockedCheckBlockReplaceRegions = new LinkedList<>();
        blockedCheckBlockReplaceRegions.addAll(config.node("blocked_check_replace_regions").getList(String.class, new ArrayList<>()));
        checkedWorlds = new LinkedList<>();
        checkedWorlds.addAll(config.node("checked_worlds").getList(String.class, new ArrayList<>()));
        isBlockedCache.invalidateAll();
    }

    public boolean isBlocked(Location location, Player player, FlagKey flagKey) {
        if (location.getWorld() == null) return false;
        if (!checkedWorlds.isEmpty() && !checkedWorlds.contains(location.getWorld().getName())) {
            return false;
        }
        String key = location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ() + ":" + player.getUniqueId() + ":" + flagKey.name();
        Boolean cached = isBlockedCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        boolean result = isBlockedUncached(location, player, flagKey);
        isBlockedCache.put(key, result);
        return result;
    }

    private boolean isBlockedUncached(Location location, Player player, FlagKey flagKey) {
        if (isInBlockedRegion(location)) {
            return true;
        }
        return blockedByFlag(location, player, flagKey);
    }

    public boolean isBlocked(Location location, Player player, Skill skill) {
        if (location.getWorld() == null) return false;
        if (!checkedWorlds.isEmpty() && !checkedWorlds.contains(location.getWorld().getName())) {
            return false;
        }
        String key = location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ() + ":" + player.getUniqueId() + ":" + skill.getId().toString();
        Boolean cached = isBlockedCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        boolean result = isBlockedUncached(location, player, skill);
        isBlockedCache.put(key, result);
        return result;
    }

    private boolean isBlockedUncached(Location location, Player player, Skill skill) {
        if (isInBlockedRegion(location)) {
            return true;
        }
        if (blockedByFlag(location, player, FlagKey.XP_GAIN)) {
            return true;
        }
        return blockedBySkillFlag(location, player, skill);
    }

    private boolean isInBlockedRegion(Location location) {
        return isInRegionList(location, blockedRegions);
    }

    public boolean isInBlockedCheckRegion(Location location) {
        if (location.getWorld() == null) {
            return false;
        }
        if (!checkedWorlds.isEmpty() && !checkedWorlds.contains(location.getWorld().getName())) {
            return false;
        }
        return isInRegionList(location, blockedCheckBlockReplaceRegions);
    }

    private boolean isInRegionList(Location location, List<String> blockedRegions) {
        if (location.getWorld() == null) {
            return false;
        }
        RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (regions == null) {
            return false;
        }
        ApplicableRegionSet set = regions.getApplicableRegions(BukkitAdapter.adapt(location).toVector().toBlockPoint());
        for (ProtectedRegion region : set) {
            if (blockedRegions.contains(region.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean blockedByFlag(Location location, Player player, FlagKey flagKey) {
        WorldGuardFlags flags = plugin.getWorldGuardFlags();
        if (flags == null) return false;

        StateFlag flag = flags.getStateFlag(flagKey.toString());
        return queryFlagState(location, player, flag);
    }

    private boolean blockedBySkillFlag(Location location, Player player, Skill skill) {
        WorldGuardFlags flags = plugin.getWorldGuardFlags();
        if (flags == null) return false;

        String flagKey = "xp-gain-" + TextUtil.replace(skill.name().toLowerCase(Locale.ROOT), "_", "-");
        StateFlag flag = flags.getStateFlag(flagKey);
        return queryFlagState(location, player, flag);
    }

    private boolean queryFlagState(Location location, Player player, StateFlag flag) {
        if (flag == null) return false;

        World world = location.getWorld();
        if (world == null) return false;

        RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (regions == null) {
            return false;
        }
        ApplicableRegionSet set = regions.getApplicableRegions(BukkitAdapter.adapt(location).toVector().toBlockPoint());
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        StateFlag.State state = set.queryState(localPlayer, flag);
        return state == StateFlag.State.DENY;
    }

    @Override
    public Class<? extends Hook> getTypeClass() {
        return WorldGuardHook.class;
    }

}
