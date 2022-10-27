package xiamomc.pluginbase.messages;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.JsonBasedStorage;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class MessageStore<T extends XiaMoJavaPlugin> extends JsonBasedStorage<Map<String, String>, T>
{
    public String get(String key, String defaultValue)
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
        //todo: 扫描整个messages下继承IStrings的对象，而不是用List.of暴力枚举
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
        catch (Exception e)
        {
            logger.warn(e.getMessage());
            e.printStackTrace();
        }
    }
}
