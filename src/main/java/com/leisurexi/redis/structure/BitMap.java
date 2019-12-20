package com.leisurexi.redis.structure;

import com.leisurexi.redis.pool.RedisPool;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: leisurexi
 * @date: 2019-12-20 8:46 下午
 * @description: redis 位图数据结构
 * @since JDK 1.8
 */
@Slf4j
public class BitMap {

    private static final String KEY = "signin";

    public static void init(int month) {
        checkMoth(month);
        RedisPool.execute(jedis -> {
            int days = 0;
            LocalDate localDate = LocalDate.now();
            for (int i = 1; i <= 12; i++) {
                LocalDate date = localDate.withMonth(i);
                LocalDate lastDay = date.with(TemporalAdjusters.lastDayOfMonth());
                days += lastDay.getDayOfMonth();
            }
            log.info("今年的总天数是: {}", days);
            jedis.setbit(KEY, days, false);
        });
        signIn(month);
    }

    private static void signIn(int month) {
        checkMoth(month);
        RedisPool.execute(jedis -> {
            Month month1 = new Month(month);
            int first = month1.firstDayOfYear();
            int last = month1.lastDayOfYear();
            Pipeline pipelined = jedis.pipelined();
            for (int i = first; i <= last; i++) {
                if (i % 2 == 0) {
                    pipelined.setbit(KEY, i, true);
                } else {
                    pipelined.setbit(KEY, i, false);
                }
            }
            pipelined.sync();
            pipelined.close();
        });
    }

    /**
     * 签到的天数
     *
     * @param month 月份 取值 1-12
     * @return
     */
    public static void signInDays(int month) {
        checkMoth(month);
        RedisPool.execute(jedis -> {
            Month month1 = new Month(month);
            int first = month1.firstDayOfYear();
            int last = month1.lastDayOfYear();
            int signInDays = 0;
            Pipeline pipelined = jedis.pipelined();
            List<Response<Boolean>> responses = new ArrayList<>(last - first + 1);
            for (int i = first; i < last; i++) {
                Response<Boolean> response = pipelined.getbit(KEY, i);
                responses.add(response);
            }
            pipelined.sync();
            pipelined.close();
            for (Response<Boolean> response : responses) {
                if (response.get()) {
                    signInDays++;
                }
            }
            log.info("用户在{}月，一共签到了{}天", month, signInDays);
        });
    }

    private static void checkMoth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must between 1 and 12");
        }
    }

    private static class Month {

        private LocalDate localDate;

        public Month(int month) {
            LocalDate date = LocalDate.now();
            localDate = date.withMonth(month);
        }

        /**
         * 获取月份第一天在当前年中是第几天
         *
         * @return
         */
        public int firstDayOfYear() {
            return localDate.with(TemporalAdjusters.firstDayOfMonth()).getDayOfYear();
        }

        /**
         * 获取月份最后一天在当前年中是第几天
         *
         * @return
         */
        public int lastDayOfYear() {
            return localDate.with(TemporalAdjusters.lastDayOfMonth()).getDayOfYear();
        }

    }

    public static void main(String[] args) {
        init(12);
        signInDays(12);
    }

}
