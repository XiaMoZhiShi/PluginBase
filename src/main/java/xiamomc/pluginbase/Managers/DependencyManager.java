package xiamomc.pluginbase.Managers;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Exceptions.DependencyAlreadyRegistedException;
import xiamomc.pluginbase.Exceptions.NullDependencyException;
import xiamomc.pluginbase.XiaMoJavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DependencyManager
{
    //region 实例相关
    private final static Map<String, DependencyManager> instances = new ConcurrentHashMap<>();

    @Deprecated
    public static DependencyManager GetInstance(String namespace)
    {
        return getInstance(namespace);
    }

    public static DependencyManager getInstance(String namespace)
    {
        return instances.get(namespace);
    }

    public DependencyManager(XiaMoJavaPlugin plugin)
    {
        registerPluginInstance(plugin);
    }

    public void registerPluginInstance(XiaMoJavaPlugin plugin)
    {
        if (instances.containsKey(plugin.getNameSpace()))
            throw new RuntimeException("已经有一个DependencyManager的实例了");

        instances.put(plugin.getNameSpace(), this);
    }

    public void unRegisterPluginInstance(XiaMoJavaPlugin plugin)
    {
        instances.remove(plugin.getNameSpace());
    }
    //endregion 实例相关

    //注册表
    private final Map<Class<?>, Object> registers = new ConcurrentHashMap<>();

    /**
     * 注册一个对象到依赖表中
     *
     * @param obj 要注册的对象
     * @throws DependencyAlreadyRegistedException 该对象所对应的Class是否已被注册
     */
    public void Cache(Object obj) throws DependencyAlreadyRegistedException
    {
        CacheAs(obj.getClass(), obj);
    }

    /**
     * 将一个对象作为某个Class类型注册到依赖表中
     *
     * @param classType 要注册的Class类型
     * @param obj       要注册的对象
     * @throws DependencyAlreadyRegistedException 是否已经注册过一个相同的classType了
     * @throws IllegalArgumentException           传入的对象不能转化为classType的实例
     */
    public void CacheAs(Class<?> classType, Object obj) throws DependencyAlreadyRegistedException
    {
        synchronized (registers)
        {
            //检查obj是否能cast成classType
            if (!classType.isInstance(obj))
                throw new IllegalArgumentException(obj + "不能注册为" + classType);

            //检查是否重复注册
            if (registers.containsKey(classType))
                throw new DependencyAlreadyRegistedException("已经注册过一个" + classType.getSimpleName() + "的依赖了");

            registers.put(classType, obj);
        }
    }

    /**
     * 反注册一个对象
     *
     * @param obj 要反注册的对象
     * @return 是否成功
     */
    public Boolean UnCache(Object obj)
    {
        if (!registers.containsValue(obj))
            return false;

        registers.remove(obj.getClass(), obj);
        return true;
    }

    /**
     * 反注册所有对象
     */
    public void UnCacheAll()
    {
        registers.clear();
    }

    /**
     * 从依赖表获取classType所对应的对象
     *
     * @param classType 目标Class类型
     * @return 找到的对象，返回null则未找到
     * @throws NullDependencyException 依赖未找到时抛出的异常
     */
    public <T> T Get(Class<T> classType)
    {
        return this.Get(classType, true);
    }

    @Nullable
    public <T> T Get(Class<T> classType, boolean throwOnNotFound)
    {
        if (registers.containsKey(classType))
            return (T) registers.get(classType);

        if (throwOnNotFound) throw new NullDependencyException("依赖的对象（" + classType + "）未找到");
        else return null;
    }
}
