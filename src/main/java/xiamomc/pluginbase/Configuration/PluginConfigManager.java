package xiamomc.pluginbase.Configuration;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.pluginbase.Bindables.BindableList;
import xiamomc.pluginbase.Utilities.ConfigSerializeUtils;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.*;
import java.util.function.Consumer;

public class PluginConfigManager implements IConfigManager
{
    private static final Logger logger = LoggerFactory.getLogger(PluginConfigManager.class);
    protected FileConfiguration backendConfig;
    protected final XiaMoJavaPlugin plugin;

    public PluginConfigManager(XiaMoJavaPlugin plugin)
    {
        this.plugin = plugin;
        plugin.saveDefaultConfig();

        this.reload();
    }

    //region get and get bindable methods

    public <T> T get(ConfigOption<T> option)
    {
        Class<?> clazz = option.getDefault().getClass();

        if (option.getDefault() instanceof List<?>)
            clazz = List.class;

        return get((Class<? extends T>) clazz, option.node());
    }

    @Override
    @Nullable
    public <T> T get(Class<T> type, ConfigNode node)
    {
        if (type == Object.class)
            logger.warn("[PluginBase] Trying to get an object instance from a node, this is bad!");

        var value = backendConfig.get(node.toString(), null);

        if (value == null)
            return null;

        //检查是否可以cast过去
        if (!type.isAssignableFrom(value.getClass()))
        {
            var numClass = Number.class;
            if (numClass.isAssignableFrom(type) && value instanceof Number numVal)
            {
                var num = ConfigSerializeUtils.convertNumber(type, numVal, true);
                if (num != null) return (T) num;
            }

            logger.warn("Unable to convert value under node '%s' from '%s' to '%s'"
                    .formatted(node, value.getClass().getSimpleName(), type.getSimpleName()));

            Thread.dumpStack();

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

    public <T> T getOrDefault(ConfigOption<T> option)
    {
        var val = get(option);

        if (val == null)
        {
            var defaultVal = option.getDefault();
            set(option, defaultVal);
            return defaultVal;
        }

        return val;
    }

    public <T> T getOrDefault(ConfigOption<T> option, @Nullable T defaultValue)
    {
        var val = get(option);
        return val == null ? defaultValue : val;
    }

    @NotNull
    public Map<ConfigNode, Object> getAllNotDefault()
    {
        return emptyMap;
    }

    @Override
    public <T> Bindable<T> getBindable(Class<T> type, ConfigNode path, T defaultValue)
    {
        var bindable = stringConfigNodeMap.getOrDefault(path.toString(), null);

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

    @Override
    public <T> Bindable<T> getBindable(Class<T> type, ConfigNode path)
    {
        return getBindable(type, path, null);
    }

    public <T> Bindable<T> getBindable(ConfigOption<T> option)
    {
        return getBindable(option, option.getDefault());
    }

    public <T> Bindable<T> getBindable(ConfigOption<T> path, T defaultValue)
    {
        return getBindable((Class<T>) path.getDefault().getClass(), path.node(), defaultValue);
    }

    private Map<String, BindableList<?>> bindableLists;

    private void ensureBindableListNotNull()
    {
        if (bindableLists == null)
            bindableLists = new Object2ObjectOpenHashMap<>();
    }

    public <T> BindableList<T> getBindableList(Class<T> elementClass, ConfigOption<List<T>> option)
    {
        ensureBindableListNotNull();

        //System.out.println("GET LIST " + option.toString());

        var val = bindableLists.getOrDefault(option.node().toString(), null);
        if (val != null)
        {
            //System.out.println("FIND EXISTING LIST, RETURNING " + val);
            return (BindableList<T>) val;
        }

        List<?> originalList = backendConfig.getList(option.node().toString(), new ArrayList<T>());

        originalList.removeIf(listVal -> !elementClass.isInstance(listVal)); //Don't work for somehow

        var list = new BindableList<T>();
        list.addAll((List<T>)originalList);

        list.onListChanged((diffList, reason) ->
        {
            //System.out.println("LIST CHANGED: " + diffList + " WITH REASON " + reason);
            backendConfig.set(option.node().toString(), new ArrayList<>(list));
            save();
        }, true);

        bindableLists.put(option.node().toString(), list);

        //System.out.println("RETURN " + list);

        return list;
    }

    //endregion

    //region bind bindable

    public <T> void bind(Bindable<T> bindable, ConfigNode node, Class<T> type)
    {
        var bb = this.getBindable(type, node);

        bindable.bindTo(bb);
    }

    public <T> void bind(Class<T> elementClass, BindableList<T> bindable, ConfigOption<List<T>> option)
    {
        var list = this.getBindableList(elementClass, option);
        bindable.bindTo(list);
    }

    public <T> void bind(Bindable<T> bindable, ConfigOption<T> option)
    {
        var bb = this.getBindable(option);

        if (bindable.getClass().isInstance(bb))
            bindable.bindTo(bb);
        else
            throw new IllegalArgumentException("尝试将一个Bindable绑定在不兼容的配置(" + option + ")上");
    }

    public <T> void bind(BindableList<T> bindable, ConfigOption<List<T>> option, Class<T> elementClass)
    {
        var bb = this.getBindableList(elementClass, option);

        if (bindable.getClass().isInstance(bb))
            bindable.bindTo(bb);
        else
            throw new IllegalArgumentException("尝试将一个Bindable绑定在不兼容的配置(" + option + ")上");
    }

    //endregion

    //region set methods

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
                var bindable = stringConfigNodeMap.getOrDefault(node.toString(), null);

                if (bindable != null)
                    bindable.set(null);
                else
                    logger.warn("No bindable found for node '%s', is it still in the config file?".formatted(node));
            }
        }

        return true;
    }

    @Override
    public <T> boolean set(ConfigNode node, T value)
    {
        return set(node, value, false);
    }

    public <T> void set(ConfigOption<T> option, T val)
    {
        this.set(option.node(), val);
    }

    //endregion

    private final Map<String, Bindable<?>> stringConfigNodeMap = new Object2ObjectOpenHashMap<>();

    private final Map<ConfigNode, Object> emptyMap = new HashMap<>();

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

    @Override
    public void reload()
    {
        // First, reload backend config
        plugin.reloadConfig();
        backendConfig = plugin.getConfig();

        // Ensure bindableLists is not null
        ensureBindableListNotNull();

        // Update values
        stringConfigNodeMap.forEach((str, bindable) ->
        {
            try
            {
                // Get raw value from the backend
                Object valRaw = backendConfig.get(str);

                // Then try cast
                if (valRaw instanceof Number numVal)
                    ConfigSerializeUtils.tryCastNumberBindable(bindable, numVal);
                else
                    bindable.setInternal(bindable.tryCast(valRaw));
            }
            catch (Throwable t)
            {
                logger.warn("[PluginBase] Unable to set value for Bindable bind to config node %s: %s".formatted(str, t.getMessage()));
                t.printStackTrace();
            }
        });

        // Then, update bindable lists
        bindableLists.forEach((node, list) ->
        {
            var configList = backendConfig.getList(node);

            configList = configList == null
                    ? new ObjectArrayList<>()
                    : new ObjectArrayList<>(configList);

            list.clear();
            list.addAllInternal(configList);
        });

        // Run all onRefresh hooks
        for (var c : onRefresh)
        {
            try
            {
                c.accept(null);
            }
            catch (Throwable t)
            {
                logger.warn("Exception thrown while calling one of the onRefresh hooks: " + t.getMessage());
                t.printStackTrace();
            }
        }

        // Finally, save the configuration
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
