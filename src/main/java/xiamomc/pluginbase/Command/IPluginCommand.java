package xiamomc.pluginbase.Command;

import org.bukkit.command.CommandExecutor;

public interface IPluginCommand extends CommandExecutor
{
    public String getCommandName();
}
