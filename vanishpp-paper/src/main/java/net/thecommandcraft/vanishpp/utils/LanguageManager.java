package net.thecommandcraft.vanishpp.utils;

import net.thecommandcraft.vanishpp.Vanishpp;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageManager {
    private final Vanishpp plugin;
    private final Map<String, String> messages = new HashMap<>();
    /** Raw config of each language file type, used by getStringList for cross-file list lookups. */
    private final Map<String, YamlConfiguration> typeConfigs = new HashMap<>();
    private String currentLang;

    // GUI text lives in the messages files; only messages and the separate scoreboards
    // files are loaded. (The old gui_*.yml files were deleted when their keys were
    // integrated into messages_*.yml.)
    private static final String[] FILE_TYPES = {"messages", "scoreboards"};

    public LanguageManager(Vanishpp plugin) {
        this.plugin = plugin;
    }

    /** Default (en-us) string table, kept as a per-key fallback when the active language is missing a key. */
    private final Map<String, String> fallbackMessages = new HashMap<>();
    /** Raw config of the bundled en-us files, used as the source for self-healing missing keys. */
    private final Map<String, YamlConfiguration> defaultTypeConfigs = new HashMap<>();

    public void load() {
        String lang = plugin.getConfigManager().getLanguage();
        loadLanguage(lang);
    }

    private void loadLanguage(String lang) {
        messages.clear();
        typeConfigs.clear();
        fallbackMessages.clear();
        defaultTypeConfigs.clear();
        currentLang = lang;
        int loaded = 0;

        // 1. Load the bundled en-us files as the fallback/default source. These always
        //    exist in the jar, so they provide both the en-us per-key fallback and the
        //    definitive set of keys for self-healing an out-of-date user file.
        for (String type : FILE_TYPES) {
            YamlConfiguration defaults = loadFromJar(type, "en-us");
            if (defaults != null) {
                defaultTypeConfigs.put(type, defaults);
                for (String key : defaults.getKeys(true)) {
                    if (defaults.isString(key)) {
                        String flatKey = stripTypeWrapper(key, type);
                        fallbackMessages.put(type + "." + flatKey, defaults.getString(key));
                    }
                }
            }
        }

        // 2. Load the active-language files (user copy first, then jar), self-healing any
        //    keys the user's file is missing by writing the bundled default back to disk.
        for (String type : FILE_TYPES) {
            YamlConfiguration config = loadRawAndSelfHeal(type, lang);
            if (config == null) {
                continue;
            }
            typeConfigs.put(type, config);

            for (String key : config.getKeys(true)) {
                if (config.isString(key)) {
                    String flatKey = stripTypeWrapper(key, type);
                    messages.put(type + "." + flatKey, config.getString(key));
                }
            }
            loaded += config.getKeys(true).size();
        }
        plugin.getLogger().info("Loaded " + loaded + " messages for language: " + lang);
    }

    /**
     * Loads the active-language file for a type, returning the raw config. Unlike the old
     * loader this also sails through the self-heal step: any string key present in the
     * bundled en-us default but missing from the active language file is filled in with the
     * en-us value and (when the file lives on disk) persisted back. This restores the
     * original behavior where a plugin update that adds keys just worked for existing installs.
     */
    private YamlConfiguration loadRawAndSelfHeal(String type, String lang) {
        if ("en-us".equalsIgnoreCase(lang)) {
            // en-us itself may have a customized on-disk copy missing newer keys — heal it too.
            File disk = findOnDisk(type, lang);
            if (disk != null) {
                YamlConfiguration user = YamlConfiguration.loadConfiguration(disk);
                boolean changed = selfHeal(user, type);
                if (changed) {
                    saveConfiguration(user, disk);
                }
                return user;
            }
            // No customized copy — the bundled en-us is authoritative and already complete.
            return loadFromJar(type, lang);
        }

        // 1. User-customized file on disk (self-heal and persist)
        File disk = findOnDisk(type, lang);
        if (disk != null) {
            YamlConfiguration user = YamlConfiguration.loadConfiguration(disk);
            boolean changed = selfHeal(user, type);
            if (changed) {
                saveConfiguration(user, disk);
            }
            return user;
        }

        // 2. No user file — extract the bundled active-language file to disk (if present) so
        //    the user gets a matching on-disk file, then self-heal it too.
        YamlConfiguration jar = loadFromJar(type, lang);
        if (jar != null) {
            File target = new File(plugin.getDataFolder(),
                    "languages/" + type + "_" + lang + ".yml");
            boolean extracted = true;
            try {
                if (!target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
                    extracted = false;
                }
                if (extracted) {
                    saveConfiguration(jar, target);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not write language file " + target.getName() + ": " + e.getMessage());
            }
            return extracted ? loadFromJar(type, lang) : jar;
        }
        return null;
    }

    /**
     * Fills any string key present in the bundled en-us default but missing from the given
     * config, using the en-us value. Returns true if anything was added. List keys are left
     * untouched (best-effort: only top-level string defaults under the type are considered
     * for the scoreboards wrapper; suite always exists for messages).
     */
    private boolean selfHeal(YamlConfiguration config, String type) {
        YamlConfiguration defaults = defaultTypeConfigs.get(type);
        if (defaults == null) {
            return false;
        }
        boolean changed = false;
        // The bundled defaults carry top-level keys (messages: flat; scoreboards: under "scoreboards").
        for (String key : defaults.getKeys(true)) {
            if (!defaults.isString(key)) {
                continue;
            }
            // Map the default's key space to what the user file expects.
            String userKey = mapKeyToUserSpace(key, type);
            if (!config.contains(userKey) || !config.isString(userKey)) {
                config.set(userKey, defaults.getString(key));
                changed = true;
            }
        }
        return changed;
    }

    /** Maps a bundled-default key to the key space of a user file of the same type. */
    private String mapKeyToUserSpace(String key, String type) {
        String flat = stripTypeWrapper(key, type);
        if ("scoreboards".equals(type)) {
            // scoreboards files are wrapped under a "scoreboards:" top level
            return "scoreboards." + flat;
        }
        return flat; // messages files are flat
    }

    /** Persists a YamlConfiguration back to disk, creating parent dirs as needed. */
    private void saveConfiguration(YamlConfiguration config, File target) {
        try {
            if (!target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
                return;
            }
            config.save(target);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not self-heal language file " + target.getName() + ": " + e.getMessage());
        }
    }

    /** Looks up an on-disk language file for a type/lang, tolerating - and _ separators. */
    private File findOnDisk(String type, String lang) {
        String fileName = type + "_" + lang + ".yml";
        for (String candidate : fileNameVariants(fileName)) {
            File langFile = new File(plugin.getDataFolder(), "languages/" + candidate);
            if (langFile.exists()) {
                return langFile;
            }
        }
        return null;
    }

    /** Loads a language file from the jar resources only, returning null if absent. */
    private YamlConfiguration loadFromJar(String type, String lang) {
        String fileName = type + "_" + lang + ".yml";
        for (String candidate : fileNameVariants(fileName)) {
            InputStream in = plugin.getResource("languages/" + candidate);
            if (in != null) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        }
        return null;
    }

    /** Generates both "-" and "_" separated candidates for a file name, covering the historical gui_en-us / gui_en_us naming difference. */
    private String[] fileNameVariants(String fileName) {
        if (fileName.contains("_")) {
            return new String[]{fileName, fileName.replace("_", "-")};
        }
        return new String[]{fileName};
    }

    /** Normalizes a raw key from a file: if it starts with "type." (the file wraps its keys under a same-named top level), strip that wrapper. */
    private String stripTypeWrapper(String key, String type) {
        String prefix = type + ".";
        if (key.startsWith(prefix)) {
            return key.substring(prefix.length());
        }
        return key;
    }

    public String getMessage(String key) {
        String msg = messages.get(key);
        // Resolve into the messages namespace:
        //  - flat keys ("config.reloaded", "console-specify") → "messages.config.reloaded"
        //  - GUI keys ("gui.admin-dashboard.title") → "messages.gui.admin-dashboard.title"
        //  - already-prefixed keys ("messages.x", "scoreboards.x") hit directly
        if (msg == null && !key.startsWith("messages.") && !key.startsWith("scoreboards.")) {
            msg = messages.get("messages." + key);
        }
        if (msg == null) {
            // Fall back to the bundled en-us value for this key before showing a missing-key
            // marker, so translations that have not caught up still render something useful.
            String fallbackKey = key;
            if (!key.startsWith("messages.") && !key.startsWith("scoreboards.")) {
                fallbackKey = "messages." + key;
            }
            msg = fallbackMessages.get(fallbackKey);
            // Try flat and scoreboard-typed spellings too.
            if (msg == null && !key.startsWith("messages.") && !key.startsWith("scoreboards.")) {
                msg = fallbackMessages.get("scoreboards." + key);
            }
            if (msg == null) {
                plugin.getLogger().warning("Missing message key: " + key);
                return "<red>[Missing: " + key + "]";
            }
        }
        return msg;
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String msg = getMessage(key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return msg;
    }

    public String getMessage(String key, Object... placeholders) {
        String msg = getMessage(key);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            if (placeholders[i] instanceof String) {
                msg = msg.replace("%" + placeholders[i] + "%", String.valueOf(placeholders[i + 1]));
            }
        }
        return msg;
    }

    /**
     * Gets a string list. List keys may live in different type files: flat list keys in the
     * messages file (e.g. "changelog.entries") and list keys in the scoreboards file
     * (e.g. "scoreboards.lines"). All loaded type files are searched in turn.
     */
    public List<String> getStringList(String key) {
        for (YamlConfiguration config : typeConfigs.values()) {
            if (config.isList(key)) {
                return config.getStringList(key);
            }
        }
        // Handle scoreboards files wrapped under a "scoreboards:" top level
        if (!key.startsWith("scoreboards.")) {
            YamlConfiguration sb = typeConfigs.get("scoreboards");
            if (sb != null && sb.isList("scoreboards." + key)) {
                return sb.getStringList("scoreboards." + key);
            }
        }
        plugin.getLogger().warning("Missing string list key: " + key);
        return java.util.Collections.emptyList();
    }

    public String getCurrentLanguage() {
        return currentLang;
    }
}