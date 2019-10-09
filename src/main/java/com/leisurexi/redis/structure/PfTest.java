package com.leisurexi.redis.structure;

import com.leisurexi.redis.pool.RedisPool;
import redis.clients.jedis.Jedis;

/**
 * Created with IntelliJ IDEA.
 * Description: HyperLogLog误差率测试
 * User: leisurexi
 * Date: 2019-10-09
 * Time: 10:25 下午
 */
public class PfTest {

    private static final String KEY = "codehole";

    public static void main(String[] args) {
        Jedis jedis = RedisPool.getConnection();
        for (int i = 0; i < 100000; i++) {
            jedis.pfadd(KEY, "user" + i);
//            long total = jedis.pfcount(KEY);
//            if (total != i + 1) {
//                System.out.printf("%d %d\n", total, i + 1);
//                break;
//            }
        }
        long total = jedis.pfcount(KEY);
        System.out.printf("%d %d\n", 100000, total);
    }

}
