package com.leisurexi.redis.queue;

import com.leisurexi.redis.pool.RedisPool;
import redis.clients.jedis.Jedis;

/**
 * Created with IntelliJ IDEA.
 * Description: Redis PubSub 生产者
 * User: leisurexi
 * Date: 2019-10-12
 * Time: 10:32 下午
 */
public class Product {

    public static void main(String[] args) {
        Jedis jedis = RedisPool.getConnection();
        jedis.publish("leisurexi", "python comes");
        jedis.publish("leisurexi", "java comes");
        jedis.publish("leisurexi", "golang comes");
    }

}
