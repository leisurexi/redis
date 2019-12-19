package com.leisurexi.redis.queue;

import com.alibaba.fastjson.JSON;
import com.leisurexi.redis.pool.RedisPool;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.Set;
import java.util.UUID;

/**
 * @author: leisurexi
 * @date: 2019-12-19 10:37 下午
 * @description: redis的zset实现的延时队列
 * @since JDK 1.8
 */
@Slf4j
public class RedisDelayingQueue<T> {

    @Data
    static class TaskItem<T> {
        private String id;
        private T msg;
    }

    private Jedis jedis;
    private String queueKey;

    public RedisDelayingQueue(String queueKey) {
        this.jedis = RedisPool.getConnection();
        this.queueKey = queueKey;
    }

    /**
     * 把消息放入队列
     *
     * @param msg
     */
    public void delay(T msg) {
        TaskItem<T> task = new TaskItem<>();
        //分配唯一的uuid
        task.setId(UUID.randomUUID().toString());
        task.msg = msg;
        //fastjson 序列化
        String jsonString = JSON.toJSONString(task);
        //塞入延时队列，5s后再试
        jedis.zadd(queueKey, System.currentTimeMillis() + 5000, jsonString);
    }

    /**
     * 从队列中取出消息
     */
    public void loop() {
        while (!Thread.interrupted()) {
            //只取一条
            Set<String> values = jedis.zrangeByScore(queueKey, 0, System.currentTimeMillis(), 0, 1);
            log.info(String.valueOf(values));
            if (values.isEmpty()) {
                try {
                    //歇会继续
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
                continue;
            }
            String value = values.iterator().next();
            //如果删除队列中的这条消息成功，代表抢到了消息
            if (jedis.zrem(queueKey, value) > 0) {
                //fastjson 反序列化
                TaskItem<T> task = JSON.parseObject(value, TaskItem.class);
                handlerMsg(task.getMsg());
            }
        }
    }

    public void handlerMsg(T msg) {
        log.info(String.valueOf(msg));
    }

    public static void main(String[] args) {
        RedisDelayingQueue<String> queue = new RedisDelayingQueue<>("queue-demo");
        Thread product = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                queue.delay("leisurexi-" + i);
            }
        }, "product");
        Thread consumer = new Thread(() -> queue.loop(), "consumer");
        product.start();
        consumer.start();
        try {
            product.join();
            Thread.sleep(6000);
            consumer.interrupt();
            consumer.join();
        } catch (InterruptedException e) {
        }
    }

}
