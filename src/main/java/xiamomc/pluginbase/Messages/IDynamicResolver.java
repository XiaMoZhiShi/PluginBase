package xiamomc.pluginbase.Messages;

import net.kyori.adventure.text.Component;

@FunctionalInterface
public interface IDynamicResolver
{
    Component resolve(String preferredLocale);
}
