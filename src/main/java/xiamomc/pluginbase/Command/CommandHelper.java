package xiamomc.pluginbase.Command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.PluginObject;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
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
                logger.error("未能注册指令：" + c.getCommandName());
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

    private static final List<String> emptyStringList = List.of("");

    @Nullable
    public List<String> onTabComplete(String rawArgs, CommandSender source)
    {
        var args = new ArrayList<>(Arrays.stream(rawArgs.split(" ")).toList());

        if (rawArgs.endsWith(" ")) args.add("");

        var baseName = args.get(0).replace("/", "");

        //如果带有namespace
        if (baseName.contains(":"))
        {
            var baseNameSpilted = baseName.split(":");

            if (!Objects.equals(baseNameSpilted[0], plugin.getName().toLowerCase())) return null;

            baseName = baseNameSpilted[1];
        }

        args.remove(0);

        for (var c : getCommands())
        {
            boolean cmdMatch = c.getCommandName().equals(baseName) || c.getAliases().contains(baseName);

            if (cmdMatch)
            {
                var result = c.onTabComplete(args, source);
                return result == null ? emptyStringList : result;
            }
        }

        return null;
    }
}

