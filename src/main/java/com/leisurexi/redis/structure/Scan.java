package com.leisurexi.redis.structure;

import com.leisurexi.redis.pool.RedisPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.List;

/**
 * @author: leisurexi
 * @date: 2019-12-21 9:59 下午
 * @description: redis scan命令基本用法示例
 * @since JDK 1.8
 */
@Slf4j
public class Scan {

    /**
     * scan命令的特点:
     * 1.复杂度虽然也是O(n)，但它是通过游标分步进行的，不会阻塞线程。
     * 2.提供limit参数，可以控制每次返回结果的最大条数，limit只是一个hint，返回的结果可多可少。
     * 3.同keys一样，它也提供模式匹配功能。
     * 4.服务器不需要为游标保存状态，游标的唯一状态就是scan返回给客户端的游标整数。
     * 5.返回的结果可能会有重复，需要客户端去重，这点非常重要。
     * 6.遍历的过程中如果有数据修改，改动后的数据能不能遍历到是不确定的。
     * 7.单词返回的结果是空的并不意味着遍历结束，而要看返回的游标值是否为零。
     */

    @Test
    public void init() {
        //先加10万条测试数据
        RedisPool.execute(jedis -> {
            for (int i = 0; i < 100000; i++) {
                jedis.set("key-" + i, String.valueOf(i));
            }
        });
    }

    @Test
    public void test() {
        RedisPool.execute(jedis -> {
            boolean finish = false;
            ScanParams params = new ScanParams();
            params.match("key-99*")
                    .count(1000);
            String cursor = "0";
            while (!finish) {
                ScanResult<String> result = jedis.scan(cursor, params);
                if ("0".equals(result.getCursor())) {
                    finish = true;
                }
                List<String> list = result.getResult();
                list.forEach(s -> log.info(s));
                cursor = result.getCursor();
            }
        });
    }

}
