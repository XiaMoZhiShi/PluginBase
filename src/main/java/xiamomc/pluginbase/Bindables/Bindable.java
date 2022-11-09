package xiamomc.pluginbase.Bindables;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

public class Bindable<T> implements IBindable<T>
{
    private T value;

    public Bindable()
    {
    }

    public Bindable(T value)
    {
        this.value = value;
    }

    /**
     * 设置此Bindable的值
     *
     * @param val 新值
     */
    public void set(T val)
    {
        if (bindTarget != null)
        {
            bindTarget.set(val);
            return;
        }

        if (value == val) return;

        var oldVal = value;
        value = val;

        valueChangeConsumers.forEach(c -> c.accept(oldVal, val));
    }

    @ApiStatus.Internal
    public void setInternal(Object val)
    {
        set((T) val);
    }

    /**
     * 获取此Bindable的值
     *
     * @return 此Bindable的值
     */
    public T get()
    {
        return value;
    }

    @Nullable
    private Bindable<T> bindTarget;

    private final BiConsumer<T, T> ttBiConsumer = (o, n) -> this.set(n);

    /**
     * 将此Bindable和另外一个Bindable绑定
     * @apiNote 目前一旦绑定将不能解绑
     *
     * @param other 目标Bindable
     */
    public void bindTo(Bindable<T> other)
    {
        if (other == null) return;

        if (bindTarget != null)
            bindTarget.valueChangeConsumers.remove(ttBiConsumer);

        set(other.value);
        other.valueChangeConsumers.add(0, ttBiConsumer);
        bindTarget = other;
    }

    public void bindTo(IBindable<T> other)
    {
        if (!(other instanceof Bindable<T> bindable))
            throw new IllegalArgumentException("指定的目标不是Bindable实例");

        this.bindTo(bindable);
    }

    private final List<BiConsumer<T, T>> valueChangeConsumers = new ObjectArrayList<>();

    /**
     * 添加此Bindable值改变时要做的事
     *
     * @param consumer {@link BiConsumer}，调用时第一个参数为旧值，第二个为新值
     */
    public void onValueChanged(BiConsumer<T, T> consumer)
    {
        onValueChanged(consumer, false);
    }

    /**
     * 添加此Bindable值改变时要做的事
     *
     * @param consumer {@link BiConsumer}
     * @param runOnce 是否要立即执行一次
     * @apiNote 立即执行时，旧值永远为null
     */
    public void onValueChanged(BiConsumer<T, T> consumer, boolean runOnce)
    {
        valueChangeConsumers.add(consumer);

        if (runOnce)
            consumer.accept(null, value);
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
