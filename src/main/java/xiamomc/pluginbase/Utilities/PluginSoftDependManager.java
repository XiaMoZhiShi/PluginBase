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
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PluginSoftDependManager implements Listener
{
    public static final PluginSoftDependManager INSTANCE = new PluginSoftDependManager();

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
