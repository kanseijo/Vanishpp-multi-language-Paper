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
    /** 每种语言文件的原始配置，用于 getStringList 跨文件查找列表键。 */
    private final Map<String, YamlConfiguration> typeConfigs = new HashMap<>();
    private String currentLang;

    private static final String[] FILE_TYPES = {"gui", "messages", "scoreboards"};

    public LanguageManager(Vanishpp plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String lang = plugin.getConfigManager().getLanguage();
        loadLanguage(lang);
    }

    private void loadLanguage(String lang) {
        messages.clear();
        typeConfigs.clear();
        currentLang = lang;
        int loaded = 0;

        for (String type : FILE_TYPES) {
            YamlConfiguration config = loadRaw(type, lang);
            if (config == null) {
                continue;
            }
            typeConfigs.put(type, config);

            // 将配置键统一到 "type.扁平键" 命名空间，并剥离文件内多余的顶层包装（如 scoreboards 文件自带 "scoreboards:" 顶层）。
            // 这样调用方无论使用扁平键（"config.reloaded"）还是带类型前缀的键（"messages.config.reloaded"、
            // "scoreboards.title"、"gui.admin.title"）都能命中。列表键由 getStringList 处理，不进入字符串表。
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
     * 加载某类型的一个语言文件（先按语言名，回退到 en-us）。返回 null 表示该类型无法加载。
     * 兼容语言代码分隔符差异（如 "en-us" 与 "en_us"），保证文件名无论在插件数据文件夹还是 jar 内都能命中。
     */
    private YamlConfiguration loadRaw(String type, String lang) {
        String fileName = type + "_" + lang + ".yml";
        // 1. 插件数据文件夹（用户自定义优先）
        for (String candidate : fileNameVariants(fileName)) {
            File langFile = new File(plugin.getDataFolder(), "languages/" + candidate);
            if (langFile.exists()) {
                return YamlConfiguration.loadConfiguration(langFile);
            }
        }
        // 2. jar 内 resources
        for (String candidate : fileNameVariants(fileName)) {
            InputStream in = plugin.getResource("languages/" + candidate);
            if (in != null) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        }
        return null;
    }

    /** 根据一个文件名生成带连字符与下划线两种分隔的候选名，兼容历史上 gui_en-us / gui_en_us 的命名差异。 */
    private String[] fileNameVariants(String fileName) {
        if (fileName.contains("_")) {
            return new String[]{fileName, fileName.replace("_", "-")};
        }
        return new String[]{fileName};
    }

    /** 将文件内原始键规范化：若键以 "type." 引导（文件顶层自带了与类型同名的包装），剥掉它。 */
    private String stripTypeWrapper(String key, String type) {
        String prefix = type + ".";
        if (key.startsWith(prefix)) {
            return key.substring(prefix.length());
        }
        return key;
    }

    public String getMessage(String key) {
        String msg = messages.get(key);
        // 兼容编程中直接使用扁平 messages 键（如 "config.reloaded"、"console-specify"、"no-permission"）
        // 与带类型前缀的键（"messages.config.reloaded"、"gui.admin.title"、"scoreboards.title"）。
        if (msg == null && !key.startsWith("messages.") && !key.startsWith("gui.") && !key.startsWith("scoreboards.")) {
            msg = messages.get("messages." + key);
        }
        if (msg == null) {
            plugin.getLogger().warning("Missing message key: " + key);
            return "<red>[Missing: " + key + "]";
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
     * 获取字符串列表。列表键可能位于不同的类型文件中：messages 文件中的扁平列表键
     * （如 "changelog.entries"）以及 scoreboards 文件中的列表键（如 "scoreboards.lines"）。
     * 因此会依次在所有已加载的类型文件中查找。
     */
    public List<String> getStringList(String key) {
        for (YamlConfiguration config : typeConfigs.values()) {
            if (config.isList(key)) {
                return config.getStringList(key);
            }
        }
        // 兼容评分板文件带 "scoreboards:" 顶层包装的情况
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