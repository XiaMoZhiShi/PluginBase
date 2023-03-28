package xiamomc.pluginbase.Messages;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.JsonBasedStorage;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class MessageStore<T extends XiaMoJavaPlugin> extends JsonBasedStorage<Map<String, String>, T>
{
    /**
     * 从此消息存储获取与消息对应的字符串
     * @param key 消息的存储键名
     * @param defaultValue 默认值
     * @param locale locale
     * @return 此消息键对应的字符串
     * @apiNote 默认实现不会考虑locale参数
     */
    public String get(String key, @Nullable String defaultValue, @Nullable String locale)
    {
        var val = storingObject.get(key);

        if (val == null)
        {
            val = defaultValue;
            storingObject.put(key, defaultValue);
        }

        return val;
    }

    @Override
    protected @NotNull String getFileName()
    {
        return "messages.json";
    }

    @Override
    protected @NotNull Map<String, String> createDefault()
    {
        return new Object2ObjectAVLTreeMap<>();
    }

    @Override
    protected @NotNull String getDisplayName()
    {
        return "消息存储";
    }

    @Override
    public boolean reloadConfiguration()
    {
        var val = super.reloadConfiguration();

        addMissingStrings();

        return val;
    }

    protected abstract List<Class<? extends IStrings>> getStrings();

    public void addMissingStrings()
    {
        var classes = getStrings();

        try
        {
            for (var c : classes)
            {
                //获取所有返回FormattableMessage的方法
                var methods = Arrays.stream(c.getMethods()).filter(m -> m.getReturnType().equals(FormattableMessage.class)).toList();

                for (var m : methods)
                {
                    var formattable = (FormattableMessage) m.invoke(null);

                    var key = formattable.getKey();

                    //如果存储里没有此键，添加进去
                    if (!storingObject.containsKey(key))
                        storingObject.put(key, formattable.getDefaultString());

                    //同步到文件
                    saveConfiguration();
                }
            }
        }
        catch (Throwable t)
        {
            logger.warn(t.getMessage());
            t.printStackTrace();
        }
    }
}
