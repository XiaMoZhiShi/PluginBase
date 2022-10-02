package xiamomc.pluginbase.Command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ISubCommand
{
    public String getCommandName();

    public List<String> onTabComplete(List<String> args, CommandSender source);

    public String getPermissionRequirement();

    public String getHelpMessage();
    public boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args);
}
