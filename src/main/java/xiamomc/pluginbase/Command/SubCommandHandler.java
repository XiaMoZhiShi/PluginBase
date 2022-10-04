package xiamomc.pluginbase.Command;

import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import xiamomc.pluginbase.PluginObject;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.ArrayList;
import java.util.List;

public abstract class SubCommandHandler<T extends XiaMoJavaPlugin> extends PluginObject<T> implements IPluginCommand
{
    /**
     * 获取所有可用的子命令
     * @return 子命令列表
     */
    public abstract List<ISubCommand> getSubCommands();

    /**
     * 获取注记
     * @return 注记列表
     * @implNote 注记列表数量不宜过长
     */
    public abstract List<String> getNotes();

    private ISubCommand findSubCommandOrNull(String subCommandBaseName, List<ISubCommand> subCommands)
    {
        if (subCommands == null || subCommandBaseName == null) return null;

        var cmd = subCommands.stream()
                .filter(c -> c.getCommandName().equals(subCommandBaseName)).findFirst();

        return cmd.orElse(null);
    }

    private final List<String> emptyStringList = new ArrayList<>();

    @Override
    public List<String> onTabComplete(List<String> args, CommandSender source)
    {
        String subBaseName;

        subBaseName = args.size() >= 1 ? args.get(0) : "";

        //匹配所有可用的子命令
        var avaliableSubCommands = new ArrayList<ISubCommand>();

        //只添加有权限的指令
        getSubCommands().forEach(c ->
        {
            var perm = c.getPermissionRequirement();

            if (perm == null || source.hasPermission(perm))
                avaliableSubCommands.add(c);
        });

        //Logger.warn("BUFFER: '" + Arrays.toString(args) + "'");

        //如果buffer长度大于等于2（有给定子命令名）
        if (args.size() >= 2)
        {
            //查询匹配的子命令
            var subCommand = findSubCommandOrNull(subBaseName, avaliableSubCommands);

            //如果有
            if (subCommand != null)
            {
                //移除列表中的命令名
                var argsCopy = new ArrayList<>(args);
                argsCopy.remove(0);

                var result = subCommand.onTabComplete(argsCopy, source);

                return result == null ? emptyStringList : result;
            }

            //没有，返回空列表
            return emptyStringList;
        }

        //否则，则返回所有可用的子命令
        var list = new ArrayList<String>();

        avaliableSubCommands.forEach(c ->
        {
            if (c.getCommandName().startsWith(subBaseName))
                list.add(c.getCommandName());
        });

        return list;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args)
    {
        String subBaseName;

        if (args.length >= 1)
        {
            subBaseName = args[0];
            args = ArrayUtils.remove(args, 0);
        }
        else subBaseName = "";

        var cmd = findSubCommandOrNull(subBaseName, getSubCommands());

        if (cmd != null)
        {
            var perm = cmd.getPermissionRequirement();
            if (perm != null && !sender.hasPermission(perm)) return false;

            return cmd.onCommand(sender, args);
        }
        else
            return false;
    }
}
