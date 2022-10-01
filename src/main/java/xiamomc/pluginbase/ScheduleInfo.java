package xiamomc.pluginbase;

import java.util.function.Consumer;

public class ScheduleInfo
{
    public final Consumer<?> Function;
    public final int Delay;
    public final long TickScheduled;

    private boolean isCanceled = false;

    public void cancel()
    {
        this.isCanceled = true;
    }

    public boolean isCanceled()
    {
        return isCanceled;
    }

    public ScheduleInfo(Consumer<?> function, int delay, long tickScheduled)
    {
        this.Function = function;
        this.Delay = delay;
        this.TickScheduled = tickScheduled;
    }

    @Override
    public String toString()
    {
        return "于第" + this.TickScheduled + "刻创建，"
                + "并计划于" + this.Delay + "刻后执行的计划任务"
                + "（" + this.Function + "）";
    }
}
