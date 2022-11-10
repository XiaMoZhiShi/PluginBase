package xiamomc.pluginbase.Bindables;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BiConsumer;

public class BindableList<T> implements IBindableList<T>
{
    final List<T> list = new ObjectArrayList<>();

    private ListIterator<T> getBindableIterator()
    {
        return new BindableListIterator<>(this);
    }

    public BindableList()
    {
    }

    public BindableList(Collection<T> collection)
    {
        this.list.addAll(collection);
    }

    //region Bindable

    private final List<BiConsumer<List<T>, TriggerReason>> consumers = new ObjectArrayList<>();

    @Override
    public void onListChanged(BiConsumer<List<T>, TriggerReason> consumer)
    {
        this.onListChanged(consumer, false);
    }

    @Override
    public void onListChanged(BiConsumer<List<T>, TriggerReason> consumer, boolean runOnce)
    {
        consumers.add(consumer);

        if (runOnce)
            consumer.accept(new ObjectArrayList<>(list), TriggerReason.ADD);
    }

    private void triggerChange(BindableList<T> source, List<T> value, TriggerReason reason)
    {
        binds.forEach(b -> b.syncValue(source, value, reason));
        consumers.forEach(c -> c.accept(value, reason));
    }

    private void triggerChange(BindableList<T> source, T value, TriggerReason reason)
    {
        this.triggerChange(source, List.of(value), reason);
    }

    void triggerChange(T value, TriggerReason reason)
    {
        this.triggerChange(this, value, reason);
    }

    void triggerChange(List<T> value, TriggerReason reason)
    {
        this.triggerChange(this, value, reason);
    }

    /**
     * Sync value between different lists
     *
     * @param source Sync source
     * @param changes Changes
     * @param operation Target operation
     */
    private void syncValue(BindableList<T> source, Collection<T> changes, TriggerReason operation)
    {
        if (source == this) return;

        if (operation == TriggerReason.ADD)
            list.addAll(changes);
        else
            list.removeAll(changes);
    }

    final List<BindableList<T>> binds = new ObjectArrayList<>();

    private BindableList<T> bindTarget;

    @Override
    public void bindTo(IBindableList<T> other)
    {
        if (!(other instanceof BindableList<T> bindable))
            throw new IllegalArgumentException("指定的目标不是BindableList实例");

        this.bindTo(bindable);
    }

    public void bindTo(BindableList<T> other)
    {
        if (other == null || other == this || binds.contains(other)) return;

        if (bindTarget != null)
        {
            this.binds.remove(bindTarget);
            bindTarget.binds.remove(this);
        }

        this.clear();
        this.addAll(other.list);

        //让other改变值时可以触发这里的triggerValueChange
        other.binds.add(this);

        //让这里改变值时也可以触发other的triggerValueChange
        this.binds.add(other);

        bindTarget = other;
    }

    //endregion Bindable

    //region List

    public T insert(int i, T t)
    {
        if (list.contains(t)) return null;

        list.add(i, t);
        this.triggerChange(t, TriggerReason.ADD);

        return t;
    }

    public int size()
    {
        return list.size();
    }

    public boolean isEmpty()
    {
        return list.isEmpty();
    }

    public boolean contains(Object o)
    {
        return list.contains(o);
    }

    @NotNull
    public Object[] toArray()
    {
        return list.toArray();
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] t1s)
    {
        return list.toArray(t1s);
    }

    public boolean add(T t)
    {
        list.add(t);
        this.triggerChange(t, TriggerReason.ADD);

        return true;
    }

    public T remove(int i)
    {
        var val = list.remove(i);

        if (val != null)
            this.triggerChange(val, TriggerReason.REMOVE);

        return val;
    }

    public boolean remove(Object o)
    {
        var success = list.remove(o);

        if (success)
            this.triggerChange((T) o, TriggerReason.REMOVE);

        return success;
    }

    public boolean addAll(@NotNull Collection<? extends T> collection)
    {
        var success = list.addAll(collection);

        if (success)
            this.triggerChange((List<T>) collection, TriggerReason.ADD);

        return success;
    }

    public boolean addAll(int i, @NotNull Collection<? extends T> collection)
    {
        var success = list.addAll(i, collection);

        if (success)
            this.triggerChange((List<T>) collection, TriggerReason.ADD);

        return success;
    }

    public boolean removeAll(@NotNull Collection<?> collection)
    {
        var success = list.removeAll(collection);

        if (success)
            this.triggerChange((List<T>) collection.stream().toList(), TriggerReason.REMOVE);

        return success;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection)
    {
        var changes = new ObjectArrayList<>(list);

        if (list.retainAll(collection))
        {
            changes.removeIf(list::contains);
            this.triggerChange(changes, TriggerReason.REMOVE);

            return true;
        }

        return false;
    }

    public boolean containsAll(@NotNull Collection<?> collection)
    {
        return list.containsAll(collection);
    }

    public void clear()
    {
        var values = new ObjectArrayList<>(list);
        list.clear();

        this.triggerChange(values, TriggerReason.REMOVE);
    }

    public T get(int i)
    {
        return list.get(i);
    }

    @Override
    public T set(int i, T t)
    {
        var val = this.insert(i, t);

        if (val != null)
            triggerChange(t, TriggerReason.ADD);

        return val;
    }

    @Override
    public void add(int i, T t)
    {
        list.add(i, t);

        triggerChange(t, TriggerReason.ADD);
    }

    public int indexOf(Object o)
    {
        return list.indexOf(o);
    }

    public int lastIndexOf(Object o)
    {
        return list.lastIndexOf(o);
    }

    @NotNull
    @Override
    public Iterator<T> iterator()
    {
        return getBindableIterator();
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator()
    {
        return getBindableIterator();
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator(int i)
    {
        return getBindableIterator();
    }

    @NotNull
    public List<T> subList(int i, int i1)
    {
        return list.subList(i, i1);
    }
    //endregion

    @Override
    public String toString()
    {
        var builder = new StringBuilder();
        var it = list.listIterator();

        while (it.hasNext())
        {
            var obj = it.next();
            builder.append(obj.toString());

            if (it.hasNext()) builder.append(", ");
        }

        return "[" + builder + "]@" + Integer.toHexString(this.hashCode());
    }
}
