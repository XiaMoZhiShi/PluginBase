package xiamomc.pluginbase;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Exceptions.NullDependencyException;
import xiamomc.pluginbase.Managers.DependencyManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public abstract class PluginObject<P extends XiaMoJavaPlugin>
{
    protected final XiaMoJavaPlugin Plugin = P.getInstance(getPluginNamespace());
    protected final DependencyManager Dependencies = DependencyManager.getInstance(getPluginNamespace());
    protected final Logger Logger = Plugin.getSLF4JLogger();

    private List<Field> fieldsToResolve = new ArrayList<>();

    private final List<Method> initializerMethods = new ArrayList<>();

    protected abstract String getPluginNamespace();

    protected PluginObject()
    {
        initialDependencyResolve();
    }

    //region 依赖处理

    private void addInitializermethods(Class<?> clazz)
    {
        var methods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Initializer.class)).toList();

        if (methods.size() > 1) throw new RuntimeException(clazz + "中不能拥有多个初始化方法");

        methods.stream().findFirst().ifPresent(this.initializerMethods::add);
    }

    private void resolveFields(Class<?> clazz)
    {
        var ftr = new ArrayList<>(Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Resolved.class)).toList());

        var fieldToResolveNow = ftr.stream()
                .filter(f -> f.getAnnotation(Resolved.class).shouldSolveImmediately()).toList();

        for (Field f : fieldToResolveNow)
        {
            resolveField(f);
            ftr.remove(f);
        }

        fieldsToResolve.addAll(ftr);
    }

    private void initialDependencyResolve()
    {
        //region 获取初始化方法

        var superclasses = ClassUtils.getAllSuperclasses(this.getClass());
        Collections.reverse(superclasses);

        for (var c : superclasses)
            addInitializermethods(c);

        addInitializermethods(this.getClass());

        //endregion

        //region 解析需要获取依赖的字段

        for (var c : superclasses)
            resolveFields(c);

        resolveFields(this.getClass());

        this.addSchedule(d -> this.resolveRemainingDependencies());

        //endregion
    }

    private void resolveRemainingDependencies()
    {
        //自动对有Resolved的字段获取依赖
        for (Field field : fieldsToResolve)
        {
            resolveField(field);
        }

        fieldsToResolve.clear();
        fieldsToResolve = null;

        //执行初始化方法
        for (var initializerMethod : initializerMethods)
        {
            //和Resolved一样，只对private生效
            if (Modifier.isPrivate(initializerMethod.getModifiers()))
            {
                //获取参数
                var parameters = initializerMethod.getParameters();

                //对应的值
                var values = new ArrayList<>();

                //逐个获取依赖
                for (var p : parameters)
                {
                    var targetClassType = p.getType();
                    Object value = Dependencies.Get(targetClassType, false);

                    if (value == null) throwDependencyNotFound(targetClassType);

                    values.add(value);
                }

                //尝试调用
                try
                {
                    initializerMethod.setAccessible(true);

                    initializerMethod.invoke(this, values.toArray());

                    initializerMethod.setAccessible(false);
                }
                catch (IllegalAccessException | InvocationTargetException e)
                {
                    initializerMethod.setAccessible(false);
                    throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
                }
            }
            else
                throw new RuntimeException("初始化方法不能是private");
        }
    }

    private void resolveField(Field field)
    {
        //暂时让Resolved只对private生效
        if (Modifier.isPrivate(field.getModifiers()))
        {
            field.setAccessible(true);

            //Logger.info("Resolving " + field.getName() + "(" + field.getType() + ")" + "in " + this);

            try
            {
                //获取目标Class
                Class<?> targetClassType = field.getType();

                //从DependencyManager获取值
                Object value = Dependencies.Get(targetClassType, false);

                //判断是不是null
                if (value == null && !field.getAnnotation(Resolved.class).allowNull())
                    throwDependencyNotFound(targetClassType);

                //设置值
                field.set(this, value);
            }
            catch (IllegalAccessException e)
            {
                field.setAccessible(false);
                throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
            }

            field.setAccessible(false);
        }
        else
            throw new RuntimeException("字段必须是private");
    }

    private void throwDependencyNotFound(Class<?> targetClassType)
    {
        throw new NullDependencyException(this.getClass().getSimpleName()
                + "依赖"
                + targetClassType.getSimpleName()
                + ", 但其尚未被注册");
    }

    //endregion

    //region Schedules

    //todo: 使AddSchedule最终由PluginObject自己处理，而不是发给插件
    protected ScheduleInfo addSchedule(Consumer<?> c)
    {
        return this.addSchedule(c, 0);
    }

    protected ScheduleInfo addSchedule(Consumer<?> c, int delay)
    {
        return Plugin.schedule(c, delay);
    }

    //endregion
}
