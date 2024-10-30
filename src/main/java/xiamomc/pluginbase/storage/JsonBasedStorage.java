package xiamomc.pluginbase.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.PluginObject;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class JsonBasedStorage<T, P extends XiaMoJavaPlugin> extends PluginObject<P>
{
    protected File configurationFile;

    private static final Gson defaultGson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Gson gson;

    protected Gson createGson()
    {
        return defaultGson;
    }

    protected JsonBasedStorage()
    {
        gson = createGson();

        storingObject = createDefault();
    }

    protected boolean storageInitialized;

    public void initializeStorage()
    {
        this.initializeStorage(false);
    }

    public void initializeStorage(boolean noReload)
    {
        try
        {
            //初始化配置文件
            if (configurationFile == null)
                configurationFile = new File(URI.create(plugin.getDataFolder().toURI() + "/" + getFileName()));

            if (!configurationFile.exists())
            {
                //创建父目录
                if (!configurationFile.getParentFile().exists())
                    Files.createDirectories(Paths.get(configurationFile.getParentFile().toURI()));

                if (!configurationFile.createNewFile())
                {
                    logger.error("Unable to create file '%s', not loading storage data!".formatted(getFileName()));
                    return;
                }
            }
        }
        catch (Throwable t)
        {
            logger.warn("未能初始化" + getDisplayName() + "的JSON配置：" + t.getMessage());
            t.printStackTrace();
        }

        if (!noReload)
            reloadConfiguration();

        storageInitialized = true;
    }

    @Initializer
    private void load()
    {
        if (!storageInitialized)
            initializeStorage();
    }

    @NotNull
    protected abstract String getFileName();

    @NotNull
    protected abstract T createDefault();

    @NotNull
    protected abstract String getDisplayName();

    protected T storingObject;

    public boolean reloadConfiguration()
    {
        //加载JSON配置
        Object targetStore = null;
        var success = false;

        if (!configurationFile.exists())
            initializeStorage(true);

        //从文件读取并反序列化为配置
        try (var fileStream = new FileInputStream(configurationFile); var jsonStream = new InputStreamReader(fileStream))
        {
            targetStore = gson.fromJson(jsonStream, storingObject.getClass());
            success = true;
        }
        catch (Throwable t)
        {
            logger.warn("Can't load JSON configuration for " + getDisplayName() + ": " + t.getMessage());
            t.printStackTrace();
        }

        //确保targetStore不是null
        if (targetStore == null) targetStore = createDefault();

        //设置并保存
        storingObject = (T) targetStore;

        return success;
    }

    public boolean saveConfiguration()
    {
        try
        {
            var jsonString = gson.toJson(storingObject);

            if (configurationFile.exists()) configurationFile.delete();

            if (!configurationFile.createNewFile())
            {
                logger.error("Unable to create file, no saving configuration for '%s'!".formatted(getDisplayName()));
                return false;
            }

            try (var stream = new FileOutputStream(configurationFile))
            {
                stream.write(jsonString.getBytes());
            }
        }
        catch (Throwable t)
        {
            logger.error("Unable to save configuration for %s: %s".formatted(this.getDisplayName(), t.getMessage()));
            throw new RuntimeException(t);
        }

        return true;
    }
}
