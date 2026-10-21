package dev.aurelium.auraskills.bukkit.util;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class VersionUtils {

    private static final int FALLBACK_MAJOR_VERSION = 20;
    private static final int FALLBACK_MINOR_VERSION = 0;

    public static final int MAJOR_VERSION;
    public static final int MINOR_VERSION;
    public static final int PATCH_VERSION;

    static {
        int major = FALLBACK_MAJOR_VERSION;
        int minor = FALLBACK_MINOR_VERSION;
        int patch = 0;
        try {
            String version = getDetectedVersion();
            major = getMajorVersion(version);
            minor = getMinorVersion(version);
            patch = getPatchVersion(version);
        } catch (Throwable ignored) {
        }
        MAJOR_VERSION = major;
        MINOR_VERSION = minor;
        PATCH_VERSION = patch;
    }


    public static boolean isAtLeastVersion(int version) {
        return MAJOR_VERSION >= version;
    }

    public static boolean isAtLeastVersion(int majorVersionReq, int minorVersionReq) {
        if (MAJOR_VERSION > majorVersionReq) {
            return true;
        } else if (MAJOR_VERSION == majorVersionReq) {
            return MINOR_VERSION >= minorVersionReq;
        } else {
            return false;
        }
    }

    public static boolean isAtLeastVersion(int majorVersionReq, int minorVersionReq, int patchVersionReq) {
        if (MAJOR_VERSION > majorVersionReq) {
            return true;
        } else if (MAJOR_VERSION == majorVersionReq) {
            if (MINOR_VERSION > minorVersionReq) {
                return true;
            } else {
                return PATCH_VERSION >= patchVersionReq;
            }
        } else {
            return false;
        }
    }

    public static int getMinorVersion(String version) {
        try {
            if (version == null) {
                return FALLBACK_MINOR_VERSION;
            }
            if (version.startsWith("1.")) {
                int lastDot = version.lastIndexOf('.');
                if (version.indexOf('.') != lastDot) {
                    String minorStr = version.substring(lastDot + 1).replaceAll("[^0-9].*", "");
                    return Integer.parseInt(minorStr);
                } else {
                    return 0;
                }
            } else {
                int firstDot = version.indexOf(".");
                int lastDot = version.lastIndexOf(".");
                String minorStr;
                if (firstDot != -1 && firstDot != lastDot) {
                    minorStr = version.substring(firstDot + 1, lastDot);
                } else if (firstDot != -1) {
                    minorStr = version.substring(firstDot + 1);
                } else {
                    return FALLBACK_MINOR_VERSION;
                }
                minorStr = minorStr.replaceAll("[^0-9].*", "");
                if (!minorStr.isEmpty()) {
                    return Integer.parseInt(minorStr);
                }
            }
        } catch (Exception ignored) {
        }
        return FALLBACK_MINOR_VERSION;
    }

    public static int getMajorVersion(String version) {
        try {
            if (version == null) {
                return FALLBACK_MAJOR_VERSION;
            }
            int firstDot = version.indexOf(".");
            if (firstDot == -1) {
                String digits = version.replaceAll("[^0-9].*", "");
                if (!digits.isEmpty()) {
                    return Integer.parseInt(digits);
                }
                return FALLBACK_MAJOR_VERSION;
            }
            if (version.startsWith("1.")) {
                int lastDot = version.lastIndexOf(".");
                if (firstDot != lastDot) { // x.x.x format
                    String sub = version.substring(firstDot + 1, lastDot);
                    if (sub.contains(".")) { // In case there are extra dots at the end
                        sub = sub.substring(0, sub.indexOf("."));
                    }
                    sub = sub.replaceAll("[^0-9].*", "");
                    return Integer.parseInt(sub);
                } else { // x.x format
                    String sub = version.substring(firstDot + 1).replaceAll("[^0-9].*", "");
                    return Integer.parseInt(sub);
                }
            } else { // New version system since 26.1
                String sub = version.substring(0, firstDot).replaceAll("^[^0-9]+", "").replaceAll("[^0-9].*", "");
                return Integer.parseInt(sub);
            }
        } catch (Exception ignored) {
        }
        return FALLBACK_MAJOR_VERSION;
    }

    public static int getPatchVersion(String version) {
        if (version == null || version.startsWith("1.")) {
            return 0;
        }
        try {
            int firstDot = version.indexOf(".");
            int lastDot = version.lastIndexOf(".");
            if (firstDot != -1 && firstDot != lastDot) {
                String patchStr = version.substring(lastDot + 1).replaceAll("[^0-9].*", "");
                if (!patchStr.isEmpty()) {
                    return Integer.parseInt(patchStr);
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    public static String getDetectedVersion() {
        try {
            Method method = Bukkit.class.getMethod("getMinecraftVersion");
            Object result = method.invoke(null);
            if (result instanceof String s && !s.isEmpty()) {
                return s;
            }
        } catch (Throwable ignored) {
        }
        try {
            String bukkitVersion = Bukkit.getBukkitVersion();
            String parsed = getVersionString(bukkitVersion);
            if (parsed != null && !parsed.isEmpty()) {
                return parsed;
            }
        } catch (Throwable ignored) {
        }
        try {
            return getVersionString(Bukkit.getVersion());
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static String getVersionString(@Nullable String version) {
        if (version == null || version.isEmpty()) {
            return null;
        }

        // getVersion()
        int index = version.lastIndexOf("MC:");
        if (index != -1) {
            version = version.substring(index + 4);
            int closingParen = version.indexOf(')');
            if (closingParen != -1) {
                version = version.substring(0, closingParen);
            }
        } else if (version.contains(".build.")) {
            int buildIndex = version.indexOf(".build.");
            version = version.substring(0, buildIndex);
        } else if (version.contains("-")) {
            // getBukkitVersion() e.g. 26.3-R0.1-SNAPSHOT, 26.3.0-stable, 26.3-SNAPSHOT
            int dashIndex = version.indexOf('-');
            version = version.substring(0, dashIndex);
        }

        version = version.split(" ")[0].trim(); // Remove extra words

        return version;
    }
}
