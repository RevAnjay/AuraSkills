package dev.aurelium.auraskills.bukkit.trait;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;
import dev.aurelium.auraskills.api.damage.DamageMeta;
import dev.aurelium.auraskills.api.damage.DamageModifier;
import dev.aurelium.auraskills.api.event.damage.DamageEvent;
import dev.aurelium.auraskills.api.trait.Trait;
import dev.aurelium.auraskills.api.trait.Traits;
import dev.aurelium.auraskills.api.util.NumberUtil;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DamageReductionTrait extends TraitImpl {

    @Nullable
    private Expression formula;
    private final Map<Double, Double> cache = new ConcurrentHashMap<>();

    DamageReductionTrait(AuraSkills plugin) {
        super(plugin, Traits.DAMAGE_REDUCTION);
    }

    @Override
    public double getBaseLevel(Player player, Trait trait) {
        return 0;
    }

    @Override
    public String getMenuDisplay(double value, Trait trait, Locale locale) {
        return NumberUtil.format1(getReductionValue(value) * 100) + "%";
    }

    @Override
    public boolean displayMatchesValue() {
        return false;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void damageListener(DamageEvent event) {
        if (!Traits.DAMAGE_REDUCTION.isEnabled()) {
            return;
        }

        // LOW to make sure it runs before ability modifiers
        DamageMeta meta = event.getDamageMeta();
        Player player = meta.getTargetAsPlayer();

        if (player != null) {
            User user = plugin.getUser(player);
            double reduction = user.getEffectiveTraitLevel(Traits.DAMAGE_REDUCTION);

            meta.addDefenseModifier(
                    new DamageModifier((1 - getReductionValue(reduction)) - 1, DamageModifier.Operation.MULTIPLY));
        }
    }

    public void resetFormula() {
        formula = null;
        cache.clear();
    }

    private double getReductionValue(double value) {
        return cache.computeIfAbsent(value, val -> {
            Trait trait = Traits.DAMAGE_REDUCTION;
            try {
                if (formula == null) {
                    synchronized (this) {
                        if (formula == null) {
                            formula = new Expression(trait.optionString("formula"));
                        }
                    }
                }
                Expression localFormula = formula;
                synchronized (localFormula) {
                    localFormula.with("value", val);
                    return localFormula.evaluate().getNumberValue().doubleValue();
                }
            } catch (EvaluationException | ParseException | UnsupportedOperationException e) {
                plugin.logger().warn("Failed to evaluate formula for trait auraskills/damage_reduction: " + e.getMessage());
            }
            // Default formula
            return -1.0 * Math.pow(1.01, -1.0 * val) + 1;
        });
    }

}
