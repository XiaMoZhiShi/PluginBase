package xiamomc.pluginbase.messages;

import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.XiaMoJavaPlugin;

/**
 * @deprecated 转移到了 {@link xiamomc.pluginbase.Messages.FormattableMessage}
 */
@Deprecated
public class FormattableMessage extends xiamomc.pluginbase.Messages.FormattableMessage
{
    public FormattableMessage(@NotNull String pluginNameSpace, @NotNull String key, @NotNull String defaultString) {
        super(pluginNameSpace, key, defaultString);
    }

    public FormattableMessage(@NotNull XiaMoJavaPlugin owningPlugin, @NotNull String key, @NotNull String defaultString)
    {
        this(owningPlugin.getNameSpace(), key, defaultString);
    }

    public FormattableMessage(@NotNull XiaMoJavaPlugin owningPlugin, String value)
    {
        this(owningPlugin.getNameSpace(), "_", value);
    }

}
