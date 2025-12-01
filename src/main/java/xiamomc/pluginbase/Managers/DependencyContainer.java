package xiamomc.pluginbase.Managers;

import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Exceptions.DependencyAlreadyRegisteredException;
import xiamomc.pluginbase.Exceptions.NullDependencyException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DependencyContainer
{
    //region 实例相关
    public static final DependencyContainer GLOBAL = new DependencyContainer();

    //注册表
    private final Map<Class<?>, Object> cacheMap = new ConcurrentHashMap<>();

    /**
     * 注册一个对象到依赖表中
     *
     * @param obj 要注册的对象
     * @throws DependencyAlreadyRegisteredException 该对象所对应的Class是否已被注册
     */
    public void cache(Object obj) throws DependencyAlreadyRegisteredException
    {
        cacheAs(obj.getClass(), obj);
    }

    /**
     * 将一个对象作为某个Class类型注册到依赖表中
     *
     * @param classType 要注册的Class类型
     * @param obj       要注册的对象
     * @throws DependencyAlreadyRegisteredException 是否已经注册过一个相同的classType了
     * @throws IllegalArgumentException           传入的对象不能转化为classType的实例
     */
    public void cacheAs(Class<?> classType, Object obj) throws DependencyAlreadyRegisteredException
    {
        //检查obj是否能cast成classType
        if (!classType.isInstance(obj))
            throw new IllegalArgumentException("The object %s cannot be registered as type %s".formatted(obj, classType));

        //检查是否重复注册
        if (cacheMap.containsKey(classType))
            throw new DependencyAlreadyRegisteredException("Already registered an instance of type %s".formatted(classType));

        cacheMap.put(classType, obj);
    }

    /**
     * 反注册一个对象
     *
     * @param obj 要反注册的对象
     * @return 是否成功
     */
    public boolean unCache(Object obj)
    {
        if (!cacheMap.containsValue(obj))
            return false;

        cacheMap.remove(obj.getClass(), obj);
        return true;
    }

    /**
     * 反注册所有对象
     */
    public void unCacheAll()
    {
        cacheMap.clear();
    }

    /**
     * 从依赖表获取classType所对应的对象
     *
     * @param classType 目标Class类型
     * @return 找到的对象，返回null则未找到
     * @throws NullDependencyException 依赖未找到时抛出的异常
     */
    public <T> T get(Class<T> classType)
    {
        return this.get(classType, true);
    }

    @Nullable
    public <T> T get(Class<T> classType, boolean throwOnNotFound)
    {
        if (cacheMap.containsKey(classType))
        {
            var instance = cacheMap.get(classType);
            if (classType.isInstance(instance))
                return (T) instance;
        }

        if (throwOnNotFound) throw new NullDependencyException("The depending type %s is not registered".formatted(classType));
        else return null;
    }
}
