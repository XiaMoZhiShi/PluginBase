package xiamomc.pluginbase.Command;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface IPluginCommand extends CommandExecutor
{
    public String getCommandName();

    public default List<String> onTabComplete(List<String> args, CommandSender source)
    {
        return null;
    }

    public default String getPermissionRequirement()
    {
        return null;
    }

    public String getHelpMessage();
}
