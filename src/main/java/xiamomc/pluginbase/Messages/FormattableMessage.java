package xiamomc.pluginbase.Messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Managers.DependencyContainer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FormattableMessage implements Comparable<FormattableMessage>
{
    private final String key;

    private final String fallbackString;

    private final DependencyContainer depManager = DependencyContainer.GLOBAL;

    public static final String KEY_NO_RESOLVE = "_";

    public FormattableMessage(@NotNull String key, @NotNull String fallbackString)
    {
        this.fallbackString = fallbackString;
        this.key = key;
    }

    public FormattableMessage(String value)
    {
        this(KEY_NO_RESOLVE, value);
    }

    /**
     * 获取消息的Key
     * @return 消息的key
     */
    public String getKey()
    {
        return key;
    }

    /**
     * 获取消息的默认消息
     * @return 默认消息
     */
    public String getFallbackString()
    {
        return fallbackString;
    }

    @NotNull
    private String priorityLocale = "";

    @NotNull
    public String getPriorityLocale()
    {
        return priorityLocale;
    }

    public boolean hasPriorityLocale()
    {
        return !priorityLocale.isBlank();
    }

    public FormattableMessage preferredLocale(@Nullable String locale)
    {
        this.priorityLocale = locale == null ? "" : locale;
        return this;
    }

    private final Map<String, IDynamicResolver> resolvers = new ConcurrentHashMap<>();

    public FormattableMessage resolveIfNotSet(String key, IDynamicResolver resolver)
    {
        if (!resolvers.containsKey(key))
            resolvers.put(key, resolver);

        return this;
    }

    public FormattableMessage resolve(String key, IDynamicResolver resolver)
    {
        resolvers.put(key, resolver);
        return this;
    }

    public FormattableMessage resolve(String key, FormattableMessage other)
    {
        resolvers.put(key, other::createComponent);
        return this;
    }

    public FormattableMessage resolve(String key, String plainText)
    {
        resolvers.put(key, locale -> Component.text(plainText));
        return this;
    }

    public FormattableMessage resolve(String key, Component component)
    {
        resolvers.put(key, locale -> component);
        return this;
    }

    public FormattableMessage resolve(String key, Object obj)
    {
        resolve(key, obj.toString());
        return this;
    }

    private MessageStore<?> cachedStore;

    private MessageStore<?> getCachedStore()
    {
        if (cachedStore == null)
            cachedStore = depManager.get(MessageStore.class);

        return cachedStore;
    };

    /**
     * 从给定的MessageStore转换为Component
     * @param store MessageStore
     * @return 可以显示的Component
     */
    public Component createComponent(@Nullable String preferredLocale, MessageStore<?> store)
    {
        if (store == null) return Component.text(fallbackString);

        String locale = null;

        if (!this.priorityLocale.isBlank())
        {
            locale = this.priorityLocale;
        }
        else
        {
            locale = (preferredLocale == null || preferredLocale.isBlank())
                    ? "en_us"
                    : preferredLocale;
        }

        String msg = key.equals(KEY_NO_RESOLVE)
                     ? fallbackString
                     : store.get(key, fallbackString, locale);

        @Nullable String finalLocale = locale;
        var resolvers = this.resolvers.entrySet().stream()
                .map(entry -> Placeholder.component(entry.getKey(), entry.getValue().resolve(finalLocale)))
                .toList();

        return MiniMessage.miniMessage().deserialize(msg, TagResolver.resolver(resolvers));
    }

    /**
     * 尝试从给定的Class获取MessageStore并转换为Component
     * @param depClass 继承MessageStore的对象的Class
     * @return 可以显示的Component
     */
    public Component createComponent(@Nullable String preferredLocale, Class<? extends MessageStore<?>> depClass)
    {
        return createComponent(preferredLocale, depManager.get(depClass));
    }

    /**
     * 尝试自动转换为Component
     * @return 可以显示的Component
     */
    public Component createComponent(@Nullable String preferredLocale)
    {
        return createComponent(preferredLocale, getCachedStore());
    }

    /**
     * 尝试自动转换为Component
     * @return 可以显示的Component
     */
    public Component createComponent()
    {
        return createComponent(null, getCachedStore());
    }

    private static final PlainTextComponentSerializer plainTextComponentSerializer = PlainTextComponentSerializer.plainText();

    public String createString(@Nullable String preferredLocale)
    {
        return plainTextComponentSerializer.serialize(createComponent(preferredLocale));
    }

    public String createString()
    {
        return createString(null);
    }

    @Override
    public String toString()
    {
        return "FormattableMessage[key=%s, fallbackString=%s]".formatted(this.key, this.fallbackString);
    }

    @Override
    public int compareTo(@NotNull FormattableMessage formattableMessage)
    {
        return this.key.compareTo(formattableMessage.key);
    }

    public static FormattableMessage translatable(String key, String fallback)
    {
        return new FormattableMessage(key, fallback);
    }

    public static FormattableMessage literal(String message)
    {
        return new FormattableMessage(message);
    }
}
