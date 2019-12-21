package com.leisurexi.redis.structure;

import com.leisurexi.redis.pool.RedisPool;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created with IntelliJ IDEA.
 * Description: HyperLogLog误差率测试 标准误差率是0.81%
 * User: leisurexi
 * Date: 2019-10-09
 * Time: 10:25 下午
 */
@Slf4j
public class PfTest {

    private static final String KEY = "codehole";

//    public static void main(String[] args) {
//        Jedis jedis = RedisPool.getConnection();
//        for (int i = 0; i < 100000; i++) {
//            jedis.pfadd(KEY, "user" + i);
//            long total = jedis.pfcount(KEY);
//            if (total != i + 1) {
//                System.out.printf("%d %d\n", total, i + 1);
//                break;
//            }
//        }
//    }

    static class BitKeeper {
        private int maxbits;

        public void random() {
            long value = ThreadLocalRandom.current().nextLong(2L << 32);
            int bits = lowZeros(value);
            if (bits > maxbits) {
                maxbits = bits;
            }
        }

        private int lowZeros(long value) {
            int i = 1;
            for (; i < 32; i++) {
                if (value >> i << i != value) {
                    break;
                }
            }
            return i - 1;
        }
    }

    static class Experiment {
        private int n;
        private BitKeeper keeper;

        public Experiment(int n) {
            this.n = n;
            this.keeper = new BitKeeper();
        }

        public void work() {
            for (int i = 0; i < n; i++) {
                keeper.random();
            }
        }

        public void debug() {
            System.out.printf("%d %.2f %d\n", n, Math.log(n) / Math.log(2), keeper.maxbits);
        }

    }

    public static void main(String[] args) {
        for (int i = 1000; i < 100000; i += 100) {
            Experiment exp = new Experiment(i);
            exp.work();
            exp.debug();
        }
    }

}
