package xiamomc.pluginbase.Utilities;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Managers.DependencyManager;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PluginSoftDependManager implements Listener
{
    //region 实例相关
    private final static Map<String, PluginSoftDependManager> instances = new ConcurrentHashMap<>();

    public static PluginSoftDependManager getInstance(String namespace)
    {
        return instances.get(namespace);
    }

    /**
     * 获取或创建某个插件的依赖管理器
     * @param pluginInstance 插件实例
     * @return 此插件的依赖管理器
     */
    @Contract("null -> null; !null -> !null")
    @Nullable
    public static PluginSoftDependManager getManagerOrCreate(XiaMoJavaPlugin pluginInstance)
    {
        if (pluginInstance == null) return null;

        var depMgr = instances.get(pluginInstance.getNameSpace());
        if (depMgr != null) return depMgr;

        depMgr = new PluginSoftDependManager(pluginInstance);

        return depMgr;
    }

    @Deprecated
    public PluginSoftDependManager(XiaMoJavaPlugin plugin)
    {
        registerPluginInstance(plugin);
    }

    public void registerPluginInstance(XiaMoJavaPlugin plugin)
    {
        if (instances.containsKey(plugin.getNameSpace()))
            throw new RuntimeException("已经有一个SoftDependManager的实例了");

        instances.put(plugin.getNameSpace(), this);
    }

    public void unRegisterPluginInstance(XiaMoJavaPlugin plugin)
    {
        instances.remove(plugin.getNameSpace());
    }
    //endregion 实例相关

    private final Map<String, Consumer<Plugin>> onEnableStrToConsumerMap = new Object2ObjectOpenHashMap<>();
    private final Map<String, Consumer<Plugin>> onDisableStrToConsumerMap = new Object2ObjectOpenHashMap<>();

    public void setHandle(String id, Consumer<Plugin> onEnable, boolean runOnce)
    {
        this.setHandle(id, onEnable, null, runOnce);
    }

    public void setHandle(String id, Consumer<Plugin> onEnable)
    {
        this.setHandle(id, onEnable, null, false);
    }

    public void setHandle(String id, Consumer<Plugin> onEnable, @Nullable Consumer<Plugin> onDisable, boolean runOnce)
    {
        var plugins = runOnce ? Bukkit.getPluginManager().getPlugins() : new Plugin[]{};
        if (onEnable == null)
            this.onEnableStrToConsumerMap.remove(id);
        else
        {
            this.onEnableStrToConsumerMap.put(id, onEnable);

            if (runOnce)
            {
                var plugin = Arrays.stream(plugins).filter(p -> p.getName().equals(id)).findFirst().orElse(null);

                if (plugin != null && plugin.isEnabled())
                    onEnable.accept(plugin);
            }
        }

        if (onDisable == null)
            this.onDisableStrToConsumerMap.remove(id);
        else
        {
            this.onDisableStrToConsumerMap.put(id, onDisable);

            if (runOnce)
            {
                var plugin = Arrays.stream(plugins).filter(p -> p.getName().equals(id)).findFirst().orElse(null);

                if (plugin != null && !plugin.isEnabled())
                    onDisable.accept(plugin);
            }
        }
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent e)
    {
        this.onEnable(e.getPlugin());
    }

    private void onEnable(Plugin plugin)
    {
        var name = plugin.getName();
        var consumer = onEnableStrToConsumerMap.getOrDefault(name, null);

        if (consumer != null)
            consumer.accept(plugin);
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent e)
    {
        this.onDisable(e.getPlugin().getName());
    }

    private void onDisable(String name)
    {
        var consumer = onDisableStrToConsumerMap.getOrDefault(name, null);

        if (consumer != null)
            consumer.accept(null);
    }

    public void clearHandles()
    {
        onDisableStrToConsumerMap.clear();
        onEnableStrToConsumerMap.clear();
    }
}
