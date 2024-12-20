package xiamomc.pluginbase;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import xiamomc.pluginbase.Managers.DependencyManager;
import xiamomc.pluginbase.Utilities.PluginSoftDependManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public abstract class XiaMoJavaPlugin extends JavaPlugin implements ISchedulablePlugin
{
    private static final Map<String, XiaMoJavaPlugin> instances = new ConcurrentHashMap<>();

    @Deprecated
    public static XiaMoJavaPlugin GetInstance(String nameSpace)
    {
        return getInstance(nameSpace);
    }

    public static XiaMoJavaPlugin getInstance(String nameSpace)
    {
        return instances.get(nameSpace);
    }

    public abstract String getNamespace();

    protected final DependencyManager dependencyManager;

    protected final PluginSoftDependManager softDeps;

    protected final Logger logger = this.getSLF4JLogger();

    public XiaMoJavaPlugin()
    {
        dependencyManager = DependencyManager.getManagerOrCreate(this);
        softDeps = PluginSoftDependManager.getManagerOrCreate(this);

        instances.put(getNamespace(), this);
    }

    protected void enable()
    {
    }

    @Override
    public final void onEnable()
    {
        super.onEnable();

        //region 注册依赖
        dependencyManager.unRegisterPluginInstance(this);
        dependencyManager.registerPluginInstance(this);

        softDeps.clearHandles();

        //先反注册一遍所有依赖再注册插件
        dependencyManager.unCacheAll();

        processExceptionCount();

        dependencyManager.cacheAs(XiaMoJavaPlugin.class, this);

        //endregion

        this.cancelSchedules = false;
        this.acceptSchedules = true;

        this.enable();
        startMainLoop(this::tick);
    }

    public abstract void startMainLoop(Runnable r);
    public abstract void runAsync(Runnable r);

    protected void disable()
    {
    }

    @Override
    public final void onDisable()
    {
        super.onDisable();
        disable();

        //禁止tick
        this.cancelSchedules = true;
        this.acceptSchedules = false;

        //反注册依赖
        dependencyManager.unCacheAll();
        dependencyManager.unRegisterPluginInstance(this);
        softDeps.unRegisterPluginInstance(this);
    }

    public boolean doInternalDebugOutput = false;

    //region tick相关

    protected long currentTick = 0;

    protected void tick()
    {
        currentTick += 1;

        if (cancelSchedules) return;

        var schedulesTemp = new ObjectArrayList<ScheduleInfo>();

        synchronized (schedules)
        {
            schedulesTemp.addAll(this.schedules);
        }

        schedulesTemp.forEach(c ->
        {
            if (c == null)
            {
                if (doInternalDebugOutput)
                    logger.warn("Trying to execute a NULL ScheduleInfo?! This shouldn't happen!");

                return;
            }

            if (c.isCanceled())
            {
                this.schedules.remove(c);
                return;
            }

            if (currentTick - c.TickScheduled >= c.Delay)
            {
                this.schedules.remove(c);

                //Allows us to cancel half-way
                if (cancelSchedules) return;

                //logger.info("执行：" + c + "，当前TICK：" + currentTick);\
                if (c.isAsync)
                    runAsync(() -> runFunction(c));
                else
                    runFunction(c);
            }
        });

        schedulesTemp.clear();
    }

    protected void runFunction(ScheduleInfo c)
    {
        if (cancelSchedules) return;

        try
        {
            c.Function.run();
        }
        catch (Throwable t)
        {
            this.onTaskExceptionCaught(t, c);
        }
    }

    //region tick异常捕捉与处理

    protected int getExceptionLimit()
    {
        //5 ticks内最多能接受多少异常
        return 5;
    }

    //已经捕获的异常
    private final AtomicInteger exceptionCaught = new AtomicInteger(0);

    /**
     * Should we cancel executing schedules?
     */
    protected boolean cancelSchedules = false;

    /**
     * Should we accept any further {@link XiaMoJavaPlugin#schedule} calls?
     */
    protected boolean acceptSchedules = true;

    @Override
    public boolean acceptSchedules()
    {
        return acceptSchedules;
    }

    protected void onTaskExceptionCaught(Throwable exception, ScheduleInfo scheduleInfo)
    {
        if (exception == null) return;

        int exceptions;
        synchronized (exceptionCaught)
        {
            exceptions = exceptionCaught.incrementAndGet();
        }

        logger.warn("执行" + scheduleInfo + "时捕获到未处理的异常：");
        exception.printStackTrace();

        if (exceptions >= getExceptionLimit())
        {
            logger.error("可接受异常已到达最大限制: " + exceptionCaught + " -> " + getExceptionLimit());

            this.schedules.clear();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void processExceptionCount()
    {
        exceptionCaught.decrementAndGet();

        this.schedule(this::processExceptionCount, 5);
    }

    //endregion tick异常捕捉与处理

    //endregion tick相关

    protected final List<ScheduleInfo> schedules = new ObjectArrayList<>();

    @Deprecated
    public ScheduleInfo schedule(Consumer<?> consumer)
    {
        return this.schedule(() -> consumer.accept(null));
    }

    @Deprecated
    public ScheduleInfo schedule(Consumer<?> c, int delay)
    {
        return this.schedule(() -> c.accept(null), delay);
    }

    @Deprecated
    public ScheduleInfo schedule(Consumer<?> c, int delay, boolean isAsync)
    {
        return this.schedule(() -> c.accept(null), delay, isAsync);
    }

    public ScheduleInfo schedule(Runnable runnable)
    {
        return this.schedule(runnable, 1);
    }

    public ScheduleInfo schedule(Runnable function, int delay)
    {
        return this.schedule(function, delay, false);
    }

    public ScheduleInfo schedule(Runnable function, int delay, boolean async)
    {
        var si = new ScheduleInfo(function, delay, currentTick, async);

        if (!acceptSchedules)
        {
            si.cancel();
            return si;
        }

        synchronized (schedules)
        {
            //Logger.info("添加：" + si + "，当前TICK：" + currentTick);
            schedules.add(si);
        }

        return si;
    }

    public long getCurrentTick()
    {
        return currentTick;
    }
}