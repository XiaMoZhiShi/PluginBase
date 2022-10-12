package xiamomc.pluginbase.Configuration;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.ArrayList;
import java.util.function.Consumer;

public class PluginConfigManager implements IConfigManager
{
    private FileConfiguration backendConfig;
    private final XiaMoJavaPlugin plugin;

    public PluginConfigManager(XiaMoJavaPlugin plugin)
    {
        this.plugin = plugin;
        this.reload();
    }

    @Override
    @Nullable
    public <T> T get(Class<T> type, ConfigNode node)
    {
        var value = backendConfig.get(node.toString());

        if (value == null)
        {
            return null;
        }

        //检查是否可以cast过去
        if (!type.isAssignableFrom(value.getClass()))
        {
            plugin.getSLF4JLogger().warn("未能将处于" + node + "的配置转换为" + type.getSimpleName());
            return null;
        }

        return (T) value;
    }

    public <T> T getOrDefault(Class<T> type, ConfigNode node, @Nullable T defaultValue)
    {
        var val = get(type, node);

        if (val == null)
        {
            set(node, defaultValue);
            return defaultValue;
        }

        return val;
    }

    @Override
    public boolean set(ConfigNode node, Object value)
    {
        //spigot的配置管理器没有返回值
        backendConfig.set(node.toString(), value);
        plugin.saveConfig();
        return true;
    }

    @Override
    public boolean restoreDefaults()
    {
        //没有返回值+1
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        return true;
    }

    @Override
    public void reload()
    {
        plugin.reloadConfig();
        backendConfig = plugin.getConfig();

        for (var c : onRefresh)
            c.accept(null);

        plugin.saveConfig();
    }

    private final ArrayList<Consumer<?>> onRefresh = new ArrayList<>();

    @Override
    public boolean onConfigRefresh(Consumer<?> c)
    {
        if (onRefresh.contains(c)) return false;
        onRefresh.add(c);
        return true;
    }

    @Override
    public boolean onConfigRefresh(Consumer<?> c, boolean runOnce)
    {
        var ocr = this.onConfigRefresh(c);

        if (!ocr) return false;

        c.accept(null);

        return true;
    }
}
