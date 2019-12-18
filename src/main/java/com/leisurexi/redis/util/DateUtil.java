package com.leisurexi.redis.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author: leisurexi
 * @date: 2019-12-18 10:43 下午
 * @description:
 * @since JDK 1.8
 */
public class DateUtil {

    private static final DateTimeFormatter YMD_HMS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String convertToString(LocalDateTime localDateTime) {
        return localDateTime == null ? "" : YMD_HMS.format(localDateTime);
    }

    public static LocalDateTime convertToLocalTime(String value) {
        return value == null ? null : LocalDateTime.from(YMD_HMS.parse(value));
    }

}
