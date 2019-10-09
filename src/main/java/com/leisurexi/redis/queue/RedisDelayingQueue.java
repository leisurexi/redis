package com.leisurexi.redis.queue;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.leisurexi.redis.pool.RedisPool;
import lombok.Data;
import lombok.ToString;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * Description: Redis的zset实现的延时队列
 * User: leisurexi
 * Date: 2019-10-08
 * Time: 9:40 下午
 */
public class RedisDelayingQueue<T> {

    @Data
    @ToString
    static class TaskItem<T> {
        private String id;
        private T msg;
    }

    //fastjson序列化对象中存在动态类型时，需要使用TypeReference
    private Type taskType = new TypeReference<TaskItem<T>>() {}.getType();

    private Jedis jedis;
    private String queueKey;

    public RedisDelayingQueue(Jedis jedis, String queueKey) {
        this.jedis = jedis;
        this.queueKey = queueKey;
    }

    public void delay(T msg) {
        TaskItem<T> taskItem = new TaskItem<>();
        //分配唯一的uuid
        taskItem.setId(UUID.randomUUID().toString());
        taskItem.setMsg(msg);
        String jsonString = JSON.toJSONString(taskItem);
        //塞入延时队列，5s后再试
        jedis.zadd(queueKey, System.currentTimeMillis() + 5000, jsonString);
    }

    public void loop() {
        //获取当前线程的中断状态，且这个方法会清除中断状态
        while (!Thread.interrupted()) {
            //按score的升序排序，取第一个
            Set<String> values = jedis.zrangeByScore(queueKey, 0, System.currentTimeMillis(), 0, 1);
            if (values.isEmpty()) {
                try {
                    Thread.sleep(500); //歇会继续
                } catch (InterruptedException e) {
                    break;
                }
                continue;
            }
            String s = values.iterator().next();
            //移除集合中的一个元素，如果返回值大于0，代表移除成功，在这里也就代表抢到了任务来消费
            if (jedis.zrem(queueKey, s) > 0) {
                TaskItem<T> task = JSON.parseObject(s, taskType);
                System.out.println(task);
            }
        }
    }

    public static void main(String[] args) {
        Jedis jedis = RedisPool.getConnection();
        RedisDelayingQueue<Object> queue = new RedisDelayingQueue<>(jedis, "q-demo");
        Thread product = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                queue.delay("codehole " + i);
            }
        });

        Thread consumer = new Thread(() -> queue.loop());

        product.start();
        consumer.start();

        try {
            product.join();
            Thread.sleep(6000);
            consumer.interrupt();
            consumer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
