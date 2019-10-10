package com.leisurexi.redis.limiter;

import com.leisurexi.redis.pool.RedisPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

/**
 * Created with IntelliJ IDEA.
 * Description: Redis实现的简单限流策略
 * User: leisurexi
 * Date: 2019-10-10
 * Time: 9:32 下午
 */
public class SimpleRateLimiter {

    private Jedis jedis;

    public SimpleRateLimiter(Jedis jedis) {
        this.jedis = jedis;
    }

    public boolean isActionAllowed(String userId, String actionKey, int period, int maxCount) {
        String key = String.format("hist:%s:%s", userId, actionKey);
        long nowTs = System.currentTimeMillis();
        Pipeline pipelined = jedis.pipelined();
        pipelined.multi();
        //记录行为 value和score都是用毫秒时间戳
        pipelined.zadd(key, nowTs, "" + nowTs);
        //移除时间窗口之前的行为记录，剩下的都是时间窗口内的
        pipelined.zremrangeByScore(key, 0, nowTs - period * 1000);
        //获取窗口内的行为数量
        Response<Long> count = pipelined.zcard(key);
        //设置zset过期时间，避免冷用户持续占用内存
        //过期时间应该等于时间窗口的长度，再多宽限1s
        pipelined.expire(key, period + 1);
        //批量执行操作
        pipelined.exec();
        pipelined.close();
        //比较数量是否超标
        return count.get() <= maxCount;
    }

    public static void main(String[] args) {
        Jedis jedis = RedisPool.getConnection();
        SimpleRateLimiter limiter = new SimpleRateLimiter(jedis);
        for (int i = 0; i < 20; i++) {
            //限定用户60秒内只可以请求5次
            System.out.println(limiter.isActionAllowed("leisurexi", "reply", 60, 5));
        }
    }

}
