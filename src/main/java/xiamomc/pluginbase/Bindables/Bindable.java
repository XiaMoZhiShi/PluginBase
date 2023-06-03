package xiamomc.pluginbase.Bindables;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.WeakReferenceList;

import java.lang.ref.WeakReference;
import java.util.Arrays;
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

    private final WeakReferenceList<Bindable<T>> binds = new WeakReferenceList<>();

    /**
     * 设置此Bindable的值
     *
     * @param val 新值
     */
    public void set(T val)
    {
        if (value == val) return;

        var oldVal = value;
        value = val;

        this.triggerValueChange(this, oldVal, val);
    }

    /**
     * 在不同的Bindable之间同步值
     *
     * @param source 同步来源
     * @param newVal 新值
     */
    private void syncValue(Bindable<T> source, T newVal)
    {
        if (source == this || value == newVal) return;

        var oldVal = this.value;
        this.value = newVal;

        triggerValueChange(source, oldVal, newVal);
    }

    private int triggers;

    /**
     * 触发一次变动时间
     *
     * @param source 触发来源
     * @param newVal 新值
     */
    private void triggerValueChange(Bindable<T> source, T oldVal, T newVal)
    {
        triggers++;

        if (triggers >= 5)
        {
            removeReleasedRefs();
            triggers = 0;
        }

        valueChangeConsumers.forEach(c -> c.accept(oldVal, newVal));

        binds.forEach(b ->
        {
            if (b == this || b == null) return;

            b.syncValue(source, newVal);
        });
    }

    @ApiStatus.Internal
    public void setInternal(Object val)
    {
        set((T) val);
    }

    @ApiStatus.Internal
    @Nullable
    public T tryCast(Object val)
    {
        try
        {
            //var typeParam = Arrays.stream(this.getClass().getTypeParameters())
            //        .findFirst().orElseThrow();

            //var typeParamClazz = (Class<T>) typeParam.getGenericDeclaration().componentType();
            return (T) val;
        }
        catch (Throwable t)
        {
            return null;
        }
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

    private void removeReleasedRefs()
    {
        binds.removeNull();
    }

    /**
     * 将此Bindable和另外一个Bindable绑定
     * 如果此Bindable已经绑定了一个对象，那么将从上一个对象处解绑
     *
     * @param other 目标Bindable
     */
    public void bindTo(Bindable<T> other)
    {
        if (other == null || other == this || binds.contains(other.weakRef)) return;

        if (bindTarget != null)
            unBindFrom(bindTarget);

        //让other改变值时可以触发这里的triggerValueChange
        other.binds.add(weakRef);

        //让这里改变值时也可以触发other的triggerValueChange
        this.binds.add(other.weakRef);

        set(other.value);
        bindTarget = other;
    }

    private final WeakReference<Bindable<T>> weakRef = new WeakReference<>(this);

    public void bindTo(@NotNull IBindable<T> other)
    {
        if (!(other instanceof Bindable<T> bindable))
            throw new IllegalArgumentException("指定的目标不是Bindable实例");

        this.bindTo(bindable);
    }

    public void unBindFrom(Bindable<T> other)
    {
        if (other == null || other == this) return;

        if (bindTarget == null) return;

        if (this.bindTarget != other)
            throw new RuntimeException("Trying to unbind from a target that were not bind to: %s"
                    .formatted(toStringSuper(), other.toStringSuper()));

        bindTarget.binds.remove(weakRef);
        this.binds.remove(bindTarget.weakRef);

        this.bindTarget = null;
    }

    /**
     * 移除所有通过 {@link Bindable#onValueChanged(BiConsumer)} 添加的Consumer
     */
    public void unBindListeners()
    {
        valueChangeConsumers.clear();
    }

    /**
     * 移除所有绑定到此Bindable的对象
     */
    public void unBindBindings()
    {
        for (Bindable<T> bind : this.binds)
            bind.unBindFrom(this);
    }

    /**
     * 移除所有通过 {@link Bindable#onValueChanged(BiConsumer)} 添加的Consumer，
     * 解除所有绑定到此Bindable的对象，
     * 并解除自身的绑定（如果有）
     */
    public void unBindAll()
    {
        unBindBindings();
        unBindListeners();

        if (bindTarget != null)
            unBindFrom(bindTarget);
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

    public void dispose()
    {
        this.unBindAll();
    }

    @Override
    public String toString()
    {
        return "%s(%s)".formatted(value, toStringSuper());
    }

    private String toStringSuper()
    {
        return super.toString();
    }
}
