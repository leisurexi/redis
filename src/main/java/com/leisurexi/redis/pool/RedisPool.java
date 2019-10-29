package com.leisurexi.redis.pool;

import io.rebloom.client.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

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

    public static Jedis getConnection() {
        return pool.getResource();
    }

    public static Client getClientConnection() {
        return new Client(pool);
    }

    public static void execute(CallWithJedis caller) {
        Jedis jedis = pool.getResource();
        try {
            caller.call(jedis);
        } catch (JedisConnectionException e) {
            caller.call(jedis);
        } finally {
            jedis.close();
        }
    }

}

@FunctionalInterface
interface CallWithJedis {

    void call(Jedis jedis);

}
