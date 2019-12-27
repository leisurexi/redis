package com.leisurexi.redis.cluster;

import com.leisurexi.redis.pool.RedisPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import redis.clients.jedis.JedisCluster;

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
        JedisCluster cluster = RedisPool.getClusterConnection();
        for (int i = 0; i < 10000; i++) {
            try {
                cluster.set("leusurexi-" + i, UUID.randomUUID().toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
