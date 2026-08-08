package dev.aurelium.auraskills.common.ui;

import com.google.common.collect.Sets;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.util.NumberUtil;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.config.Option;
import dev.aurelium.auraskills.common.hooks.PlaceholderHook;
import dev.aurelium.auraskills.common.message.type.ActionBarMessage;
import dev.aurelium.auraskills.common.scheduler.TaskRunnable;
import dev.aurelium.auraskills.common.ui.UiProvider.FormatType;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class ActionBarManager {

    public static final int PAUSE_MS = 750;
    protected final AuraSkillsPlugin plugin;
    private final UiProvider uiProvider;

    private final Set<UUID> isPaused = Sets.newConcurrentHashSet();
    private final Set<UUID> isGainingXp = Sets.newConcurrentHashSet();
    private final Map<UUID, Integer> timer = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> currentAction = new ConcurrentHashMap<>();
    private final Map<Locale, String> idleMessageCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastSentMessage = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSentTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pauseUntil = new ConcurrentHashMap<>();
    private final Map<UUID, TaskRunnable> activeXpTasks = new ConcurrentHashMap<>();

    public ActionBarManager(AuraSkillsPlugin plugin, UiProvider uiProvider) {
        this.plugin = plugin;
        this.uiProvider = uiProvider;
        startTimerCountdown();
        startUpdatingIdleActionBar();
        startPauseCleanupTask();
    }

    public void startPauseCleanupTask() {
        var task = new TaskRunnable() {
            @Override
            public void run() {
                if (pauseUntil.isEmpty()) return;
                long now = System.currentTimeMillis();
                Iterator<Map.Entry<UUID, Long>> it = pauseUntil.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, Long> entry = it.next();
                    if (now >= entry.getValue()) {
                        isPaused.remove(entry.getKey());
                        it.remove();
                    }
                }
            }
        };
        plugin.getScheduler().timerSync(task, 0, 10 * 50L, TimeUnit.MILLISECONDS); // Check every 10 ticks
    }

    public void startTimerCountdown() {
        var task = new TaskRunnable() {
            @Override
            public void run() {
                if (!plugin.configBoolean(Option.ACTION_BAR_ENABLED)) {
                    return;
                }

                if (!plugin.configBoolean(Option.ACTION_BAR_ENABLED) || timer.isEmpty()) {
                    return;
                }

                Iterator<Map.Entry<UUID, Integer>> it = timer.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, Integer> entry = it.next();
                    int time = entry.getValue();
                    if (time > 0) {
                        entry.setValue(time - 1);
                    }
                }
            }
        };
        plugin.getScheduler().timerSync(task, 0, 2 * 50L, TimeUnit.MILLISECONDS);
    }

    public void startUpdatingIdleActionBar() {
        var task = new TaskRunnable() {
            @Override
            public void run() {
                if (!plugin.configBoolean(Option.ACTION_BAR_IDLE) || !plugin.configBoolean(Option.ACTION_BAR_ENABLED)) {
                    return;
                }
                for (User user : plugin.getUserManager().getOnlineUsers()) {
                    UUID uuid = user.getUuid();
                    // Check player setting
                    if (!user.isActionBarEnabled(ActionBarType.IDLE)) {
                        continue;
                    }
                    // Check disabled worlds
                    if (plugin.getWorldManager().isDisabledWorld(getWorldName(user))) {
                        continue;
                    }

                    if (!currentAction.containsKey(uuid)) {
                        currentAction.put(uuid, 0);
                    }
                    if (isGainingXp.contains(uuid) || isPaused.contains(uuid)) {
                        continue;
                    }

                    boolean formatLast = plugin.configBoolean(Option.ACTION_BAR_FORMAT_LAST);
                    Locale locale = user.getLocale();

                    String base;
                    if (formatLast) {
                        base = plugin.getMessageProvider().getRaw(ActionBarMessage.IDLE, locale);
                    } else {
                        String cache = idleMessageCache.get(locale);
                        if (cache != null) { // Cache hit
                            base = cache;
                        } else { // Cache miss
                            base = plugin.getMsg(ActionBarMessage.IDLE, locale);
                            idleMessageCache.put(locale, base);
                        }
                    }

                    String message = TextUtil.replace(base,
                            "{hp}", getHp(user),
                            "{max_hp}", getMaxHp(user),
                            "{mana}", getMana(user),
                            "{max_mana}", getMaxMana(user));
                    message = replacePlaceholderApi(user, message);

                    if (formatLast) {
                        message = plugin.getMessageProvider().applyFormatting(message);
                    }

                    // Optimization: Only send if the message actually changed OR it's been > 2s (keep-alive)
                    String last = lastSentMessage.get(uuid);
                    Long lastTime = lastSentTime.get(uuid);
                    long now = System.currentTimeMillis();
                    
                    if (message.equals(last) && lastTime != null && (now - lastTime) < 2000) {
                        continue;
                    }

                    uiProvider.sendActionBar(user, message);
                    lastSentMessage.put(uuid, message);
                    lastSentTime.put(uuid, now);
                }
            }
        };
        if (plugin.configBoolean(Option.ACTION_BAR_UPDATE_ASYNC)) {
            plugin.getScheduler().timerAsync(task, 0, plugin.configInt(Option.ACTION_BAR_UPDATE_PERIOD) * 50L, TimeUnit.MILLISECONDS);
        } else {
            plugin.getScheduler().timerSync(task, 0, plugin.configInt(Option.ACTION_BAR_UPDATE_PERIOD) * 50L, TimeUnit.MILLISECONDS);
        }
    }

    public void sendXpActionBar(User user, Skill skill, double currentXp, double levelXp, double xpGained, int level, boolean maxed, double income) {
        ActionBarType sendType = maxed ? ActionBarType.MAXED : ActionBarType.XP;
        // Return if the type is disabled in config
        if (!plugin.configBoolean(sendType.getOption()) || !plugin.configBoolean(Option.ACTION_BAR_ENABLED)) {
            return;
        }

        // Return if player has it disabled
        if (!user.isActionBarEnabled(sendType)) {
            return;
        }

        UUID uuid = user.getUuid();
        if (isPaused.contains(uuid)) {
            return;
        }
        // Set timer and is gaining xp
        isGainingXp.add(uuid);
        timer.put(uuid, 20);

        // Increment action number
        int thisAction = currentAction.getOrDefault(uuid, 0) + 1;
        currentAction.put(uuid, thisAction);

        TaskRunnable previousTask = activeXpTasks.remove(uuid);
        if (previousTask != null) {
            try {
                previousTask.cancel();
            } catch (IllegalStateException ignored) {
            }
        }

        TaskRunnable xpTask = new TaskRunnable() {
            @Override
            public void run() {
                if (!isGainingXp.contains(uuid)) {
                    cancel();
                    return;
                }
                Integer actionNum = currentAction.get(uuid);
                if (actionNum == null) {
                    cancel();
                    return;
                }
                if (thisAction != actionNum) {
                    cancel();
                    return;
                }
                
                if (isPaused.contains(uuid)) {
                    return;
                }

                String message = getXpActionBarMessage(user, skill, currentXp, levelXp, xpGained, level, maxed, income);
                uiProvider.sendActionBar(user, message);
            }
        };
        activeXpTasks.put(uuid, xpTask);
        plugin.getScheduler().timerSync(xpTask, 0, plugin.configInt(Option.ACTION_BAR_UPDATE_PERIOD) * 50L, TimeUnit.MILLISECONDS);
        plugin.getScheduler().scheduleSync(() -> {
            Integer timerNum = timer.get(uuid);
            if (timerNum != null) {
                if (timerNum.equals(0)) {
                    isGainingXp.remove(uuid);
                }
            }
        }, 41 * 50L, TimeUnit.MILLISECONDS);
    }

    public void resetActionBars() {
        isGainingXp.clear();
        timer.clear();
        currentAction.clear();
        isPaused.clear();
        pauseUntil.clear();
        lastSentMessage.clear();
        lastSentTime.clear();
        for (TaskRunnable task : activeXpTasks.values()) {
            try { task.cancel(); } catch (IllegalStateException ignored) { }
        }
        activeXpTasks.clear();
        clearMessageCache();
    }

    public void resetActionBar(User user) {
        UUID uuid = user.getUuid();
        isGainingXp.remove(uuid);
        timer.remove(uuid);
        currentAction.remove(uuid);
        isPaused.remove(uuid);
        pauseUntil.remove(uuid);
        lastSentMessage.remove(uuid);
        lastSentTime.remove(uuid);
        TaskRunnable task = activeXpTasks.remove(uuid);
        if (task != null) {
            try { task.cancel(); } catch (IllegalStateException ignored) { }
        }
    }

    public void setPaused(User user, int time, TimeUnit timeUnit) {
        UUID uuid = user.getUuid();
        long pauseMs = timeUnit.toMillis(time);
        long newPauseUntil = System.currentTimeMillis() + pauseMs;
        
        Long currentPauseUntil = pauseUntil.get(uuid);
        if (currentPauseUntil != null && currentPauseUntil > System.currentTimeMillis()) {
            if (newPauseUntil > currentPauseUntil) {
                pauseUntil.put(uuid, newPauseUntil);
            }
            return;
        }
        
        isPaused.add(uuid);
        pauseUntil.put(uuid, newPauseUntil);
    }

    public void sendAbilityActionBar(User user, String message) {
        if (!user.isActionBarEnabled(ActionBarType.ABILITY)) return;

        boolean formatLast = plugin.configBoolean(Option.ACTION_BAR_FORMAT_LAST);

        String base;
        if (formatLast) {
            base = plugin.getMessageProvider().getRaw(ActionBarMessage.ABILITY, user.getLocale());
        } else {
            base = plugin.getMsg(ActionBarMessage.ABILITY, user.getLocale());
        }

        String actionBarText = TextUtil.replace(base,
                "{hp}", getHp(user),
                "{max_hp}", getMaxHp(user),
                "{mana}", getMana(user),
                "{max_mana}", getMaxMana(user),
                "{message}", message);
        actionBarText = replacePlaceholderApi(user, actionBarText);

        if (formatLast) {
            actionBarText = plugin.getMessageProvider().applyFormatting(actionBarText);
        }

        plugin.getUiProvider().sendActionBar(user, actionBarText);
        setPaused(user, PAUSE_MS, TimeUnit.MILLISECONDS);
    }

    private String getXpActionBarMessage(User user, Skill skill, double currentXp, double levelXp, double xpGained, int level, boolean maxed, double income) {
        ActionBarMessage messageKey = maxed ? ActionBarMessage.MAXED : ActionBarMessage.XP;
        Locale locale = user.getLocale();

        boolean formatLast = plugin.configBoolean(Option.ACTION_BAR_FORMAT_LAST);

        String base;
        if (formatLast) {
            base = plugin.getMessageProvider().getRaw(messageKey, locale);
        } else {
            base = plugin.getMsg(messageKey, locale);
        }

        // Replace built in placeholders
        String message = TextUtil.replace(base,
                "{hp}", getHp(user),
                "{max_hp}", getMaxHp(user),
                "{xp_gained}", xpGained > 0 ? "+" + NumberUtil.format1(xpGained) : NumberUtil.format1(xpGained),
                "{skill}", skill.getDisplayName(locale),
                "{current_xp}", plugin.getUiProvider().getFormat(FormatType.XP).format(currentXp),
                "{level_xp}", NumberUtil.format1(levelXp),
                "{skill_level}", String.valueOf(level),
                "{mana}", getMana(user),
                "{max_mana}", getMaxMana(user),
                "{income}", plugin.getUiProvider().getFormat(FormatType.MONEY).format(income));
        // Replace PlaceholderAPI placeholders
        message = replacePlaceholderApi(user, message);

        if (formatLast) {
            message = plugin.getMessageProvider().applyFormatting(message);
        }

        return message;
    }

    @NotNull
    public abstract String getHp(User user);

    @NotNull
    public abstract String getMaxHp(User user);

    @NotNull
    public abstract String getWorldName(User user);

    private String getMana(User user) {
        return String.valueOf((int) Math.floor(user.getMana()));
    }

    private String getMaxMana(User user) {
        return String.valueOf((int) Math.floor(user.getMaxMana()));
    }

    private String replacePlaceholderApi(User user, String message) {
        if (!message.contains("%")) {
            return message; // Skip PAPI if no % tokens found (huge performance gain)
        }
        if (plugin.getHookManager().isRegistered(PlaceholderHook.class) && plugin.configBoolean(Option.ACTION_BAR_PLACEHOLDER_API)) {
            return plugin.getHookManager().getHook(PlaceholderHook.class).setPlaceholders(user, message);
        }
        return message;
    }

    public void clearMessageCache() {
        idleMessageCache.clear();
    }

}
