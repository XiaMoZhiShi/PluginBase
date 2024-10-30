package xiamomc.pluginbase.storage;

import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.PluginObject;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public abstract class DirectoryStorage<P extends XiaMoJavaPlugin> extends PluginObject<P>
{
    private final String directoryBaseName;
    private final URI pluginStorageBaseUri;
    private final URI absoluteDirectoryPath;

    public boolean initializeFailed()
    {
        return initializeFailed;
    }

    private boolean initializeFailed = false;

    private URI getAbsoulteURI(String path)
    {
        return new File(path).getAbsoluteFile().toURI();
    }

    private final String separator = "/"; //FileSystems.getDefault().getSeparator();

    public DirectoryStorage(String directoryBaseName)
    {
        this.directoryBaseName = directoryBaseName;
        this.pluginStorageBaseUri = this.plugin.getDataFolder().getAbsoluteFile().toURI();
        this.absoluteDirectoryPath = this.getAbsoulteURI(pluginStorageBaseUri.getPath() + separator + directoryBaseName);

        var dirFile = new File(absoluteDirectoryPath);
        if (!dirFile.exists())
        {
            try
            {
                var success = dirFile.mkdirs();
                if (!success)
                    logger.error("Unable to create directory '%s': Unknown error".formatted(directoryBaseName));

                this.initializeFailed = true;
            }
            catch (Throwable t)
            {
                logger.error("Unable to create directory '%s' for logging: %s".formatted(directoryBaseName, t.getLocalizedMessage()));
                t.printStackTrace();

                this.initializeFailed = true;
            }
        }

        this.initializeFailed = false;
    }

    public File[] getFiles(String pattern)
    {
        var path = Path.of(this.absoluteDirectoryPath).toFile();
        return path.listFiles(f -> f.isFile() && f.getName().matches(pattern));
    }

    public File[] getFiles()
    {
        return Path.of(this.absoluteDirectoryPath).toFile().listFiles(File::isFile);
    }

    public File[] getDirectories(String pattern)
    {
        var path = Path.of(this.absoluteDirectoryPath).toFile();
        return path.listFiles(f -> f.isDirectory() && f.getName().matches(pattern));
    }

    public File[] getDirectories()
    {
        return Path.of(this.absoluteDirectoryPath).toFile().listFiles(File::isDirectory);
    }

    public File getDirectory(String relativePath, boolean createIfNotExist)
    {
        var file = new File(this.getAbsoulteURI(absoluteDirectoryPath.getPath() + separator + relativePath));

        if (!file.toPath().toUri().getPath().startsWith(absoluteDirectoryPath.getPath()))
            throw new RuntimeException("Trying to access a file that does not belongs to this plugin: %s".formatted(file.toURI()));

        if (!file.exists() && createIfNotExist)
        {
            try
            {
                var success = file.mkdirs();
                if (!success)
                {
                    logger.warn("Unable to create directory: Unknown error");
                    return null;
                }
            }
            catch (Throwable t)
            {
                logger.error("Unable to create directory '%s': %s".formatted(relativePath, t.getLocalizedMessage()));
                t.printStackTrace();
            }
        }

        if (!file.isDirectory()) return null;

        return file;
    }

    /**
     * @return NULL if the file does not exist.
     */
    @Nullable
    public File getFile(String fileName, boolean createIfNotExist)
    {
        var file = new File(this.getAbsoulteURI(absoluteDirectoryPath.getPath() + separator + fileName));

        if (!file.toPath().toUri().getPath().startsWith(absoluteDirectoryPath.getPath()))
        {
            logger.error("Trying to access a file that does not belongs to this plugin: %s".formatted(file.toURI()));
            return null;
        }

        if (!file.exists())
        {
            if (!createIfNotExist) return null;

            try
            {
                boolean success = true;

                var parent = file.getParentFile();
                if (!parent.exists())
                    success = parent.mkdirs();

                success = success && file.createNewFile();

                if (!success)
                {
                    logger.warn("Unable to create file: Unknown error");
                    return null;
                }
            }
            catch (Throwable t)
            {
                logger.error("Unable to create file '%s': %s".formatted(fileName, t.getLocalizedMessage()));
                t.printStackTrace();
            }
        }

        if (!file.isFile()) return null;

        return file;
    }

    /**
     * @return 操作是否成功
     */
    private boolean ensureParentAlwaysPresent(File baseFile, boolean isDirectory)
    {
        var parent = baseFile.getParentFile();
        if (!parent.exists()) ensureParentAlwaysPresent(parent, true);

        try
        {
            return baseFile.createNewFile();
        }
        catch (IOException e)
        {
            logger.warn("Can't create file: " + e.getMessage());
            return false;
        }
    }
}
