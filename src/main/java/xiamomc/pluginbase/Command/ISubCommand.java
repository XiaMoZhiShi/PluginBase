package xiamomc.pluginbase.Command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ISubCommand
{
    public String getCommandName();

    @Nullable
    public default List<String> onTabComplete(List<String> args, CommandSender source)
    {
        return null;
    }

    @Nullable
    public default String getPermissionRequirement()
    {
        return null;
    }

    public String getHelpMessage();
    public boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args);
}
