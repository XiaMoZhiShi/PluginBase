package xiamomc.pluginbase.Utilities;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiamomc.pluginbase.Annotations.NotSerializable;
import xiamomc.pluginbase.Annotations.Serializable;
import xiamomc.pluginbase.Bindables.Bindable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

public class ConfigSerializeUtils
{
    private static final Logger Logger = LoggerFactory.getLogger("XiaMoBase");

    public static Map<String, Object> serialize(Object o)
    {
        checkDuplicateNames(o);

        //找到所有要序列化的值
        var fields = Arrays.stream(o.getClass().getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(NotSerializable.class)).toList();

        //准备HashMap
        var map = new Object2ObjectOpenHashMap<String, Object>();

        //尝试序列化
        for (var f : fields)
        {
            //不要序列化非Public和Final字段
            var mod = f.getModifiers();
            if (!Modifier.isPublic(mod) || Modifier.isFinal(mod))
                throw new IllegalStateException("不能序列化非public或final字段：" + f.getName());

            //放进map
            try
            {
                String serializeName;
                if (f.isAnnotationPresent(Serializable.class)) serializeName = f.getAnnotation(Serializable.class).value();
                else serializeName = f.getName();

                map.put(serializeName, f.get(o));
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        }

        return map;
    }

    private static void checkDuplicateNames(Object o)
    {
        var fields = new ObjectArrayList<>(Arrays.stream(o.getClass().getFields())
                .filter(f -> !f.isAnnotationPresent(NotSerializable.class)).toList());

        var fieldsWithAnnotation = fields.stream()
                .filter(f -> f.isAnnotationPresent(Serializable.class)).toList();

        fields.removeAll(fieldsWithAnnotation);

        for (var fwa : fieldsWithAnnotation)
        {
            var name = fwa.getAnnotation(Serializable.class).value();
            if (fields.stream().anyMatch(f -> f.getName().equals(name)))
                throw new DuplicateSerializeNameException("在" + o + "中找到了多个叫" + name + "的可反序列化对象");
        }
    }

    public static <T> T deSerialize(T o, Map<String, Object> map)
    {
        checkDuplicateNames(o);

        //找到所有要反序列化的字段
        var fieldsToDeSerialize = new ObjectArrayList<>(Arrays.stream(o.getClass().getDeclaredFields())
                        .filter(f -> !f.isAnnotationPresent(NotSerializable.class)).toList());

        //把带有Serializable的字段单独剔出来
        var fieldsWithAnnotation = new ObjectArrayList<>(fieldsToDeSerialize.stream()
                .filter(f -> f.isAnnotationPresent(Serializable.class)).toList());

        //并从原来的列表里移出
        fieldsToDeSerialize.removeAll(fieldsWithAnnotation);

        //对map里的Key逐个查询
        for (var key : map.keySet())
        {
            //跳过"=="
            if (key.equals("==")) continue;

            //找到对应的字段
            Field field = fieldsToDeSerialize.stream()
                    .filter(f -> f.getName().equals(key))
                    .findFirst().orElse(null);

            var fieldWithAnnotation = fieldsWithAnnotation.stream()
                    .filter(f -> f.getAnnotation(Serializable.class).value().equals(key))
                    .findFirst().orElse(null);

            if (field == null && fieldWithAnnotation != null)
            {
                field = fieldWithAnnotation;
                fieldsWithAnnotation.remove(field);
            }

            //设置值
            if (field != null)
            {
                try
                {
                    field.set(o, map.get(key));
                    fieldsToDeSerialize.remove(field);
                }
                catch (IllegalAccessException e)
                {
                    Logger.warn("反序列化目标错误：" + o);
                    throw new RuntimeException(e);
                }
            }
            else
                Logger.warn("反序列化目标未找到：" + key + " in " + o);
        }

        return o;
    }

    private static class DuplicateSerializeNameException extends RuntimeException
    {
        public DuplicateSerializeNameException(String s)
        {
            super(s);
        }
    }

    /**
     *
     * @param bindable
     * @param val
     * @return Whether the operation was successful
     */
    public static boolean tryCastNumberBindable(Bindable<?> bindable, Number val)
    {
        try
        {
            //if (!(val instanceof Number numVal)) return false;

            var bindableVal = bindable.get();
            if (bindableVal == null)
            {
                Logger.error("Bindable %s has a null value and cannot be used for number converting");
                return false;
            }

            var typeParamClazz = bindableVal.getClass();

            var numConv = convertNumber(typeParamClazz, val, true);
            if (numConv == null)
            {
                Logger.error("Cannot convert input %s(%s) to a compat value for target bindable %s(%s)"
                        .formatted(val, val.getClass(), bindable, bindable));

                return false;
            }

            bindable.setInternal(numConv);
            return true;
        }
        catch (Throwable t)
        {
            Logger.warn("Unable to cast value for Bindable: %s".formatted(t.getMessage()));
            t.printStackTrace();
        }

        return false;
    }

    /**
     * Convert the input value to the target type
     * @param type The target type
     * @param input A valid Number instance
     * @param nullIfFailed Should we return null if not convertable?
     * @return A number that converts from the giving inputs
     */
    public static Number convertNumber(Class<?> type, Number input, boolean nullIfFailed)
    {
        //System.out.println("Input type: %s, From value: %s(%s)".formatted(type, input, input.getClass()));
        if (type == Integer.class || type == int.class)
            return input.intValue();
        else if (type == Float.class || type == float.class)
            return input.floatValue();
        else if (type == Double.class || type == double.class)
            return input.doubleValue();
        else if (type == Long.class || type == long.class)
            return input.longValue();
        else if (type == Short.class || type == short.class)
            return input.shortValue();

        if (nullIfFailed) return null;

        throw new RuntimeException("Unable to convert input ('%s') to the target type: %s".formatted(input, type));
    }
}
