package xiamomc.pluginbase.Command;

import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public interface ISubCommand
{
    @NotNull
    public String getCommandName();

    @Nullable
    public default List<String> onTabComplete(List<String> args, CommandSender source)
    {
        var subCommands = getSubCommands();
        if (subCommands == null || subCommands.size() == 0) return null;

        var cmdBaseName = args.get(0);

        //找找有没有符合条件的子命令
        var cmd = subCommands.stream().filter(c -> c.getCommandName().equals(cmdBaseName)).findFirst();
        if (cmd.isPresent()) //如果有则让子命令补全
        {
            args.remove(0);
            return cmd.get().onTabComplete(args, source);
        }

        //没有则列出所有可用命令
        var list = new ArrayList<String>();

        for (var c : subCommands)
        {
            var perm = c.getPermissionRequirement();

            if (perm != null && !source.hasPermission(perm)) continue;

            var name = c.getCommandName();

            if (name.toLowerCase().startsWith(cmdBaseName.toLowerCase())) list.add(name);
        }

        return list;
    }

    /**
     * 获取权限要求
     * @return 权限要求，若为null则没有要求
     */
    @Nullable
    public default String getPermissionRequirement()
    {
        return null;
    }

    /**
     * 获取此指令的帮助信息
     * @return 帮助信息
     */
    @Nullable
    public String getHelpMessage();

    /**
     * 获取此子指令下面可用的指令
     * @return 指令列表
     */
    @Nullable
    public default List<ISubCommand> getSubCommands() { return null; }

    /**
     *
     * @param sender 执行指令的一方
     * @param args 参数
     * @return 是否成功
     */
    public default boolean onCommand(@NotNull CommandSender sender, @NotNull String[] args)
    {
        //todo: 可以和SubCommandHandler中onCommand里面的内容合并？
        String baseName;

        if (args.length >= 1)
        {
            baseName = args[0];
            args = ArrayUtils.remove(args, 0);
        }
        else baseName = "";

        if (getSubCommands() == null) return false;

        var cmdOptional = getSubCommands().stream().filter(Objects::nonNull)
                .filter(c -> c.getCommandName().equals(baseName)).findFirst();

        if (cmdOptional.isPresent())
        {
            var cmd = cmdOptional.get();

            var perm = cmd.getPermissionRequirement();
            if (perm != null && !sender.hasPermission(perm)) return false;

            return cmd.onCommand(sender, args);
        }
        else
            return false;
    }
}
