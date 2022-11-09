package xiamomc.pluginbase.Configuration;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PluginConfigManager implements IConfigManager
{
    protected FileConfiguration backendConfig;
    protected final XiaMoJavaPlugin plugin;

    public PluginConfigManager(XiaMoJavaPlugin plugin)
    {
        this.plugin = plugin;
        plugin.saveDefaultConfig();

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

    private final Map<String, Bindable<?>> stringConfigNodeMap = new Object2ObjectOpenHashMap<>();

    @Override
    public <T> Bindable<T> getBindable(Class<T> type, ConfigNode path)
    {
        return getBindable(type, path, null);
    }

    @Override
    public <T> Bindable<T> getBindable(Class<T> type, ConfigNode path, T defaultValue)
    {
        var bindable = stringConfigNodeMap.get(path.toString());

        if (bindable != null)
        {
            if (type.isInstance(bindable.get()))
                return (Bindable<T>) bindable;
        }

        var newBindable = new Bindable<>(getOrDefault(type, path, defaultValue));
        stringConfigNodeMap.put(path.toString(), newBindable);

        newBindable.onValueChanged((o, n) -> this.set(path, n, true));

        return newBindable;
    }

    public <T> void bind(Bindable<T> bindable, ConfigNode node, Class<T> type)
    {
        var bb = this.getBindable(type, node);

        bindable.bindTo(bb);
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

    private final Map<ConfigNode, Object> emptyMap = new HashMap<>();

    public Map<ConfigNode, Object> getAllNotDefault()
    {
        return emptyMap;
    }

    private <T> boolean set(ConfigNode node, T value, boolean isInternal)
    {
        //spigot的配置管理器没有返回值
        backendConfig.set(node.toString(), value);
        save();

        if (!isInternal)
        {
            if (value != null)
            {
                var bindable = (Bindable<T>) getBindable(value.getClass(), node);
                bindable.set(value);
            }
            else
            {
                var bindable = stringConfigNodeMap.get(node.toString());
                bindable.set(null);
            }
        }

        return true;
    }

    @Override
    public <T> boolean set(ConfigNode node, T value)
    {
        return set(node, value, false);
    }

    @Override
    public boolean restoreDefaults()
    {
        //没有返回值+1
        plugin.saveResource("config.yml", true);
        plugin.saveConfig();

        reload();

        return true;
    }

    @Override
    public boolean save()
    {
        plugin.saveConfig();
        return true;
    }

    private static Logger logger = LoggerFactory.getLogger("XiaMoBase");

    @Override
    public void reload()
    {
        plugin.reloadConfig();
        backendConfig = plugin.getConfig();

        for (var c : onRefresh)
            c.accept(null);

        stringConfigNodeMap.forEach((str, bindable) ->
        {
            try
            {
                bindable.setInternal(backendConfig.get(str));
            }
            catch (Throwable t)
            {
                logger.warn("无法为" + str + "的Bindable设置值：" + t.getMessage());
                t.printStackTrace();
            }
        });

        plugin.saveConfig();
    }

    private final ArrayList<Consumer<?>> onRefresh = new ArrayList<>();

    @Override
    @Deprecated
    public boolean onConfigRefresh(Consumer<?> c)
    {
        if (onRefresh.contains(c)) return false;
        onRefresh.add(c);
        return true;
    }

    @Override
    @Deprecated
    public boolean onConfigRefresh(Consumer<?> c, boolean runOnce)
    {
        var ocr = this.onConfigRefresh(c);

        if (!ocr) return false;

        c.accept(null);

        return true;
    }
}
