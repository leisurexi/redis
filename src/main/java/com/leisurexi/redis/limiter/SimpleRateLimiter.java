package com.leisurexi.redis.limiter;

import com.leisurexi.redis.pool.Holder;
import com.leisurexi.redis.pool.RedisPool;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

/**
 * @author: leisurexi
 * @date: 2019-12-21 2:11 下午
 * @description: redis实现的简单限流
 * @since JDK 1.8
 */
@Slf4j
public class SimpleRateLimiter {

    /**
     * @param userId    用户编号
     * @param actionKey key
     * @param period    限制时间
     * @param maxCount  访问的最大次数
     * @return
     */
    public static boolean isActionAllowed(String userId, String actionKey, int period, int maxCount) {
        Holder<Boolean> holder = new Holder<>();
        RedisPool.execute(jedis -> {
            String key = String.format("hist:%s:%s", userId, actionKey);
            long nowTs = System.currentTimeMillis();
            Pipeline pipeline = jedis.pipelined();
            //redis中事务的开始
            pipeline.multi();
            //记录行为
            //value和score都使用毫秒时间戳
            pipeline.zadd(key, nowTs, "" + nowTs);
            //移除时间窗口之前的行为记录，剩下的都是时间窗口内的
            pipeline.zremrangeByScore(key, 0, nowTs - period * 1000);
            //获取窗口内的行为数量
            Response<Long> count = pipeline.zcard(key);
            //设置zset过期时间，避免冷用户持续占用内存
            //过期时间应该等于时间窗口的长度，再多宽限1s
            pipeline.expire(key, period + 1);
            //批量执行操作
            //redis中事务的执行
            pipeline.exec();
            //redis中事务的丢弃
//        pipelined.discard();
            pipeline.close();
            //比较数量是否超标
            holder.setValue(count.get() <= maxCount);
        });
        return holder.getValue();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 20; i++) {
            //限定用户60秒内只可以请求5次
            log.info(String.valueOf(isActionAllowed("leisurexi", "replay", 60, 5)));
        }
    }

}
