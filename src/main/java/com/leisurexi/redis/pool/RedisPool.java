package com.leisurexi.redis.pool;

import io.rebloom.client.Client;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: leisurexi
 * Date: 2019-10-08
 * Time: 9:02 下午
 */
@Slf4j
public class RedisPool {

    private static JedisPool pool;
    private static JedisCluster JEDIS_CLUSTER;

    static {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(5);
        config.setMaxIdle(20);

//        pool = new JedisPool(config, "192.168.1.98", 6379, 5000, "a1s2d3f4");
        JEDIS_CLUSTER = new JedisCluster(new HostAndPort("127.0.0.1", 6380), config);
        Map<String, JedisPool> nodes = JEDIS_CLUSTER.getClusterNodes();
        for (Map.Entry<String, JedisPool> entry : nodes.entrySet()) {
            log.info("节点ip地址和端口: {}", entry.getKey());
            Jedis jedis = entry.getValue().getResource();
//            log.info("节点信息: {}", jedis.info());
            log.info("集群信息: {}", jedis.clusterInfo());
            jedis.close();
        }
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

    public static JedisCluster getClusterConnection() {
        return JEDIS_CLUSTER;
    }

}
