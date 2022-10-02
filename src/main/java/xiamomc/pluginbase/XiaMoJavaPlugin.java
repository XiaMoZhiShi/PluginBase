package xiamomc.pluginbase;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import xiamomc.pluginbase.Managers.DependencyManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class XiaMoJavaPlugin extends JavaPlugin
{
    private static final Map<String, XiaMoJavaPlugin> instances = new ConcurrentHashMap<>();

    public static XiaMoJavaPlugin GetInstance(String nameSpace)
    {
        return instances.get(nameSpace);
    }

    protected final DependencyManager dependencyManager;

    public abstract String getNameSpace();

    private final Logger logger = this.getSLF4JLogger();

    public XiaMoJavaPlugin()
    {
        if (instances.containsKey(getNameSpace()))
            logger.warn("之前似乎已经创建过一个插件实例了...除非你是故意这么做的，不然可能代码又有哪里出bug了！");

        dependencyManager = new DependencyManager(this);

        instances.put(getNameSpace(), this);
    }

    @Override
    public void onEnable()
    {
        //region 注册依赖
        dependencyManager.unRegisterPluginInstance(this);
        dependencyManager.registerPluginInstance(this);

        //先反注册一遍所有依赖再注册插件
        dependencyManager.UnCacheAll();

        processExceptionCount();

        dependencyManager.CacheAs(XiaMoJavaPlugin.class, this);

        //endregion

        this.shouldAbortTicking = false;

        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, this::tick, 0, 1);

        super.onEnable();
    }

    @Override
    public void onDisable()
    {
        super.onDisable();

        //禁止tick
        this.shouldAbortTicking = true;

        //反注册依赖
        dependencyManager.UnCacheAll();
        dependencyManager.unRegisterPluginInstance(this);
    }

    //region tick相关

    private long currentTick = 0;

    private void tick()
    {
        currentTick += 1;

        if (shouldAbortTicking) return;

        var schedules = new ArrayList<>(runnables);
        schedules.forEach(c ->
        {
            if (currentTick - c.TickScheduled >= c.Delay)
            {
                runnables.remove(c);

                if (c.isCanceled()) return;

                //logger.info("执行：" + c + "，当前TICK：" + currentTick);
                try
                {
                    c.Function.accept(null);
                }
                catch (Exception e)
                {
                    this.onExceptionCaught(e, c);
                }
            }
        });

        schedules.clear();
    }

    //region tick异常捕捉与处理

    //一秒内最多能接受多少异常
    //todo: 之后考虑做进配置里让它可调？
    @SuppressWarnings("FieldCanBeLocal")
    private final int exceptionLimit = 5;

    //已经捕获的异常
    private int exceptionCaught = 0;

    //是否应该中断tick
    private boolean shouldAbortTicking = false;

    private boolean onExceptionCaught(Exception exception, ScheduleInfo scheduleInfo)
    {
        if (exception == null) return false;

        exceptionCaught += 1;

        logger.warn("执行" + scheduleInfo + "时捕获到未处理的异常：");
        exception.printStackTrace();

        if (exceptionCaught >= exceptionLimit)
        {
            logger.error("可接受异常已到达最大限制");
            this.setEnabled(false);
        }

        return true;
    }

    private void processExceptionCount()
    {
        exceptionCaught -= 1;

        this.schedule(c -> processExceptionCount(), 5);
    }

    //endregion tick异常捕捉与处理

    //endregion tick相关

    private final List<ScheduleInfo> runnables = new ArrayList<>();

    public ScheduleInfo schedule(Consumer<?> runnable)
    {
        return this.schedule(runnable, 1);
    }

    public ScheduleInfo schedule(Consumer<?> function, int delay)
    {
        var si = new ScheduleInfo(function, delay, currentTick);
        synchronized (runnables)
        {
            //Logger.info("添加：" + si + "，当前TICK：" + currentTick);
            runnables.add(si);
        }

        return si;
    }
}