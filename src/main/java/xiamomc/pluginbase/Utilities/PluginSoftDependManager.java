package xiamomc.pluginbase.Utilities;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.XiaMoJavaPlugin;

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

    public PluginSoftDependManager(XiaMoJavaPlugin plugin)
    {
        registerPluginInstance(plugin);

        plugin.schedule(() ->
        {
            for (Plugin pl : Bukkit.getPluginManager().getPlugins())
                this.onEnable(pl.getName());
        });
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

    private final Map<String, Consumer<?>> onEnableStrToConsumerMap = new Object2ObjectOpenHashMap<>();
    private final Map<String, Consumer<?>> onDisableStrToConsumerMap = new Object2ObjectOpenHashMap<>();

    public void setHandle(String id, Consumer<?> onEnable)
    {
        this.setHandle(id, onEnable, null);
    }

    public void setHandle(String id, Consumer<?> onEnable, @Nullable Consumer<?> onDisable)
    {
        if (onEnable == null)
            this.onEnableStrToConsumerMap.remove(id);
        else
            this.onEnableStrToConsumerMap.put(id, onEnable);

        if (onDisable == null)
            this.onEnableStrToConsumerMap.remove(id);
        else
            this.onDisableStrToConsumerMap.put(id, onDisable);
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent e)
    {
        this.onEnable(e.getPlugin().getName());
    }

    private void onEnable(String name)
    {
        var consumer = onEnableStrToConsumerMap.getOrDefault(name, null);

        if (consumer != null)
            consumer.accept(null);
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
}
