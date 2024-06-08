package xiamomc.pluginbase.Command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Messages.FormattableMessage;

import java.util.Arrays;
import java.util.List;

public interface IPluginCommand extends CommandExecutor, TabCompleter
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

    public default @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String basename, @NotNull String[] args)
    {
        return onTabComplete(Arrays.stream(args).toList(), sender);
    }
}
