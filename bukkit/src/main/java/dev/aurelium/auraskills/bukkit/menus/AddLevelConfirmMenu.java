package dev.aurelium.auraskills.bukkit.menus;

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
            new SkillCommand(plugin).setSkillLevel(c.player(), target, skill, newLevel);
            c.player().closeInventory();
        }));
        menu.item("cancel", item -> item.onClick(c -> c.player().closeInventory()));
    }

    private int getCurrentLevel(Object target, Object skill) {
        User user = plugin.getUser((Player) target);
        return user.getSkillLevel((Skill) skill);
    }
}
