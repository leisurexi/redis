package com.leisurexi.redis.structure;

import com.leisurexi.redis.pool.RedisPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * @author: leisurexi
 * @date: 2019-12-22 2:36 下午
 * @description: 小对象压缩
 * @since JDK 1.8
 */
@Slf4j
public class Compression {

    /**
     * redis规定小对象存储结构的限制条件如下
     * hash-max-ziplist-entries 512 hash的元素个数超过512就必须用标准结构存储
     * hash-max-ziplist-value 64    hash的任意元素的key/value的长度超过64就必须用标准结构存储
     * list-max-ziplist-entries 512 list的元素个数超过512就必须用标准结构存储
     * list-max-ziplist-value 64    list的任意元素的长度超过64就必须用标准结构存储
     * zset-max-ziplist-entries 128 zset的元素个数超过128就必须用标准结构存储
     * zset-max-ziplist-value 64    zset的任意元素的长度超过64就必须用标准结构存储
     * set-max-intset-entries 512   set的整个元素个数超过512就必须用标准结构存储
     */

    /**
     * hash结构的元素个数超过512的时候，存储结构就会从 ziplist 转换成 hashtable
     */
    @Test
    public void test1() {
        String key = "hello";
        RedisPool.execute(jedis -> {
            jedis.del(key);
            for (int i = 0; i < 512; i++) {
                jedis.hset(key, String.valueOf(i), String.valueOf(i));
            }
            log.info("{}的数据结构: {}", key, jedis.objectEncoding(key));
            jedis.hset(key, "512", "512");
            log.info("{}的数据结构: {}", key, jedis.objectEncoding(key));
        });
    }

    /**
     * hash结构的任意entry的value超过了64，存储结构就会从 ziplist 转换成 hashtable
     */
    @Test
    public void test2() {
        String key = "hello";
        RedisPool.execute(jedis -> {
            jedis.del(key);
            for (int i = 0; i < 64; i++) {
                jedis.hset(key, String.valueOf(i), String.valueOf(i + 1));
            }
            log.info("{}的数据结构: {}", key, jedis.objectEncoding(key));
            jedis.hset(key.getBytes(), "512".getBytes(), new byte[65]);
            log.info("{}的数据结构: {}", key, jedis.objectEncoding(key));
        });
    }

}
