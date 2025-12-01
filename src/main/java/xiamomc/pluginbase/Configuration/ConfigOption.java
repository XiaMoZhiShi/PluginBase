package xiamomc.pluginbase.Configuration;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ConfigOption<T>
{
    private final ConfigNode node;
    private final T defaultValue;
    private final List<String> flags = new ObjectArrayList<>();
    private final Class<T> type;

    public Class<T> type()
    {
        return type;
    }

    public ConfigNode node()
    {
        return node;
    }

    @NotNull
    public T getDefault()
    {
        return defaultValue;
    }

    public ConfigOption(ConfigNode node, Class<T> type, T defaultValue)
    {
        this.node = node;
        this.type = type;
        this.defaultValue = defaultValue;
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

    @Override
    public String toString()
    {
        return node.toString();
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

    public static <X> ConfigOptionBuilder<X> builder()
    {
        return new ConfigOptionBuilder<>();
    }

    public static <X> ConfigOptionBuilder<X> builder(Class<X> type)
    {
        return new ConfigOptionBuilder<X>().type(type);
    }

    public static class ConfigOptionBuilder<T>
    {
        private ConfigNode node;

        private boolean typeAssigned;

        @Nullable
        private Class<T> type;

        private T defaultValue;

        private boolean excludeFromInit;

        public ConfigOptionBuilder<T> node(ConfigNode node)
        {
            this.node = node;
            return this;
        }

        public ConfigOptionBuilder<T> defaultValue(T defaultValue)
        {
            if (!typeAssigned)
                type = (Class<T>) defaultValue.getClass();

            this.defaultValue = defaultValue;
            return this;
        }

        public ConfigOptionBuilder<T> type(Class<T> type)
        {
            this.typeAssigned = true;
            this.type = type;
            return this;
        }

        public ConfigOptionBuilder<T> excludeFromInit(boolean excludeFromInit)
        {
            this.excludeFromInit = excludeFromInit;
            return this;
        }

        public ConfigOption<T> build()
        {
            ConfigOption<T> option = new ConfigOption<>(node, type, defaultValue);

            if (excludeFromInit)
                option.setExcludeFromInit();

            return option;
        }
    }
}
