package com.leisurexi.redis.pool;

import io.rebloom.client.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: leisurexi
 * Date: 2019-10-08
 * Time: 9:02 下午
 */
public class RedisPool {

    private static final JedisPool pool;

    static {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(5);
        config.setMaxIdle(20);
        pool = new JedisPool(config, "192.168.1.91");
    }

    /**
     * 获取一个连接
     */
    public static Jedis getConnection() {
        return pool.getResource();
    }

    public static Client getClientConnection() {
        return new Client(pool);
    }

}
