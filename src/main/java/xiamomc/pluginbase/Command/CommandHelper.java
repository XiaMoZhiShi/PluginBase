package xiamomc.pluginbase.Command;

import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.PluginObject;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.List;
import java.util.Objects;

public abstract class CommandHelper<P extends XiaMoJavaPlugin> extends PluginObject<P>
{
    public abstract List<IPluginCommand> getCommands();

    public CommandHelper()
    {
    }

    protected abstract XiaMoJavaPlugin getPlugin();

    @Initializer
    private void initializeCommands()
    {
        var commands = getCommands();

        for (var c : commands)
        {
            if (!this.registerCommand(c))
                Logger.error("未能注册指令：" + c.getCommandName());
        }
    }

    public boolean registerCommand(IPluginCommand command)
    {
        if (Objects.equals(command.getCommandName(), ""))
            return false;

        var cmd = Bukkit.getPluginCommand(command.getCommandName());
        if (cmd != null && cmd.getExecutor().equals(getPlugin()))
        {
            cmd.setExecutor(command);
            return true;
        }
        else
            return false;
    }

    protected List<String> onTabComplete(String[] buffer, CommandSender source)
    {
        var baseName = buffer[0];

        buffer = ArrayUtils.remove(buffer, 0);

        for (var c : getCommands())
        {
            if (c.getCommandName().equals(baseName))
                return c.onTabComplete(baseName, buffer, source);
        }

        return List.of("");
    }
}
