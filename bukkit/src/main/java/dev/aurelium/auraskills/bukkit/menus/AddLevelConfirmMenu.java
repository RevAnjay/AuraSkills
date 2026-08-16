package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.api.event.skill.AddLevelConfirmEvent;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.commands.SkillCommand;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.slate.builder.MenuBuilder;
import org.bukkit.entity.Player;

public class AddLevelConfirmMenu {

    private final AuraSkills plugin;

    public AddLevelConfirmMenu(AuraSkills plugin) {
        this.plugin = plugin;
    }

    public void build(MenuBuilder menu) {
        menu.onOpen(m -> AddLevelConfirmTracker.get().registerOpen(m.player(), (Skill) m.menu().getProperty("skill"), (int) m.menu().getProperty("new_level"), (int) m.menu().getProperty("wasted")));
        menu.item("info", item -> {
            item.replace("player", p -> ((Player) p.menu().getProperty("target")).getName());
            item.replace("skill", p -> ((Skill) p.menu().getProperty("skill")).getDisplayName(p.locale(), false));
            item.replace("current_level", p -> String.valueOf(getCurrentLevel(p.menu().getProperty("target"), p.menu().getProperty("skill"))));
            item.replace("new_level", p -> String.valueOf((int) p.menu().getProperty("new_level")));
            item.replace("wasted", p -> String.valueOf((int) p.menu().getProperty("wasted")));
        });
        menu.item("confirm", item -> item.onClick(c -> {
            Player target = (Player) c.menu().getProperty("target");
            Skill skill = (Skill) c.menu().getProperty("skill");
            int newLevel = (int) c.menu().getProperty("new_level");
            int wasted = (int) c.menu().getProperty("wasted");
            new SkillCommand(plugin).setSkillLevel(c.player(), target, skill, newLevel);
            AddLevelConfirmTracker.get().markHandled(target);
            // Notify listeners
            plugin.getServer().getPluginManager().callEvent(new AddLevelConfirmEvent(target, skill, newLevel, wasted, true));
            c.player().closeInventory();
        }));
        menu.item("cancel", item -> item.onClick(c -> {
            Player target = (Player) c.menu().getProperty("target");
            Skill skill = (Skill) c.menu().getProperty("skill");
            int newLevel = (int) c.menu().getProperty("new_level");
            int wasted = (int) c.menu().getProperty("wasted");
            AddLevelConfirmTracker.get().markHandled(target);
            // Notify listeners
            plugin.getServer().getPluginManager().callEvent(new AddLevelConfirmEvent(target, skill, newLevel, wasted, false));
            c.player().closeInventory();
        }));
    }

    private int getCurrentLevel(Object target, Object skill) {
        User user = plugin.getUser((Player) target);
        return user.getSkillLevel((Skill) skill);
    }
}
