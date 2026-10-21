package dev.aurelium.auraskills.api.event.skill;

import dev.aurelium.auraskills.api.skill.Skill;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player confirms or cancels an add-level confirmation GUI.
 * <p>
 * {@code confirmed=true} means the level was (or will be) applied; {@code confirmed=false} means the
 * player cancelled the confirmation GUI.
 */
public class AddLevelConfirmEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final Skill skill;
    private final int newLevel;
    private final int wasted;
    private final boolean confirmed;

    public AddLevelConfirmEvent(Player player, Skill skill, int newLevel, int wasted, boolean confirmed) {
        this.player = player;
        this.skill = skill;
        this.newLevel = newLevel;
        this.wasted = wasted;
        this.confirmed = confirmed;
    }

    public Player getPlayer() {
        return player;
    }

    public Skill getSkill() {
        return skill;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public int getWasted() {
        return wasted;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
