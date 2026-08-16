package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.api.event.skill.AddLevelConfirmEvent;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks open add-level confirmation GUIs so that closing the inventory without
 * clicking a button is treated as a cancel (firing {@link AddLevelConfirmEvent} with
 * {@code confirmed=false}). Consumers such as the coupon plugin rely on this to keep
 * a coupon valid when the player abandons the confirmation.
 */
public class AddLevelConfirmTracker implements Listener {

    private final AuraSkills plugin;
    // player -> open confirmation menu data
    private final ConcurrentMap<Player, Data> open = new ConcurrentHashMap<>();

    private record Data(Skill skill, int newLevel, int wasted) {}

    private static AddLevelConfirmTracker instance;

    public AddLevelConfirmTracker(AuraSkills plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static AddLevelConfirmTracker get() {
        return instance;
    }

    public void registerOpen(Player player, Skill skill, int newLevel, int wasted) {
        open.put(player, new Data(skill, newLevel, wasted));
    }

    public void markHandled(Player player) {
        open.remove(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Data data = open.remove(player);
        if (data == null) return; // Not an open add-level confirmation
        // Closed without confirming -> treat as cancel
        plugin.getServer().getPluginManager().callEvent(
                new AddLevelConfirmEvent(player, data.skill(), data.newLevel(), data.wasted(), false));
    }
}
