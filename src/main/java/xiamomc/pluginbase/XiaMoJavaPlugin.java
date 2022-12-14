package xiamomc.pluginbase;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import xiamomc.pluginbase.Managers.DependencyManager;
import xiamomc.pluginbase.Utilities.PluginSoftDependManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class XiaMoJavaPlugin extends JavaPlugin
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

    public abstract String getNameSpace();

    protected final DependencyManager dependencyManager;

    protected final PluginSoftDependManager softDeps;

    protected final Logger logger = this.getSLF4JLogger();

    public XiaMoJavaPlugin()
    {
        dependencyManager = DependencyManager.getManagerOrCreate(this);
        softDeps = PluginSoftDependManager.getManagerOrCreate(this);

        instances.put(getNameSpace(), this);
    }

    @Override
    public void onEnable()
    {
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

        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, this::tick, 0, 1);

        super.onEnable();
    }

    @Override
    public void onDisable()
    {
        super.onDisable();

        //禁止tick
        this.cancelSchedules = true;
        this.acceptSchedules = false;

        //反注册依赖
        dependencyManager.unCacheAll();
        dependencyManager.unRegisterPluginInstance(this);
        softDeps.unRegisterPluginInstance(this);
    }

    //region tick相关

    private long currentTick = 0;

    private void tick()
    {
        currentTick += 1;

        if (cancelSchedules) return;

        var schedules = new ArrayList<>(this.schedules);
        schedules.forEach(c ->
        {
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
                    getServer().getScheduler().runTaskAsynchronously(this, r -> runFunction(c));
                else
                    runFunction(c);
            }
        });

        schedules.clear();
    }

    private void runFunction(ScheduleInfo c)
    {
        if (cancelSchedules) return;

        try
        {
            c.Function.run();
        }
        catch (Exception e)
        {
            this.onExceptionCaught(e, c);
        }
    }

    //region tick异常捕捉与处理

    //5 ticks内最多能接受多少异常
    protected final int exceptionLimit = 5;

    protected int getExceptionLimit()
    {
        return exceptionLimit;
    }

    //已经捕获的异常
    private int exceptionCaught = 0;

    /**
     * Should we cancel executing schedules?
     */
    private boolean cancelSchedules = false;

    /**
     * Should we accept any further {@link XiaMoJavaPlugin#schedule} calls?
     */
    private boolean acceptSchedules = true;

    private synchronized void onExceptionCaught(Exception exception, ScheduleInfo scheduleInfo)
    {
        if (exception == null) return;

        exceptionCaught += 1;

        logger.warn("执行" + scheduleInfo + "时捕获到未处理的异常：");
        exception.printStackTrace();

        if (exceptionCaught >= getExceptionLimit())
        {
            logger.error("可接受异常已到达最大限制: " + exceptionCaught + " -> " + getExceptionLimit());

            this.schedules.clear();
            this.setEnabled(false);
        }
    }

    private void processExceptionCount()
    {
        exceptionCaught -= 1;

        this.schedule(this::processExceptionCount, 5);
    }

    //endregion tick异常捕捉与处理

    //endregion tick相关

    private final List<ScheduleInfo> schedules = new ObjectArrayList<>();

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