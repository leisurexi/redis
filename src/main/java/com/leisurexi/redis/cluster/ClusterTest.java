package com.leisurexi.redis.cluster;

import com.leisurexi.redis.pool.RedisPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import redis.clients.jedis.JedisCluster;

import java.util.List;
import java.util.UUID;

/**
 * @Author: leisurexi
 * @Description:
 * @Date: 2019/12/27 16:48
 */
@Slf4j
public class ClusterTest {

    @Test
    public void test1() {
        try (JedisCluster cluster = RedisPool.getClusterConnection()) {
            for (int i = 0; i < 10000; i++) {
                String key = "leusurexi-" + i;
                try {
                    cluster.set(key, UUID.randomUUID().toString());
                } catch (Exception e) {
                    //e.printStackTrace();
                    log.error("{}写入失败", key);
                }
            }
        }
    }

    /**
     * 不支持获取多个key不再用一个slot的情况，会直接抛出异常
     */
    @Test
    public void test2() {
        try (JedisCluster cluster = RedisPool.getClusterConnection()) {
            List<String> list = cluster.mget("leisurexi-108", "leisurexi-1087", "leisurexi-110");
            log.info(String.valueOf(list));
        }
    }

}
