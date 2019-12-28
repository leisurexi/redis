package com.leisurexi.redis.lock;

import com.leisurexi.redis.pool.RedisPool;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author: leisurexi
 * @date: 2019-12-19 9:16 下午
 * @description: redis实现的可重入分布式锁
 * @since JDK 1.8
 */
@Slf4j
public class RedisReentrantLock {

    private ThreadLocal<Map<String, Integer>> lockers = new ThreadLocal<>();

//    private Jedis jedis;

    private JedisCluster jedis;

    private String value;

    public RedisReentrantLock() {
//        jedis = RedisPool.getConnection();
        jedis = RedisPool.getClusterConnection();
    }

    private boolean _lock(String key) {
        SetParams setParams = new SetParams();
        setParams.nx(); //设置不存在才创建该Key
        setParams.ex(10);//设置过期时间为10秒
        value = UUID.randomUUID().toString();
        return jedis.set(key, value, setParams) != null;
    }

    private void _unlock(String key) {
        if (value == null) {
            throw new IllegalStateException("you must get lock before unlock");
        }
        String luaScript = "if redis.call(\"get\", KEYS[1]) == ARGV[1] then\n" +
                "return redis.call(\"del\", KEYS[1])\n" +
                "else\n" +
                "return 0 \n" +
                "end";
        long result = (long) jedis.eval(luaScript, Arrays.asList(key), Arrays.asList(value));
        if (result > 0) {
            log.info("成功释放锁");
        } else {
            log.info("锁释放失败");
        }
    }

    private Map<String, Integer> currentLockers() {
        Map<String, Integer> refs = lockers.get();
        if (refs != null) {
            return refs;
        }
        lockers.set(new HashMap<>());
        return lockers.get();
    }

    /**
     * 获取锁
     *
     * @param key
     * @return
     */
    public boolean lock(String key) {
        Map<String, Integer> refs = currentLockers();
        Integer count = refs.get(key);
        if (count != null) {
            refs.put(key, count++);
            return true;
        }
        boolean result = this._lock(key);
        if (!result) {
            return false;
        }
        refs.put(key, 1);
        return true;
    }

    /**
     * 释放锁
     *
     * @param key
     * @return
     */
    public boolean unlock(String key) {
        Map<String, Integer> refs = currentLockers();
        Integer count = refs.get(key);
        if (count == null) {
            return false;
        }
        count--;
        if (count > 0) {
            refs.put(key, count);
        } else {
            refs.remove(key);
            this._unlock(key);
        }
        return true;
    }

    public static void main(String[] args) throws InterruptedException {
        RedisReentrantLock lock = new RedisReentrantLock();
        log.info("{} 线程获取锁: {}", Thread.currentThread().getName(), lock.lock("leisurexi"));
        Thread thread = new Thread(() -> {
            RedisReentrantLock lock1 = new RedisReentrantLock();
            log.info("{} 线程释放锁: {}", Thread.currentThread().getName(), lock1.unlock("leisurexi"));
            boolean result = false;
            while (!result) {
                result = lock1.lock("leisurexi");
                log.info("{} 线程获取锁: {}", Thread.currentThread().getName(), result);
                if (!result) {
                    try {
                        //等会重试
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();

//        log.info("{} 线程释放锁: {}", Thread.currentThread().getName(), lock.unlock("leisurexi"));

        thread.join();
    }

}
