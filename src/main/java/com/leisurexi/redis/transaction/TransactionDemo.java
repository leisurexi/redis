package com.leisurexi.redis.transaction;

import com.leisurexi.redis.pool.RedisPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Created with IntelliJ IDEA.
 * Description: Redis中事务的示例 watch命令 Redis中乐观锁
 * User: leisurexi
 * Date: 2019-10-12
 * Time: 10:09 下午
 */
public class TransactionDemo {

    public static void main(String[] args) throws InterruptedException {
        Jedis jedis = RedisPool.getConnection();
        String userId = "abc";
        String key = keyFor(userId);
        jedis.set(key, String.valueOf(5));
        CyclicBarrier barrier = new CyclicBarrier(2);
        Thread thread1 = new Thread(() -> {
            try {
                barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
            System.out.println(doubleAccount(jedis, userId));
        });

        Thread thread2 = new Thread(() -> {
            try {
                barrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
            System.out.println(doubleAccount(jedis, userId));
        });
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        jedis.close();
    }

    public synchronized static int doubleAccount(Jedis jedis, String userId) {
        String key = keyFor(userId);
        while (true) {
            jedis.watch(key);
            int value = Integer.parseInt(jedis.get(key));
            value *= 2; //加倍
            Transaction tx = jedis.multi();
            tx.set(key, String.valueOf(value));
            List<Object> result = tx.exec();
            if (result != null) {
                break; //执行成功
            }
            System.out.println("出现并发情况，执行失败");
        }
        return Integer.parseInt(jedis.get(key)); //重新获取余额
    }

    public static String keyFor(String userId) {
        return String.format("account_%s", userId);
    }

}
