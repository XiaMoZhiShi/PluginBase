package xiamomc.pluginbase.Configuration;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.function.Function;

public class ConfigOption<T>
{
    private final ConfigNode node;
    private final Function<Object, T> defaultValueFunc;
    private final List<String> flags = new ObjectArrayList<>();

    public ConfigNode node()
    {
        return node;
    }

    public T getDefault()
    {
        return defaultValueFunc.apply(null);
    }

    public ConfigOption(ConfigNode node, T defaultValue)
    {
        this(node, (o) -> defaultValue);
    }

    /**
     * @param node The binding ConfigNode
     * @param defaultValueFunc A function that returns the default value, the input to this function will always be null
     */
    public ConfigOption(ConfigNode node, Function<Object, T> defaultValueFunc)
    {
        this.node = node;
        this.defaultValueFunc = defaultValueFunc;
    }

    public ConfigOption<T> withFlag(String flag)
    {
        if (containsFlag(flag))
            return this;

        flags.add(flag);
        return this;
    }

    public boolean containsFlag(String flag)
    {
        return flags.stream().anyMatch(f -> f.equalsIgnoreCase(flag));
    }

    //region Utilities

    public ConfigOption<T> setExcludeFromInit()
    {
        return this.withFlag("exclude_from_init");
    }

    public boolean excludeFromInit()
    {
        return containsFlag("exclude_from_init");
    }

    //endregion
}
