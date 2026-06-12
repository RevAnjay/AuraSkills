package dev.aurelium.auraskills.bukkit.source;

import com.google.common.collect.Sets;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.source.type.JumpingXpSource;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.util.CompatUtil;
import dev.aurelium.auraskills.common.source.SourceTypes;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;

import java.util.Set;
import java.util.UUID;

public class JumpingLeveler extends SourceLeveler {

    private final Set<UUID> prevPlayersOnGround = Sets.newConcurrentHashSet();
    private final java.util.Map<UUID, Integer> jumpCounts = new java.util.concurrent.ConcurrentHashMap<>();

    public JumpingLeveler(AuraSkills plugin) {
        super(plugin, SourceTypes.JUMPING);
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onJump(PlayerMoveEvent event) {
        if (disabled()) return;
        
        if (event.getFrom().getY() == event.getTo().getY()) {
            return;
        }

        Player player = event.getPlayer();

        handleJump(player, event);

        if (player.isOnGround()) {
            prevPlayersOnGround.add(player.getUniqueId());
        } else {
            prevPlayersOnGround.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        prevPlayersOnGround.remove(uuid);
        jumpCounts.remove(uuid);
    }

    @SuppressWarnings("deprecation")
    private void handleJump(Player player, PlayerMoveEvent event) {
        if (player.getVelocity().getY() <= 0) {
            return;
        }

        if (!prevPlayersOnGround.contains(player.getUniqueId()) || player.getLocation().getBlock().getType() == Material.LADDER) {
            return;
        }

        double jumpVelocity = 0.42F;
        org.bukkit.potion.PotionEffectType jumpBoostType = CompatUtil.jumpBoost();
        if (jumpBoostType != null && player.hasPotionEffect(jumpBoostType)) {
            PotionEffect effect = player.getPotionEffect(jumpBoostType);
            if (effect != null) {
                jumpVelocity += ((float) (effect.getAmplifier() + 1) * 0.1F);
            }
        }

        if (player.isOnGround() || Double.compare(player.getVelocity().getY(), jumpVelocity) != 0) {
            return;
        }
        var skillSource = plugin.getSkillManager().getSingleSourceOfType(JumpingXpSource.class);
        if (skillSource == null) return;

        JumpingXpSource source = skillSource.source();
        Skill skill = skillSource.skill();

        UUID uuid = player.getUniqueId();
        int jumps = jumpCounts.getOrDefault(uuid, 0) + 1;
        if (jumps >= source.getInterval()) {
            if (failsChecks(event, player, player.getLocation(), skill)) {
                jumpCounts.put(uuid, jumps);
                return;
            }
            plugin.getLevelManager().addXp(plugin.getUser(player), skill, source, source.getXp());
            jumpCounts.remove(uuid);
        } else {
            jumpCounts.put(uuid, jumps);
        }
    }

}
