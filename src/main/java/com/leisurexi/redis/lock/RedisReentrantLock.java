package com.leisurexi.redis.lock;

import com.leisurexi.redis.pool.RedisPool;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: leisurexi
 * @date: 2019-12-19 9:16 下午
 * @description: redis实现的可重入分布式锁
 * @since JDK 1.8
 */
@Slf4j
public class RedisReentrantLock {

    private ThreadLocal<Map<String, Integer>> lockers = new ThreadLocal<>();

    private Jedis jedis;

    public RedisReentrantLock() {
        jedis = RedisPool.getConnection();
    }

    private boolean _lock(String key) {
        SetParams setParams = new SetParams();
        setParams.nx(); //设置不存在才创建该Key
        setParams.ex(60);//设置过期时间为5秒
        return jedis.set(key, "", setParams) != null;
    }

    private void _unlock(String key) {
        jedis.del(key);
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
            log.info("{} 线程获取锁: {}", Thread.currentThread().getName(), lock1.lock("leisurexi"));
        });
        thread.start();
        log.info("{} 线程释放锁: {}", Thread.currentThread().getName(), lock.unlock("leisurexi"));
        log.info("{} 线程释放锁: {}", Thread.currentThread().getName(), lock.unlock( "leisurexi"));

        thread.join();

    }

}
