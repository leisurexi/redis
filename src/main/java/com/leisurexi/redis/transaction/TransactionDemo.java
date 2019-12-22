package com.leisurexi.redis.transaction;

import com.leisurexi.redis.pool.RedisPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.util.List;

/**
 * @author: leisurexi
 * @date: 2019-12-22 1:04 下午
 * @description: redis中的事务示例
 * @since JDK 1.8
 */
@Slf4j
public class TransactionDemo {

    /**
     * redis事务的原子性
     * 下面的例子是事务执行到中间时遇到失败了，因为我们不能对一个字符串进行数学运算。
     * 事务在遇到指令执行失败后，后面的指令还会继续执行，所以poorman的值能继续得到设置。
     * 这说明redis的事务不具备"原子性"，而仅仅满足了事务的"隔离性"中串行化，即当前执行
     * 的事务有着不被其他事务打断的权利。
     */
    @Test
    public void test1() {
        RedisPool.execute(jedis -> {
            //使用事务时最好和Pipeline一起使用，这样可以将多次IO操作压缩为单次IO操作
            Pipeline pipeline = jedis.pipelined();
            //事务开启
            pipeline.multi();
            pipeline.set("books", "iamastring");
            pipeline.incr("books");
            pipeline.set("poorman", "iamdesperate");
            /*
            执行事务，所有的命令在执行exec命令之前不执行，而是缓存在一个服务器的
            一个时间队列中，服务器一旦受到exec指令，才开始执行整个事务队列，执行
            完毕后一次性返回所有指令的运行结果。因为redis的单线程特性，它不用担心
            自己在执行队列的时候被其他执行打搅，可以保证它们得到的"原子性"执行
            */
            Response<List<Object>> listResponse = pipeline.exec();
            //discard 用于丢弃事务缓存队列中的所有指令，在exec执行之前
//            pipeline.discard();
            //关闭管道
            pipeline.close();
            for (Object response : listResponse.get()) {
                log.info(String.valueOf(response));
            }
        });
    }

    @Test
    public void test2() {
        RedisPool.execute(jedis -> {
            String userId = "abc";
            String key = keyFor(userId);
            jedis.setnx(key, String.valueOf(5));
            log.info(String.valueOf(doubleAccount(jedis, userId)));
        });
    }

    private static int doubleAccount(Jedis jedis, String userId) {
        String key = keyFor(userId);
        while (true) {
            /*
                redis提供了watch机制，它是一种乐观锁。
                watch会在事务开始之前盯住一个或多个关键变量，当事务执行时，也就是
                服务器收到了exec指令要顺序执行缓存的事务队列时，redis会检查关键
                变量自watch之后是否被修改了（包括当前事务所在的客户端）。如果变量
                被人动过了，exec指令就会返回null回复告知客户端事务执行失败，这个
                时候客户端一般会选择重试。
             */
            jedis.watch(key);
            int value = Integer.parseInt(jedis.get(key));
            value *= 2;
            Transaction tx = jedis.multi();
            tx.set(key, String.valueOf(value));
            List<Object> results = tx.exec();
            if (results != null) {
                //代表修改成功
                break;
            }
        }
        //重新获取余额
        return Integer.parseInt(jedis.get(key));
    }

    private static String keyFor(String userId) {
        return String.format("account_%s", userId);
    }

}
