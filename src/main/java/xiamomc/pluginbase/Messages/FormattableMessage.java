package xiamomc.pluginbase.Messages;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Managers.DependencyManager;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.List;

public class FormattableMessage implements Comparable<FormattableMessage>
{
    private final String key;

    private final String defaultString;

    private final DependencyManager depManager;

    public FormattableMessage(@NotNull String pluginNameSpace, @NotNull String key, @NotNull String defaultString)
    {
        this.defaultString = defaultString;
        this.key = key;

        depManager = DependencyManager.getInstance(pluginNameSpace);
    }

    public FormattableMessage(@NotNull XiaMoJavaPlugin owningPlugin, @NotNull String key, @NotNull String defaultString)
    {
        this(owningPlugin.getNameSpace(), key, defaultString);
    }

    public FormattableMessage(@NotNull XiaMoJavaPlugin owningPlugin, String value)
    {
        this(owningPlugin.getNameSpace(), "_", value);
    }

    private final List<TagResolver> resolvers = new ObjectArrayList<>();

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
    public String getDefaultString()
    {
        return defaultString;
    }

    /**
     * 添加解析
     * @param target 要解析的Key
     * @param value 要解析的值
     * @return 对此对象的引用
     */
    public FormattableMessage resolve(String target, String value)
    {
        resolvers.add(Placeholder.parsed(target, value));

        return this;
    }

    /**
     * 添加解析
     * @param target 要解析的Key
     * @param value 要解析的值
     * @return 对此对象的引用
     */
    public FormattableMessage resolve(String target, Component value)
    {
        resolvers.add(Placeholder.component(target, value));

        return this;
    }

    /**
     * 添加解析
     * @param target 要解析的Key
     * @param formattable 要解析的值
     * @return 对此对象的引用
     */
    public FormattableMessage resolve(String target, FormattableMessage formattable, String locale)
    {
        if (locale == null) locale = this.locale;

        resolvers.add(Placeholder.component(target, formattable.toComponent(locale)));

        return this;
    }

    public FormattableMessage resolve(String target, FormattableMessage formattableMessage)
    {
        return this.resolve(target, formattableMessage, this.locale);
    }

    private String locale = null;

    @Nullable
    public String getLocale()
    {
        return locale;
    }

    public FormattableMessage withLocale(String locale)
    {
        this.locale = locale;

        return this;
    }

    private MessageStore<?> store;

    private MessageStore<?> getStore()
    {
        if (store == null)
            store = depManager.get(MessageStore.class);

        return store;
    };

    /**
     * 从给定的MessageStore转换为Component
     * @param store MessageStore
     * @return 可以显示的Component
     */
    public Component toComponent(@Nullable String locale, MessageStore<?> store)
    {
        if (store == null) return Component.text(defaultString);

        if (locale == null) locale = this.locale;

        String msg = key.equals("_") ? defaultString : store.get(key, defaultString, locale);

        return MiniMessage.miniMessage().deserialize(msg, TagResolver.resolver(resolvers));
    }

    /**
     * 尝试从给定的Class获取MessageStore并转换为Component
     * @param depClass 继承MessageStore的对象的Class
     * @return 可以显示的Component
     */
    public Component toComponent(@Nullable String locale, Class<? extends MessageStore<?>> depClass)
    {
        return toComponent(locale, depManager.get(depClass));
    }

    /**
     * 尝试自动转换为Component
     * @return 可以显示的Component
     */
    public Component toComponent(@Nullable String locale)
    {
        return toComponent(locale, getStore());
    }

    @Override
    public int compareTo(@NotNull FormattableMessage formattableMessage)
    {
        return this.key.compareTo(formattableMessage.key);
    }
}
