package xiamomc.pluginbase.Command;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface IPluginCommand extends CommandExecutor
{
    public String getCommandName();

    public List<String> onTabComplete(String baseName, String[] args, CommandSender source);
}
