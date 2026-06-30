package dev.aurelium.auraskills.bukkit.hooks;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.util.VersionUtils;
import dev.aurelium.auraskills.common.hooks.Hook;
import dev.aurelium.auraskills.common.ui.ActionBarManager;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spongepowered.configurate.ConfigurationNode;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PacketEventsHook extends Hook implements Listener {

    private static final int PAUSE_MILLIS = 2500;
    private static final boolean IS_1_19_PLUS = VersionUtils.isAtLeastVersion(19);

    private final Map<UUID, AtomicInteger> pendingPackets = new ConcurrentHashMap<>();

    private final Set<UUID> quittingPlayers = ConcurrentHashMap.newKeySet();

    private final AuraSkills skillsPlugin;
    private ActionBarListener listener;

    public PacketEventsHook(AuraSkills plugin, ConfigurationNode config) {
        super(plugin, config);
        this.skillsPlugin = plugin;
        registerListeners();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[PacketEventsHook] Registered successfully!");
    }

    @Override
    public Class<? extends Hook> getTypeClass() {
        return PacketEventsHook.class;
    }

    @SuppressWarnings("deprecation")
    public void sendActionBar(Player player, String message) {
        UUID uuid = player.getUniqueId();

        if (quittingPlayers.contains(uuid)) return;

        pendingPackets.computeIfAbsent(uuid, k -> new AtomicInteger(0)).incrementAndGet();

        player.spigot().sendMessage(
            ChatMessageType.ACTION_BAR,
            TextComponent.fromLegacyText(message)
        );
    }

    private boolean consumeOwnPacket(UUID uuid) {
        AtomicInteger counter = pendingPackets.get(uuid);
        if (counter == null) return false;

        int current;
        do {
            current = counter.get();
            if (current <= 0) return false;
        } while (!counter.compareAndSet(current, current - 1));
        return true;
    }

    private void registerListeners() {
        listener = new ActionBarListener();
        PacketEvents.getAPI().getEventManager().registerListener(listener);
        skillsPlugin.getLogger().info("[PacketEventsHook] Packet listeners registered");
    }

    private User getUser(Player player) {
        return skillsPlugin.getUser(player);
    }

    private ActionBarManager getActionBar() {
        return skillsPlugin.getUiProvider().getActionBarManager();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        quittingPlayers.add(uuid);
        pendingPackets.remove(uuid);
        skillsPlugin.getScheduler().scheduleSync(() -> quittingPlayers.remove(uuid), 50L, TimeUnit.MILLISECONDS);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        quittingPlayers.remove(event.getPlayer().getUniqueId());
    }

    public void cleanup() {
        pendingPackets.clear();
        quittingPlayers.clear();
        if (listener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(listener);
        }
    }

    private class ActionBarListener extends PacketListenerAbstract {

        public ActionBarListener() {
            super(PacketListenerPriority.MONITOR);
        }

        @Override
        public void onPacketSend(PacketSendEvent event) {
            var packetType = event.getPacketType();
            if (packetType != Server.ACTION_BAR
                    && packetType != Server.SYSTEM_CHAT_MESSAGE) {
                return;
            }

            try {
                Player player = (Player) event.getPlayer();
                if (player == null) return;

                UUID uuid = player.getUniqueId();

                if (packetType == Server.ACTION_BAR) {
                    if (!consumeOwnPacket(uuid)) {
                        pauseForOtherPlugin(player);
                    }
                    return;
                }

                if (IS_1_19_PLUS) {
                    handleSystemChat(event, player, uuid);
                }
            } catch (Exception e) {
                // Silently ignore
            }
        }

        private void handleSystemChat(PacketSendEvent event, Player player, UUID uuid) {
            try {
                WrapperPlayServerSystemChatMessage wrapper = new WrapperPlayServerSystemChatMessage(event);
                if (wrapper.isOverlay()) {
                    if (!consumeOwnPacket(uuid)) {
                        pauseForOtherPlugin(player);
                    }
                }
            } catch (Exception e) {
                // Silently ignore
            }
        }

        private void pauseForOtherPlugin(Player player) {
            User user = getUser(player);
            if (user != null) {
                getActionBar().setPaused(user, PAUSE_MILLIS, TimeUnit.MILLISECONDS);
            }
        }
    }
}
