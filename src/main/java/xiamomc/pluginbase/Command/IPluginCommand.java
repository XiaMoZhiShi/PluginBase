package xiamomc.pluginbase.Command;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import xiamomc.pluginbase.Messages.FormattableMessage;

import java.util.List;

public interface IPluginCommand extends CommandExecutor
{
    public String getCommandName();

    default List<String> getAliases()
    {
        return List.of();
    }

    public default List<String> onTabComplete(List<String> args, CommandSender source)
    {
        return null;
    }

    public default String getPermissionRequirement()
    {
        return null;
    }

    public FormattableMessage getHelpMessage();
}
