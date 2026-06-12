package dev.aurelium.auraskills.common.reward.type;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.hooks.EconomyHook;
import dev.aurelium.auraskills.common.reward.SkillReward;
import dev.aurelium.auraskills.common.user.User;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class MoneyReward extends SkillReward {

    private final double amount;
    @Nullable
    private final String formula;
    @Nullable
    private Expression parsedFormula;

    public MoneyReward(AuraSkillsPlugin plugin, Skill skill, double amount, @Nullable String formula) {
        super(plugin, skill);
        this.amount = amount;
        this.formula = formula;
        if (formula != null) {
            try {
                this.parsedFormula = new Expression(formula);
            } catch (Exception e) {
                plugin.logger().warn("Failed to parse money reward expression: " + formula);
            }
        }
    }

    @Override
    public void giveReward(User user, Skill skill, int level) {
        if (!hooks.isRegistered(EconomyHook.class)) {
            return;
        }
        hooks.getHook(EconomyHook.class).deposit(user, getAmount(level));
    }

    public double getAmount(int level) {
        if (parsedFormula == null && amount > 0) {
            return amount;
        } else if (parsedFormula != null) {
            synchronized (parsedFormula) {
                parsedFormula.with("level", level);
                try {
                    return parsedFormula.evaluate().getNumberValue().doubleValue();
                } catch (EvaluationException | ParseException e) {
                    plugin.logger().warn("Failed to evaluate money reward expression " + formula);
                    e.printStackTrace();
                }
            }
        }
        return 0.0;
    }

    @Override
    public String getMenuMessage(User player, Locale locale, Skill skill, int level) {
        return ""; // All money rewards have to be added into one line
    }

    @Override
    public String getChatMessage(User player, Locale locale, Skill skill, int level) {
        return ""; // ALl money rewards have to be added into one line
    }

}
