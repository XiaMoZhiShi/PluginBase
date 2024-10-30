package xiamomc.pluginbase.Utilities;

import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.nio.charset.StandardCharsets;

public class PluginAssetUtils
{
    /**
     * 通过给定的路径获取资源内容
     * @param path 目标路径
     * @return 原始数据，返回null则未找到或出现异常
     */
    public static <X extends XiaMoJavaPlugin> byte @Nullable [] getFileBytes(X plugin, String path)
    {
        var stream = plugin.getResource(path);

        if (stream == null) return null;

        try
        {
            return stream.readAllBytes();
        }
        catch (Throwable t)
        {
            plugin.getSLF4JLogger().error("Can't read '%s' from plugin file: '%s'".formatted(path, t.getMessage()));
        }

        return null;
    }

    /**
     * 以字符串的形式获取资源内容
     * @param path 资源路径
     * @return 文件资源内容，返回空则未找到或出现异常
     */
    public static <X extends XiaMoJavaPlugin> String getFileStrings(X plugin, String path)
    {
        var bytes = getFileBytes(plugin, path);

        if (bytes == null) return "";

        return new String(bytes, StandardCharsets.UTF_8);
    }
}
