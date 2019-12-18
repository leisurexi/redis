package com.leisurexi.redis.pool;

import redis.clients.jedis.Jedis;

/**
 * @author: leisurexi
 * @date: 2019-12-18 10:36 下午
 * @description:
 * @since JDK 1.8
 */
@FunctionalInterface
public interface CallWithJedis {

    void call(Jedis jedis);

}
