package com.leisurexi.redis.structure;

import com.leisurexi.redis.pool.RedisPool;
import com.leisurexi.redis.util.DateUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: leisurexi
 * @date: 2019-12-18 10:33 下午
 * @description: redis hash结构存储用户信息
 * @since JDK 1.8
 */
@Slf4j
public class Hash {

    public static void setUser(User user) {
        RedisPool.execute(jedis -> {
            Class<? extends User> clazz = user.getClass();
            Field[] fields = clazz.getDeclaredFields();
            Map<String, String> map = new HashMap<>(fields.length);
            for (Field field : fields) {
                String fieldName = field.getName();
                map.put(fieldName, convertFieldValueToString(user, fieldName));
            }
            jedis.hset("user", map);
        });
    }

    public static User getUser() {
        try {
            Class<User> clazz = User.class;
            Constructor<User> constructor = clazz.getConstructor();
            User user = constructor.newInstance();
            RedisPool.execute(jedis -> {
                try {
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field : fields) {
                        String fieldName = field.getName();
                        Method method = user.getClass().getMethod("set" + getMethodName(fieldName), field.getType());
                        method.invoke(user, convertStringToFieldValue(jedis.hget("user", fieldName), field.getType()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            return user;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 把字段值转换为String，因为redis的hash数据结构value只能为String
     *
     * @param object
     * @param fieldName
     * @return
     */
    private static String convertFieldValueToString(Object object, String fieldName) {
        try {
            String methodName = "get" + getMethodName(fieldName);
            Method method = object.getClass().getMethod(methodName);
            if (method == null) {
                throw new NoSuchMethodException(methodName);
            }
            Object value = method.invoke(object);
            if (value != null) {
                if (value instanceof LocalDateTime) {
                    return DateUtil.convertToString((LocalDateTime) value);
                } else if (value instanceof Integer || value instanceof Long ||
                        value instanceof Float || value instanceof Double) {
                    return String.valueOf(value);
                }
                return (String) value;
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static <T> T convertStringToFieldValue(String value, Class<T> clazz) {
        if (clazz.equals(LocalDateTime.class)) {
            return (T) DateUtil.convertToLocalTime(value);
        } else if (clazz.equals(Integer.class)) {
            return (T) Integer.valueOf(value);
        } else if (clazz.equals(Long.class)) {
            return (T) Long.valueOf(value);
        } else if (clazz.equals(Float.class)) {
            return (T) Float.valueOf(value);
        } else if (clazz.equals(Double.class)) {
            return (T) Double.valueOf(value);
        }
        return (T) value;
    }

    /**
     * 把一个字符串的第一个字母大写、效率是最高的
     *
     * @param fieldName 字段名
     * @return
     */
    private static String getMethodName(String fieldName) {
        byte[] items = fieldName.getBytes();
        items[0] = (byte) ((char) items[0] - 'a' + 'A');
        return new String(items);

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class User {
        private Long id;
        private String name;
        private Integer age;
        private LocalDateTime birthday;
    }

    public static void main(String[] args) {
        User user = User.builder()
                .id(231231L)
                .name("leisurexi")
                .age(21)
                .birthday(LocalDateTime.now())
                .build();
        setUser(user);
        log.info(String.valueOf(getUser()));
    }

}
