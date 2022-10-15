package xiamomc.pluginbase.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.Managers.DependencyManager;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class FormattableMessage implements Comparable<FormattableMessage>
{
    private final String key;

    private final String defaultString;

    private MessageStore<?> store;

    private MessageStore<?> getStore()
    {
        if (store == null)
            store = DependencyManager.getInstance(nameSpace).Get(MessageStore.class);

        return store;
    };

    private final String nameSpace;

    public FormattableMessage(@NotNull String pluginNameSpace, @NotNull String key, @NotNull String defaultString)
    {
        this.defaultString = defaultString;
        this.key = key;

        this.nameSpace = pluginNameSpace;
    }

    public FormattableMessage(@NotNull XiaMoJavaPlugin owningPlugin, @NotNull String key, @NotNull String defaultString)
    {
        this(owningPlugin.getNameSpace(), key, defaultString);
    }

    public FormattableMessage(@NotNull XiaMoJavaPlugin owningPlugin, String value)
    {
        this(owningPlugin.getNameSpace(), "_", value);
    }

    private final List<TagResolver> resolvers = new ArrayList<>();

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
    public FormattableMessage resolve(String target, FormattableMessage formattable)
    {
        resolvers.add(Placeholder.component(target, formattable.toComponent()));

        return this;
    }

    /**
     * 转换为Component
     * @return 可以显示的Component
     */
    public Component toComponent()
    {
        String msg = key.equals("_") ? defaultString : getStore().get(key, defaultString);

        return MiniMessage.miniMessage().deserialize(msg, TagResolver.resolver(resolvers));
    }

    @Override
    public int compareTo(@NotNull FormattableMessage formattableMessage)
    {
        return this.key.compareTo(formattableMessage.key);
    }
}
